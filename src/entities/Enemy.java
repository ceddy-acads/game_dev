package entities;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;
import javax.imageio.ImageIO;
import java.io.IOException;


public class Enemy {

    public enum EnemyType {
        BASIC, FAST, TANK, MINI_BOSS, MINOTAUR
    }

    private EnemyType type;

    //FOR DAMAGE
    private int attackDamage = 10;

    // Use doubles for precise position tracking
    private double x, y;
    public int width, height;
    public int hp;
    public double speed;
    private Image sprite;
    private boolean alive = true;
    private int flashRed = 0;
    private Image[] idleFrames;
    private Image[] walkFrames;
    private boolean isAnimatedGif = false;
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
    private Image[] attackFrames;
    private int attackFrame = 0;
    private int attackTimer = 0;
    private int attackDelay = 4; // lower = faster animation
    private boolean attacking = false;
    private int attackCooldown = 0; // prevents constant attacking
    private int attackFrameDelay = 3; // lower = faster attack animation (reduced from 4 to 2)
    private int attackFrameTimer = 0;

    //FOR DEATH ANIMATION
    private Image[] deathFrames;
    private int deathFrame = 0;
    private int deathFrameDelay = 8; // Slower death animation
    private int deathFrameTimer = 0;
    private boolean dying = false; // Flag for death animation state

    // Freeze effect
    private int freezeTimer = 0; // in frames, 0 = not frozen

    // Collision detection - MUST MATCH PLAYER COLLISION SYSTEM
    private Object tileManager; // Reference to TileManager for collision
    private Object inventory; // Reference to InventoryUI for powerup drops
    private Object objectManager; // Reference to ObjectManager for spawning dropped powerups
    private int collisionWidth;  // Collision box size (will be set based on enemy type)
    private int collisionHeight; // Collision box size (will be set based on enemy type)

