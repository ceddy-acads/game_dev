package main;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import input.KeyHandler;
import entities.Enemy;
import entities.Player;
import entities.SlashAttack;
import entities.SkillWAttack;
import entities.InventoryUI;
import entities.Hotbar;d
import entities.NPC;
import entities.DialogueUI;

import tile.TileManager;
import world.ObjectManager;

public class GameLoop extends JLayeredPane implements Runnable {

    int width = 800;
    int height = 600;
    final int TILE_SIZE = 80;

    private boolean inventoryOpen = false;
    private InventoryUI gameInventory;
    private DialogueUI dialogueUI;

    private Thread gameThread;
    private KeyHandler keyH;
    private Player player;
    private TileManager tileM;
    private ObjectManager objectM;
    private Hotbar hotbar;
    private List<Enemy> enemies;
    private List<NPC> npcs;
    private GameOverCallback gameOverCallback;

    // Wave system
    private int currentWave = 0;
    private boolean waveActive = false;
    private boolean waitingForDialogue = false;
    private boolean miniBossSpawned = false;

    // Skill icons
    private Image skillIcePiercerIcon;
    private Image skillLightningStormIcon;
    private Image skillFireSplashIcon;
    private Image playerPortrait; // Field to store player portrait
    private Image swordIcon; // Dropped sword icon

    public GameLoop(GameOverCallback gameOverCallback) {
        this.gameOverCallback = gameOverCallback;

        this.setPreferredSize(new Dimension(width, height));
        this.setBackground(Color.BLACK);
        this.setDoubleBuffered(true);

        keyH = new KeyHandler();
        this.setFocusable(true);
        this.requestFocusInWindow();
        this.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int code = e.getKeyCode();

                if (code == KeyEvent.VK_F) {
                    Main.toggleFullscreen();
                    return;
                }

                if (dialogueUI.isDialogueVisible()) {
                    dialogueUI.handleKeyPress(code);
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

        tileM = new TileManager(this);
        objectM = new ObjectManager(tileM);

        player = new Player(400, 400, keyH);
        player.setTileManager(tileM);
        player.setObjectManager(objectM);

        gameInventory = new InventoryUI(this.width, this.height, itemId -> {
            if ("potion_blue".equals(itemId)) {
                int oldMana = player.getMana();
                player.restoreMana(50);
                System.out.println("Used Mana Potion: Mana " + oldMana + " -> " + player.getMana());
            } else if (itemId.equals("sword")) {
                // Equip sword: add attack boost
                player.setEquippedStats(player.getEquippedAttack() + 15, player.getEquippedDefense());
                System.out.println("Equipped sword, attack increased by 15");
            }
        });

        dialogueUI = new DialogueUI(this.width, this.height);
        dialogueUI.setBounds(0, 0, this.width, this.height);
        dialogueUI.setVisible(false);
        this.add(dialogueUI, JLayeredPane.MODAL_LAYER);

        // Set inventory reference for player
        player.setInventory(gameInventory);

        // Initialize hotbar
        hotbar = new Hotbar(this.width, this.height, gameInventory);
        hotbar.setPlayer(player);
        gameInventory.setBounds(0, 0, this.width, this.height);
        gameInventory.setVisible(false);
        this.add(gameInventory, JLayeredPane.PALETTE_LAYER);

        enemies = new ArrayList<>();
        enemies.add(new Enemy(600, 800, Enemy.EnemyType.BASIC));
        enemies.add(new Enemy(700, 850, Enemy.EnemyType.BASIC));

        for (Enemy enemy : enemies) {
            enemy.setTileManager(tileM);
            enemy.setInventory(gameInventory);
            enemy.setObjectManager(objectM);
        }

        npcs = new ArrayList<>();
        npcs.add(new NPC(1200, 480));
        npcs.get(0).setTileManager(tileM);
        npcs.get(0).setPlayer(player);
        player.setNPCs(npcs);
        player.setDialogueUI(dialogueUI);
        dialogueUI.setKeyHandler(keyH);

        objectM.setNPCs(npcs);

        loadSkillIcons();

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

        // Load player portrait once
        try {
            playerPortrait = new ImageIcon(getClass().getResource("/assets/characters/char_portrait.png")).getImage();
        } catch (Exception e) {
            System.err.println("Failed to load player portrait: " + e.getMessage());
            playerPortrait = null; // Fallback
        }
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
        player = new Player(400, 400, keyH);
        player.setTileManager(tileM);
        player.setObjectManager(objectM);

        gameInventory.reset();
        inventoryOpen = false;
        gameInventory.setVisible(false);

        if (gameThread != null) {
            gameThread = null;
        }
    }

    public void updateWindowSize(int newWidth, int newHeight) {
        this.width = newWidth;
        this.height = newHeight;

        this.setPreferredSize(new Dimension(width, height));

        if (gameInventory != null) {
            gameInventory.updateSize(width, height);
        }
        if (hotbar != null) {
            hotbar.updateSize(width, height);
        }
        if (dialogueUI != null) {
            dialogueUI.updateSize(width, height);
        }

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
            gameInventory.requestFocusInWindow();
        } else {
            this.requestFocusInWindow();
        }
    }

