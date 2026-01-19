package com.storyteller.entity;

/**
 * Behavior modes for Storyteller NPCs.
 * Controls how the NPC moves and interacts with its environment.
 */
public enum NPCBehaviorMode {
    /**
     * Default mode - NPC stays mostly in place with rare wandering
     */
    STATIONARY("stationary", "Stays in place with rare wandering"),

    /**
     * NPC wanders within a specified radius of an anchor position
     */
    ANCHORED("anchored", "Wanders within radius of anchor point"),

    /**
     * NPC follows a specific player at a configured distance
     */
    FOLLOW_PLAYER("follow", "Follows a specific player"),

    /**
     * NPC tries to hide from players using line-of-sight checks
     */
    HIDING("hiding", "Hides from players behind blocks");

    private final String id;
    private final String description;

    NPCBehaviorMode(String id, String description) {
        this.id = id;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Get a behavior mode from its string ID
     * @param id The mode ID (case-insensitive)
     * @return The behavior mode, or STATIONARY if not found
     */
    public static NPCBehaviorMode fromId(String id) {
        if (id == null || id.isEmpty()) {
            return STATIONARY;
        }
        for (NPCBehaviorMode mode : values()) {
            if (mode.id.equalsIgnoreCase(id)) {
                return mode;
            }
        }
        return STATIONARY;
    }
}
