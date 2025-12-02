package entities;
import java.awt.*;
import java.awt.Graphics2D;
import java.awt.Image;
import javax.swing.ImageIcon;
import input.KeyHandler;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;
import main.Main; // Import Main class for sound effects

public class Player {
    private int qCooldown = 0;
    private final int Q_COOLDOWN_MAX = 30; // 30 frames = 0.5 sec at 60FPS
    private int wCooldown = 0;
    private final int W_COOLDOWN_MAX = 60; // 1 sec
    private int bCooldown = 0;
    private final int B_COOLDOWN_MAX = 180; // 3 sec
    private int nCooldown = 0;
    private final int N_COOLDOWN_MAX = 420; // 7 sec
    private int mCooldown = 0;
    private final int M_COOLDOWN_MAX = 720; // 12 sec

    //HP of the character
    private int maxHp = 100;
    private int hp = 100;
    private boolean alive = true;
    private boolean takingDamage = false;
    private int flashTimer = 0;

    //Mana system
    private int maxMana = 100;
    private int mana = 100;
    private float manaRegenRate = 0.1f; // Mana per frame (57 mana per second at 60 FPS)
    private float manaFraction = 0.0f; // Accumulate fractional mana

    // Player stats
    private int baseAttack = 100;

    // Initial position for respawn
    private final int initialX;
    private final int initialY;
    private int baseDefense = 5;
    private int equippedAttack = 0;
    private int equippedDefense = 0;

    // Position stored as doubles to support diagonal normalization cleanly
    public double px, py; // Made public for direct access in GameLoop for camera
    private double speed;
    private KeyHandler keyH;
    private Object tileManager; // Reference to TileManager for collision
    private java.util.List<NPC> npcs; // Reference to NPCs for collision
    private Object objectManager; // Reference to ObjectManager for collision
    private Object inventory; // Reference to InventoryUI

    // State constants
    private static final int IDLE = 0;
    private static final int WALKING = 1;
    private static final int ATTACKING = 2;
    private static final int DYING = 3;
    private static final int HURT = 4; // New state for taking damage
    private static final int FIRESPLASH = 5;
    private static final int ICEPIERCER = 6; // New state for Ice Piercer skill
    private static final int LIGHTNINGSTORM = 7; // New state for Lightning Storm skill
    private int state = IDLE;  // Start in idle state

    // Animation frames [direction][frameIndex]
    private Image[][] frames;
    private Image[][] dieFrames; // New array for death animation frames
    private Image[][] attackFrames;
    private Image[][] idleFrames;
    private Image[][] hurtFrames;
    private Image[][] firesplashFrames;
    private Image[][] icepiercerFrames; // New array for Ice Piercer animation frames
    private Image[][] lightningstormFrames; // New array for Lightning Storm animation frames
    private Image currentImg;  // General image
    private int deathDirection = DOWN;
    private int frameIndex = 0;        // Default for walking
    private float accumulatedAnimationTime = 0f;  // For time-based animation
    private final float playerFrameDuration = 0.1f;  // Time per frame
    private boolean deathAnimationFinished = false; // Flag to indicate if death animation is done
    private static final int HURT_FRAMES = 5;

    // Direction constants
    private static final int DOWN = 0;
    private static final int LEFT = 1;
    private static final int RIGHT = 2;
    private static final int UP = 3;
    private static final int UP_LEFT = 4;
    private static final int UP_RIGHT = 5;
    private static final int DOWN_LEFT = 6;
    private static final int DOWN_RIGHT = 7;
    private int currentDirection = DOWN;
    
    // Player dimensions for collision
    public final int playerWidth = 256;
    public final int playerHeight = 256;

    // Smaller collision box for better movement
    private final int collisionWidth = 48;
    private final int collisionHeight = 48;

    // Slash attacks for SkillQ
    private final ArrayList<SlashAttack> slashes = new ArrayList<>();
    // SkillW attacks
    private final ArrayList<SkillWAttack> skillWAttacks = new ArrayList<>();

    // Freeze skill area
    private Rectangle freezeArea = null;
    // Lightning storm skill area
    private Rectangle lightningArea = null;

    public ArrayList<SlashAttack> getSlashes() {
        return slashes;
    }

    public ArrayList<SkillWAttack> getSkillWAttacks() {
        return skillWAttacks;
    }

    public int getTotalAttack() {
        return baseAttack + equippedAttack;
    }

    public int getTotalDefense() {
        return baseDefense + equippedDefense;
    }

    public int getEquippedAttack() {
        return equippedAttack;
    }

    public int getEquippedDefense() {
        return equippedDefense;
    }

    public void setEquippedStats(int attack, int defense) {
        this.equippedAttack = attack;
        this.equippedDefense = defense;
    }

    public void takeDamage(int amount) {
        if (!alive || state == DYING || state == HURT) return;

        int damageTaken = Math.max(0, amount - getTotalDefense());
        hp -= damageTaken;
        state = HURT;
        frameIndex = 0;
        accumulatedAnimationTime = 0f;

        if (hp <= 0) {
            hp = 0;
            alive = false;
            this.deathDirection = this.currentDirection; // Store direction at time of death
            state = DYING; // Set state to DYING
            frameIndex = 0; // Start death animation from first frame
            accumulatedAnimationTime = 0f; // Reset animation timer
            System.out.println("Player defeated!");
        } else {
            System.out.println("Player HP: " + hp);
        }
    }