    @Override
    public void addNotify() {
        super.addNotify();
    }

    public void startGameThread() {
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public void run() {
        double drawInterval = 1000000000.0 / 60.0;
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
            return;
        }

        float deltaTime = 1.0f / 60.0f;

        int cameraX = (int) player.px - this.width / 2;
        int cameraY = (int) player.py - this.height / 2;
        int mapPixelWidth = tileM.getMapWidth() * TILE_SIZE;
        int mapPixelHeight = tileM.getMapHeight() * TILE_SIZE;
        cameraX = Math.max(0, Math.min(cameraX, mapPixelWidth - this.width));
        cameraY = Math.max(0, Math.min(cameraY, mapPixelHeight - this.height));

        // Update player
        player.update(deltaTime);
        player.updateDialogue();

        for (Enemy enemy : enemies) {
            enemy.update(player.getX(), player.getY(), player, cameraX, cameraY, this.width, this.height);
        }

        // Check for dropped item pickup
        try {
            java.util.List<int[]> droppedItems = (java.util.List<int[]>) objectM.getClass().getMethod("getDroppedItems").invoke(objectM);
            for (int i = droppedItems.size() - 1; i >= 0; i--) {
                int[] pos = droppedItems.get(i);
                double distance = Math.sqrt(Math.pow(player.getX() - pos[0], 2) + Math.pow(player.getY() - pos[1], 2));
                if (distance < 150) { // Increased pickup radius for better gameplay
                    try {
                        gameInventory.getClass().getMethod("addItem", String.class, int.class).invoke(gameInventory, "sword", 1);
                        objectM.getClass().getMethod("removeDrop", int.class).invoke(objectM, i);
                        System.out.println("Picked up sword! Added to inventory.");
                    } catch (Exception e) {
                        // Ignore
                    }
                }
            }
        } catch (Exception e) {
            // Ignore if method fails
        }

        // Update NPCs
        for (NPC npc : npcs) {
            npc.update();
        }

        for (SlashAttack slash : player.getSlashes()) {
            for (Enemy enemy : enemies) {
                if (enemy.isAlive() && slash.getBounds().intersects(enemy.getBounds()) && !slash.hasHit(enemy)) {
                    enemy.takeDamage(slash.getDamage());
                    slash.addHitEnemy(enemy);
                    System.out.println("Slash dealt " + slash.getDamage() + " damage to enemy!");
                }
            }
        }

        for (SkillWAttack skillW : player.getSkillWAttacks()) {
            for (Enemy enemy : enemies) {
                if (enemy.isAlive() && skillW.getBounds().intersects(enemy.getBounds()) && !skillW.hasHit(enemy)) {
                    enemy.takeDamage(skillW.getDamage());
                    skillW.addHitEnemy(enemy);
                    System.out.println("SkillW dealt " + skillW.getDamage() + " damage to enemy!");
                }
            }
        }

        Rectangle freezeArea = player.getFreezeArea();
        if (freezeArea != null) {
            for (Enemy enemy : enemies) {
                if (enemy.isAlive() && freezeArea.intersects(enemy.getBounds())) {
                    enemy.freeze(60);
                    enemy.takeDamage(player.getTotalAttack());
                    System.out.println("Ice Piercer dealt " + player.getTotalAttack() + " damage to enemy!");
                }
            }
            player.clearFreezeArea();
        }

        Rectangle lightningArea = player.getLightningArea();
        if (lightningArea != null) {
            for (Enemy enemy : enemies) {
                if (enemy.isAlive() && lightningArea.intersects(enemy.getBounds())) {
                    enemy.takeDamage(player.getTotalAttack() * 2);
                    System.out.println("Lightning Storm dealt " + (player.getTotalAttack() * 2) + " damage to enemy!");
                }
            }
            player.clearLightningArea();
        }

        if (!waveActive && !waitingForDialogue && !miniBossSpawned) {
            startNextWave();
        }

        if (waveActive && checkWaveCompleted()) {
            waveActive = false;
            waitingForDialogue = true;
            triggerWaveDialogue();
        }

        if (waitingForDialogue && !dialogueUI.isDialogueVisible()) {
            onDialogueFinished();
        }

        if (!player.isAlive() && player.isDeathAnimationFinished()) {
            gameThread = null;
            BufferedImage screenshot = new BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = screenshot.createGraphics();
            paintComponent(g2d);
            g2d.dispose();

            gameOverCallback.onGameOver(screenshot);
            return;
        }
    }

