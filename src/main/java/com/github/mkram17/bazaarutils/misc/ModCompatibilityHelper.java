package com.github.mkram17.bazaarutils.misc;

import com.github.mkram17.bazaarutils.utils.Util;
import com.google.gson.*;
import net.minecraftforge.fml.common.Loader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tiny, self-contained “shim” that tries to inter-operate with a few
 * popular client-side mods – but **never** declares a hard compile-time
 * dependency on them.  
 * <p>
 * Everything is done either via:
 * <ul>
 *   <li>{@code Loader.isModLoaded(...)} – Forge’s runtime check</li>
 *   <li>plain Java reflection (catching <em>NoClassDefFoundError</em>
 *       and <em>ReflectiveOperationException</em>)</li>
 * </ul>
 * so the whole class is 100 % optional on non-modded installs and will
 * happily no-op when a target mod is missing.
 */
public final class ModCompatibilityHelper {

    /* ────────────────────────── target mod-ids ────────────────────────── */

    private static final String MOD_REI        = "roughlyenoughitems";
    private static final String MOD_SKYBLOCKER = "skyblocker";
    private static final String MOD_AMECS      = "amecs-reborn";

    /* ────────────────────────── misc constants  ───────────────────────── */

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final String REI_CFG_FILE =
            "config/roughlyenoughitems/config.json5";                 // relative to .minecraft

    private static final String REI_APPEARANCE     = "appearance";
    private static final String REI_BOUNDS_COLUMNS = "horizontalEntriesBoundariesColumns";
    private static final int    REI_BOUNDS_VALUE   = 16;

    /* ────────────────────────── public flags     ───────────────────────── */

    /** <b>true</b> iff Amecs-Reborn is present (set during init). */
    public static boolean AMECS_PRESENT = false;

    /* ────────────────────────── one-shot initialiser ───────────────────── */

    public static void init() {

        /* 1) REI – patch one JSON value to a saner default */
        if (Loader.isModLoaded(MOD_REI)) {
            Util.notifyAll("REI detected – patching its config", Util.NotificationType.FEATURE);
            patchReiConfig();
        }

        /* 2) Amecs-Reborn – remember presence for key-binding helpers */
        AMECS_PRESENT = Loader.isModLoaded(MOD_AMECS);

        /* 3) Skyblocker – nothing to do at launch; changes are done on demand */
    }

    /* ────────────────────────── REI helper  ───────────────────────────── */

    private static void patchReiConfig() {
        File  mcDir   = net.minecraftforge.fml.common.FMLCommonHandler.instance()
                              .getMinecraftServerInstance().getFile(".");
        Path  cfgPath = mcDir.toPath().resolve(REI_CFG_FILE);

        if (!Files.isRegularFile(cfgPath)) {
            Util.notifyError("REI config not found at " + cfgPath, null);
            return;
        }

        JsonObject root;
        try (BufferedReader r = Files.newBufferedReader(cfgPath, StandardCharsets.UTF_8)) {
            root = JsonParser.parseReader(r).getAsJsonObject();
        } catch (Exception e) {
            Util.notifyError("Cannot read REI config – JSON5 comments present?", e);
            return;
        }

        /* Drill down: appearance.horizontalEntriesBoundariesColumns = 16 */
        if (root.has(REI_APPEARANCE) && root.get(REI_APPEARANCE).isJsonObject()) {
            JsonObject appearance = root.getAsJsonObject(REI_APPEARANCE);
            appearance.addProperty(REI_BOUNDS_COLUMNS, REI_BOUNDS_VALUE);
            Util.notifyAll("Patched REI → " + REI_BOUNDS_COLUMNS + " = " + REI_BOUNDS_VALUE,
                           Util.NotificationType.FEATURE);
        } else {
            Util.notifyError("Unexpected REI config layout – “appearance” missing", null);
            return;
        }

        try (BufferedWriter w = Files.newBufferedWriter(cfgPath, StandardCharsets.UTF_8)) {
            GSON.toJson(root, w);
        } catch (IOException e) {
            Util.notifyError("Failed to write REI config", e);
        }
    }

    /* ────────────────────────── Skyblocker toggles ───────────────────── */

    /**
     * Temporarily **disable** Skyblocker’s Bazaar overlay (if present).
     * Call again with {@link #enableSkyblockerBazaarOverlay()} afterwards.
     *
     * @return {@code true} if the flag was successfully changed or the
     *         overlay was already off – {@code false} on errors.
     */
    public static boolean disableSkyblockerBazaarOverlay() {
        return setSkyblockerBazaarOverlay(false);
    }

    /** Re-enable the overlay after a previous disable call. */
    public static boolean enableSkyblockerBazaarOverlay() {
        return setSkyblockerBazaarOverlay(true);
    }

    /* reflection-based flag toggle so we do not depend on Skyblocker at compile-time */
    private static boolean setSkyblockerBazaarOverlay(boolean state) {
        if (!Loader.isModLoaded(MOD_SKYBLOCKER)) return false;

        try {
            Class<?> mgrCls   = Class.forName("de.hysky.skyblocker.config.SkyblockerConfigManager");
            Class<?> cfgCls   = Class.forName("de.hysky.skyblocker.config.SkyblockerConfig");

            Object   cfg      = mgrCls.getMethod("get").invoke(null);
            Object   ui       = cfgCls.getField("uiAndVisuals").get(cfg);
            Object   overlay  = ui.getClass().getField("searchOverlay").get(ui);

            java.lang.reflect.Field f = overlay.getClass().getField("enableBazaar");
            boolean current = f.getBoolean(overlay);

            if (current == state) return true;            // already desired value
            f.setBoolean(overlay, state);

            // persist
            mgrCls.getMethod("update", java.util.function.Consumer.class)
                  .invoke(null, (java.util.function.Consumer<Object>) (o) -> { /* already set */ });

            return true;
        } catch (Throwable t) {                           // NoClassDefFound, reflection etc.
            Util.notifyError("Skyblocker overlay toggle failed", t);
            return false;
        }
    }

    /* ────────────────────────── utility – no instantiation ───────────── */

    private ModCompatibilityHelper() {}
}
