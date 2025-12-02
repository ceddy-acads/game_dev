package entities;

import java.awt.Graphics;
import java.awt.Image;
import javax.imageio.ImageIO;
import java.io.IOException;

public class WorldObject {
    protected int x, y;
    protected int width, height;
    protected int displayWidth, displayHeight;
    protected Image image;
    protected String name;
    protected boolean collision;

    public WorldObject(int x, int y, String imagePath, String name, boolean collision) {
        this.x = x;
        this.y = y;
        this.name = name;
        this.collision = collision;

        try {
            this.image = ImageIO.read(getClass().getResourceAsStream(imagePath));
            if (this.image != null) {
                this.width = this.image.getWidth(null);
                this.height = this.image.getHeight(null);
                // Make objects larger - scale them up
                this.displayWidth = this.width * 2; // Double the size
                this.displayHeight = this.height * 2;
            }
        } catch (IOException e) {
            System.err.println("Failed to load object image: " + imagePath);
            this.image = null;
            this.width = 140; // Default tile size
            this.height = 160;
            this.displayWidth = 150; // Scaled up
            this.displayHeight = 150;
        }
    }

    public void draw(Graphics g, int cameraX, int cameraY) {
        drawScaled(g, cameraX, cameraY, 2.0f); // Default 2x scaling
    }

    public void drawScaled(Graphics g, int cameraX, int cameraY, float scaleFactor) {
        if (image != null) {
            int screenX = x - cameraX;
            int screenY = y - cameraY;
            int scaledWidth = (int)(width * scaleFactor);
            int scaledHeight = (int)(height * scaleFactor);
            g.drawImage(image, screenX, screenY, scaledWidth, scaledHeight, null);
        }
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public boolean hasCollision() { return collision; }
    public String getName() { return name; }
}
