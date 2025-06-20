package com.github.mkram17.bazaarutils.features;

import com.github.mkram17.bazaarutils.BazaarUtils;
import com.github.mkram17.bazaarutils.events.BUListener;
import com.github.mkram17.bazaarutils.events.OutdatedItemEvent;
import com.github.mkram17.bazaarutils.utils.SoundUtil;
import com.github.mkram17.bazaarutils.utils.Util;
import net.minecraft.util.EnumChatFormatting;

/**
 * Warns the player when a watched order becomes outdated and (optionally)
 * auto-opens the Bazaar.  
 * Pure Forge-1.8.9 – no Fabric/YACL/Lombok APIs.
 */
public final class OutdatedItems implements BUListener {

    /* ------------------------------------------------------------------ */
    /*  Simple, runtime-configurable flags                                */
    /* ------------------------------------------------------------------ */
    private boolean autoOpen        = true;  // open Bazaar automatically
    private boolean chatNotice      = true;  // send chat message
    private boolean playBeep        = true;  // play XP-orb ping 3×

    /* ------------------------------------------------------------------ */
    /*  Event hook: fired by OutdatedItemEvent                            */
    /* ------------------------------------------------------------------ */
    public void onOutdated(OutdatedItemEvent ev) {

        /* ───────── Chat + sound notification ───────── */
        if (chatNotice) {
            Util.notifyAll(
                    EnumChatFormatting.GOLD + "[Bazaar-Utils] " +
                    EnumChatFormatting.RESET +
                    "Your " +
                    ev.getItem().getPriceType().getHumanName() +     // changed
                    " for " +
                    EnumChatFormatting.DARK_PURPLE + "" + EnumChatFormatting.BOLD +
                    ev.getItem().getVolume() + "× " +
                    EnumChatFormatting.GOLD + "" + EnumChatFormatting.BOLD +
                    ev.getItem().getName() +
                    EnumChatFormatting.RESET +
                    " is now outdated.");
        }

        if (playBeep) SoundUtil.notifyMultipleTimes(3);

        /* ───────── Auto-open Bazaar after 3-sec countdown ───────── */
        if (autoOpen && !BazaarUtils.GUI.inBazaar()) {
            new Thread(() -> {
                try {
                    for (int i = 3; i > 0; i--) {
                        Util.notifyAll("Opening Bazaar in " + i + " …");
                        Thread.sleep(1_000);
                    }
                    Util.sendCommand("bz");
                } catch (InterruptedException ignored) {}
            },"BU-AutoOpen").start();
        }
    }

    /* ------------------------------------------------------------------ */
    /*  BUListener implementation                                         */
    /* ------------------------------------------------------------------ */
    @Override public void subscribe() {
        BazaarUtils.EVENT_BUS.subscribe(this);
    }

    /* ------------------------------------------------------------------ */
    /*  Tiny toggles – could be wired into your GUI later                 */
    /* ------------------------------------------------------------------ */
    public boolean isAutoOpen()  { return autoOpen;  }
    public boolean isChatNotice(){ return chatNotice;}
    public boolean isPlayBeep() { return playBeep;  }

    public void setAutoOpen (boolean v){ autoOpen  = v; }
    public void setChatNotice(boolean v){ chatNotice= v; }
    public void setPlayBeep (boolean v){ playBeep  = v; }
}
