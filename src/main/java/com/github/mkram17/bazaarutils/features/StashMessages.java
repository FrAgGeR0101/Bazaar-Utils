package com.github.mkram17.bazaarutils.features;

import com.github.mkram17.bazaarutils.BazaarUtils;
import com.github.mkram17.bazaarutils.config.BUConfig;
import com.github.mkram17.bazaarutils.events.BUListener;
import com.github.mkram17.bazaarutils.utils.Util;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.util.ChatComponentText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Filters / shortens Hypixel material-stash reminders and shows a one-time
 * usage tip after the first manual stash claim.
 * <p>Pure Forge 1.8.9 – no Fabric / Lombok / YACL dependencies.</p>
 */
public final class StashMessages implements BUListener {

    /* ───────────────────────── config flags ───────────────────────── */
    private boolean removeMessages;          // ON/OFF switch in GUI
    private boolean stashPreviouslyClaimed;  // one-time hint already shown?

    /* ───────────────────────── pattern helpers ────────────────────── */
    private final List<String> recent = new ArrayList<>(Collections.singleton(""));
    private static final String[] PATTERN = {
            " ",                               // single space line
            "materials stashed away",
            "types of material stashed",
            "to pick them up",
            "  "                               // double-space line
    };

    /* ───────────────────────── construction ───────────────────────── */
    public StashMessages(boolean remove) {
        this.removeMessages        = remove;
        this.stashPreviouslyClaimed = BUConfig.get().isStashTipShown();
    }

    /* ───────────────────────── BUListener ─────────────────────────── */
    @Override
    public void subscribe() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    /* ───────────────────────── public toggle ──────────────────────── */
    public boolean isRemoveMessages()          { return removeMessages; }
    public void    setRemoveMessages(boolean b){
        removeMessages = b;
        BUConfig.get().setRemoveStashMessages(b);
        BUConfig.save();                       // single static save helper
    }

    /* ───────────────────────── chat hook ──────────────────────────── */
    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent ev) {
        String raw = ev.message.getUnformattedText();

        /* 1) one-time hint after manual stash pickup */
        if (raw.contains("You picked up") && raw.contains("from your material stash")) {
            if (!stashPreviouslyClaimed) {
                stashPreviouslyClaimed = true;
                BUConfig.get().setStashTipShown(true);
                BUConfig.save();

                Util.tickExecuteLater(2, () -> Util.notifyAll(
                        "TIP – Use " + BazaarUtils.STASH_HELPER.getUsage() +
                        " to auto-claim stash!  Disable these messages in the BU config."));
            }
            return;                        // never filtered
        }

        /* 2) optional five-line reminder suppression */
        if (!removeMessages) return;

        int role = classify(raw);
        if (role == -1) {                  // unrelated chat line
            recent.clear();
            return;
        }

        /* rolling-window comparison */
        if (role == recent.size()) {
            recent.add(raw);
            ev.setCanceled(true);          // suppress this line
            if (recent.size() == PATTERN.length) recent.clear();
        } else {
            recent.clear();                // pattern broken – reset
            if (role == 0) recent.add(raw);
        }
    }

    /* ───────────────────────── helpers ───────────────────────────── */
    private static int classify(String s) {
        for (int i = 0; i < PATTERN.length; i++) {
            if (PATTERN[i].trim().isEmpty()) {
                if (s.trim().isEmpty()) return i;
            } else if (s.contains(PATTERN[i])) {
                return i;
            }
        }
        return -1;
    }
}
