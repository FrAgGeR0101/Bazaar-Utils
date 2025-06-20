package com.github.mkram17.bazaarutils.misc;

import com.github.mkram17.bazaarutils.BazaarUtils;
import com.github.mkram17.bazaarutils.config.BUConfig;
import com.github.mkram17.bazaarutils.events.BUListener;
import com.github.mkram17.bazaarutils.utils.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

/**
 * Sends a one-time welcome / update message after the player joins
 * a world or server.  Forge 1.8.9 – no Fabric classes.
 */
public final class JoinMessages implements BUListener {

    /* ─────────────────────────── formatted texts ─────────────────────────── */

    private static final ChatComponentText WELCOME = new ChatComponentText(
            EnumChatFormatting.WHITE  + "[Bazaar Utils] " +
            EnumChatFormatting.GREEN  + "Thanks for installing!  Use /buconfig to configure the mod.");

    private static final ChatComponentText DISCORD = new ChatComponentText(
            EnumChatFormatting.WHITE  + "[Bazaar Utils] " +
            EnumChatFormatting.GREEN  + "Need help or found a bug?  Join the Discord: "
            + EnumChatFormatting.AQUA + "https://discord.gg/xDKjvm5hQd");

    private static ChatComponentText UPDATE()
    {
        return new ChatComponentText(EnumChatFormatting.WHITE + "[Bazaar Utils] "
                + EnumChatFormatting.DARK_GREEN + BazaarUtils.getUpdateNotes());
    }

    /* ─────────────────────────── state flags ─────────────────────────────── */

    private boolean sentThisSession = false;
    private boolean waitingForPlayer = true;      // true until first player tick

    /* ─────────────────────────── BUListener hook ─────────────────────────── */

    @Override
    public void subscribe() {

        /* Poll once every client-tick until a player exists, then send messages */
        Util.tickExecuteLater(1, new Runnable() {
            @Override public void run() {
                Minecraft mc = Minecraft.getMinecraft();
                if (mc.thePlayer == null) {              // still on title-screen?
                    Util.tickExecuteLater(10, this);     // try again in 10 ticks
                    return;
                }
                if (sentThisSession) return;             // already greeted

                if (BUConfig.get().firstLoad) {
                    sendDelayed(WELCOME,  40);
                    sendDelayed(new ChatComponentText(Util.HELPMESSAGE), 60);
                    sendDelayed(DISCORD,  100);

                    BUConfig.get().firstLoad = false;
                    BUConfig.HANDLER.save();
                }

                if (BazaarUtils.updatedMajorVersion) {
                    sendDelayed(UPDATE(), 40);
                    BazaarUtils.updatedMajorVersion = false;
                }
                sentThisSession = true;
            }
        });
    }

    /* ─────────────────────────── tiny helper ────────────────────────────── */

    private static void sendDelayed(final ChatComponentText msg, int ticks) {
        Util.tickExecuteLater(ticks, () -> {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.thePlayer != null) mc.thePlayer.addChatMessage(msg);
        });
    }
}
