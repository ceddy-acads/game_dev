package entities;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.List;

public class Hotbar {

    private int screenWidth;
    private int screenHeight;
    private final InventoryUI inventory;
    private Object player; // Reference to player for cooldowns
    private final int slotSize = 48;
    private final int slotSpacing = 8; // 8 pixels spacing between slots
    private final int numSlots = 3;
    private final int hotbarWidth = numSlots * slotSize + (numSlots - 1) * slotSpacing;
    private final int hotbarHeight = slotSize;
    private int hotbarX;
    private int hotbarY;

    public Hotbar(int screenWidth, int screenHeight, InventoryUI inventory) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.inventory = inventory;
        updatePosition();
    }

    // Set player reference for cooldown access
    public void setPlayer(Object player) {
        this.player = player;
    }

    private void updatePosition() {
        this.hotbarX = (screenWidth - hotbarWidth) / 2;
        this.hotbarY = screenHeight - hotbarHeight - 50;
    }

    public void draw(Graphics2D g2d) {
        // Draw hotbar background
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRect(hotbarX, hotbarY, hotbarWidth, hotbarHeight);

        // Draw hotbar slots with spacing
        g2d.setColor(Color.GRAY);
        for (int i = 0; i < numSlots; i++) {
            int slotX = hotbarX + i * (slotSize + slotSpacing);
            g2d.drawRect(slotX, hotbarY, slotSize, slotSize);
        }

        // Draw items in hotbar with spacing
        List<InventoryUI.Slot> slots = inventory.getInventorySlots();
        for (int i = 0; i < numSlots && i < slots.size(); i++) {
            InventoryUI.Slot slot = slots.get(i);
            if (slot.item != null) {
                int slotX = hotbarX + i * (slotSize + slotSpacing);
                g2d.drawImage(slot.item.getIcon().getImage(), slotX, hotbarY, slotSize, slotSize, null);

                // Draw cooldown overlay for skill items
                drawCooldownOverlay(g2d, i, slot.item.id, slotX, hotbarY, slotSize);
            }
        }
    }

    private void drawCooldownOverlay(Graphics2D g2d, int slotIndex, String itemId, int x, int y, int size) {
        if (player == null) return;

        boolean onCooldown = false;
        float cooldownProgress = 0f;
        int cooldownMax = 0;

        try {
            // Check cooldown based on item ID using reflection
            if ("skill_fire".equals(itemId)) {
                int cooldown = (Integer) player.getClass().getMethod("getBCooldown").invoke(player);
                if (cooldown > 0) {
                    onCooldown = true;
                    cooldownMax = (Integer) player.getClass().getMethod("getBCooldownMax").invoke(player);
                    cooldownProgress = (float) cooldown / cooldownMax;
                }
            } else if ("skill_ice".equals(itemId)) {
                int cooldown = (Integer) player.getClass().getMethod("getNCooldown").invoke(player);
                if (cooldown > 0) {
                    onCooldown = true;
                    cooldownMax = (Integer) player.getClass().getMethod("getNCooldownMax").invoke(player);
                    cooldownProgress = (float) cooldown / cooldownMax;
                }
            } else if ("skill_lightning".equals(itemId)) {
                int cooldown = (Integer) player.getClass().getMethod("getMCooldown").invoke(player);
                if (cooldown > 0) {
                    onCooldown = true;
                    cooldownMax = (Integer) player.getClass().getMethod("getMCooldownMax").invoke(player);
                    cooldownProgress = (float) cooldown / cooldownMax;
                }
            }

            // Draw cooldown overlay
            if (onCooldown) {
                g2d.setColor(new Color(0, 0, 0, 150));
                g2d.fillRect(x, y, size, (int) (size * cooldownProgress));

                // Draw cooldown text
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Arial", Font.BOLD, 10));
                String timeLeft = String.format("%.1f", cooldownProgress * (cooldownMax / 60.0f));
                java.awt.FontMetrics fm = g2d.getFontMetrics();
                int textWidth = fm.stringWidth(timeLeft);
                int textX = x + (size - textWidth) / 2;
                int textY = y + size / 2 + fm.getAscent() / 2;
                g2d.drawString(timeLeft, textX, textY);
            }
        } catch (Exception e) {
            // Silently fail if reflection fails
        }
    }

    // Update size for responsive layout
    public void updateSize(int newWidth, int newHeight) {
        this.screenWidth = newWidth;
        this.screenHeight = newHeight;
        updatePosition();
    }
}