    public Player(int startX, int startY, KeyHandler keyH) {
        this.initialX = startX; // Store initial X
        this.initialY = startY; // Store initial Y
        this.keyH = keyH;
        this.px = startX;
        this.py = startY;
        this.speed = 4.0;
        this.hp = maxHp; // Start with full HP
        this.alive = true;
        this.state = IDLE;
        this.deathAnimationFinished = false;
        loadFrames();
        currentImg = idleFrames[DOWN][0];
    }

    public void resetPlayerState() {
        this.px = initialX;
        this.py = initialY;
        this.hp = maxHp;
        this.mana = maxMana;
        this.alive = true;
        this.state = IDLE;
        this.deathAnimationFinished = false;
        this.qCooldown = 0;
        this.wCooldown = 0;
        this.bCooldown = 0;
        this.nCooldown = 0;
        this.mCooldown = 0;
        this.slashes.clear();
        this.skillWAttacks.clear();
        this.conversationCount = 0; // Reset conversation progress on death/continue
    }

    // Load frames for 4 directions and reuse for diagonals if needed
    private void loadFrames() {
        frames = new Image[8][6]; // 8 directions, 6 frames each
        attackFrames = new Image[8][6];
        idleFrames = new Image[8][6];
        BufferedImage spriteSheet = loadSpriteSheet("/assets/characters/player_walk.png");

        if (spriteSheet != null) {
            
            for (int i = 0; i < 6; i++) { 
                frames[DOWN][i] = getSubImage(spriteSheet, i, 0);
                frames[LEFT][i] = getSubImage(spriteSheet, i, 1); 
                frames[RIGHT][i] = getSubImage(spriteSheet, i, 2);
                frames[UP][i] = getSubImage(spriteSheet, i, 3);
            }
        }

        // Diagonals reuse vertical/horizontal frames
        frames[UP_LEFT] = frames[UP];
        frames[UP_RIGHT] = frames[UP];
        frames[DOWN_LEFT] = frames[DOWN];
        frames[DOWN_RIGHT] = frames[DOWN];

        // Load death animation frames from sprite sheet
        dieFrames = new Image[8][6]; // Match frames structure
        BufferedImage dieSpriteSheet = loadSpriteSheet("/assets/characters/player_death.png");
        if (dieSpriteSheet != null) {
            // Row 0: Die Down
            // Row 1: Die Left
            // Row 2: Die Right
            // Row 3: Die Up (Assuming typo from Row 4)
            for (int i = 0; i < 6; i++) { // 6 frames per direction
                dieFrames[DOWN][i] = getSubImage(dieSpriteSheet, i, 0);
                dieFrames[LEFT][i] = getSubImage(dieSpriteSheet, i, 1);
                dieFrames[RIGHT][i] = getSubImage(dieSpriteSheet, i, 2);
                dieFrames[UP][i] = getSubImage(dieSpriteSheet, i, 3);
            }
             // Map diagonals
            dieFrames[UP_LEFT] = dieFrames[LEFT];
            dieFrames[UP_RIGHT] = dieFrames[RIGHT];
            dieFrames[DOWN_LEFT] = dieFrames[LEFT];
            dieFrames[DOWN_RIGHT] = dieFrames[RIGHT];
        } else {
            System.err.println("Player: missing die sprite sheet.");
        }
        
        BufferedImage attackSpriteSheet = loadSpriteSheet("/assets/characters/playerwalk_attack.png");
        if(attackSpriteSheet != null){
            for (int i = 0; i < 6; i++) { 
                attackFrames[DOWN][i] = getSubImage(attackSpriteSheet, i, 0);
                attackFrames[LEFT][i] = getSubImage(attackSpriteSheet, i, 1); 
                attackFrames[RIGHT][i] = getSubImage(attackSpriteSheet, i, 2);
                attackFrames[UP][i] = getSubImage(attackSpriteSheet, i, 3);
            }
        }
        
        attackFrames[UP_LEFT] = attackFrames[UP];
        attackFrames[UP_RIGHT] = attackFrames[UP];
        attackFrames[DOWN_LEFT] = attackFrames[DOWN];
        attackFrames[DOWN_RIGHT] = attackFrames[DOWN];

        // Use the first frame of walking animation as the idle frame
        for (int i = 0; i < 6; i++) { // Assign the first frame to all idle frames to stop animation
            idleFrames[DOWN][i] = frames[DOWN][0];
            idleFrames[LEFT][i] = frames[LEFT][0];
            idleFrames[RIGHT][i] = frames[RIGHT][0];
            idleFrames[UP][i] = frames[UP][0];
        }

        idleFrames[UP_LEFT] = idleFrames[UP];
        idleFrames[UP_RIGHT] = idleFrames[UP];
        idleFrames[DOWN_LEFT] = idleFrames[DOWN];
        idleFrames[DOWN_RIGHT] = idleFrames[DOWN];

        // Load hurt animation frames
        hurtFrames = new Image[8][HURT_FRAMES];
        BufferedImage hurtSpriteSheet = loadSpriteSheet("/assets/characters/player_hurt.png");
        if (hurtSpriteSheet != null) {
            for (int i = 0; i < HURT_FRAMES; i++) {
                hurtFrames[DOWN][i] = getSubImage(hurtSpriteSheet, i, 0);
                hurtFrames[LEFT][i] = getSubImage(hurtSpriteSheet, i, 1);
                hurtFrames[RIGHT][i] = getSubImage(hurtSpriteSheet, i, 2);
                hurtFrames[UP][i] = getSubImage(hurtSpriteSheet, i, 3);
            }
        }
        hurtFrames[UP_LEFT] = hurtFrames[UP];
        hurtFrames[UP_RIGHT] = hurtFrames[UP];
        hurtFrames[DOWN_LEFT] = hurtFrames[DOWN];
        hurtFrames[DOWN_RIGHT] = hurtFrames[DOWN];

        firesplashFrames = new Image[8][6]; // Corrected to 6 frames per direction
        BufferedImage firesplashSpriteSheet = loadSpriteSheet("/assets/characters/player_firesplash.png");
        if (firesplashSpriteSheet != null) {
            for (int i = 0; i < 6; i++) { // Corrected loop to 6 frames
                firesplashFrames[DOWN][i] = getSubImage(firesplashSpriteSheet, i, 0);
                firesplashFrames[LEFT][i] = getSubImage(firesplashSpriteSheet, i, 1);
                firesplashFrames[RIGHT][i] = getSubImage(firesplashSpriteSheet, i, 2);
                firesplashFrames[UP][i] = getSubImage(firesplashSpriteSheet, i, 3);
            }
            firesplashFrames[UP_LEFT] = firesplashFrames[LEFT];
            firesplashFrames[UP_RIGHT] = firesplashFrames[RIGHT];
        firesplashFrames[DOWN_LEFT] = firesplashFrames[LEFT];
        firesplashFrames[DOWN_RIGHT] = firesplashFrames[RIGHT];
        }

        icepiercerFrames = new Image[8][6];
        BufferedImage icepiercerSpriteSheet = loadSpriteSheet("/assets/characters/player_icepiercer.png");
        if (icepiercerSpriteSheet != null) {
            for (int i = 0; i < 6; i++) {
                icepiercerFrames[DOWN][i] = getSubImage(icepiercerSpriteSheet, i, 0);
                icepiercerFrames[LEFT][i] = getSubImage(icepiercerSpriteSheet, i, 1);
                icepiercerFrames[RIGHT][i] = getSubImage(icepiercerSpriteSheet, i, 2);
                icepiercerFrames[UP][i] = getSubImage(icepiercerSpriteSheet, i, 3);
            }
            icepiercerFrames[UP_LEFT] = icepiercerFrames[LEFT];
            icepiercerFrames[UP_RIGHT] = icepiercerFrames[RIGHT];
            icepiercerFrames[DOWN_LEFT] = icepiercerFrames[LEFT];
            icepiercerFrames[DOWN_RIGHT] = icepiercerFrames[RIGHT];
        }

        lightningstormFrames = new Image[8][6];
        BufferedImage lightningstormSpriteSheet = loadSpriteSheet("/assets/characters/player_lightningstorm.png");
        if (lightningstormSpriteSheet != null) {
            for (int i = 0; i < 6; i++) {
                lightningstormFrames[DOWN][i] = getSubImage(lightningstormSpriteSheet, i, 0);
                lightningstormFrames[LEFT][i] = getSubImage(lightningstormSpriteSheet, i, 1);
                lightningstormFrames[RIGHT][i] = getSubImage(lightningstormSpriteSheet, i, 2);
                lightningstormFrames[UP][i] = getSubImage(lightningstormSpriteSheet, i, 3);
            }
            lightningstormFrames[UP_LEFT] = lightningstormFrames[LEFT];
            lightningstormFrames[UP_RIGHT] = lightningstormFrames[RIGHT];
            lightningstormFrames[DOWN_LEFT] = lightningstormFrames[LEFT];
            lightningstormFrames[DOWN_RIGHT] = lightningstormFrames[RIGHT];
        }
    }
    
