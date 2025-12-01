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
    private double speed = 0.5;
    private int walkTimer = 0;
    private int walkDuration = 120; // frames to walk in one direction
    private boolean isWalking = false;
    private tile.TileManager tileM;
    private String[] loopDirections = {"right", "down", "left", "up"};
    private int loopIndex = 0;
    private String name = "Yorme";

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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public NPC(int x, int y) {
        this.x = (double) x;
        this.y = (double) y;
        this.width = 48; // assuming tile size
        this.height = 48;
        loadSprites();
    }

    public void setTileManager(tile.TileManager tileM) {
        this.tileM = tileM;
    }

    public void update() {
        // Handle walking logic
        walkTimer++;
        if (walkTimer >= walkDuration) {
            walkTimer = 0;
            isWalking = !isWalking; // toggle walking/idle
            if (isWalking) {
                // Choose next direction in loop
                direction = loopDirections[loopIndex];
                loopIndex = (loopIndex + 1) % loopDirections.length;
            }
        }

        if (isWalking) {
            // Calculate new position
            switch (direction) {
                case "up": y -= speed; break;
                case "down": y += speed; break;
                case "left": x -= speed; break;
                case "right": x += speed; break;
            }

            // Simple animation update
            frameTimer++;
            if (frameTimer >= frameDelay) {
                currentFrame = (currentFrame + 1) % 2; // only 2 frames
                frameTimer = 0;
            }
        } else {
            // Idle animation (slower)
            frameTimer++;
            if (frameTimer >= frameDelay * 2) {
                currentFrame = (currentFrame + 1) % 2;
                frameTimer = 0;
            }
        }

        updateSprite();
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
}
