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
 * No Fabric, no Lombok – just plain vanilla + Forge classes.
 */
@Mod(modid           = BazaarUtils.MODID,
     name            = "Bazaar Utils",
     version         = "1.0.0",
     clientSideOnly  = true)
public final class BazaarUtils {

    /* ─────────────────────────────────────────────────────────
       Public constants / singletons
       ───────────────────────────────────────────────────────── */
    public static final String MODID = "bazaarutils";

    /** Lightweight GUI helper used by most features. */
    public static final GUIUtils GUI = new GUIUtils();

    /** Optional helper that closes the Bazaar and runs /pickupstash. */
    public static StashHelper STASH_HELPER;

    /** All listeners that need to be (un)subscribed at runtime. */
    private static final List<BUListener> ALL_LISTENERS = new ArrayList<>();

    /* ─────────────────────────────────────────────────────────
       Forge lifecycle
       ───────────────────────────────────────────────────────── */
    @EventHandler
    public void init(FMLInitializationEvent e) {

        /* 1 ─ Load (or create) the JSON config */
        BUConfig.load();                       // static helper

        /* 2 ─ Runtime compatibility tweaks for other mods */
        ModCompatibilityHelper.initPatches();

        /* 3 ─ Register chat-based “/bu …” command */
        Commands.register();                   // no args since refactor

        /* 4 ─ Create optional key-binding helper */
        STASH_HELPER = new StashHelper();
        STASH_HELPER.startTickCounter();       // hooks END_CLIENT_TICK

        /* 5 ─ Gather and subscribe every event listener */
        BUListener.addTransientEvents();                       // build on-the-fly
        ALL_LISTENERS.addAll(BUListener.getTransientEvents());
        ALL_LISTENERS.addAll(BUConfig.get().getSerializedEvents());
        ALL_LISTENERS.forEach(BUListener::subscribe);

        /* 6 ─ First-run defaults: add a single “Diamond” bookmark */
        if (BUConfig.get().getBookmarks().isEmpty()) {
            BUConfig.get().getBookmarks()
                    .add(new Bookmark("Diamond", new ItemStack(Items.diamond)));
        }

        /* 7 ─ Register the mod itself on the Forge event bus */
        MinecraftForge.EVENT_BUS.register(this);
    }

    /* ─────────────────────────────────────────────────────────
       Convenience
       ───────────────────────────────────────────────────────── */
    public static Minecraft mc() {                   // short-hand
        return Minecraft.getMinecraft();
    }
}