    private BufferedImage loadSpriteSheet(String path) {
        try {
            java.net.URL res = getClass().getResource(path);
            if (res != null) {
                return ImageIO.read(res);
            }
        } catch (IOException e) {
            System.err.println("Could not load sprite sheet: " + path);
        }
        return null;
    }

    private Image getSubImage(BufferedImage spriteSheet, int col, int row) {
        int spriteWidth = 64;
        int spriteHeight = 64;
        return spriteSheet.getSubimage(col * spriteWidth, row * spriteHeight, spriteWidth, spriteHeight);
    }

    private Image loadImg(String path) {
        String[] candidates = { path, "/sprites/" + path.substring(path.lastIndexOf('/') + 1), "/assets/sprites/" + path.substring(path.lastIndexOf('/') + 1) };
        for (String p : candidates) {
            try {
                java.net.URL res = getClass().getResource(p);
                if (res != null) {
                    return new ImageIcon(res).getImage();
                }
            } catch (Exception e) {
                // Ignore and try next
                System.err.println("Could not load image: " + p);
            }
        }
        return null;  // Return null if not found
    }

    public void update(float deltaTime) { // Removed map parameter, now uses stored map
        if (!alive && deathAnimationFinished) {
            return; // Stop updating if dead and animation finished
        }

        if (state == DYING) {
            accumulatedAnimationTime += deltaTime;
            if (accumulatedAnimationTime >= playerFrameDuration) {
                frameIndex++;
                accumulatedAnimationTime -= playerFrameDuration;
                if (frameIndex >= dieFrames[0].length) { // Check against number of frames in one sequence
                    frameIndex = dieFrames[0].length - 1; // Stay on the last frame
                    deathAnimationFinished = true;
                }
            }
            // If dying, skip all movement and attack logic
            return;
        }

        if (state == HURT) {
            accumulatedAnimationTime += deltaTime;
            if (accumulatedAnimationTime >= playerFrameDuration) {
                frameIndex++;
                accumulatedAnimationTime -= playerFrameDuration;
                if (frameIndex >= HURT_FRAMES) {
                    frameIndex = 0;
                    state = IDLE; // Return to idle after hurt animation
                }
            }
            // Skip other logic when hurt
            return;
        }

        if (qCooldown > 0) qCooldown--;
        if (wCooldown > 0) wCooldown--;
        if (bCooldown > 0) bCooldown--;
        if (nCooldown > 0) nCooldown--;
        if (mCooldown > 0) mCooldown--;

        // Mana regeneration - properly handles fractional accumulation
        if (mana < maxMana) {
            // Double regeneration rate when mana bar is empty (mana <= 0)
            float currentRegenRate = (mana <= 0) ? manaRegenRate * 2.0f : manaRegenRate;

            // Accumulate fractional mana
            manaFraction += currentRegenRate;

            // Add whole mana points when fraction reaches 1.0
            int manaToAdd = (int) manaFraction;
            if (manaToAdd > 0) {
                mana += manaToAdd;
                manaFraction -= manaToAdd;

                // Cap at max mana
                if (mana > maxMana) {
                    mana = maxMana;
                    manaFraction = 0.0f; // Reset fraction if capped
                }

                // Debug: uncomment to see regeneration
                // System.out.println("Mana regenerated: " + mana + "/" + maxMana + " (fraction: " + manaFraction + ")");
            }
        }



        // --- Q Attack: cooldown-limited, one press = one attack ---
        if (keyH.skillSPACE && qCooldown == 0) { // Changed to skillSPACE
            useSkillQ();
            qCooldown = Q_COOLDOWN_MAX;
            keyH.skillSPACE = false; // Reset the skill key after use
        }

        // --- W Attack: cooldown-limited, one press = one attack ---
        if (keyH.skillW && wCooldown == 0) {
            useSkillW();
            wCooldown = W_COOLDOWN_MAX;
            keyH.skillW = false; // Reset the skill key after use
        }

        // --- B Attack: cooldown-limited, mana cost, one press = one attack ---
        if (keyH.skillB && bCooldown == 0 && mana >= 30) {
            useSkillB();
            mana -= 30;
            bCooldown = B_COOLDOWN_MAX;
            keyH.skillB = false; // Reset to prevent continuous skill use
        }

        // --- N Attack: cooldown-limited, mana cost, one press = one attack ---
        if (keyH.skillN && nCooldown == 0 && mana >= 45) {
            useSkillN();
            mana -= 45;
            nCooldown = N_COOLDOWN_MAX;
            keyH.skillN = false; // Reset to prevent continuous skill use
        }

        // --- M Attack: cooldown-limited, mana cost, one press = one attack ---
        if (keyH.skillM && mCooldown == 0 && mana >= 80) {
            useSkillM();
            mana -= 80;
            mCooldown = M_COOLDOWN_MAX;
            keyH.skillM = false; // Reset to prevent continuous skill use
        }

        // --- Interact with NPCs ---
        if (keyH.interactJ) {
            checkNPCInteraction();
            keyH.interactJ = false; // Reset after use
        }



        // Movement input aggregated - frame rate independent
        double dx = 0.0, dy = 0.0;
        if (keyH.upPressed) dy -= 1.0;
        if (keyH.downPressed) dy += 1.0;
        if (keyH.leftPressed) dx -= 1.0;
        if (keyH.rightPressed) dx += 1.0;

        // Normalize diagonal
        if (dx != 0 && dy != 0) {
            dx *= 0.7071067811865476; // 1/sqrt(2)
            dy *= 0.7071067811865476;
        }

        // Apply speed and deltaTime for smooth, frame-rate independent movement
        dx *= speed * deltaTime * 60.0f; // Convert back to pixels per frame equivalent
        dy *= speed * deltaTime * 60.0f;

        // Apply movement with tile and NPC collision detection using smaller collision box
        Rectangle playerBounds = new Rectangle(
            (int) Math.round(px - collisionWidth / 2.0),
            (int) Math.round(py - collisionHeight / 2.0),
            collisionWidth, collisionHeight);

        // Try horizontal movement
        double proposedX = px + dx;
        Rectangle proposedXBounds = new Rectangle(
            (int) Math.round(proposedX - collisionWidth / 2.0),
            (int) Math.round(py - collisionHeight / 2.0),
            collisionWidth, collisionHeight);

        boolean canMoveX = true;

        // Check map boundaries first (strict boundary collision)
        if (tileManager != null) {
            int mapWidth = ((tile.TileManager) tileManager).getMapWidth() * ((tile.TileManager) tileManager).getTileSize();
            int leftBound = (int) Math.round(proposedX - collisionWidth / 2.0);
            int rightBound = (int) Math.round(proposedX + collisionWidth / 2.0);
            if (leftBound < 0 || rightBound >= mapWidth) {
                canMoveX = false;
            }
        }

        // Check tile collision (solid tiles block movement)
        if (canMoveX && tileManager != null) {
            if (!((tile.TileManager) tileManager).isWalkable(
                (int) Math.round(proposedX - collisionWidth / 2.0),
                (int) Math.round(py - collisionHeight / 2.0),
                collisionWidth, collisionHeight)) {
                canMoveX = false;
            }
        }

        // Check NPC collision for horizontal movement
        if (canMoveX && npcs != null) {
            for (NPC npc : npcs) {
                if (proposedXBounds.intersects(npc.getBounds())) {
                    canMoveX = false;
                    break;
                }
            }
        }

        // Check object collision for horizontal movement
        if (canMoveX && objectManager != null) {
            if (((world.ObjectManager) objectManager).isObjectCollision(
                (int) Math.round(proposedX - collisionWidth / 2.0),
                (int) Math.round(py - collisionHeight / 2.0),
                collisionWidth, collisionHeight)) {
                canMoveX = false;
            }
        }

        if (canMoveX) {
            px = proposedX;
            playerBounds = proposedXBounds;
        }

        // Try vertical movement
        double proposedY = py + dy;
        Rectangle proposedYBounds = new Rectangle(
            (int) Math.round(px - collisionWidth / 2.0),
            (int) Math.round(proposedY - collisionHeight / 2.0),
            collisionWidth, collisionHeight);

        boolean canMoveY = true;

        // Check map boundaries first (strict boundary collision)
        if (tileManager != null) {
            int mapHeight = ((tile.TileManager) tileManager).getMapHeight() * ((tile.TileManager) tileManager).getTileSize();
            int topBound = (int) Math.round(proposedY - collisionHeight / 2.0);
            int bottomBound = (int) Math.round(proposedY + collisionHeight / 2.0);
            if (topBound < 0 || bottomBound >= mapHeight) {
                canMoveY = false;
            }
        }

        // Check tile collision (solid tiles block movement)
        if (canMoveY && tileManager != null) {
            if (!((tile.TileManager) tileManager).isWalkable(
                (int) Math.round(px - collisionWidth / 2.0),
                (int) Math.round(proposedY - collisionHeight / 2.0),
                collisionWidth, collisionHeight)) {
                canMoveY = false;
            }
        }

        // Check NPC collision for vertical movement
        if (canMoveY && npcs != null) {
            for (NPC npc : npcs) {
                if (proposedYBounds.intersects(npc.getBounds())) {
                    canMoveY = false;
                    break;
                }
            }
        }

        // Check object collision for vertical movement
        if (canMoveY && objectManager != null) {
            if (((world.ObjectManager) objectManager).isObjectCollision(
                (int) Math.round(px - collisionWidth / 2.0),
                (int) Math.round(proposedY - collisionHeight / 2.0),
                collisionWidth, collisionHeight)) {
                canMoveY = false;
            }
        }

        if (canMoveY) {
            py = proposedY;
        }
        
        boolean isAttacking = !slashes.isEmpty() || !skillWAttacks.isEmpty();
        // Only update state if not in a non-interruptible state
        if (state != FIRESPLASH && state != HURT && state != DYING && state != ICEPIERCER && state != LIGHTNINGSTORM) {
            boolean isMoving = (dx != 0 || dy != 0);

            if (isAttacking) {
                state = ATTACKING;
            } else if (isMoving) {
                state = WALKING;
            } else {
                state = IDLE;
            }
        }
        // Update active attacks
        Iterator<SlashAttack> it = slashes.iterator();
        while (it.hasNext()) {
            SlashAttack s = it.next();
            s.update(deltaTime);
            if (!s.active) it.remove();
        }
        Iterator<SkillWAttack> skillWIt = skillWAttacks.iterator();
        while (skillWIt.hasNext()) {
            SkillWAttack s = skillWIt.next();
            s.update(deltaTime);
            if (!s.active) skillWIt.remove();
        }
        
        // Determine current facing based on last input vector, but only if not attacking
        if (!isAttacking) { 
            if (dx > 0 && dy < 0) currentDirection = UP_RIGHT;
            else if (dx > 0 && dy > 0) currentDirection = DOWN_RIGHT;
            else if (dx < 0 && dy < 0) currentDirection = UP_LEFT;
            else if (dx < 0 && dy > 0) currentDirection = DOWN_LEFT;
            else if (dx > 0) currentDirection = RIGHT;
            else if (dx < 0) currentDirection = LEFT;
            else if (dy < 0) currentDirection = UP;
            else if (dy > 0) currentDirection = DOWN;
        }

        // Animation logic based on state
        switch (state) {
            case ICEPIERCER:
                accumulatedAnimationTime += deltaTime;
                if (accumulatedAnimationTime >= playerFrameDuration) {
                    frameIndex++;
                    accumulatedAnimationTime -= playerFrameDuration;
                    if (frameIndex >= 6) { // Ice Piercer has 6 frames
                        frameIndex = 0;
                        state = IDLE;
                    }
                }
                if (frameIndex < 6) {
                    currentImg = icepiercerFrames[currentDirection][frameIndex];
                }
                break;
            case FIRESPLASH:
                accumulatedAnimationTime += deltaTime;
                if (accumulatedAnimationTime >= playerFrameDuration) {
                    frameIndex++;
                    accumulatedAnimationTime -= playerFrameDuration;
                    if (frameIndex >= 6) { // Corrected to 6 frames
                        frameIndex = 0;
                        state = IDLE;
                    }
                }
                // Ensure frameIndex is within bounds before accessing array
                if (frameIndex < 6) { // Corrected to 6 frames
                    currentImg = firesplashFrames[currentDirection][frameIndex];
                }
                break;
            case LIGHTNINGSTORM:
                accumulatedAnimationTime += deltaTime;
                if (accumulatedAnimationTime >= playerFrameDuration) {
                    frameIndex++;
                    accumulatedAnimationTime -= playerFrameDuration;
                    if (frameIndex >= 6) { // Lightning Storm has 6 frames
                        frameIndex = 0;
                        state = IDLE;
                    }
                }
                if (frameIndex < 6) {
                    currentImg = lightningstormFrames[currentDirection][frameIndex];
                }
                break;
            case HURT:
                // Similar logic as DYING and HURT at the top of update()
                break;
            case ATTACKING:
                accumulatedAnimationTime += deltaTime;
                while (accumulatedAnimationTime >= playerFrameDuration) {
                    frameIndex++;
                    if (frameIndex > 5) frameIndex = 0;
                    accumulatedAnimationTime -= playerFrameDuration;
                }
                currentImg = attackFrames[currentDirection][frameIndex];
                break;
            case WALKING:
                accumulatedAnimationTime += deltaTime;
                while (accumulatedAnimationTime >= playerFrameDuration) {
                    frameIndex++;
                    if (frameIndex > 5) frameIndex = 0;
                    accumulatedAnimationTime -= playerFrameDuration;
                }
                currentImg = frames[currentDirection][frameIndex];
                break;
            case IDLE:
                accumulatedAnimationTime += deltaTime;
                while (accumulatedAnimationTime >= playerFrameDuration) {
                    frameIndex++;
                    if (frameIndex > 5) frameIndex = 0;
                    accumulatedAnimationTime -= playerFrameDuration;
                }
                currentImg = idleFrames[currentDirection][frameIndex];
                break;
        }

        // All skills are now handled above with proper cooldown checking
    }

