package dev.livedin.entity.activity;

import dev.livedin.entity.NpcRole;

/** Ordinary bounded walking and looking need no additional scene state. */
public final class TravelerActivity implements NpcActivity {
    @Override public NpcRole role() { return NpcRole.TRAVELER; }
}
