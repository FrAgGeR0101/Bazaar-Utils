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
 * Filters / shortens Hypixel “stash” reminder messages and
 * shows a one-time usage hint after the first manual stash claim.
 *
 * <p>No Fabric, Lombok or YACL – pure 1.8.9 Forge.</p>
 */
public final class StashMessages implements BUListener {

    /* ───────────────────────── config flags ───────────────────────── */

    private boolean removeMessages;           // main ON/OFF switch
    private boolean stashPreviouslyClaimed;   // one-time hint shown?

    /* ───────────────────────── internal helpers ───────────────────── */

    /** Rolling window of already-seen chat lines for pattern matching */
    private final List<String> lastLines = new ArrayList<>(
            Collections.singleton(""));      // dummy element

    /** Static fragments that make up the multi-line reminder */
    private static final String[] PATTERN = {
            " ",                                          // empty spacer
            "materials stashed away",
            "types of material stashed",
            "to pick them up",
            "  "                                          // double-space line
    };

    /* ───────────────────────── construction ───────────────────────── */

    public StashMessages(boolean remove) {
        this.removeMessages        = remove;
        this.stashPreviouslyClaimed = BUConfig.get().stashMessages.stashPreviouslyClaimed;
    }

    /* ───────────────────────── BUListener ─────────────────────────── */

    @Override
    public void subscribe() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    /* ───────────────────────── public toggles ─────────────────────── */

    public boolean isRemoveMessages()        { return removeMessages; }
    public void    setRemoveMessages(boolean b) {
        this.removeMessages = b;
        BUConfig.HANDLER.save();
    }

    /* ───────────────────────── chat hook ──────────────────────────── */

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent ev) {
        String raw = ev.message.getUnformattedText();

        /* 1) one-time tip after user claims stash manually */
        if (raw.contains("You picked up") && raw.contains("from your material stash")) {
            if (!stashPreviouslyClaimed) {
                stashPreviouslyClaimed = true;
                BUConfig.get().stashMessages.stashPreviouslyClaimed = true;
                BUConfig.HANDLER.save();

                Util.tickExecuteLater(2, () -> Util.notifyAll(
                        "TIP – Use " + BazaarUtils.STASH_HELPER.getUsage() +
                        " to auto-claim stash!  Disable these messages in BU config."));
            }
            return;                                     // never filtered
        }

        /* 2) optional auto-removal of the 5-line reminder */
        if (!removeMessages) return;

        int role = classify(raw);
        if (role == -1) { lastLines.clear(); return; }  // unrelated message

        /* build rolling window and decide whether to suppress */
        if (role == lastLines.size()) {
            lastLines.add(raw);                         // next expected line
            if (lastLines.size() == PATTERN.length)     // reached the end
                lastLines.clear();
            ev.setCanceled(true);                       // hide this line
        } else {
            lastLines.clear();                          // pattern broken
            if (role == 0) lastLines.add(raw);          // maybe new start
        }
    }

    /* ───────────────────────── helper methods ─────────────────────── */

    /** Identify which PATTERN fragment the line belongs to (-1 = none). */
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
