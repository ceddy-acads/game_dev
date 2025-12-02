package world;

import entities.WorldObject;
import tile.TileManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ObjectManager {
    private List<WorldObject> objects;
    private TileManager tileM;
    private List<?> npcs; // Reference to NPCs for collision avoidance
    private Random random;
    private int tileSize;
    private int mapWidth;
    private int mapHeight;

    // Available objects (grass objects and structures)
    private String[] objectImages = {
        "/assets/objects/Black_mushrooms1_grass_shadow.png",
        "/assets/objects/Black_mushrooms2_grass_shadow.png",
        "/assets/objects/Orange_mushrooms1_grass_shadow.png",
        "/assets/objects/Orange_mushrooms2_grass_shadow.png",
        "/assets/objects/Caury_pearl1_grass_shadow.png",
        "/assets/objects/Caury_pearl2_grass_shadow.png",
        "/assets/objects/Caury_white1_grass_shadow.png",
        "/assets/objects/Caury_white2_grass_shadow.png",
        "/assets/objects/Oval_rock1_grass_shadow.png",
        "/assets/objects/Oval_rock2_grass_shadow.png",
        "/assets/objects/Oval_rock3_grass_shadow.png",
        "/assets/objects/Oval_rock4_grass_shadow.png",
        "/assets/objects/Oval_rock5_grass_shadow.png",
        "/assets/objects/Fern_tree2.png",
        "/assets/objects/Fern_tree3.png",
        "/assets/objects/Oval_leaf_tree1.png",
        "/assets/objects/Oval_leaf_tree2.png",
        "/assets/objects/Oval_leaf_tree3.png",
        "/assets/tiles/hut.png" // Add hut as an object
    };

    private String[] objectNames = {
        "Black Mushrooms 1", "Black Mushrooms 2", "Orange Mushrooms 1", "Orange Mushrooms 2",
        "Caury Pearl 1", "Caury Pearl 2", "Caury White 1", "Caury White 2",
        "Oval Rock 1", "Oval Rock 2", "Oval Rock 3", "Oval Rock 4", "Oval Rock 5",
        "Fern Tree 2", "Fern Tree 3",
        "Oval Leaf Tree 1", "Oval Leaf Tree 2", "Oval Leaf Tree 3",
        "Hut" // Add hut name
    };

    // Define which objects can spawn on which tile types
    private boolean[] canSpawnOnGrass; // true for objects that can spawn on grass tiles
    private boolean[] canSpawnOnEarth; // true for objects that can spawn on earth tiles

    public ObjectManager(TileManager tileM) {
        this.tileM = tileM;
        this.objects = new ArrayList<>();
        this.random = new Random();
        this.tileSize = tileM.getTileSize();
        this.mapWidth = tileM.getMapWidth();
        this.mapHeight = tileM.getMapHeight();

        // Initialize spawn restrictions
        initializeSpawnRestrictions();

        spawnObjects();
    }

    private void initializeSpawnRestrictions() {
        int numObjects = objectImages.length;
        canSpawnOnGrass = new boolean[numObjects];
        canSpawnOnEarth = new boolean[numObjects];

        // ALL objects can ONLY spawn on grass tiles (tile 0)
        // No objects should spawn on roads, walls, water, earth, or any other tile types
        for (int i = 0; i < numObjects; i++) {
            canSpawnOnGrass[i] = true;
            canSpawnOnEarth[i] = false;
        }
    }

    private void spawnObjects() {
        int maxObjects = 150; // Maximum number of objects to spawn
        int minSpacing = 3; // Minimum spacing in tiles between objects

        for (int i = 0; i < maxObjects; i++) {
            if (!trySpawnObject(minSpacing)) {
                // If we can't find a spot after several attempts, stop
                break;
            }
        }

        System.out.println("Spawned " + objects.size() + " objects on the map");
    }

    private boolean trySpawnObject(int minSpacing) {
        int maxAttempts = 50; // Maximum attempts to find a valid spawn location

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            // Generate random position
            int tileX = random.nextInt(mapWidth);
            int tileY = random.nextInt(mapHeight);

            int tileId = tileM.getTileId(tileX, tileY);

            // Select random object that can spawn on this tile type
            int objectIndex = getRandomObjectForTile(tileId);
            if (objectIndex == -1) {
                continue; // No object can spawn on this tile
            }

            // Check spacing from other objects
            if (!isSpacingValid(tileX, tileY, minSpacing)) {
                continue; // Too close to another object
            }

            // Valid location found, spawn object
            int pixelX = tileX * tileSize;
            int pixelY = tileY * tileSize;

            String imagePath = objectImages[objectIndex];
            String name = objectNames[objectIndex];

            // All objects should have collision
            boolean collision = true;

            WorldObject obj = new WorldObject(pixelX, pixelY, imagePath, name, collision);
            objects.add(obj);

            return true;
        }

        return false; // Could not find valid location
    }

    private int getRandomObjectForTile(int tileId) {
        List<Integer> validObjects = new ArrayList<>();

        for (int i = 0; i < objectImages.length; i++) {
            boolean canSpawn = false;

            if (tileId == 0 && canSpawnOnGrass[i]) { // Grass tile
                canSpawn = true;
            } else if (tileId == 3 && canSpawnOnEarth[i]) { // Earth tile
                canSpawn = true;
            }

            if (canSpawn) {
                validObjects.add(i);
            }
        }

        if (validObjects.isEmpty()) {
            return -1; // No valid objects for this tile
        }

        return validObjects.get(random.nextInt(validObjects.size()));
    }

    private boolean isSpacingValid(int tileX, int tileY, int minSpacing) {
        // Check spacing from other objects
        for (WorldObject obj : objects) {
            int objTileX = obj.getX() / tileSize;
            int objTileY = obj.getY() / tileSize;

            int dx = Math.abs(tileX - objTileX);
            int dy = Math.abs(tileY - objTileY);

            if (dx < minSpacing && dy < minSpacing) {
                return false; // Too close to another object
            }
        }

        // Check spacing from NPCs (prevent objects from spawning near NPCs)
        if (npcs != null) {
            for (Object npc : npcs) {
                try {
                    // Use reflection to get NPC position
                    int npcX = (Integer) npc.getClass().getMethod("getX").invoke(npc);
                    int npcY = (Integer) npc.getClass().getMethod("getY").invoke(npc);

                    int npcTileX = npcX / tileSize;
                    int npcTileY = npcY / tileSize;

                    int dx = Math.abs(tileX - npcTileX);
                    int dy = Math.abs(tileY - npcTileY);

                    // Use larger spacing for NPCs (5 tiles instead of 3)
                    if (dx < 5 && dy < 5) {
                        return false; // Too close to NPC
                    }
                } catch (Exception e) {
                    // If reflection fails, continue
                }
            }
        }

        return true;
    }

    public void draw(java.awt.Graphics2D g2, int cameraX, int cameraY, int screenWidth, int screenHeight) {
        for (WorldObject obj : objects) {
            // Only draw objects visible on screen (using scaled dimensions)
            int objX = obj.getX();
            int objY = obj.getY();

            // Hut gets much larger scaling (5x instead of 2x)
            float scaleFactor = obj.getName().equals("Hut") ? 5.0f : 2.0f;
            int objWidth = (int)(obj.getWidth() * scaleFactor);
            int objHeight = (int)(obj.getHeight() * scaleFactor);

            if (objX + objWidth >= cameraX && objX <= cameraX + screenWidth &&
                objY + objHeight >= cameraY && objY <= cameraY + screenHeight) {
                // Draw with appropriate scale
                obj.drawScaled(g2, cameraX, cameraY, scaleFactor);
            }
        }
    }

    public List<WorldObject> getObjects() {
        return objects;
    }

    // Set NPC reference for spawn avoidance
    public void setNPCs(List<?> npcs) {
        this.npcs = npcs;
    }

    // Check if a position collides with any object
    // Use smaller collision dimensions for better gameplay
    public boolean isObjectCollision(int x, int y, int width, int height) {
        for (WorldObject obj : objects) {
            if (!obj.hasCollision()) continue;

            // Use original dimensions for collision (not scaled display size)
            // This prevents objects from having oversized collision boxes
            int objWidth = obj.getWidth();
            int objHeight = obj.getHeight();

            if (x < obj.getX() + objWidth &&
                x + width > obj.getX() &&
                y < obj.getY() + objHeight &&
                y + height > obj.getY()) {
                return true;
            }
        }
        return false;
    }
}
