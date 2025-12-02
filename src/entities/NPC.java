package entities;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;

public class NPC {

    // Use doubles for precise position tracking
    private double x, y;
    public int width, height;
    private BufferedImage sprite;
    private BufferedImage[] downFrames;
    private BufferedImage[] upFrames;
    private BufferedImage[] leftFrames;
    private BufferedImage[] rightFrames;
    private int currentFrame = 0;
    private int frameDelay = 20;
    private int frameTimer = 0;
    private String direction = "down"; // default direction

    // Movement
    private double speed = 1.0; // Increased speed for more visible movement
    private int directionChangeTimer = 0;
    private int directionChangeInterval = 180; // Change direction every 3 seconds at 60 FPS
    private int collisionCooldown = 0; // Prevent rapid direction changes on collision
    private int collisionCooldownMax = 60; // 1 second cooldown after collision
    private tile.TileManager tileM;
    private String[] loopDirections = {"right", "down", "left", "up"};
    private int loopIndex = 0;
    private String name = "YORME";

    // Mission indicator animation
    private BufferedImage[] missionFrames;
    private int missionFrameIndex = 0;
    private int missionFrameDelay = 15; // Animation speed
    private int missionFrameTimer = 0;
    private Object player; // Reference to player for conversation tracking

    private void loadSprites() {
        try {
            // Load down frames
            downFrames = new BufferedImage[2];
            downFrames[0] = ImageIO.read(getClass().getResourceAsStream("/assets/characters/NPC/oldman_down_1.png"));
            downFrames[1] = ImageIO.read(getClass().getResourceAsStream("/assets/characters/NPC/oldman_down_2.png"));

            // Load up frames
            upFrames = new BufferedImage[2];
            upFrames[0] = ImageIO.read(getClass().getResourceAsStream("/assets/characters/NPC/oldman_up_1.png"));
            upFrames[1] = ImageIO.read(getClass().getResourceAsStream("/assets/characters/NPC/oldman_up_2.png"));

            // Load left frames
            leftFrames = new BufferedImage[2];
            leftFrames[0] = ImageIO.read(getClass().getResourceAsStream("/assets/characters/NPC/oldman_left_1.png"));
            leftFrames[1] = ImageIO.read(getClass().getResourceAsStream("/assets/characters/NPC/oldman_left_2.png"));

            // Load right frames
            rightFrames = new BufferedImage[2];
            rightFrames[0] = ImageIO.read(getClass().getResourceAsStream("/assets/characters/NPC/oldman_right_1.png"));
            rightFrames[1] = ImageIO.read(getClass().getResourceAsStream("/assets/characters/NPC/oldman_right_2.png"));

            sprite = downFrames[0]; // default image

            // Load mission indicator frames (128x32, 4 frames in a single row)
            BufferedImage missionSpriteSheet = ImageIO.read(getClass().getResourceAsStream("/assets/ui/mission_check.png"));
            if (missionSpriteSheet != null) {
                missionFrames = new BufferedImage[4];
                int frameWidth = 32; // 128 / 4 = 32
                int frameHeight = 32;
                for (int i = 0; i < 4; i++) {
                    missionFrames[i] = missionSpriteSheet.getSubimage(i * frameWidth, 0, frameWidth, frameHeight);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public NPC(int x, int y) {
        this.x = (double) x;
        this.y = (double) y;
        this.width = 96; // Set to 256x256 size
        this.height = 96; // Set to 256x256 size
        loadSprites();
    }

    public void setTileManager(tile.TileManager tileM) {
        this.tileM = tileM;
    }

    // Set player reference for conversation tracking
    public void setPlayer(Object player) {
        this.player = player;
    }

    // Check if mission indicator should be shown
    private boolean shouldShowMissionIndicator() {
        if (player == null) return false;

        try {
            // Use reflection to check if conversations are completed
            return !(Boolean) player.getClass().getMethod("hasCompletedAllConversationsWith", String.class)
                    .invoke(player, name);
        } catch (Exception e) {
            // If reflection fails, default to showing indicator
            return true;
        }
    }

    public void update() {
        // Update collision cooldown
        if (collisionCooldown > 0) {
            collisionCooldown--;
        }

        // Handle direction changes (only if not in collision cooldown)
        if (collisionCooldown == 0) {
            directionChangeTimer++;
            if (directionChangeTimer >= directionChangeInterval) {
                directionChangeTimer = 0;
                // Choose next direction in loop
                direction = loopDirections[loopIndex];
                loopIndex = (loopIndex + 1) % loopDirections.length;
            }
        }

        // Always try to move in current direction (continuous movement)
        double proposedX = x;
        double proposedY = y;
        switch (direction) {
            case "up": proposedY -= speed; break;
            case "down": proposedY += speed; break;
            case "left": proposedX -= speed; break;
            case "right": proposedX += speed; break;
        }

        // Check collision with player before moving
        Rectangle proposedBounds = new Rectangle((int) proposedX, (int) proposedY, width, height);
        boolean canMove = true;

        // Check tile collision
        if (tileM != null) {
            int checkX = (int) proposedX;
            int checkY = (int) proposedY;
            if (!tileM.isWalkable(checkX, checkY, width, height)) {
                // Tile collision detected, can't move
                canMove = false;
                // Immediately change direction when collision detected
                directionChangeTimer = 0; // Reset timer
                direction = loopDirections[loopIndex]; // Change to next direction immediately
                loopIndex = (loopIndex + 1) % loopDirections.length;
                collisionCooldown = 10; // Much shorter cooldown (0.17 seconds at 60 FPS)
            }
        }

        // Check player collision (if player reference exists)
        if (player != null && canMove) {
            try {
                // Get player position using reflection
                int playerX = (Integer) player.getClass().getMethod("getX").invoke(player);
                int playerY = (Integer) player.getClass().getMethod("getY").invoke(player);

                // Create player collision bounds (48x48 centered on player)
                Rectangle playerBounds = new Rectangle(playerX - 24, playerY - 24, 48, 48);

                if (proposedBounds.intersects(playerBounds)) {
                    canMove = false;
                    // Immediately change direction when player collision detected
                    directionChangeTimer = 0; // Reset timer
                    direction = loopDirections[loopIndex]; // Change to next direction immediately
                    loopIndex = (loopIndex + 1) % loopDirections.length;
                    collisionCooldown = 10; // Much shorter cooldown (0.17 seconds at 60 FPS)
                }
            } catch (Exception e) {
                // If reflection fails, allow movement
            }
        }

        // Apply movement if no collision
        if (canMove) {
            x = proposedX;
            y = proposedY;

            // Animation update (walking animation)
            frameTimer++;
            if (frameTimer >= frameDelay) {
                currentFrame = (currentFrame + 1) % 2; // only 2 frames
                frameTimer = 0;
            }
        }

        updateSprite();

        // Update mission indicator animation
        if (shouldShowMissionIndicator() && missionFrames != null && missionFrames.length > 0) {
            missionFrameTimer++;
            if (missionFrameTimer >= missionFrameDelay) {
                missionFrameIndex = (missionFrameIndex + 1) % missionFrames.length;
                missionFrameTimer = 0;
            }
        }
    }

    private void updateSprite() {
        switch (direction) {
            case "down":
                sprite = downFrames[currentFrame];
                break;
            case "up":
                sprite = upFrames[currentFrame];
                break;
            case "left":
                sprite = leftFrames[currentFrame];
                break;
            case "right":
                sprite = rightFrames[currentFrame];
                break;
        }
    }

    public void draw(Graphics g, int screenX, int screenY) {
        // Draw the sprite
        g.drawImage(sprite, screenX, screenY, width, height, null);

        // Draw mission indicator above the NPC if available
        if (shouldShowMissionIndicator() && missionFrames != null && missionFrameIndex < missionFrames.length) {
            int indicatorX = screenX + (width - 32) / 2; // Center the 32x32 indicator
            int indicatorY = screenY - 60; // Position above the NPC
            g.drawImage(missionFrames[missionFrameIndex], indicatorX, indicatorY, 32, 32, null);
        }

        // Draw name above the NPC
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 12));
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(name);
        int textX = screenX + (width - textWidth) / 2;
        int textY = screenY - 5;
        g.drawString(name, textX, textY);
    }

    public Rectangle getBounds() {
        return new Rectangle((int) x, (int) y, width, height);
    }

    public int getX() {
        return (int) x;
    }

    public int getY() {
        return (int) y;
    }

    public String getName() {
        return name;
    }
}
