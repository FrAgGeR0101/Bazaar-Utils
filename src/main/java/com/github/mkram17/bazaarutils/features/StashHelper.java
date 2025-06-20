package com.github.mkram17.bazaarutils.features;

import com.github.mkram17.bazaarutils.BazaarUtils;
import com.github.mkram17.bazaarutils.events.BUListener;
import com.github.mkram17.bazaarutils.utils.GUIUtils;
import com.github.mkram17.bazaarutils.utils.Util;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.event.InputUpdateEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

/**
 * ALT+V helper that closes the current GUI and runs “/pickupstash”.
 * <p>Pure Forge 1.8.9 – no Amecs or Fabric APIs.</p>
 */
public final class StashHelper implements BUListener {

    /* ───────────────────────── key-binding ───────────────────────── */
    private static final KeyBinding KEY = new KeyBinding(
            "key.bu.pickupstash",            // localisation key
            Keyboard.KEY_V,                  // default
            "Bazaar-Utils");                 // category

    private static boolean altHeld() {
        return Keyboard.isKeyDown(Keyboard.KEY_LMENU) ||
               Keyboard.isKeyDown(Keyboard.KEY_RMENU);
    }

    /* ───────────────────────── debounce ──────────────────────────── */
    private int ticksSinceLastPress = 20;    // “ready” at start

    /* ───────────────────────── ctor / install ────────────────────── */
    public StashHelper() {
        ClientRegistry.registerKeyBinding(KEY);
    }

    /** Modern entry-point used by fresh code. */
    public void registerTickCounter() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    /** Legacy alias kept for older source files. */
    public void startTickCounter() {
        registerTickCounter();               // delegate
    }

    @Override public void subscribe() {
        // also subscribe to the lightweight Orbit bus, if needed
        BazaarUtils.EVENT_BUS.subscribe(this);
    }

    /* ───────────────────────── tick handler ──────────────────────── */
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent ev) {
        if (ev.phase != TickEvent.Phase.END) return;

        ++ticksSinceLastPress;

        if (KEY.isKeyDown() && altHeld() && ticksSinceLastPress > 10) {
            ticksSinceLastPress = 0;
            GUIUtils.closeHandledScreen();      // safely dismiss any GUI
            Util.sendCommand("pickupstash");    // run the command
        }
    }

    /* ───────────────────────── small helper ──────────────────────── */
    public String getUsage() {
        return "ALT + " + Keyboard.getKeyName(KEY.getKeyCode());
    }
}
