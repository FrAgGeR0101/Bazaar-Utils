package com.github.mkram17.bazaarutils.utils;

import com.github.mkram17.bazaarutils.config.BUConfig;
import com.github.mkram17.bazaarutils.events.BUListener;
import com.github.mkram17.bazaarutils.misc.ItemData;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;                      // 1.8.9 class
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import org.apache.logging.log4j.LogManager;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Generic helpers (chat notifications, clipboard, file-I/O, tick scheduling …).
 */
public final class Util implements BUListener {

    /* ────────────────────────────────────────────────────────────────
       Developer-notification helper
       ──────────────────────────────────────────────────────────────── */
    public enum notificationTypes {GUI, FEATURE, BAZAARDATA, COMMAND, ITEMDATA}

    /* ------------------------------------------------------------------
       Help-text shown in chat
       ------------------------------------------------------------------ */
    public static final String HELPMESSAGE =
            "§eCommands: §7/bu or /bazaarutils opens the settings GUI\n" +
            "§6----------------------------------------\n" +
            "§7/bu tax <amount> – set Bazaar tax\n" +
            "§7/bu customorder … – manage custom orders\n" +
            "§6----------------------------------------";

    /* ------------------------------------------------------------------
       Simple in-game task scheduler
       ------------------------------------------------------------------ */
    private static final class ScheduledTask {
        int     ticksLeft;
        Runnable action;
        ScheduledTask(int t, Runnable a) { ticksLeft = t; action = a; }
    }
    private static final LinkedList<ScheduledTask> TASKS = new LinkedList<>();

    /* ------------------------------------------------------------------
       BUListener – subscribe to the client-tick callback once
       ------------------------------------------------------------------ */
    @Override
    public void subscribe() {
        ClientTickEvents.END_CLIENT_TICK.register(c -> {
            synchronized (TASKS) {
                Iterator<ScheduledTask> it = TASKS.iterator();
                while (it.hasNext()) {
                    ScheduledTask t = it.next();
                    if (--t.ticksLeft <= 0) {
                        t.action.run();
                        it.remove();
                    }
                }
            }
        });
    }

    /** schedule a task for <code>ticks</code> client-ticks later */
    public static void tickExecuteLater(int ticks, Runnable action) {
        synchronized (TASKS) {
            TASKS.add(new ScheduledTask(ticks, action));
        }
    }

    /* ────────────────────────────────────────────────────────────────
       Chat + log helpers (Forge-1.8.9 API)
       ──────────────────────────────────────────────────────────────── */

    private static void sendChat(String raw) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null)
            mc.thePlayer.addChatMessage(new ChatComponentText(raw));
    }

    public static void notifyAll(String msg) {
        notifyAll(msg, notificationTypes.GUI);
    }

    public static void notifyAll(String msg, notificationTypes type) {
        if (!type.isEnabled() && !BUConfig.get().developer.allMessages) return;

        String prefix = EnumChatFormatting.GOLD + "[Bazaar Utils] " + EnumChatFormatting.RESET;
        sendChat(prefix + msg);
        LogManager.getLogger(getCallingClassName()).info(msg);
    }

    public static void notifyError(String msg, Throwable t) {
        String prefix = EnumChatFormatting.RED + "[Bazaar-Utils Error] " + EnumChatFormatting.RESET;
        sendChat(prefix + msg + " (click for support)",
                new ClickEvent(ClickEvent.Action.OPEN_URL, "https://discord.gg/xDKjvm5hQd"),
                new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                               new ChatComponentText("Open Discord support server")));

        if (t != null)
            LogManager.getLogger(getCallingClassName()).error(msg, t);
        else
            LogManager.getLogger(getCallingClassName()).error(msg);
    }

    /** helper for clickable messages */
    private static void sendChat(String base,
                                 ClickEvent click, HoverEvent hover) {
        ChatComponentText comp = new ChatComponentText(base);
        ChatStyle style = new ChatStyle();
        style.setChatClickEvent(click);
        style.setChatHoverEvent(hover);
        comp.setChatStyle(style);
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null) mc.thePlayer.addChatMessage(comp);
    }

    /* ────────────────────────────────────────────────────────────────
       Misc. helpers
       ──────────────────────────────────────────────────────────────── */

    public static void sendCommand(String cmd) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null) mc.thePlayer.sendChatMessage("/" + cmd);
    }

    public static void copyToClipboard(String s) {
        Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
        cb.setContents(new StringSelection(s), null);
    }

    public static String removeFormatting(String s) {
        return s.replaceAll("§.", "").replace(",", "").trim();
    }

    public static double pretty(double d) {
        return Math.round(d * 100) / 100.0;
    }

    public static void writeFile(Object obj) {
        try {
            Files.writeString(Path.of("bazaar_data.json"), Objects.toString(obj));
            notifyAll("Data written to bazaar_data.json");
        } catch (Exception e) {
            notifyError("Failed writing file", e);
        }
    }

    /* ────────────────────────────────────────────────────────────────
       Watched-item convenience
       ──────────────────────────────────────────────────────────────── */
    public static void addWatchedItem(ItemData d) {
        if (d == null) return;
        BUConfig.get().watchedItems.add(d);
        BUConfig.HANDLER.save();
        notifyAll("Added item: " + d.getGeneralInfo(), notificationTypes.ITEMDATA);
        ItemData.update();
    }

    /* ──────────────────────────────────────────────────────────────── */
    private static String getCallingClassName() {
        StackTraceElement[] st = Thread.currentThread().getStackTrace();
        return st.length > 3 ? st[3].getClassName() : "Unknown";
    }
}
