package dev.livedin;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/** Entry point shared by the client and server. Gameplay comes in the next milestone. */
@Mod(LivedIn.MOD_ID)
public final class LivedIn {
    public static final String MOD_ID = "livedin";
    private static final Logger LOGGER = LogUtils.getLogger();

    public LivedIn() {
        LOGGER.info("Lived-In loaded: the journey toward a more inhabited world begins.");
    }
}
