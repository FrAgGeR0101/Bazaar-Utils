package com.github.mkram17.bazaarutils;

import com.github.mkram17.bazaarutils.config.BUConfig;
import com.github.mkram17.bazaarutils.events.BUListener;
import com.github.mkram17.bazaarutils.features.Bookmark;
import com.github.mkram17.bazaarutils.features.StashHelper;
import com.github.mkram17.bazaarutils.misc.ModCompatibilityHelper;
import com.github.mkram17.bazaarutils.utils.Commands;
import com.github.mkram17.bazaarutils.utils.GUIUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Forge-1.8.9 entry-point for Bazaar-Utils.
 *
 * Completely standalone – no Fabric-API, Lombok, Orbit, Amecs, or modern
 * 1.20.x component system required.
 */
@Mod(modid = BazaarUtils.MODID,
     name  = "Bazaar Utils",
     version = "1.0.0",
     clientSideOnly = true)
public final class BazaarUtils {

    /* -------------------------------------------------------- */
    /*  Public constants / globals                              */
    /* -------------------------------------------------------- */

    public static final String MODID = "bazaarutils";

    /** Lightweight helpers used everywhere in the mod. */
    public static final GUIUtils GUI = new GUIUtils();

    /** Optional stash-helper (key-binding, tick-handler …). */
    public static StashHelper STASH_HELPER;

    /* Transient listeners created at run-time (plus those deserialised
       from the config) – kept so we can unsubscribe on shutdown later
       if that ever becomes necessary. */
    private static final List<BUListener> ALL_LISTENERS = new ArrayList<>();

    /* -------------------------------------------------------- */
    /*  Forge lifecycle                                         */
    /* -------------------------------------------------------- */

    @EventHandler
    public void init(FMLInitializationEvent event) {

        /* 1) Load or create the JSON config ------------------------ */
        BUConfig.HANDLER.load();

        /* 2) Apply run-time compatibility patches for other mods --- */
        ModCompatibilityHelper.initializePatches();

        /* 3) Register “/bu …” chat-based commands ------------------ */
        Commands.register();   // implemented with a chat-listener internally

        /* 4) Key-binding helper (simple tick-counter in 1.8.9) ------ */
        STASH_HELPER = new StashHelper();
        STASH_HELPER.registerTickCounter();   // hooks END_CLIENT_TICK

        /* 5) Gather & subscribe every listener --------------------- */
        BUListener.addTransientEvents();                     // create on-the-fly
        ALL_LISTENERS.addAll(BUListener.getTransientEvents());
        ALL_LISTENERS.addAll(BUConfig.get().getSerializedEvents());
        ALL_LISTENERS.forEach(BUListener::subscribe);

        /* 6) First-run defaults – add a single “Diamond” bookmark --- */
        if (BUConfig.get().bookmarks.isEmpty()) {
            BUConfig.get().bookmarks.add(
                    new Bookmark("Diamond", new ItemStack(Items.diamond))
            );
        }

        /* 7) Register our utility listeners on the Forge bus -------- */
        MinecraftForge.EVENT_BUS.register(this);
    }

    /* -------------------------------------------------------- */
    /*  Tiny helpers                                            */
    /* -------------------------------------------------------- */

    /** Convenience shortcut: returns the client instance. */
    public static Minecraft mc() {
        return Minecraft.getMinecraft();
    }
}