    private void onDialogueFinished() {
        waitingForDialogue = false;
        if (currentWave >= 5 && miniBossSpawned) {
            tileM.setTile(32, 6, 0);
            System.out.println("Congratulations! All waves completed! The sacred tree has opened!");
        }
    }

    private void loadSkillIcons() {
        try {
            skillIcePiercerIcon = new ImageIcon(getClass().getResource("/assets/ui/skill_icepiercer.png")).getImage();
            skillLightningStormIcon = new ImageIcon(getClass().getResource("/assets/ui/skill_lightningstorm.png")).getImage();
            skillFireSplashIcon = new ImageIcon(getClass().getResource("/assets/ui/skill_firesplash.png")).getImage();
            swordIcon = new ImageIcon(getClass().getResource("/assets/icons/Icon33.png")).getImage();
        } catch (Exception e) {
            System.err.println("Failed to load icons: " + e.getMessage());
            skillIcePiercerIcon = null;
            skillLightningStormIcon = null;
            skillFireSplashIcon = null;
            swordIcon = null;
        }
    }

    // Wave system methods
    private void startNextWave() {
        currentWave++;
        waveActive = true;
        enemies.clear();

        if (currentWave <= 5) {
            spawnWaveEnemies(currentWave);
        } else if (!miniBossSpawned) {
            spawnMiniBoss();
            miniBossSpawned = true;
        }

        for (Enemy enemy : enemies) {
            enemy.setTileManager(tileM);
            enemy.setInventory(gameInventory);
            enemy.setObjectManager(objectM);
        }

        System.out.println("Wave " + currentWave + " started!");
    }

