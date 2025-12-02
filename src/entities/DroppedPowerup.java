package entities;

import java.awt.Graphics;
import java.awt.Color;

public class DroppedPowerup extends WorldObject {
    private String powerupType; // "potion_red" or "potion_blue"
    private int animationFrame = 0;
    private int animationTimer = 0;
    private final int ANIMATION_SPEED = 5; // frames between animation updates
    private final int BOB_HEIGHT = 10; // pixels to bob up and down
    private int baseY; // Original Y position for bobbing animation

    public DroppedPowerup(int x, int y, String powerupType) {
        // Create a placeholder object without loading an image
        super(x, y, "", "Dropped Powerup: " + powerupType, false); // No collision for pickups
        this.powerupType = powerupType;
        this.baseY = y;
        
        // Set size for the placeholder
        this.width = 32;
        this.height = 32;
        this.displayWidth = 32;
        this.displayHeight = 32;
        this.image = null; // No image, we'll draw it manually
    }

    public void update() {
        // Update animation
        animationTimer++;
        if (animationTimer >= ANIMATION_SPEED) {
            animationFrame = (animationFrame + 1) % 4; // 4 frame animation
            animationTimer = 0;
        }

        // Bob up and down
        double bobOffset = Math.sin((animationFrame / 4.0) * Math.PI * 2) * BOB_HEIGHT;
        this.y = (int)(baseY + bobOffset);
    }

    @Override
    public void draw(Graphics g, int cameraX, int cameraY) {
        drawScaled(g, cameraX, cameraY, 1.0f);
    }

    @Override
    public void drawScaled(Graphics g, int cameraX, int cameraY, float scaleFactor) {
        int screenX = x - cameraX;
        int screenY = y - cameraY;
        int size = (int)(32 * scaleFactor);

        // Draw based on powerup type
        if ("potion_red".equals(powerupType)) {
            // Red potion - HP restore
            drawRedPotion(g, screenX, screenY, size);
        } else if ("potion_blue".equals(powerupType)) {
            // Blue potion - Mana restore
            drawBluePotion(g, screenX, screenY, size);
        }

        // Draw a glow effect
        drawGlow(g, screenX, screenY, size);
    }

    private void drawRedPotion(Graphics g, int x, int y, int size) {
        // Draw bottle shape
        int bottleWidth = size / 2;
        int bottleHeight = (int)(size * 0.7);
        int bottleX = x + (size - bottleWidth) / 2;
        int bottleY = y + (size - bottleHeight) / 2;

        // Bottle body (red)
        g.setColor(new Color(200, 50, 50)); // Dark red
        g.fillRect(bottleX, bottleY, bottleWidth, bottleHeight);

        // Bottle highlight (bright red)
        g.setColor(new Color(255, 100, 100)); // Bright red
        g.fillRect(bottleX + 2, bottleY + 2, bottleWidth / 3, bottleHeight / 2);

        // Bottle cap (brown)
        g.setColor(new Color(139, 69, 19)); // Brown
        g.fillRect(bottleX + bottleWidth / 4, bottleY - size / 8, bottleWidth / 2, size / 8);

        // Liquid inside (bright red)
        g.setColor(new Color(255, 150, 150)); // Light red
        g.fillRect(bottleX + 1, bottleY + bottleHeight / 2, bottleWidth - 2, bottleHeight / 2 - 1);
    }

    private void drawBluePotion(Graphics g, int x, int y, int size) {
        // Draw bottle shape
        int bottleWidth = size / 2;
        int bottleHeight = (int)(size * 0.7);
        int bottleX = x + (size - bottleWidth) / 2;
        int bottleY = y + (size - bottleHeight) / 2;

        // Bottle body (dark blue)
        g.setColor(new Color(50, 100, 200)); // Dark blue
        g.fillRect(bottleX, bottleY, bottleWidth, bottleHeight);

        // Bottle highlight (bright blue)
        g.setColor(new Color(100, 150, 255)); // Bright blue
        g.fillRect(bottleX + 2, bottleY + 2, bottleWidth / 3, bottleHeight / 2);

        // Bottle cap (brown)
        g.setColor(new Color(139, 69, 19)); // Brown
        g.fillRect(bottleX + bottleWidth / 4, bottleY - size / 8, bottleWidth / 2, size / 8);

        // Liquid inside (bright blue)
        g.setColor(new Color(150, 200, 255)); // Light blue
        g.fillRect(bottleX + 1, bottleY + bottleHeight / 2, bottleWidth - 2, bottleHeight / 2 - 1);
    }

    private void drawGlow(Graphics g, int x, int y, int size) {
        // Draw a subtle glow effect that pulses
        int glowSize = (int)(size * 1.3);
        int glowX = x + (size - glowSize) / 2;
        int glowY = y + (size - glowSize) / 2;

        // Pulsing glow based on animation frame
        int alpha = 50 + (animationFrame * 30); // 50-170 alpha
        g.setColor(new Color(255, 255, 200, Math.min(alpha, 255))); // Yellow glow

        // Draw glow circle (approximated with rectangles)
        g.drawOval(glowX, glowY, glowSize, glowSize);
    }

    public String getPowerupType() {
        return powerupType;
    }

    public int getBaseY() {
        return baseY;
    }
}
