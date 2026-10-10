package dev.livedin.entity;

/** Stable IDs are saved in worlds; do not reorder them when adding future roles. */
public enum NpcRole {
    TRAVELER(0), MUSICIAN(1), CAMP_COMPANION(2), FISHER(3), BOAT_RIDER(4);

    private final int id;
    NpcRole(int id) { this.id = id; }
    public int id() { return id; }
    public static NpcRole fromId(int id) {
        for (NpcRole role : values()) {
            if (role.id == id) return role;
        }
        return TRAVELER;
    }
}
