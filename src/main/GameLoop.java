package main;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*; // Import AWT event classes for KeyAdapter and KeyEvent
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import input.KeyHandler;
import entities.Enemy;
import entities.Player;
import entities.SlashAttack;
import entities.SkillWAttack;
import entities.InventoryUI;
import entities.Hotbar;
import entities.NPC;
import entities.DialogueUI;

import tile.TileManager;

public class GameLoop extends JLayeredPane implements Runnable {

    private int WIDTH = 800;
    private int HEIGHT = 600;
    final int TILE_SIZE = 48; // Consistent tile size

    private boolean inventoryOpen = false; // To track inventory state
    private InventoryUI gameInventory; // The inventory panel
    private DialogueUI dialogueUI; // The dialogue panel

    private Thread gameThread;
    private KeyHandler keyH;
    private Player player;
    private TileManager tileM; // Tile manager for rendering tiles
    private Hotbar hotbar;
    private List<Enemy> enemies;
    private List<NPC> npcs;
    private GameOverCallback gameOverCallback; // Callback for game over

    // Skill icons
    private Image skillIcePiercerIcon;
    private Image skillLightningStormIcon;
    private Image skillFireSplashIcon;

    public GameLoop(GameOverCallback gameOverCallback) { // Modified constructor
        this.gameOverCallback = gameOverCallback;

        this.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        this.setBackground(Color.BLACK); // Set background to black to remove white
        this.setDoubleBuffered(true);

        keyH = new KeyHandler();
        this.setFocusable(true);
        this.requestFocusInWindow();
        this.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (dialogueUI.isDialogueVisible()) {
                    dialogueUI.handleKeyPress(e.getKeyCode());
                } else {
                    keyH.keyPressed(e);
                }
            }
            @Override
            public void keyReleased(KeyEvent e) {
                if (!dialogueUI.isDialogueVisible()) {
                    keyH.keyReleased(e);
                }
            }
        });

        setupKeyBindings();

        // Initialize TileManager first
        tileM = new TileManager(this);

        // Initialize player with KeyHandler (start in walkable grass area)
        player = new Player(336, 336, keyH); // Position (336,336) = center of open grass area away from obstacles
        player.setTileManager(tileM); // Pass TileManager reference for collision

        // Initialize inventory with callback for item usage
        gameInventory = new InventoryUI(WIDTH, HEIGHT, itemId -> {
            if ("potion_blue".equals(itemId)) {
                // Mana potion: restore 50 mana
                int oldMana = player.getMana();
                player.restoreMana(50);
                System.out.println("Used Mana Potion: Mana " + oldMana + " -> " + player.getMana());
            }
            // Add other consumable effects here as needed
        });

        // Initialize dialogue UI
        dialogueUI = new DialogueUI(WIDTH, HEIGHT);
        dialogueUI.setBounds(0, 0, WIDTH, HEIGHT);
        dialogueUI.setVisible(false);
        this.add(dialogueUI, JLayeredPane.MODAL_LAYER);

        // Initialize hotbar
        hotbar = new Hotbar(WIDTH, HEIGHT, gameInventory);
        hotbar.setPlayer(player); // Set player reference for cooldown access
        gameInventory.setBounds(0, 0, WIDTH, HEIGHT);
        gameInventory.setVisible(false);
        this.add(gameInventory, JLayeredPane.PALETTE_LAYER);

        // Initialize enemies below the wall in lower map area
        enemies = new ArrayList<>();
        enemies.add(new Enemy(600, 800)); // Lower map position 1 - below wall
        enemies.add(new Enemy(700, 850)); // Lower map position 2 - below wall

        // Set tile manager for enemies for collision detection
        for (Enemy enemy : enemies) {
            enemy.setTileManager(tileM);
        }

        // Initialize NPCs
        npcs = new ArrayList<>();
        npcs.add(new NPC(356, 224)); // Place old man at row 13, column 22
        npcs.get(0).setTileManager(tileM); // Pass TileManager reference for collision
        player.setNPCs(npcs); // Pass NPCs reference to player for collision detection
        player.setDialogueUI(dialogueUI); // Pass DialogueUI reference to player
        dialogueUI.setKeyHandler(keyH); // Pass KeyHandler reference to DialogueUI

        // Load skill icons
        loadSkillIcons();
    }

    public void start() {
        startGameThread();
    }

    public void reset() {
        // Reset player state
        player = new Player(336, 336, keyH); // Re-initialize player at open grass area, full HP
        player.setTileManager(tileM); // Re-set TileManager reference

        // Reset inventory (if necessary, clear items or reset state)
        gameInventory.reset();

        // Reset any other game state variables
        inventoryOpen = false;
        gameInventory.setVisible(false);

        // Ensure gameThread is stopped before restarting, or handle appropriately
        if (gameThread != null) {
            gameThread = null; // Signal thread to stop
        }
    }

    // Update window size for responsive layout
    public void updateWindowSize(int newWidth, int newHeight) {
        this.WIDTH = newWidth;
        this.HEIGHT = newHeight;

        // Update preferred size
        this.setPreferredSize(new Dimension(WIDTH, HEIGHT));

        // Update UI components that depend on window size
        if (gameInventory != null) {
            gameInventory.updateSize(WIDTH, HEIGHT);
        }
        if (hotbar != null) {
            hotbar.updateSize(WIDTH, HEIGHT);
        }
        if (dialogueUI != null) {
            dialogueUI.updateSize(WIDTH, HEIGHT);
        }

        // Force revalidation and repaint
        this.revalidate();
        this.repaint();
    }

    private void setupKeyBindings() {
        InputMap im = this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = this.getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_I, 0), "toggleInventory");
        am.put("toggleInventory", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleInventory();
            }
        });
    }

    private void toggleInventory() {
        inventoryOpen = !inventoryOpen;
        gameInventory.setVisible(inventoryOpen);
        if (inventoryOpen) {
            gameInventory.requestFocusInWindow(); // Give focus to inventory for hotbar input
        } else {
            this.requestFocusInWindow(); // Return focus to game loop
        }
    }



    @Override
    public void addNotify() {
        super.addNotify();
        // Removed requestFocusInWindow here to allow Main class to manage focus
    }

    public void startGameThread() {
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public void run() {
        double drawInterval = 1000000000.0 / 60.0; // 60 FPS
        double delta = 0;
        long lastTime = System.nanoTime();

        while (gameThread != null) {
            long now = System.nanoTime();
            delta += (now - lastTime) / drawInterval;
            lastTime = now;

            if (delta >= 1) {
                update();
                repaint();
                delta--;
            }
        }
    }

    public void update() {
        if (inventoryOpen || dialogueUI.isDialogueVisible()) {
            // If inventory is open or dialogue is active, pause game updates
            return;
        }

        float deltaTime = 1.0f / 60.0f;

        // Calculate camera position for enemy AI
        int cameraX = (int) player.px - WIDTH / 2;
        int cameraY = (int) player.py - HEIGHT / 2;
        // Clamp camera to map bounds
        int mapPixelWidth = tileM.getMapWidth() * TILE_SIZE;
        int mapPixelHeight = tileM.getMapHeight() * TILE_SIZE;
        cameraX = Math.max(0, Math.min(cameraX, mapPixelWidth - WIDTH));
        cameraY = Math.max(0, Math.min(cameraY, mapPixelHeight - HEIGHT));

        // Update player
        player.update(deltaTime);
        player.updateDialogue();

        // Update enemies - pass camera viewport info for AI
        for (Enemy enemy : enemies) {
            enemy.update(player.getX(), player.getY(), player, cameraX, cameraY, WIDTH, HEIGHT);
        }

        // Update NPCs
        for (NPC npc : npcs) {
            npc.update();
        }

        // Check for slash attack collisions with enemies
        for (SlashAttack slash : player.getSlashes()) {
            for (Enemy enemy : enemies) {
                if (enemy.isAlive() && slash.getBounds().intersects(enemy.getBounds()) && !slash.hasHit(enemy)) {
                    enemy.takeDamage(slash.getDamage());
                    slash.addHitEnemy(enemy);
                    System.out.println("Slash dealt " + slash.getDamage() + " damage to enemy!");
                }
            }
        }

        // Check for SkillW attack collisions with enemies
        for (SkillWAttack skillW : player.getSkillWAttacks()) {
            for (Enemy enemy : enemies) {
                if (enemy.isAlive() && skillW.getBounds().intersects(enemy.getBounds()) && !skillW.hasHit(enemy)) {
                    enemy.takeDamage(skillW.getDamage());
                    skillW.addHitEnemy(enemy);
                    System.out.println("SkillW dealt " + skillW.getDamage() + " damage to enemy!");
                }
            }
        }

        // Handle freeze skill
        Rectangle freezeArea = player.getFreezeArea();
        if (freezeArea != null) {
            for (Enemy enemy : enemies) {
                if (enemy.isAlive() && freezeArea.intersects(enemy.getBounds())) {
                    enemy.freeze(60); // Freeze for 1 second (60 frames at 60 FPS)
                    enemy.takeDamage(player.getTotalAttack()); // Inflict damage
                    System.out.println("Ice Piercer dealt " + player.getTotalAttack() + " damage to enemy!");
                }
            }
            player.clearFreezeArea();
        }

        // Handle lightning storm skill
        Rectangle lightningArea = player.getLightningArea();
        if (lightningArea != null) {
            for (Enemy enemy : enemies) {
                if (enemy.isAlive() && lightningArea.intersects(enemy.getBounds())) {
                    enemy.takeDamage(player.getTotalAttack() * 2); // Inflict double damage
                    System.out.println("Lightning Storm dealt " + (player.getTotalAttack() * 2) + " damage to enemy!");
                }
            }
            player.clearLightningArea();
        }

        // Check if player is dead
        if (!player.isAlive() && player.isDeathAnimationFinished()) {
            gameThread = null; // Stop the game loop
            // Capture the current screen as a BufferedImage
            BufferedImage screenshot = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = screenshot.createGraphics();
            paintComponent(g2d); // Render the current game state to the screenshot
            g2d.dispose();

            gameOverCallback.onGameOver(screenshot); // Trigger game over screen with screenshot
            return; // Skip further updates
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); // Call super.paintComponent for JLayeredPane
        Graphics2D g2d = (Graphics2D) g;

        // Only draw game world if inventory is not open (or draw behind it)
        // If inventory is a JInternalFrame, it will manage its own painting over the background.
        // For a simple JPanel, we draw the game world first.


        // More aggressive zoom for large windossssssws
        float zoomFactor = (WIDTH > 1400 || HEIGHT > 900) ? 0.7f : 1.2f;
        int viewportWidth = (int) (WIDTH * zoomFactor);
        int viewportHeight = (int) (HEIGHT * zoomFactor);

        // Calculate camera position to center on the player with zoom
        int cameraX = (int) player.px - viewportWidth / 2;
        int cameraY = (int) player.py - viewportHeight / 2;

        // Clamp camera to map bounds to prevent white background
        int mapPixelWidth = tileM.getMapWidth() * TILE_SIZE;
        int mapPixelHeight = tileM.getMapHeight() * TILE_SIZE;
        cameraX = Math.max(0, Math.min(cameraX, mapPixelWidth - viewportWidth));
        cameraY = Math.max(0, Math.min(cameraY, mapPixelHeight - viewportHeight));

        // Draw tiles using TileManager with camera offset
        tileM.draw(g2d, cameraX, cameraY, WIDTH, HEIGHT);

        // Adjust player's draw position based on camera
        int playerScreenX = (int) player.px - cameraX; // Use player.px
        int playerScreenY = (int) player.py - cameraY; // Use player.py

        // Draw player
        player.draw(g, playerScreenX, playerScreenY);

        // Draw enemies
        for (Enemy enemy : enemies) {
            int enemyScreenX = enemy.getX() - cameraX;
            int enemyScreenY = enemy.getY() - cameraY;
            enemy.draw(g, enemyScreenX, enemyScreenY, player);
        }

        // Draw NPCs
        for (NPC npc : npcs) {
            int npcScreenX = npc.getX() - cameraX;
            int npcScreenY = npc.getY() - cameraY;
            npc.draw(g, npcScreenX, npcScreenY);
        }

        // === SKILL ANIMATIONS ===
        // Draw Slash Q skill attacks
        for (SlashAttack s : player.getSlashes()) {
            int slashScreenX = s.x - cameraX;
            int slashScreenY = s.y - cameraY;
            s.draw(g, slashScreenX, slashScreenY);
        }
        // Draw Skill W attacks
        for (SkillWAttack s : player.getSkillWAttacks()) {
            // Assuming SkillWAttack also needs screen coordinates
            int skillWScreenX = s.x - cameraX;
            int skillWScreenY = s.y - cameraY;
            s.draw(g, skillWScreenX, skillWScreenY);
        }

        // Draw hotbar (now shows skill items from inventory)
        hotbar.draw(g2d);

        // Do not dispose g2d here as JLayeredPane might manage its own children's painting.
        // The dispose will be called automatically by the Swing system.
    }

    private void drawHotbarKeys(Graphics2D g2d) {
        int slotSize = 48;
        int numSlots = 3; // Reduced from 5 to 3 skill slots
        int hotbarWidth = numSlots * slotSize;
        int hotbarX = (WIDTH - hotbarWidth) / 2; // Centers the 3 slots horizontally
        int hotbarY = HEIGHT - slotSize - 10;

        // Draw skill icons and cooldown overlays
        for (int i = 0; i < numSlots; i++) {
            int slotX = hotbarX + i * slotSize;
            int slotY = hotbarY;

            boolean onCooldown = false;
            float cooldownProgress = 0f;

            int cooldownMax = 0;
            if (i == 0) { // B - Fire Splash
                drawFireIcon(g2d, slotX, slotY, slotSize);
                if (player.getBCooldown() > 0) {
                    onCooldown = true;
                    cooldownProgress = (float) player.getBCooldown() / player.getBCooldownMax();
                    cooldownMax = player.getBCooldownMax();
                }
            } else if (i == 1) { // N - Ice Piercer
                drawIceIcon(g2d, slotX, slotY, slotSize);
                if (player.getNCooldown() > 0) {
                    onCooldown = true;
                    cooldownProgress = (float) player.getNCooldown() / player.getNCooldownMax();
                    cooldownMax = player.getNCooldownMax();
                }
            } else if (i == 2) { // M - Lightning Storm
                drawLightningIcon(g2d, slotX, slotY, slotSize);
                if (player.getMCooldown() > 0) {
                    onCooldown = true;
                    cooldownProgress = (float) player.getMCooldown() / player.getMCooldownMax();
                    cooldownMax = player.getMCooldownMax();
                }
            }

            // Draw cooldown overlay
            if (onCooldown) {
                g2d.setColor(new Color(0, 0, 0, 150));
                g2d.fillRect(slotX, slotY, slotSize, (int) (slotSize * cooldownProgress));

                // Draw cooldown text
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Arial", Font.BOLD, 10));
                String timeLeft = String.format("%.1f", cooldownProgress * (cooldownMax / 60.0f));
                FontMetrics fm = g2d.getFontMetrics();
                int textWidth = fm.stringWidth(timeLeft);
                int textX = slotX + (slotSize - textWidth) / 2;
                int textY = slotY + slotSize / 2 + fm.getAscent() / 2;
                g2d.drawString(timeLeft, textX, textY);
            }
        }
    }

    private void drawFireIcon(Graphics2D g2d, int x, int y, int size) {
        if (skillFireSplashIcon != null) {
            // Draw the 32x32 image centered in the 48x48 slot
            g2d.drawImage(skillFireSplashIcon, x + 8, y + 8, 32, 32, null);
        } else {
            // Fallback: Draw a simple red flame/triangle
            int[] xPoints = {x + size/2, x + size/4, x + 3*size/4};
            int[] yPoints = {y + size/4, y + 3*size/4, y + 3*size/4};
            g2d.setColor(Color.RED);
            g2d.fillPolygon(xPoints, yPoints, 3);

            // Add some orange outline
            g2d.setColor(Color.ORANGE);
            g2d.drawPolygon(xPoints, yPoints, 3);
        }
    }

    private void drawIceIcon(Graphics2D g2d, int x, int y, int size) {
        if (skillIcePiercerIcon != null) {
            // Draw the 32x32 image centered in the 48x48 slot
            g2d.drawImage(skillIcePiercerIcon, x + 8, y + 8, 32, 32, null);
        } else {
            // Fallback: Draw a simple blue snowflake/diamond
            int centerX = x + size/2;
            int centerY = y + size/2;
            int radius = size/3;

            g2d.setColor(Color.BLUE);
            // Draw diamond shape
            int[] xPoints = {centerX, centerX + radius/2, centerX, centerX - radius/2};
            int[] yPoints = {centerY - radius, centerY, centerY + radius, centerY};
            g2d.fillPolygon(xPoints, yPoints, 4);

            // Add white highlights
            g2d.setColor(Color.WHITE);
            g2d.drawLine(centerX - radius/2, centerY, centerX + radius/2, centerY);
            g2d.drawLine(centerX, centerY - radius, centerX, centerY + radius);
        }
    }

    private void drawLightningIcon(Graphics2D g2d, int x, int y, int size) {
        if (skillLightningStormIcon != null) {
            // Draw the 32x32 image centered in the 48x48 slot
            g2d.drawImage(skillLightningStormIcon, x + 8, y + 8, 32, 32, null);
        } else {
            // Fallback: Draw a simple yellow lightning bolt
            g2d.setColor(Color.YELLOW);
            int[] xPoints = {x + size/3, x + size/2, x + 2*size/3, x + size/2};
            int[] yPoints = {y + size/4, y + size/4, y + size/2, y + 3*size/4};
            g2d.fillPolygon(xPoints, yPoints, 4);

            // Add black outline
            g2d.setColor(Color.BLACK);
            g2d.drawPolyline(xPoints, yPoints, 4);
        }
    }

    private void drawBackgroundBlur(Graphics2D g2d) {
        // Draw a semi-transparent dark overlay over the entire screen to create blur effect
        g2d.setColor(new Color(0, 0, 0, 120)); // Semi-transparent black overlay
        g2d.fillRect(0, 0, WIDTH, HEIGHT);
    }

    private void drawDialogue(Graphics2D g2d, String text) {
        // Draw dialogue box at the bottom of the screen
        int boxWidth = WIDTH - 40;
        int boxHeight = 80;
        int boxX = 20;
        int boxY = HEIGHT - boxHeight - 20;

        // Semi-transparent background
        g2d.setColor(new Color(0, 0, 0, 200));
        g2d.fillRect(boxX, boxY, boxWidth, boxHeight);

        // Border
        g2d.setColor(Color.WHITE);
        g2d.drawRect(boxX, boxY, boxWidth, boxHeight);

        // Dialogue text
        g2d.setFont(new Font("Arial", Font.PLAIN, 16));
        FontMetrics fm = g2d.getFontMetrics();
        int textY = boxY + 25;
        // Split text if too long, but for now just draw it
        g2d.drawString(text, boxX + 10, textY);
    }

    private void loadSkillIcons() {
        try {
            skillIcePiercerIcon = new ImageIcon(getClass().getResource("/assets/ui/skill_icepiercer.png")).getImage();
            skillLightningStormIcon = new ImageIcon(getClass().getResource("/assets/ui/skill_lightningstorm.png")).getImage();
            skillFireSplashIcon = new ImageIcon(getClass().getResource("/assets/ui/skill_firesplash.png")).getImage();
        } catch (Exception e) {
            System.err.println("Failed to load skill icons: " + e.getMessage());
            // Fallback to programmatically drawn icons if images fail to load
            skillIcePiercerIcon = null;
            skillLightningStormIcon = null;
            skillFireSplashIcon = null;
        }
    }

    // Define a functional interface for the game over callback
    public interface GameOverCallback {
        void onGameOver(BufferedImage screenshot);
    }
}
