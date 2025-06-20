package com.github.mkram17.bazaarutils.features;

import com.github.mkram17.bazaarutils.BazaarUtils;
import com.github.mkram17.bazaarutils.config.BUConfig;
import com.github.mkram17.bazaarutils.events.BUListener;
import com.github.mkram17.bazaarutils.events.OutdatedItemEvent;
import com.github.mkram17.bazaarutils.misc.CustomItemButton;      // tiny Option-stub lives here
import com.github.mkram17.bazaarutils.utils.SoundUtil;
import com.github.mkram17.bazaarutils.utils.Util;
import net.minecraft.util.EnumChatFormatting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * Handles “order outdated” notifications + optional auto-opening of the Bazaar.
 * <p>No Fabric / YACL / Lombok / Orbit dependencies – pure Forge 1.8.9.</p>
 */
public final class OutdatedItems implements BUListener {

    /* ───────────────────────── configurable flags ───────────────────────── */

    private boolean autoOpenEnabled;
    private boolean notifyOutdated;
    private boolean notificationSound;

    /* ───────────────────────────── ctor / config ────────────────────────── */

    public OutdatedItems(boolean autoOpen, boolean notify, boolean sound) {
        this.autoOpenEnabled   = autoOpen;
        this.notifyOutdated    = notify;
        this.notificationSound = sound;
    }

    /* ───────────────────────────── event hook ───────────────────────────── */

    /** Called by the simple Orbit event-bus when an order becomes outdated. */
    public void onOutdated(OutdatedItemEvent ev) {

        /* 1) chat notice + optional sound */
        if (notifyOutdated) {
            Util.notifyAll(
                    EnumChatFormatting.GOLD + "[Bazaar-Utils] " + EnumChatFormatting.RESET +
                    "Your " + ev.getItem().getPriceType().getString() + " for " +
                    EnumChatFormatting.DARK_PURPLE + "" + EnumChatFormatting.BOLD +
                    ev.getItem().getVolume() + "x " +
                    EnumChatFormatting.GOLD + "" + EnumChatFormatting.BOLD +
                    ev.getItem().getName() + EnumChatFormatting.RESET +
                    " is now outdated.");

            if (notificationSound) SoundUtil.notifyMultipleTimes(3);
        }

        /* 2) auto-open the Bazaar after a short countdown */
        if (autoOpenEnabled && !BazaarUtils.GUI.inBazaar()) {
            CompletableFuture.runAsync(() -> {
                try {
                    for (int i = 3; i > 0; i--) {
                        Util.notifyAll("Opening Bazaar in " + i + "…");
                        Thread.sleep(1_000);
                    }
                    Util.sendCommand("bz");
                } catch (InterruptedException ignored) {}
            });
        }
    }

    /* ───────────────────── tiny config-GUI helpers (stubs) ───────────────── */

    public Collection<CustomItemButton.Option<Boolean>> createOptions() {
        Collection<CustomItemButton.Option<Boolean>> list = new ArrayList<>();

        list.add(CustomItemButton.Option.<Boolean>createBuilder()
                .name("Open Bazaar on outdated orders")
                .binding(false,
                         () -> autoOpenEnabled,
                         v  -> autoOpenEnabled = v)
                .controller(BUConfig::createBooleanController)       // still points to your stub
                .build());

        list.add(CustomItemButton.Option.<Boolean>createBuilder()
                .name("Chat message on outdated orders")
                .binding(true,
                         () -> notifyOutdated,
                         v  -> notifyOutdated = v)
                .controller(BUConfig::createBooleanController)
                .build());

        list.add(CustomItemButton.Option.<Boolean>createBuilder()
                .name("Play sound on outdated orders")
                .binding(true,
                         () -> notificationSound,
                         v  -> notificationSound = v)
                .controller(BUConfig::createBooleanController)
                .build());

        return list;
    }

    /* ────────────────────────── BUListener impl. ────────────────────────── */

    @Override
    public void subscribe() {
        BazaarUtils.EVENT_BUS.subscribe(this);
    }

    /* ───────────────────────── getters / setters ────────────────────────── */

    public boolean isAutoOpenEnabled()   { return autoOpenEnabled;   }
    public boolean isNotifyOutdated()    { return notifyOutdated;    }
    public boolean isNotificationSound() { return notificationSound; }

    public void setAutoOpenEnabled(boolean v)   { autoOpenEnabled   = v; }
    public void setNotifyOutdated(boolean v)    { notifyOutdated    = v; }
    public void setNotificationSound(boolean v) { notificationSound = v; }
}
