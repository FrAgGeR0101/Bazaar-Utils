package com.github.mkram17.bazaarutils.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

/**
 * Lightweight helper to play vanilla sounds client-side.
 * <p>No Fabric classes, no registry look-ups – 100 % Forge 1.8.9.</p>
 */
public final class SoundUtil {

    private SoundUtil() { /* util-class */ }

    /* ────────────────────────────────────────────────────────── */
    /*  generic helpers                                           */
    /* ────────────────────────────────────────────────────────── */

    /**
     * Plays a sound at the player’s location.
     *
     * @param soundId The vanilla 1.8.9 sound identifier
     *                (e.g. {@code "random.click"} or {@code "note.pling"}).
     * @param volume  0 – 1 f
     * @param pitch   0.5 – 2 f
     */
    public static void play(String soundId, float volume, float pitch) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer p = mc.thePlayer;
        if (p != null) {
            p.playSound(soundId, volume, pitch);
        }
    }

    /** Convenience overload with default pitch = 1 f. */
    public static void play(String soundId, float volume) {
        play(soundId, volume, 1.0f);
    }

    /* ────────────────────────────────────────────────────────── */
    /*  high-level helpers                                        */
    /* ────────────────────────────────────────────────────────── */

    /** Short UI “click” (volume 0.25). */
    public static void playClick() {
        play("random.click", 0.25f);
    }

    /**
     * Plays a notification “ding” several times, spaced ~200 ms apart.
     * @param times number of dings
     */
    public static void notifyMultipleTimes(int times) {
        final String DING = "random.orb";      // short pleasant sound
        for (int i = 0; i < times; i++) {
            final int delay = i * 4;           // 4 ticks ≈ 200 ms
            Util.tickExecuteLater(delay, () -> play(DING, 0.8f));
        }
    }
}