    private void spawnWaveEnemies(int waveNumber) {
        int enemyCount = 5 + (waveNumber - 1) * 3;
        Enemy.EnemyType[] enemyTypes = getEnemyTypesForWave(waveNumber);

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
            case 1: return new Enemy.EnemyType[]{Enemy.EnemyType.BASIC};
            case 2: return new Enemy.EnemyType[]{Enemy.EnemyType.BASIC, Enemy.EnemyType.FAST};
            case 3: return new Enemy.EnemyType[]{Enemy.EnemyType.BASIC, Enemy.EnemyType.TANK};
            case 4: return new Enemy.EnemyType[]{Enemy.EnemyType.FAST, Enemy.EnemyType.TANK, Enemy.EnemyType.MINOTAUR};
            case 5: return new Enemy.EnemyType[]{Enemy.EnemyType.BASIC, Enemy.EnemyType.FAST, Enemy.EnemyType.TANK, Enemy.EnemyType.MINOTAUR};
            default: return new Enemy.EnemyType[]{Enemy.EnemyType.BASIC};
        }
    }

    private void spawnMiniBoss() {
        enemies.add(new Enemy(1200, 2300, Enemy.EnemyType.MINI_BOSS));
        System.out.println("Mini Boss spawned!");
    }

    private boolean checkWaveCompleted() {
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
            switch (currentWave) {
                case 1: dialogueLines.add("Old Man: Well done, young warrior! You've survived the first wave.");
                        dialogueLines.add("Old Man: But don't get complacent. The creatures grow stronger.");
                        dialogueLines.add("Old Man: Prepare yourself for what's coming next.");
                        break;
                case 2: dialogueLines.add("Old Man: Impressive! The second wave falls before you.");
                        dialogueLines.add("Old Man: These beasts are learning from their defeats.");
                        dialogueLines.add("Old Man: Stay sharp and watch your surroundings.");
                        break;
                case 3: dialogueLines.add("Old Man: You continue to surprise me with your prowess.");
                        dialogueLines.add("Old Man: The third wave was no match for your skills.");
                        dialogueLines.add("Old Man: But I sense a greater darkness approaching...");
                        break;
                case 4: dialogueLines.add("Old Man: Four waves down! You're proving to be quite the formidable opponent.");
                        dialogueLines.add("Old Man: The enemies grow desperate, and with desperation comes danger.");
                        dialogueLines.add("Old Man: One more wave before the true test begins.");
                        break;
                case 5: dialogueLines.add("Old Man: The fifth wave is vanquished! You've done what few could.");
                        dialogueLines.add("Old Man: But this was merely a prelude. The real battle awaits.");
                        dialogueLines.add("Old Man: A fearsome creature approaches. Prepare for the ultimate challenge!");
                        break;
            }
        } else {
            dialogueLines.add("Old Man: Unbelievable! You've defeated the ancient guardian!");
            dialogueLines.add("Old Man: Such power... such determination. You are truly worthy.");
            dialogueLines.add("Old Man: The darkness has been pushed back, for now.");
            dialogueLines.add("Old Man: Rest well, hero. The realm owes you its gratitude.");
        }

        dialogueUI.startDialogue("Yorme", dialogueLines);
        setupDialogueCallback();
    }

    private void setupDialogueCallback() {
        waitingForDialogue = true;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        if (inventoryOpen) {
            return;
        }

        // Calculate camera position to center on the player
        int cameraX = (int) player.px - this.width / 2;
        int cameraY = (int) player.py - this.height / 2;

        // Clamp camera to map bounds to prevent white background
        int mapPixelWidth = tileM.getMapWidth() * TILE_SIZE;
        int mapPixelHeight = tileM.getMapHeight() * TILE_SIZE;
        cameraX = Math.max(0, Math.min(cameraX, mapPixelWidth - this.width));
        cameraY = Math.max(0, Math.min(cameraY, mapPixelHeight - this.height));

        // Draw tiles using TileManager with camera offset
        tileM.draw(g2d, cameraX, cameraY, this.width, this.height);

        // Draw world objects
        objectM.draw(g2d, cameraX, cameraY, this.width, this.height);

        // Draw dropped items (larger for better visibility)
        try {
            java.util.List<int[]> drops = (java.util.List<int[]>) objectM.getClass().getMethod("getDroppedItems").invoke(objectM);
            for (int[] pos : drops) {
                int screenX = pos[0] - cameraX;
                int screenY = pos[1] - cameraY;
                if (swordIcon != null) {
                    // Make sword icon larger and more visible
                    g2d.drawImage(swordIcon, screenX - 24, screenY - 24, 48, 48, null);
                    // Add a subtle glow effect
                    g2d.setColor(new Color(255, 215, 0, 100)); // Gold glow
                    g2d.fillOval(screenX - 20, screenY - 20, 40, 40);
                } else {
                    // Bright yellow circle fallback
                    g2d.setColor(Color.YELLOW);
                    g2d.fillOval(screenX - 15, screenY - 15, 30, 30);
                    g2d.setColor(Color.BLACK);
                    g2d.drawOval(screenX - 15, screenY - 15, 30, 30);
                }
                // Add "SWORD" label
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Arial", Font.BOLD, 10));
                g2d.drawString("SWORD", screenX - 15, screenY + 30);
            }
        } catch (Exception e) {
            // Ignore
        }

        // Adjust player's draw position based on camera
        int playerScreenX = (int) player.px - cameraX;
        int playerScreenY = (int) player.py - cameraY;

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

        // Draw player icon
        if (playerPortrait != null) {
            // Scale the icon to fit the UI properly
            g2d.drawImage(playerPortrait, startX, startY, iconSize, iconSize, null);
        } else {
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

    // Define a functional interface for the game over callback
    public interface GameOverCallback {
        void onGameOver(BufferedImage screenshot);
    }
}
