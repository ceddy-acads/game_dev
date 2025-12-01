package entities;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;
import javax.imageio.ImageIO;
import java.io.IOException;


public class Enemy {
    
    //FOR DAMAGE
    private int attackDamage = 10;
    
    // Use doubles for precise position tracking
    private double x, y;
    public int width, height;
    public int hp;
    public double speed;
    private BufferedImage sprite;
    private boolean alive = true;
    private int flashRed = 0;
    private BufferedImage[] idleFrames;
    private BufferedImage[] walkFrames;
    private int currentFrame = 0;
    private int frameDelay = 10;
    private int frameTimer = 0;
    private int idleCurrentFrame = 0;
    private int idleFrameTimer = 0;
    
    private boolean facingLeft = false;
    
    // Retreat logic
    private boolean retreating = false;
    private double retreatTargetX, retreatTargetY;
    private final double RETREAT_DISTANCE = 300; // How far enemies will try to spread
    private final double RETREAT_SPEED_MULTIPLIER = 0.5; // Retreat slower
    


    //FOR ATTACKING
    private BufferedImage[] attackFrames;
    private int attackFrame = 0;
    private int attackTimer = 0;
    private int attackDelay = 4; // lower = faster animation
    private boolean attacking = false;
    private int attackCooldown = 0; // prevents constant attacking
    private int attackFrameDelay = 4; // lower = faster attack animation
    private int attackFrameTimer = 0;

    // Freeze effect
    private int freezeTimer = 0; // in frames, 0 = not frozen

    // Collision detection
    private Object tileManager; // Reference to TileManager for collision
    private final int collisionWidth = 64;  // Larger collision box to properly detect trees
    private final int collisionHeight = 64;

