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
import world.ObjectManager;

public class GameLoop extends JLayeredPane implements Runnable {

    int width = 800;
    int height = 600;
    final int TILE_SIZE = 80; // Consistent tile size - matches TileManager

    private boolean inventoryOpen = false; // To track inventory state
    private InventoryUI gameInventory; // The inventory panel
    private DialogueUI dialogueUI; // The dialogue panel

    private Thread gameThread;
    private KeyHandler keyH;
    private Player player;
    private TileManager tileM; // Tile manager for rendering tiles
    private ObjectManager objectM; // Object manager for world objects
    private Hotbar hotbar;
    private List<Enemy> enemies;
    private List<NPC> npcs;
    private GameOverCallback gameOverCallback; // Callback for game over

    // Wave system
    private int currentWave = 0;
    private boolean waveActive = false;
    private boolean waitingForDialogue = false;
    private boolean miniBossSpawned = false;

    // Skill icons
    private Image skillIcePiercerIcon;
    private Image skillLightningStormIcon;
    private Image skillFireSplashIcon;

    public GameLoop(GameOverCallback gameOverCallback) { // Modified constructor
        this.gameOverCallback = gameOverCallback;

        this.setPreferredSize(new Dimension(width, height));
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

        // Initialize ObjectManager for world objects
        objectM = new ObjectManager(tileM);

        // Initialize player with KeyHandler (start in walkable grass area)
        player = new Player(400, 400, keyH); // Position (400,400) = center of open grass area away from obstacles
        player.setTileManager(tileM); // Pass TileManager reference for collision
        player.setObjectManager(objectM); // Pass ObjectManager reference for collision

        // Initialize inventory with callback for item usage
        gameInventory = new InventoryUI(this.width, this.height, itemId -> {
            if ("potion_blue".equals(itemId)) {
                // Mana potion: restore 50 mana
                int oldMana = player.getMana();
                player.restoreMana(50);
                System.out.println("Used Mana Potion: Mana " + oldMana + " -> " + player.getMana());
            }
            // Add other consumable effects here as needed
        });

        // Initialize dialogue UI
        dialogueUI = new DialogueUI(this.width, this.height);
        dialogueUI.setBounds(0, 0, this.width, this.height);
        dialogueUI.setVisible(false);
        this.add(dialogueUI, JLayeredPane.MODAL_LAYER);

        // Initialize hotbar
        hotbar = new Hotbar(this.width, this.height, gameInventory);
        hotbar.setPlayer(player); // Set player reference for cooldown access
        gameInventory.setBounds(0, 0, this.width, this.height);
        gameInventory.setVisible(false);
        this.add(gameInventory, JLayeredPane.PALETTE_LAYER);

        // Initialize enemies below the wall in lower map area
        enemies = new ArrayList<>();
        enemies.add(new Enemy(600, 800, Enemy.EnemyType.BASIC)); // Lower map position 1 - below wall
        enemies.add(new Enemy(700, 850, Enemy.EnemyType.BASIC)); // Lower map position 2 - below wall

        // Set tile manager, inventory, and object manager for enemies for collision detection and powerup drops
        for (Enemy enemy : enemies) {
            enemy.setTileManager(tileM);
            enemy.setInventory(gameInventory);
            enemy.setObjectManager(objectM);
        }

        // Initialize NPCs
        npcs = new ArrayList<>();
        npcs.add(new NPC(1200, 480)); // Place old man in open grass area (row 6, column 15)
        npcs.get(0).setTileManager(tileM); // Pass TileManager reference for collision
        npcs.get(0).setPlayer(player); // Pass player reference for conversation tracking
        player.setNPCs(npcs); // Pass NPCs reference to player for collision detection
        player.setDialogueUI(dialogueUI); // Pass DialogueUI reference to player
        dialogueUI.setKeyHandler(keyH); // Pass KeyHandler reference to DialogueUI

        // Set NPCs reference in ObjectManager to prevent objects from spawning near NPCs
        objectM.setNPCs(npcs);

        // Load skill icons
        loadSkillIcons();

        // Add component listener for resize
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                int newW = getWidth();
                int newH = getHeight();
                if (newW != width || newH != height) {
                    setSize(newW, newH);
                }
            }
        });
    }

    public void setSize(int w, int h) {
        this.width = w;
        this.height = h;
        setPreferredSize(new Dimension(w, h));
        gameInventory.setPreferredSize(new Dimension(w, h));
        hotbar.updateSize(w, h);
        dialogueUI.setPreferredSize(new Dimension(w, h));
        dialogueUI.setBounds(0, 0, w, h);
    }

    public Hotbar getHotbar() {
        return hotbar;
    }

    public void start() {
        startGameThread();
    }

    public void reset() {
        // Reset player state
        player = new Player(400, 400, keyH); // Re-initialize player at open grass area, full HP
        player.setTileManager(tileM); // Re-set TileManager reference
        player.setObjectManager(objectM); // Re-set ObjectManager reference

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
        this.width = newWidth;
        this.height = newHeight;

        // Update preferred size
        this.setPreferredSize(new Dimension(width, height));

        // Update UI components that depend on window size
        if (gameInventory != null) {
            gameInventory.updateSize(width, height);
        }
        if (hotbar != null) {
            hotbar.updateSize(width, height);
        }
        if (dialogueUI != null) {
            dialogueUI.updateSize(width, height);
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
        int cameraX = (int) player.px - this.width / 2;
        int cameraY = (int) player.py - this.height / 2;
        // Clamp camera to map bounds
        int mapPixelWidth = tileM.getMapWidth() * TILE_SIZE;
        int mapPixelHeight = tileM.getMapHeight() * TILE_SIZE;
        cameraX = Math.max(0, Math.min(cameraX, mapPixelWidth - this.width));
        cameraY = Math.max(0, Math.min(cameraY, mapPixelHeight - this.height));

        // Update player
        player.update(deltaTime);
        player.updateDialogue();

        // Update enemies - pass camera viewport info for AI
        for (Enemy enemy : enemies) {
            enemy.update(player.getX(), player.getY(), player, cameraX, cameraY, this.width, this.height);
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

        // Wave system logic
        if (!waveActive && !waitingForDialogue && !miniBossSpawned) {
            // Start next wave
            startNextWave();
        }

        // Check if current wave is completed
        if (waveActive && checkWaveCompleted()) {
            waveActive = false;
            waitingForDialogue = true;
            triggerWaveDialogue();
        }

        // Check if dialogue has finished
        if (waitingForDialogue && !dialogueUI.isDialogueVisible()) {
            onDialogueFinished();
        }

        // Check if player is dead
        if (!player.isAlive() && player.isDeathAnimationFinished()) {
            gameThread = null; // Stop the game loop
            // Capture the current screen as a BufferedImage
            BufferedImage screenshot = new BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_ARGB);
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

        // Calculate camera position to center on the player
        int cameraX = (int) player.px - this.width / 2; // Use player.px
        int cameraY = (int) player.py - this.height / 2; // Use player.py

        // Clamp camera to map bounds to prevent white background
        int mapPixelWidth = tileM.getMapWidth() * TILE_SIZE;
        int mapPixelHeight = tileM.getMapHeight() * TILE_SIZE;
        cameraX = Math.max(0, Math.min(cameraX, mapPixelWidth - this.width));
        cameraY = Math.max(0, Math.min(cameraY, mapPixelHeight - this.height));

        // Draw tiles using TileManager with camera offset
        tileM.draw(g2d, cameraX, cameraY, this.width, this.height);

        // Draw world objects
        objectM.draw(g2d, cameraX, cameraY, this.width, this.height);

        // Adjust player's draw position based on camera
        int playerScreenX = (int) player.px - cameraX; // Use player.px
        int playerScreenY = (int) player.py - cameraY; // Use player.py

        // Draw player
        player.draw(g, playerScreenX, playerScreenY);

        // Draw enemies (make a copy to avoid ConcurrentModificationException)
        List<Enemy> enemiesCopy = new ArrayList<>(enemies);
        for (Enemy enemy : enemiesCopy) {
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

        // Draw player status bars at top left
        drawPlayerStatusBars(g2d);

        // Draw hotbar (now shows skill items from inventory)
        hotbar.draw(g2d);

        // Do not dispose g2d here as JLayeredPane might manage its own children's painting.
        // The dispose will be called automatically by the Swing system.
    }

    private void drawHotbarKeys(Graphics2D g2d) {
        int slotSize = 120; // Increased from 80 to 120 for even larger skill icons
        int numSlots = 3; // Reduced from 5 to 3 skill slots
        int spacing = 30; // Increased spacing from 20 to 30 for better visual separation with larger icons
        int hotbarWidth = numSlots * slotSize + (numSlots - 1) * spacing;
        int hotbarX = (this.width - hotbarWidth) / 2; // Centers the slots horizontally
        int hotbarY = this.height - slotSize - 15; // Adjusted Y position for larger icons

        // Draw skill icons and cooldown overlays
        for (int i = 0; i < numSlots; i++) {
            int slotX = hotbarX + i * (slotSize + spacing);
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
            // Draw the image scaled to fill the slot
            g2d.drawImage(skillFireSplashIcon, x, y, size, size, null);
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
            // Draw the image scaled to fill the slot
            g2d.drawImage(skillIcePiercerIcon, x, y, size, size, null);
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
            // Draw the image scaled to fill the slot
            g2d.drawImage(skillLightningStormIcon, x, y, size, size, null);
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
        g2d.fillRect(0, 0, this.width, this.height);
    }

    private void drawDialogue(Graphics2D g2d, String text) {
        // Draw dialogue box at the bottom of the screen
        int boxWidth = this.width - 40;
        int boxHeight = 80;
        int boxX = 20;
        int boxY = this.height - boxHeight - 20;

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

    private void drawPlayerStatusBars(Graphics2D g2d) {
        // Position at top left corner
        int iconSize = 40; // Slightly larger for better visibility
        int barWidth = 160; // Slightly wider for better proportion
        int barHeight = 14; // Slightly taller for rounded look
        int arcWidth = barHeight; // Full height for pill shape
        int arcHeight = barHeight;
        int margin = 10;
        int startX = margin;
        int startY = margin;

        // Draw player icon (char_portrait.png)
        try {
            Image playerIcon = new ImageIcon(getClass().getResource("/assets/characters/char_portrait.png")).getImage();
            // Scale the icon to fit the UI properly
            g2d.drawImage(playerIcon, startX, startY, iconSize, iconSize, null);
        } catch (Exception e) {
            // Fallback: draw a simple colored circle
            g2d.setColor(Color.BLUE);
            g2d.fillOval(startX, startY, iconSize, iconSize);
            g2d.setColor(Color.WHITE);
            g2d.drawOval(startX, startY, iconSize, iconSize);
        }

        // Position bars to the right of the icon
        int barsX = startX + iconSize + margin;
        int hpBarY = startY + 4;
        int manaBarY = hpBarY + barHeight + 8;

        // Draw HP bar background (rounded, clean design)
        g2d.setColor(new Color(64, 64, 64, 200)); // Semi-transparent dark gray background
        g2d.fillRoundRect(barsX, hpBarY, barWidth, barHeight, arcWidth, arcHeight);
        g2d.setColor(Color.WHITE);
        g2d.drawRoundRect(barsX, hpBarY, barWidth, barHeight, arcWidth, arcHeight);

        // Draw HP bar fill (rounded, no text)
        g2d.setColor(Color.GREEN);
        int hpWidth = Math.max(barHeight, (int) (barWidth * ((double) player.getHp() / player.getMaxHp())));
        g2d.fillRoundRect(barsX, hpBarY, hpWidth, barHeight, arcWidth, arcHeight);

        // Draw Mana bar background (rounded, clean design)
        g2d.setColor(new Color(64, 64, 64, 200)); // Semi-transparent dark gray background
        g2d.fillRoundRect(barsX, manaBarY, barWidth, barHeight, arcWidth, arcHeight);
        g2d.setColor(Color.WHITE);
        g2d.drawRoundRect(barsX, manaBarY, barWidth, barHeight, arcWidth, arcHeight);

        // Draw Mana bar fill (rounded, no text)
        g2d.setColor(Color.BLUE);
        int manaWidth = Math.max(barHeight, (int) (barWidth * ((double) player.getMana() / player.getMaxMana())));
        g2d.fillRoundRect(barsX, manaBarY, manaWidth, barHeight, arcWidth, arcHeight);
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

    // Wave system methods
    private void startNextWave() {
        currentWave++;
        waveActive = true;
        enemies.clear(); // Clear existing enemies

        if (currentWave <= 5) {
            // Spawn regular wave enemies
            spawnWaveEnemies(currentWave);
        } else if (!miniBossSpawned) {
            // Spawn mini boss after wave 5
            spawnMiniBoss();
            miniBossSpawned = true;
        }

        // Set tile manager and inventory for new enemies
        for (Enemy enemy : enemies) {
            enemy.setTileManager(tileM);
            enemy.setInventory(gameInventory);
        }

        System.out.println("Wave " + currentWave + " started!");
    }

    private void spawnWaveEnemies(int waveNumber) {
        // Progressive enemy count: Wave 1 = 5, Wave 2 = 8, Wave 3 = 11, Wave 4 = 14, Wave 5 = 17
        int enemyCount = 5 + (waveNumber - 1) * 3;
        Enemy.EnemyType[] enemyTypes = getEnemyTypesForWave(waveNumber);

        // Spawn enemies at the bottom edge of the map (Y coordinates around 2300-2350)
        int[][] spawnPositions = {
            {200, 2300}, {400, 2320}, {600, 2280}, {800, 2350}, {1000, 2310},
            {1200, 2290}, {1400, 2330}, {1600, 2270}, {1800, 2340}, {2000, 2300},
            {2200, 2320}, {240, 2280}, {440, 2350}, {640, 2310}, {840, 2290},
            {1040, 2330}, {1240, 2270}, {1440, 2340}, {1640, 2300}, {1840, 2320},
            {2040, 2280}, {2240, 2350}, {2440, 2310}, {2640, 2290}, {2840, 2330}
        };

        for (int i = 0; i < enemyCount && i < spawnPositions.length; i++) {
            Enemy.EnemyType type = enemyTypes[i % enemyTypes.length];
            enemies.add(new Enemy(spawnPositions[i][0], spawnPositions[i][1], type));
        }
        
        System.out.println("Wave " + waveNumber + ": Spawned " + enemyCount + " enemies");
    }

    private Enemy.EnemyType[] getEnemyTypesForWave(int waveNumber) {
        switch (waveNumber) {
            case 1:
                return new Enemy.EnemyType[]{Enemy.EnemyType.BASIC};
            case 2:
                return new Enemy.EnemyType[]{Enemy.EnemyType.BASIC, Enemy.EnemyType.FAST};
            case 3:
                return new Enemy.EnemyType[]{Enemy.EnemyType.BASIC, Enemy.EnemyType.TANK};
            case 4:
                return new Enemy.EnemyType[]{Enemy.EnemyType.FAST, Enemy.EnemyType.TANK, Enemy.EnemyType.MINOTAUR};
            case 5:
                return new Enemy.EnemyType[]{Enemy.EnemyType.BASIC, Enemy.EnemyType.FAST, Enemy.EnemyType.TANK, Enemy.EnemyType.MINOTAUR};
            default:
                return new Enemy.EnemyType[]{Enemy.EnemyType.BASIC};
        }
    }

    private void spawnMiniBoss() {
        enemies.add(new Enemy(700, 800, Enemy.EnemyType.MINI_BOSS));
        System.out.println("Mini Boss spawned!");
    }

    private boolean checkWaveCompleted() {
        // Check if all enemies are dead
        for (Enemy enemy : enemies) {
            if (enemy.isAlive()) {
                return false;
            }
        }
        return true;
    }

    private void triggerWaveDialogue() {
        List<String> dialogueLines = new ArrayList<>();

        if (currentWave <= 5) {
            // Dialogue after each regular wave
            switch (currentWave) {
                case 1:
                    dialogueLines.add("Old Man: Well done, young warrior! You've survived the first wave.");
                    dialogueLines.add("Old Man: But don't get complacent. The creatures grow stronger.");
                    dialogueLines.add("Old Man: Prepare yourself for what's coming next.");
                    break;
                case 2:
                    dialogueLines.add("Old Man: Impressive! The second wave falls before you.");
                    dialogueLines.add("Old Man: These beasts are learning from their defeats.");
                    dialogueLines.add("Old Man: Stay sharp and watch your surroundings.");
                    break;
                case 3:
                    dialogueLines.add("Old Man: You continue to surprise me with your prowess.");
                    dialogueLines.add("Old Man: The third wave was no match for your skills.");
                    dialogueLines.add("Old Man: But I sense a greater darkness approaching...");
                    break;
                case 4:
                    dialogueLines.add("Old Man: Four waves down! You're proving to be quite the formidable opponent.");
                    dialogueLines.add("Old Man: The enemies grow desperate, and with desperation comes danger.");
                    dialogueLines.add("Old Man: One more wave before the true test begins.");
                    break;
                case 5:
                    dialogueLines.add("Old Man: The fifth wave is vanquished! You've done what few could.");
                    dialogueLines.add("Old Man: But this was merely a prelude. The real battle awaits.");
                    dialogueLines.add("Old Man: A fearsome creature approaches. Prepare for the ultimate challenge!");
                    break;
            }
        } else {
            // Dialogue after mini boss
            dialogueLines.add("Old Man: Unbelievable! You've defeated the ancient guardian!");
            dialogueLines.add("Old Man: Such power... such determination. You are truly worthy.");
            dialogueLines.add("Old Man: The darkness has been pushed back, for now.");
            dialogueLines.add("Old Man: Rest well, hero. The realm owes you its gratitude.");
        }

        dialogueUI.startDialogue("Yorme", dialogueLines);

        // Set up dialogue completion callback
        setupDialogueCallback();
    }

    private void setupDialogueCallback() {
        // This method will be called when dialogue ends
        // For now, we'll check in the update loop if dialogue is finished
        waitingForDialogue = true;
    }

    // Method to continue after dialogue (called from update when dialogue ends)
    private void onDialogueFinished() {
        waitingForDialogue = false;
        if (currentWave >= 5 && miniBossSpawned) {
            // Game completed - could trigger victory screen
            System.out.println("Congratulations! All waves completed!");
        }
    }

    // Define a functional interface for the game over callback
    public interface GameOverCallback {
        void onGameOver(BufferedImage screenshot);
    }
}
