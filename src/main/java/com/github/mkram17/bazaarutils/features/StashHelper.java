package com.github.mkram17.bazaarutils.features;

import com.github.mkram17.bazaarutils.utils.GUIUtils;
import com.github.mkram17.bazaarutils.utils.Util;
import com.github.mkram17.bazaarutils.events.BUListener;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;

/**
 * Simple “/pickupstash” helper:
 * <ul>
 *   <li>ALT&nbsp;+&nbsp;V by default</li>
 *   <li>Debounced – needs at least 10 client-ticks between key-presses</li>
 *   <li>Closes the current GUI, then runs the command</li>
 * </ul>
 *
 * <p>No Fabric, no Amecs – plain Forge 1.8.9 key-binding and tick-event.</p>
 */
public final class StashHelper implements BUListener {

    /* ------------------------------------------------------------ */
    /*  key binding                                                 */
    /* ------------------------------------------------------------ */

    private static final KeyBinding KEY =
            new KeyBinding("key.bu.pickupstash",
                           Keyboard.KEY_V,
                           "Bazaar-Utils");

    /* modifier: hold either left- or right-ALT together with V      */
    private static boolean altHeld() {
        return Keyboard.isKeyDown(Keyboard.KEY_LMENU) ||
               Keyboard.isKeyDown(Keyboard.KEY_RMENU);
    }

    /* ------------------------------------------------------------ */
    /*  debounce state                                              */
    /* ------------------------------------------------------------ */

    private int ticksSinceLastPress = 20;   // start “ready”

    /* ------------------------------------------------------------ */
    /*  lifecycle                                                   */
    /* ------------------------------------------------------------ */

    public StashHelper() {
        ClientRegistry.registerKeyBinding(KEY);
    }

    @Override
    public void subscribe() {
        /* Orbit already registered in BazaarUtils */
        com.github.mkram17.bazaarutils.BazaarUtils.EVENT_BUS.subscribe(this);
    }

    /* ------------------------------------------------------------ */
    /*  tick handler                                                */
    /* ------------------------------------------------------------ */

    @SubscribeEvent
    @SuppressWarnings("unused")
    public void onClientTick(TickEvent.ClientTickEvent ev) {
        if (ev.phase != TickEvent.Phase.END) return;

        ++ticksSinceLastPress;

        if (KEY.isKeyDown() && altHeld() && ticksSinceLastPress > 10) {
            ticksSinceLastPress = 0;
            GUIUtils.closeHandledScreen();
            Util.sendCommand("pickupstash");
        }
    }

    /* ------------------------------------------------------------ */
    /*  small helper shown in settings-GUI (optional)               */
    /* ------------------------------------------------------------ */

    public String getUsage() {
        return "ALT + " + Keyboard.getKeyName(KEY.getKeyCode());
    }
}
