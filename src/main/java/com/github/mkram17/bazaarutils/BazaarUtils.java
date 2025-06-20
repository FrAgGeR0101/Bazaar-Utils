package com.github.mkram17.bazaarutils;

import com.github.mkram17.bazaarutils.config.BUConfig;
import com.github.mkram17.bazaarutils.events.BUListener;
import com.github.mkram17.bazaarutils.features.Bookmark;
import com.github.mkram17.bazaarutils.features.StashHelper;
import com.github.mkram17.bazaarutils.misc.ModCompatibilityHelper;
import com.github.mkram17.bazaarutils.utils.Commands;
import com.github.mkram17.bazaarutils.utils.GUIUtils;
import com.mojang.serialization.Codec;
import de.siphalor.amecs.api.AmecsKeyBinding;
import lombok.Getter;
import meteordevelopment.orbit.EventBus;
import meteordevelopment.orbit.IEventBus;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.CustomValue;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.component.DataComponentType;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

/**
 * Main Fabric entry-point for Bazaar-Utils.
 */
public final class BazaarUtils implements ClientModInitializer {

    /* -------------------------------------------------------- */
    /*  Public constants / globals                              */
    /* -------------------------------------------------------- */

    public static final String   MODID      = "bazaarutils";
    public static final IEventBus EVENT_BUS = new EventBus();          // Orbit bus
    public static final GUIUtils  GUI        = new GUIUtils();

    public static StashHelper                STASH_HELPER;
    public static final List<KeyBinding>     KEYBINDS = new ArrayList<>();

    public static boolean updatedMajorVersion = false;

    @Getter private static String updateNotes = "n/a";

    /* -------------------------------------------------------- */
    /*  Component-types (custom NBT-style data)                 */
    /* -------------------------------------------------------- */

    public static final DataComponentType<String>  CUSTOM_SIZE_COMPONENT = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            new Identifier(MODID, "custom_size"),
            DataComponentType.<String>builder().codec(Codec.STRING).build()
    );

    public static final DataComponentType<Boolean> CUSTOM_SHOWPRICECHART_COMPONENT = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            new Identifier(MODID, "has_price_chart"),
            DataComponentType.<Boolean>builder().codec(Codec.BOOL).build()
    );

    /* -------------------------------------------------------- */
    /*  Client entry-point                                      */
    /* -------------------------------------------------------- */

    @Override
    public void onInitializeClient() {

        /* Load (or create) config ------------------------------------ */
        BUConfig.HANDLER.load();

        /* Apply run-time compatibility patches ----------------------- */
        ModCompatibilityHelper.initializePatches();

        /* Read mod-metadata (updates / changelog etc.) --------------- */
        extractModMetadata();

        /* Prepare Orbit event-bus lambda support --------------------- */
        EVENT_BUS.registerLambdaFactory(
                "com.github.mkram17.bazaarutils",
                (lookupInMethod, klass) ->
                        (MethodHandles.Lookup) lookupInMethod.invoke(null, klass, MethodHandles.lookup())
        );

        /* Register commands & key-bindings --------------------------- */
        registerCommands();
        registerKeyBindings();

        /* Subscribe all listeners (config + transient) --------------- */
        subscribeListeners();

        /* Populate default config entries on first run --------------- */
        createDefaultBookmarks();
    }

    /* -------------------------------------------------------- */
    /*  Helpers                                                 */
    /* -------------------------------------------------------- */

    /** Register /bu … commands via Fabric-API callback. */
    private static void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register(
                (dispatcher, __) -> Commands.register(dispatcher)
        );
    }

    /** Register the stash-helper key-binding (Amecs optional). */
    private static void registerKeyBindings() {
        if (!ModCompatibilityHelper.isAmecsReborn()) return;

        STASH_HELPER = new StashHelper();
        STASH_HELPER.registerTickCounter();
        KEYBINDS.add(STASH_HELPER);

        for (KeyBinding kb : KEYBINDS) {
            /* Only AmecsKeyBinding allows per-key repeat-delay options */
            if (kb instanceof AmecsKeyBinding) {
                KeyBindingHelper.registerKeyBinding(kb);
            }
        }
    }

    /** Gather & subscribe every BUListener instance. */
    private static void subscribeListeners() {
        BUListener.addTransientEvents();                        // create runtime listeners

        List<BUListener> all   = BUListener.getTransientEvents();
        all.addAll(BUConfig.get().getSerializedEvents());       // + persistent

        all.forEach(BUListener::subscribe);
    }

    /** First-run defaults (a single “Diamond” bookmark). */
    private static void createDefaultBookmarks() {
        if (BUConfig.get().bookmarks.isEmpty()) {
            BUConfig.get().bookmarks
                     .add(new Bookmark("Diamond", Items.DIAMOND.getDefaultStack()));
        }
    }

    /** Read `fabric.mod.json` custom fields & detect version bumps. */
    private static void extractModMetadata() {
        FabricLoader.getInstance().getModContainer(MODID).ifPresent(mc -> {
            ModMetadata meta = mc.getMetadata();

            /* Latest changelog entry (custom value) */
            CustomValue cv = meta.getCustomValue("latestMajorUpdateNotes");
            if (cv != null) updateNotes = cv.getAsString();

            /* Version-bump detection (major = “x.y” part) */
            String previous = BUConfig.get().MODVERSION;
            String current  = meta.getVersion().getFriendlyString();

            BUConfig.get().MODVERSION = current;
            BUConfig.HANDLER.save();

            String prevMajor = previous.contains(".")
                    ? previous.substring(previous.indexOf('.') + 1)
                    : previous;
            String currMajor = current.contains(".")
                    ? current.substring(current.indexOf('.') + 1)
                    : current;

            updatedMajorVersion = !prevMajor.equals(currMajor);
        });
    }
}