    public void draw(Graphics g, int screenX, int screenY) {
        if (!alive && deathAnimationFinished) {
            return; // Don't draw anything if dead and animation finished
        }

        Graphics2D g2 = (Graphics2D) g;
        int drawX = screenX - playerWidth / 2;
        int drawY = screenY - playerHeight / 2;
        int width = playerWidth;
        int height = playerHeight;

        // Draw death animation if dying
        if (state == DYING && !deathAnimationFinished) {
            Image currentDieFrame = dieFrames[deathDirection][frameIndex];
            if (currentDieFrame != null) {
                g2.drawImage(currentDieFrame, drawX, drawY, width, height, null);
            } else {
                g2.setColor(Color.DARK_GRAY); // Fallback for missing death frame
                g2.fillRect(drawX, drawY, width, height);
            }
            return; // Don't draw anything else if dying
        }


        if (currentImg != null) {
            g2.drawImage(currentImg, drawX, drawY, width, height, null);
        } else {
            g2.setColor(Color.BLUE);
            g2.fillRect(drawX, drawY, 32, 32);
        }

        // Draw name above player (keeping this for identification)
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 12));
        FontMetrics fm = g.getFontMetrics();
        String name = "Kael";
        int textWidth = fm.stringWidth(name);
        int textX = drawX + (width - textWidth) / 2;
        int textY = drawY - 15;
        g.drawString(name, textX, textY);

        // Skill animations are drawn by GameLoop, not Player itself.
    }

    public void useSkillQ() {
        int drawX = (int) Math.round(px);
        int drawY = (int) Math.round(py);
        int offset = 20;
        int sx = drawX;
        int sy = drawY;
        switch (currentDirection) {
            case RIGHT: sx = drawX + offset; sy = drawY; break;
            case LEFT: sx = drawX - offset; sy = drawY; break;
            case UP: sx = drawX; sy = drawY - offset; break;
            case DOWN: sx = drawX; sy = drawY + offset; break;
            case UP_LEFT: sx = drawX - offset; sy = drawY - offset; break;
            case UP_RIGHT: sx = drawX + offset; sy = drawY - offset; break;
            case DOWN_LEFT: sx = drawX - offset; sy = drawY + offset; break;
            case DOWN_RIGHT: sx = drawX + offset; sy = drawY + offset; break;
        }
        slashes.add(new SlashAttack(sx, sy, currentDirection, getTotalAttack()));
        Main.playSoundEffect("src/assets/audio/sword_slash.wav"); // Play sword sound effect
        state = ATTACKING;
    }

    public void useSkillW() {
        int drawX = (int) Math.round(px);
        int drawY = (int) Math.round(py);
        int offset = 20;
        int sx = drawX;
        int sy = drawY;
        switch (currentDirection) {
            case RIGHT: sx = drawX + offset; sy = drawY; break;
            case LEFT: sx = drawX - offset; sy = drawY; break;
            case UP: sx = drawX; sy = drawY - offset; break;
            case DOWN: sx = drawX; sy = drawY + offset; break;
            case UP_LEFT: sx = drawX - offset; sy = drawY - offset; break;
            case UP_RIGHT: sx = drawX + offset; sy = drawY - offset; break;
            case DOWN_LEFT: sx = drawX - offset; sy = drawY + offset; break;
            case DOWN_RIGHT: sx = drawX + offset; sy = drawY + offset; break;
        }
        skillWAttacks.add(new SkillWAttack(sx, sy, currentDirection, getTotalAttack()));
        Main.playSoundEffect("src/assets/audio/skill_2.wav"); // Play sound effect for Skill W
        state = ATTACKING;
    }

    // Getters
    public int getX() {
        return (int) Math.round(this.px);
    }

    public int getY() {
        return (int) Math.round(this.py);
    }

    public boolean isAlive() {
        return alive;
    }

    public boolean isDeathAnimationFinished() {
        return deathAnimationFinished;
    }

    public Rectangle getFreezeArea() {
        return freezeArea;
    }

    public void clearFreezeArea() {
        freezeArea = null;
    }

    public Rectangle getLightningArea() {
        return lightningArea;
    }

    public void clearLightningArea() {
        lightningArea = null;
    }

    // Cooldown getters
    public int getBCooldown() { return bCooldown; }
    public int getBCooldownMax() { return B_COOLDOWN_MAX; }
    public int getNCooldown() { return nCooldown; }
    public int getNCooldownMax() { return N_COOLDOWN_MAX; }
    public int getMCooldown() { return mCooldown; }
    public int getMCooldownMax() { return M_COOLDOWN_MAX; }

    // HP getters
    public int getHp() { return hp; }
    public int getMaxHp() { return maxHp; }

    // Mana getters
    public int getMana() { return mana; }
    public int getMaxMana() { return maxMana; }

    // Mana restoration method
    public void restoreMana(int amount) {
        mana += amount;
        if (mana > maxMana) mana = maxMana;
    }

    public void useSkillB() {
        int drawX = (int) Math.round(px);
        int drawY = (int) Math.round(py);
        int offset = 20;
        int sx = drawX;
        int sy = drawY;
        switch (currentDirection) {
            case RIGHT: sx = drawX + offset; sy = drawY; break;
            case LEFT: sx = drawX - offset; sy = drawY; break;
            case UP: sx = drawX; sy = drawY - offset; break;
            case DOWN: sx = drawX; sy = drawY + offset; break;
            case UP_LEFT: sx = drawX - offset; sy = drawY - offset; break;
            case UP_RIGHT: sx = drawX + offset; sy = drawY - offset; break;
            case DOWN_LEFT: sx = drawX - offset; sy = drawY + offset; break;
            case DOWN_RIGHT: sx = drawX + offset; sy = drawY + offset; break;
        }
        skillWAttacks.add(new SkillWAttack(sx, sy, currentDirection, getTotalAttack()));
        Main.playSoundEffect("src/assets/audio/skill_1.wav"); // Play sound effect for Skill B
        state = FIRESPLASH;
        frameIndex = 0;
        accumulatedAnimationTime = 0f;
    }
    public void useSkillN() {
        state = ICEPIERCER;
        frameIndex = 0;
        accumulatedAnimationTime = 0f;
        // Set freeze area around player
        int freezeRadius = 100; // pixels
        int centerX = (int) Math.round(px);
        int centerY = (int) Math.round(py);
        freezeArea = new Rectangle(centerX - freezeRadius, centerY - freezeRadius, freezeRadius * 2, freezeRadius * 2);
        Main.playSoundEffect("src/assets/audio/skill_2.wav"); // Play sound effect for Skill N
    }
    public void useSkillM() {
        state = LIGHTNINGSTORM;
        frameIndex = 0;
        accumulatedAnimationTime = 0f;
        // Set lightning area around player
        int lightningRadius = 120; // pixels, slightly larger than freeze
        int centerX = (int) Math.round(px);
        int centerY = (int) Math.round(py);
        lightningArea = new Rectangle(centerX - lightningRadius, centerY - lightningRadius, lightningRadius * 2, lightningRadius * 2);
        Main.playSoundEffect("src/assets/audio/skill_3.wav"); // Play sound effect for Skill M
    }

    // Method to set TileManager reference for collision detection
    public void setTileManager(Object tileManager) {
        this.tileManager = tileManager;
    }

    // Method to set NPCs reference for collision detection
    public void setNPCs(java.util.List<NPC> npcs) {
        this.npcs = npcs;
    }

    // Method to set ObjectManager reference for collision detection
    public void setObjectManager(Object objectManager) {
        this.objectManager = objectManager;
    }

    // Method to set DialogueUI reference
    public void setDialogueUI(Object dialogueUI) {
        this.dialogueUI = dialogueUI;
    }

    // Method to set InventoryUI reference
    public void setInventory(Object inventory) {
        this.inventory = inventory;
    }

    private Object dialogueUI;

    // Dialogue system
    public String dialogueText = null;
    private int dialogueTimer = 0;
    private final int DIALOGUE_DURATION = 300; // 5 seconds at 60 FPS
    private int conversationCount = 0; // Track how many times player has talked to NPCs

    private void checkNPCInteraction() {
        if (npcs != null) {
            for (NPC npc : npcs) {
                double distance = Math.sqrt(Math.pow(px - npc.getX(), 2) + Math.pow(py - npc.getY(), 2));
                if (distance < 80) { // Within 80 pixels (increased range for easier interaction)
                    startDialogue(npc);
                    break;
                }
            }
        }
    }

    // Method to pick up dropped sword icon (add to inventory)
    public void pickUpSword() {
        if (inventory != null) {
            try {
                inventory.getClass().getMethod("addItem", String.class, int.class).invoke(inventory, "sword", 1);
                System.out.println("Picked up sword and added to inventory");
            } catch (Exception e) {
                System.err.println("Failed to add sword to inventory");
            }
        } else {
            // Fallback: auto-equip if no inventory
            java.util.Random rand = new java.util.Random();
            int boost = 10 + rand.nextInt(21); // 10-30 attack boost
            setEquippedStats(equippedAttack + boost, equippedDefense);
            System.out.println("Auto-picked up sword, attack increased by " + boost);
        }
    }

    private boolean isFacingNPC(NPC npc) {
        double dx = npc.getX() - px;
        double dy = npc.getY() - py;

        // Determine the direction from player to NPC
        int npcDirection;
        if (Math.abs(dx) > Math.abs(dy)) {
            // Horizontal direction is dominant
            npcDirection = (dx > 0) ? RIGHT : LEFT;
        } else {
            // Vertical direction is dominant
            npcDirection = (dy > 0) ? DOWN : UP;
        }

        // Check if player's current direction matches the direction to NPC
        // Allow some tolerance for diagonal cases
        return currentDirection == npcDirection ||
               (currentDirection == UP_LEFT && (npcDirection == UP || npcDirection == LEFT)) ||
               (currentDirection == UP_RIGHT && (npcDirection == UP || npcDirection == RIGHT)) ||
               (currentDirection == DOWN_LEFT && (npcDirection == DOWN || npcDirection == LEFT)) ||
               (currentDirection == DOWN_RIGHT && (npcDirection == DOWN || npcDirection == RIGHT));
    }

    private void startDialogue(NPC npc) {
        conversationCount++;
        List<String> dialogueLines = new ArrayList<>();

        if ("Yorme".equals(npc.getName())) {
            if (conversationCount == 1) {
                // First conversation
                dialogueLines.add("Old Man: Welcome, young adventurer! The world is full of dangers.");
                dialogueLines.add("Old Man: You look like you could use some guidance...");
                dialogueLines.add("Old Man: Remember, not everything is as it seems in these lands.");
                dialogueLines.add("Old Man: Stay vigilant and trust your instincts!");
            } else if (conversationCount == 2) {
                // Second conversation - different dialogue
                dialogueLines.add("Old Man: Back again, are you? That's good.");
                dialogueLines.add("Old Man: The path ahead grows more treacherous.");
                dialogueLines.add("Old Man: Have you discovered the ancient ruins yet?");
                dialogueLines.add("Old Man: Be careful not to awaken what sleeps there.");
            } else {
                // Subsequent conversations - more dialogue
                dialogueLines.add("Old Man: Ah, you've returned once more.");
                dialogueLines.add("Old Man: The shadows grow longer with each passing day.");
                dialogueLines.add("Old Man: Remember what I told you before...");
                dialogueLines.add("Old Man: Trust no one, question everything.");
            }
        } else {
            dialogueLines.add(npc.getName() + ": Hello there!");
            dialogueLines.add(npc.getName() + ": What brings you to these parts?");
        }

        if (dialogueUI != null) {
            ((DialogueUI) dialogueUI).startDialogue(npc.getName(), dialogueLines);
        }
    }

    public void updateDialogue() {
        if (dialogueTimer > 0) {
            dialogueTimer--;
            if (dialogueTimer == 0) {
                dialogueText = null;
            }
        }
    }


}
