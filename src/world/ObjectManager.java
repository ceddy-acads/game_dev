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

    // Available objects (grass objects and structures) - TREES REMOVED
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
        "/assets/objects/Oval_rock5_grass_shadow.png"
    };

    private String[] objectNames = {
        "Black Mushrooms 1", "Black Mushrooms 2", "Orange Mushrooms 1", "Orange Mushrooms 2",
        "Caury Pearl 1", "Caury Pearl 2", "Caury White 1", "Caury White 2",
        "Oval Rock 1", "Oval Rock 2", "Oval Rock 3", "Oval Rock 4", "Oval Rock 5"
    };

    // Define which objects can spawn on which tile types
    private boolean[] canSpawnOnGrass; // true for objects that can spawn on grass tiles
    private boolean[] canSpawnOnEarth; // true for objects that can spawn on earth tiles
    private boolean[] objectCollidable; // true for objects that block movement

    public ObjectManager(TileManager tileM) {
        this.tileM = tileM;
        this.objects = new ArrayList<>();
        this.random = new Random();
        this.tileSize = tileM.getTileSize();
        this.mapWidth = tileM.getMapWidth();
        this.mapHeight = tileM.getMapHeight();

        // Initialize spawn restrictions and collision properties
        initializeSpawnRestrictions();
        initializeObjectCollidable();

        spawnObjects();
    }

    private void initializeSpawnRestrictions() {
        int numObjects = objectImages.length;
        canSpawnOnGrass = new boolean[numObjects];
        canSpawnOnEarth = new boolean[numObjects];

        // ALL objects can ONLY spawn on grass tiles (tile 0)
        // No objects should spawn on roads, walls, water, earth, or any other tile types
        // This ensures objects don't overlap roads or any tiles except grass
        for (int i = 0; i < numObjects; i++) {
            canSpawnOnGrass[i] = true;  // Only grass tiles (tile 0)
            canSpawnOnEarth[i] = false; // No earth tiles (tile 3)
        }
    }

    private void initializeObjectCollidable() {
        int numObjects = objectImages.length;
        objectCollidable = new boolean[numObjects];

        // Set specific objects as non-collidable (mushrooms, small plants)
        // Indices based on objectNames array:
        // Black Mushrooms 1, Black Mushrooms 2 (0, 1) -> false
        // Orange Mushrooms 1, Orange Mushrooms 2 (2, 3) -> false
        // Caury Pearl 1, Caury Pearl 2 (4, 5) -> false
        // Caury White 1, Caury White 2 (6, 7) -> false
        // Oval Rock 1-5 (8-12) -> true
        // Fern Tree 2, Fern Tree 3 (13, 14) -> false
        // Oval Leaf Tree 1, Oval Leaf Tree 2, Oval Leaf Tree 3 (15, 16, 17) -> false

        for (int i = 0; i < numObjects; i++) {
            String name = objectNames[i];
            if (name.contains("Mushroom") || name.contains("Caury") || name.contains("Fern Tree") || name.contains("Oval Leaf Tree") || name.contains("Sword")) {
                objectCollidable[i] = false; // Player can walk through
            } else {
                objectCollidable[i] = true; // Rocks, larger objects will block
            }
        }
    }

    private void spawnObjects() {
        // Fixed positions for objects on grass tiles only - very widely spaced and reduced amount
        // Player spawns at tile (5, 5) - avoiding positions within 3 tiles of spawn
        // Each position has a fixed object type assigned
        Object[] fixedSpawns = {
            // Upper grass area - very sparse
            new int[]{6, 3, 0}, new int[]{10, 6, 1},

            // Left side grass area - widely spaced
            new int[]{5, 18, 7}, new int[]{9, 22, 8},

            // Right side grass area - widely spaced
            new int[]{26, 18, 2}, new int[]{30, 22, 3},

            // Bottom area grass - very sparse
            new int[]{6, 26, 10}, new int[]{10, 28, 11},

            // Center area - minimal (replaced trees with rocks)
            new int[]{17, 19, 9}, new int[]{21, 21, 10}
        };

        // Spawn objects at fixed positions with fixed types
        for (Object spawn : fixedSpawns) {
            int[] data = (int[]) spawn;
            int tileX = data[0];
            int tileY = data[1];
            int objectIndex = data[2];

            // Verify this is a grass tile (tile 0)
            int tileId = tileM.getTileId(tileX, tileY);
            if (tileId != 0) {
                continue; // Skip if not grass
            }

            // Check if position is valid (not too close to NPCs or collision tiles)
            if (!isPositionValidForObject(tileX, tileY)) {
                continue; // Skip invalid positions
            }

            // Use the fixed object type
            if (objectIndex >= 0 && objectIndex < objectImages.length) {
                // Create object
                int pixelX = tileX * tileSize;
                int pixelY = tileY * tileSize;

                String imagePath = objectImages[objectIndex];
                String name = objectNames[objectIndex];
                boolean collision = true; // All objects have collision

                WorldObject obj = new WorldObject(pixelX, pixelY, imagePath, name, collision);
                objects.add(obj);
            }
        }

        System.out.println("Spawned " + objects.size() + " objects at fixed positions with fixed types on grass tiles");
    }

    private boolean isPositionValidForObject(int tileX, int tileY) {
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

        // Check spacing from collision tiles (walls, water, etc.)
        // Objects should not spawn adjacent to collision tiles to prevent overlap
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue; // Skip center tile

                int checkX = tileX + dx;
                int checkY = tileY + dy;

                // Check bounds
                if (checkX >= 0 && checkX < mapWidth && checkY >= 0 && checkY < mapHeight) {
                    int tileId = tileM.getTileId(checkX, checkY);

                    // If adjacent tile has collision, don't spawn here
                    if (tileId == 1 || tileId == 2 || tileId == 5 || tileId == 11) {
                        return false; // Too close to collision tile
                    }
                }
            }
        }

        return true;
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

            // Use the determined collision property for the object
            boolean collision = objectCollidable[objectIndex];

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

        // Check spacing from collision tiles (walls, water, etc.)
        // Objects should not spawn adjacent to collision tiles to prevent overlap
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue; // Skip center tile

                int checkX = tileX + dx;
                int checkY = tileY + dy;

                // Check bounds
                if (checkX >= 0 && checkX < mapWidth && checkY >= 0 && checkY < mapHeight) {
                    int tileId = tileM.getTileId(checkX, checkY);

                    // If adjacent tile has collision, don't spawn here
                    if (tileId == 1 || tileId == 2 || tileId == 5 || tileId == 11) {
                        return false; // Too close to collision tile
                    }
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

            // Reduced scaling for better collision sizing
            float scaleFactor = 2.0f;
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

    // Add an object dynamically (for drops, etc.)
    public void addObject(int x, int y, String imagePath, String name, boolean collision) {
        WorldObject obj = new WorldObject(x, y, imagePath, name, collision);
        objects.add(obj);
    }

    // Add dropped item icon (for enemy drops)
    private java.util.List<int[]> droppedItems = new java.util.ArrayList<>();
    public void addDrop(int x, int y) {
        droppedItems.add(new int[]{x, y});
    }

    public java.util.List<int[]> getDroppedItems() {
        return droppedItems;
    }

    public void removeDrop(int index) {
        if (index >= 0 && index < droppedItems.size()) {
            droppedItems.remove(index);
        }
    }

    // Check if a position collides with any object
    // Use display size (2x scale) with accurate positioning for right-side collision
    public boolean isObjectCollision(int x, int y, int width, int height) {
        for (WorldObject obj : objects) {
            if (!obj.hasCollision()) continue;

            // Use the same 2x scaling as display for accurate collision
            // Calculate the actual visual boundaries where the object appears
            float scaleFactor = 2.0f;
            int visualX = obj.getX(); // Visual starts at object position
            int visualY = obj.getY(); // Visual starts at object position
            int visualWidth = (int)(obj.getWidth() * scaleFactor);
            int visualHeight = (int)(obj.getHeight() * scaleFactor);

            // Collision area covers the full visual object size
            int collisionX = visualX;
            int collisionWidth = visualWidth;
            int collisionHeight = visualHeight;
            int collisionY = visualY;

            if (x < collisionX + collisionWidth &&
                x + width > collisionX &&
                y < collisionY + collisionHeight &&
                y + height > collisionY) {
                return true;
            }
        }
        return false;
    }
}