    private void loadSprites() {
        try {
            if (type == EnemyType.MINI_BOSS) {
                // Load Mini Boss-specific sprites
                loadMiniBossSprites();
            } else if (type == EnemyType.MINOTAUR) {
                // Load Minotaur-specific sprites
                loadMinotaurSprites();
            } else {
                // Load standard enemy sprites
                loadStandardSprites();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadStandardSprites() throws IOException {
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
    }

    private void loadMinotaurSprites() throws IOException {
        // FOR IDLE - Use first frame of walk animation as idle
        BufferedImage walkSpriteSheet = ImageIO.read(getClass().getResourceAsStream("/assets/characters/enemies/minotaur_walk.png"));
        int frameWidth = 785 / 8; // 785x94, 8 frames in a single row
        int frameHeight = 94;

        // Load walk frames (8 frames)
        walkFrames = new BufferedImage[8];
        for (int i = 0; i < 8; i++) {
            walkFrames[i] = walkSpriteSheet.getSubimage(i * frameWidth, 0, frameWidth, frameHeight);
        }

        // Use walk frames as idle frames too (reuse animation)
        idleFrames = new BufferedImage[8];
        for (int i = 0; i < 8; i++) {
            idleFrames[i] = walkFrames[i];
        }

        // FOR ATTACKING - Use standard Attack_1.png spritesheet (512x128, 4 frames in a single row)
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
    }

    private void loadMiniBossSprites() throws IOException {
        isAnimatedGif = true;

        // FOR WALKING - Using nightborne_run.gif
        Image nightborneRun = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/assets/characters/enemies/mini-boss/nightborne_run.gif"));
        walkFrames = new Image[1];
        walkFrames[0] = nightborneRun;

        // Use the same for idle
        idleFrames = new Image[1];
        idleFrames[0] = nightborneRun;

        // FOR ATTACKING - Use standard Attack_1.png spritesheet (512x128, 4 frames in a single row)
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
    }

    public Enemy(int x, int y, EnemyType type) {
        this.x = (double) x;
        this.y = (double) y;
        this.type = type;

        // Set stats based on enemy type
        switch (type) {
            case BASIC:
                this.width = 186;
                this.height = 186;
                this.collisionWidth = 48;
                this.collisionHeight = 48;
                this.hp = 400;
                this.speed = 1.2;
                this.attackDamage = 20; // Increased from 10 to 20
                break;
            case FAST:
                this.width = 186;
                this.height = 186;
                this.collisionWidth = 48;
                this.collisionHeight = 48;
                this.hp = 300;
                this.speed = 1.8;
                this.attackDamage = 16; // Increased from 8 to 16
                break;
            case TANK:
                this.width = 186;
                this.height = 186;
                this.collisionWidth = 48;
                this.collisionHeight = 48;
                this.hp = 600;
                this.speed = 0.75;
                this.attackDamage = 30; // Increased from 15 to 30
                break;
            case MINI_BOSS:
                this.width = 400; // Large size
                this.height = 400;
                this.collisionWidth = 200; // Large collision box
                this.collisionHeight = 200;
                this.hp = 1500;
                this.speed = 1.05;
                this.attackDamage = 50; // Increased from 25 to 50
                break;
            case MINOTAUR:
                this.width = 186;
                this.height = 186;
                this.collisionWidth = 48;
                this.collisionHeight = 48;
                this.hp = 800;
                this.speed = 1.4;
                this.attackDamage = 35; // Strong melee attacker
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
                if (!isAnimatedGif) {
                    frameTimer++;
                    if (frameTimer >= frameDelay) {
                        currentFrame = (currentFrame + 1) % walkFrames.length;
                        frameTimer = 0;
                    }
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
            if (!isAnimatedGif) {
                frameTimer++;
                if (frameTimer >= frameDelay) {
                    currentFrame = (currentFrame + 1) % walkFrames.length;
                    frameTimer = 0;
                }
            }
            sprite = walkFrames[currentFrame];
        } else if (dist <= 0.8 || attacking) { // Attack when reasonably close to player OR continue attack animation if already started
            // Start attack if not already attacking and cooldown is ready
            if (!attacking && attackCooldown <= 0) {
                attacking = true;
                attackFrame = 0;
                attackFrameTimer = 0; // Reset timer to ensure smooth start
                attackCooldown = 5; // Very short cooldown (5 frames) for continuous attacks when in range
                System.out.println("Enemy Attacking!");
            }

            if (attacking) {
                attackFrameTimer++;
                if (attackFrameTimer >= attackFrameDelay) {
                    attackFrame++;
                    attackFrameTimer = 0;

                    // Deal damage on the middle frame of the attack animation (frame 2 out of 4)
                    if (attackFrame == 2 && dist <= 1) { // Double-check range at the moment of impact
                        // Randomize enemy attack damage with a minimum of 5
                        Random rand = new Random();
                        int minEnemyDamage = 5;
                        int maxEnemyDamage = attackDamage + 5; // e.g., if base attackDamage is 10, max will be 15
                        int randomizedDamage = minEnemyDamage + rand.nextInt(maxEnemyDamage - minEnemyDamage + 1);
                        player.takeDamage(randomizedDamage); // Deal randomized damage at the moment of impact
                        System.out.println("Enemy dealt " + randomizedDamage + " damage!");
                    }

                    // End attack if finished all frames
                    if (attackFrame >= attackFrames.length) {
                        attackFrame = 0;
                        attacking = false;
                        attackCooldown = 5; // Ensure cooldown after attack completes
                    }
                }

                // Always set attack sprite if attacking (prevent stuttering)
                if (attackFrame < attackFrames.length && attackFrames[attackFrame] != null) {
                    sprite = attackFrames[attackFrame];
                }
            } else {
                // If not attacking and close, set to idle and manage cooldown
                // Animate idle
                if (!isAnimatedGif) {
                    idleFrameTimer++;
                    if (idleFrameTimer >= frameDelay) {
                        idleCurrentFrame = (idleCurrentFrame + 1) % idleFrames.length;
                        idleFrameTimer = 0;
                    }
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
            // HP bar - drawn at original position
            g.setColor(Color.WHITE);
            g.fillRect(screenX, screenY - 10, width, 5);
            g.setColor(Color.GREEN);
            double maxHp = (type == EnemyType.MINI_BOSS) ? 1500.0 : 400.0;
            g.fillRect(screenX, screenY - 10, (int) (width * (hp / maxHp)), 5);
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
        if (objectManager == null) return;

        Random rand = new Random();

        // Determine drop chance based on enemy type with visual and potion drops
        double dropChance = 0.0;
        switch (type) {
            case BASIC:
                dropChance = 0.08; // 8% chance - very low
                break;
            case FAST:
                dropChance = 0.15; // 15% chance - low
                break;
            case TANK:
                dropChance = 0.20; // 20% chance - medium
                break;
            case MINOTAUR:
                dropChance = 0.25; // 25% chance - medium
                break;
            case MINI_BOSS:
                dropChance = 0.75; // 75% chance - high (mini boss is rare and valuable)
                break;
        }

        // Drop visual sword items on ground (high chance)
        if (rand.nextDouble() < dropChance) {
            try {
                objectManager.getClass().getMethod("addDrop", int.class, int.class).invoke(objectManager, (int) x, (int) y);
                System.out.println("Enemy dropped: Sword item at (" + x + "," + y + ")");
            } catch (Exception e) {
                System.err.println("Failed to drop sword item: " + e.getMessage());
            }
        }

        // Additionally drop potions directly to inventory (lower chance for visibility)
        double potionDropChance = dropChance * 0.3; // 30% of base drop chance
        if (rand.nextDouble() < potionDropChance && inventory != null) {
            String drop = selectPowerupDrop(rand);
            try {
                inventory.getClass().getMethod("addItem", String.class, int.class).invoke(inventory, drop, 1);
                System.out.println("Enemy dropped: " + drop + " (directly to inventory)");
            } catch (Exception e) {
                System.err.println("Failed to drop potion: " + e.getMessage());
            }
        }
    }

    private String selectPowerupDrop(Random rand) {
        // Weighted drop table: more common drops are more likely
        // potion_red (HP restore) - 60% chance
        // potion_blue (Mana restore) - 40% chance
        int roll = rand.nextInt(100);

        if (roll < 60) {
            return "potion_red";  // HP potion
        } else {
            return "potion_blue"; // Mana potion
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

    // Set ObjectManager reference for spawning dropped powerups
    public void setObjectManager(Object objectManager) {
        this.objectManager = objectManager;
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
