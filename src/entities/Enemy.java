package entities;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;
import javax.imageio.ImageIO;
import java.io.IOException;


public class Enemy {

    public enum EnemyType {
        BASIC, FAST, TANK, MINI_BOSS
    }

    private EnemyType type;

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
    private int attackFrameDelay = 2; // lower = faster attack animation (reduced from 4 to 2)
    private int attackFrameTimer = 0;

    //FOR DEATH ANIMATION
    private BufferedImage[] deathFrames;
    private int deathFrame = 0;
    private int deathFrameDelay = 8; // Slower death animation
    private int deathFrameTimer = 0;
    private boolean dying = false; // Flag for death animation state

    // Freeze effect
    private int freezeTimer = 0; // in frames, 0 = not frozen

    // Collision detection
    private Object tileManager; // Reference to TileManager for collision
    private Object inventory; // Reference to InventoryUI for powerup drops
    private final int collisionWidth = 48;  // Collision box matching player size
    private final int collisionHeight = 48;

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

            // FOR DEATH ANIMATION - Using Dead.png spritesheet (512x128, 4 frames in a single row)
            BufferedImage deathSpriteSheet = ImageIO.read(getClass().getResourceAsStream("/assets/characters/enemies/Dead.png"));
            int deathFrameWidth = 128; // 512 / 4
            int deathFrameHeight = 128;
            deathFrames = new BufferedImage[4];
            for (int i = 0; i < 4; i++) {
                deathFrames[i] = deathSpriteSheet.getSubimage(i * deathFrameWidth, 0, deathFrameWidth, deathFrameHeight);
            }

            sprite = idleFrames[0]; // default image
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public Enemy(int x, int y, EnemyType type) {
        this.x = (double) x;
        this.y = (double) y;
        this.type = type;
        this.width = 128; // Set to frame width
        this.height = 128; // Set to frame height

        // Set stats based on enemy type
        switch (type) {
            case BASIC:
                this.hp = 400;
                this.speed = 0.8;
                this.attackDamage = 20; // Increased from 10 to 20
                break;
            case FAST:
                this.hp = 300;
                this.speed = 1.2;
                this.attackDamage = 16; // Increased from 8 to 16
                break;
            case TANK:
                this.hp = 600;
                this.speed = 0.5;
                this.attackDamage = 30; // Increased from 15 to 30
                break;
            case MINI_BOSS:
                this.hp = 1500;
                this.speed = 0.7;
                this.attackDamage = 50; // Increased from 25 to 50
                break;
        }

        loadSprites();
    }


    public void update(int playerX, int playerY, Player player, int cameraX, int cameraY, int viewportWidth, int viewportHeight) {
        if (!alive) return;

        // Handle death animation
        if (dying) {
            deathFrameTimer++;
            if (deathFrameTimer >= deathFrameDelay) {
                deathFrame++;
                deathFrameTimer = 0;

                // End death animation and mark as truly dead after showing all frames
                if (deathFrame >= deathFrames.length) {
                    alive = false; // Now the enemy is truly dead
                    dying = false; // Also set dying to false so it stops being drawn
                    return;
                }
            }

            // Only set sprite if we have a valid frame
            if (deathFrame < deathFrames.length) {
                sprite = deathFrames[deathFrame];
            }
            return; // Skip all other logic while dying
        }

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
                attackCooldown = 45; // Reduced from 90 to 45 (attack twice as fast)
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
        if (!alive && !dying) return; // Don't draw if truly dead

        int drawX = screenX;
        int drawY = screenY;

        if (facingLeft && !dying) {
            g.drawImage(sprite, drawX + width, drawY, -width, height, null);  // flip horizontally
        } else {
            g.drawImage(sprite, drawX, drawY, width, height, null);
        }

        // Only draw HP bar if not dying
        if (!dying && hp > 0) {
            // HP bar (using 400 as max HP) - drawn at original position
            g.setColor(Color.WHITE);
            g.fillRect(screenX, screenY - 10, width, 5);
            g.setColor(Color.GREEN);
            g.fillRect(screenX, screenY - 10, (int) (width * (hp / 400.0)), 5);
        }
    }
 
    public Rectangle getBounds() {
        return new Rectangle((int) x, (int) y, width, height);
    }

    public void takeDamage(int amount) {
        hp -= amount;
        flashRed = 5; // for hit flash effect (optional)
        if (hp <= 0 && !dying) {
            // Start death animation instead of immediately dying
            hp = 0;
            dying = true;
            deathFrame = 0;
            deathFrameTimer = 0;

            // Drop powerups with low probability
            dropPowerup();

            System.out.println("Enemy defeated!");
        }
        if (hp > 0) {
            System.out.println("Enemy HP: " + hp);
        }
    }

    private void dropPowerup() {
        if (inventory == null) return;

        Random rand = new Random();
        // Low probability: 10% chance for basic enemies, 20% for others, 50% for mini boss
        double dropChance = 0.1;
        if (type == EnemyType.FAST || type == EnemyType.TANK) {
            dropChance = 0.2;
        } else if (type == EnemyType.MINI_BOSS) {
            dropChance = 0.5;
        }

        if (rand.nextDouble() < dropChance) {
            // Drop a random powerup
            String[] possibleDrops = {"potion_red", "potion_blue"};
            String drop = possibleDrops[rand.nextInt(possibleDrops.length)];

            try {
                // Use reflection to call addItem method on InventoryUI
                inventory.getClass().getMethod("addItem", String.class, int.class).invoke(inventory, drop, 1);
                System.out.println("Enemy dropped: " + drop);
            } catch (Exception e) {
                System.err.println("Failed to add item to inventory: " + e.getMessage());
            }
        }
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

    // Set InventoryUI reference for powerup drops
    public void setInventory(Object inventory) {
        this.inventory = inventory;
    }

    // Apply collision-aware movement similar to player
    private void applyCollisionMovement(double moveX, double moveY) {
        if (tileManager == null) {
            // No collision detection available, move freely
            x += moveX;
            y += moveY;
            return;
        }

        // Store original position
        double originalX = x;
        double originalY = y;

        // Try to move in both directions together first (diagonal movement)
        double newX = x + moveX;
        double newY = y + moveY;

        int topLeftX = (int) Math.round(newX - collisionWidth / 2.0);
        int topLeftY = (int) Math.round(newY - collisionHeight / 2.0);

        if (((tile.TileManager) tileManager).isWalkable(topLeftX, topLeftY, collisionWidth, collisionHeight)) {
            // Full movement is possible
            x = newX;
            y = newY;
            return;
        }

        // If diagonal movement fails, try horizontal movement only
        newX = x + moveX;
        newY = y; // Keep Y the same

        topLeftX = (int) Math.round(newX - collisionWidth / 2.0);
        topLeftY = (int) Math.round(y - collisionHeight / 2.0);

        if (((tile.TileManager) tileManager).isWalkable(topLeftX, topLeftY, collisionWidth, collisionHeight)) {
            x = newX;
            // y stays the same
            return;
        }

        // If horizontal fails, try vertical movement only
        newX = x; // Keep X the same
        newY = y + moveY;

        topLeftX = (int) Math.round(x - collisionWidth / 2.0);
        topLeftY = (int) Math.round(newY - collisionHeight / 2.0);

        if (((tile.TileManager) tileManager).isWalkable(topLeftX, topLeftY, collisionWidth, collisionHeight)) {
            // x stays the same
            y = newY;
            return;
        }

        // If all movement attempts fail, enemy stays in place
        // This prevents enemies from getting stuck in trees or other obstacles
    }
}