    private void loadSprites() {
        try {
            // FOR IDLE - Using Idle.png spritesheet (640x128, 5 frames in a single row)
            BufferedImage idleSpriteSheet = ImageIO.read(getClass().getResourceAsStream("/assets/characters/enemies/Idle.png"));
            int idleFrameWidth = 128; // 640 / 5
            int idleFrameHeight = 128;
            idleFrames = new BufferedImage[5];
            for (int i = 0; i < 5; i++) {
                idleFrames[i] = idleSpriteSheet.getSubimage(i * idleFrameWidth, 0, idleFrameWidth, idleFrameHeight);
            }


            //FOR WALKING - Using Walk.png spritesheet (640x128, 5 frames in a single row)
            BufferedImage walkSpriteSheet = ImageIO.read(getClass().getResourceAsStream("/assets/characters/enemies/Walk.png"));
            int frameWidth = 128; // 640 / 5
            int frameHeight = 128;
            walkFrames = new BufferedImage[5];
            for (int i = 0; i < 5; i++) {
                walkFrames[i] = walkSpriteSheet.getSubimage(i * frameWidth, 0, frameWidth, frameHeight);
            }
            
            // FOR ATTACKING - Using Attack_1.png spritesheet (512x128, 4 frames in a single row)
            BufferedImage attackSpriteSheet = ImageIO.read(getClass().getResourceAsStream("/assets/characters/enemies/Attack_1.png"));
            int attackFrameWidth = 128; // 512 / 4
            int attackFrameHeight = 128;
            attackFrames = new BufferedImage[4];
            for (int i = 0; i < 4; i++) {
                attackFrames[i] = attackSpriteSheet.getSubimage(i * attackFrameWidth, 0, attackFrameWidth, attackFrameHeight);
            }
            sprite = idleFrames[0]; // default image
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public Enemy(int x, int y) {
        this.x = (double) x;
        this.y = (double) y;
        this.width = 128; // Set to frame width
        this.height = 128; // Set to frame height
        this.hp = 400;
        this.speed = 0.8; // Further reduced speed

        loadSprites();
    }


    public void update(int playerX, int playerY, Player player, int cameraX, int cameraY, int viewportWidth, int viewportHeight) {
        if (!alive) return;

        // Handle freeze effect
        if (freezeTimer > 0) {
            freezeTimer--;
            // Skip all movement and attack logic while frozen
            return;
        }



        if (!player.isAlive()) {
            // Player is dead, retreat by moving away from player's position
            retreating = true;
            
            // Calculate direction away from player
            double dx = x - playerX; // Reversed: away from player
            double dy = y - playerY; // Reversed: away from player
            double dist = Math.sqrt(dx * dx + dy * dy);

            if (dist > 0.1) { // Avoid division by zero
                // Move away from player - normalize and multiply by speed
                double moveX = (dx / dist) * speed * RETREAT_SPEED_MULTIPLIER;
                double moveY = (dy / dist) * speed * RETREAT_SPEED_MULTIPLIER;

                // Apply collision detection
                applyCollisionMovement(moveX, moveY);

                facingLeft = dx < 0; // Face the direction of retreat

                // Animate walking during retreat
                frameTimer++;
                if (frameTimer >= frameDelay) {
                    currentFrame = (currentFrame + 1) % walkFrames.length;
                    frameTimer = 0;
                }
                sprite = walkFrames[currentFrame];
            } else {
                // Enemy is on the exact same position, move in a random direction
                Random rand = new Random();
                double angle = rand.nextDouble() * 2 * Math.PI;
                double moveX = speed * RETREAT_SPEED_MULTIPLIER * Math.cos(angle);
                double moveY = speed * RETREAT_SPEED_MULTIPLIER * Math.sin(angle);

                // Apply collision detection
                applyCollisionMovement(moveX, moveY);
            }
            return; // Stop further updates if retreating
        } else {
            retreating = false; // Reset retreating flag if player is alive again
        }
        
        // Normal enemy behavior (move toward player and attack)
        double dx = playerX - x;
        double dy = playerY - y;
        double dist = Math.sqrt(dx * dx + dy * dy);

        facingLeft = dx < 0;  // face left if player is to the left

        // Check if enemy is in camera viewport
        boolean inViewport = (x >= cameraX && x <= cameraX + viewportWidth &&
                             y >= cameraY && y <= cameraY + viewportHeight);

        // Detection radius (larger than attack radius) - enemy can "detect" player from further away
        final double DETECTION_RADIUS = 400.0; // pixels - increased for better responsiveness
        boolean canDetectPlayer = dist <= DETECTION_RADIUS;

        if ((canDetectPlayer || inViewport) && dist > 1 && !attacking) { // Move toward player if detected or in viewport
            double moveX = (dx / dist) * speed;
            double moveY = (dy / dist) * speed;

            // Apply collision detection
            applyCollisionMovement(moveX, moveY);

            // Animate walking
            frameTimer++;
            if (frameTimer >= frameDelay) {
                currentFrame = (currentFrame + 1) % walkFrames.length;
                frameTimer = 0;
            }
            sprite = walkFrames[currentFrame];
        } else if (dist <= 1) { // Only attack when physically close to player
            // Close enough to attack or already attacking
            if (!attacking && attackCooldown <= 0) {
                attacking = true;
                attackFrame = 0;
                attackCooldown = 90;
                System.out.println("Enemy Attacking!");
                // Randomize enemy attack damage with a minimum of 5
                Random rand = new Random();
                int minEnemyDamage = 5;
                int maxEnemyDamage = attackDamage + 5; // e.g., if base attackDamage is 10, max will be 15
                int randomizedDamage = minEnemyDamage + rand.nextInt(maxEnemyDamage - minEnemyDamage + 1);
                player.takeDamage(randomizedDamage); // Deal randomized damage at the start of the attack animation
            }

            if (attacking) {
                attackFrameTimer++;
                if (attackFrameTimer >= attackFrameDelay) {
                    attackFrame++;
                    attackFrameTimer = 0;

                    // End attack if finished all frames
                    if (attackFrame >= attackFrames.length) {
                        attackFrame = 0;
                        attacking = false;
                    }
                }
                
                if (attacking) {
                    sprite = attackFrames[attackFrame];
                }
            } else {
                // If not attacking and close, set to idle and manage cooldown
                // Animate idle
                idleFrameTimer++;
                if (idleFrameTimer >= frameDelay) {
                    idleCurrentFrame = (idleCurrentFrame + 1) % idleFrames.length;
                    idleFrameTimer = 0;
                }
                sprite = idleFrames[idleCurrentFrame];
                if (attackCooldown > 0) {
                    attackCooldown--;
                }
            }
        }
    }


    public void draw(Graphics g, int screenX, int screenY, Player player) {
        if (!alive) return;

        int drawX = screenX;
        int drawY = screenY;
        
        if (facingLeft) {
            g.drawImage(sprite, drawX + width, drawY, -width, height, null);  // flip horizontally
        } else {
            g.drawImage(sprite, drawX, drawY, width, height, null);
        }

        // HP bar (using 400 as max HP) - drawn at original position
        g.setColor(Color.WHITE);
        g.fillRect(screenX, screenY - 10, width, 5);
        g.setColor(Color.GREEN);
        g.fillRect(screenX, screenY - 10, (int) (width * (hp / 400.0)), 5);
    }
 
    public Rectangle getBounds() {
        return new Rectangle((int) x, (int) y, width, height);
    }

    public void takeDamage(int amount) {
        hp -= amount;
        flashRed = 5; // for hit flash effect (optional)
        if (hp <= 0) {
            alive = false;
            System.out.println("Enemy defeated!");
        }
        System.out.println("Enemy HP: " + hp);

    }

    public void freeze(int frames) {
        freezeTimer = frames;
    }

    public boolean isAlive() {
        return alive;
    }

    public boolean isRetreating() {
        return retreating;
    }

    public int getX() {
        return (int) x;
    }

    public int getY() {
        return (int) y;
    }

    // Set TileManager reference for collision detection
    public void setTileManager(Object tileManager) {
        this.tileManager = tileManager;
    }

    // Apply collision-aware movement similar to player
    private void applyCollisionMovement(double moveX, double moveY) {
        if (tileManager != null) {
            // Try horizontal movement first
            double proposedX = x + moveX;
            int topLeftX = (int) Math.round(proposedX - collisionWidth / 2.0);
            int topLeftY = (int) Math.round(y - collisionHeight / 2.0);
            if (((tile.TileManager) tileManager).isWalkable(topLeftX, topLeftY, collisionWidth, collisionHeight)) {
                x = proposedX;
            }

            // Try vertical movement
            double proposedY = y + moveY;
            topLeftX = (int) Math.round(x - collisionWidth / 2.0);
            topLeftY = (int) Math.round(proposedY - collisionHeight / 2.0);
            if (((tile.TileManager) tileManager).isWalkable(topLeftX, topLeftY, collisionWidth, collisionHeight)) {
                y = proposedY;
            }
        } else {
            // No collision detection available, move freely
            x += moveX;
            y += moveY;
        }
    }
}
