package com.github.mkram17.bazaarutils.utils;

import com.github.mkram17.bazaarutils.config.BUConfig;
import com.github.mkram17.bazaarutils.events.BUListener;
import com.github.mkram17.bazaarutils.misc.ItemData;
import net.minecraft.client.Minecraft;                         // 1.8.9
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.LogManager;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Generic helpers (chat notifications, clipboard, file-I/O, tick scheduling…)
 * pure Forge-1.8.9, no Fabric imports.
 */
public final class Util implements BUListener {

    /* ────────────────────────────────────────────────────────────────
       Notification helpers
       ──────────────────────────────────────────────────────────────── */

    public enum notificationTypes {GUI, FEATURE, BAZAARDATA, COMMAND, ITEMDATA}

    /* ────────────────────────────────────────────────────────────────
       Simple help-text
       ──────────────────────────────────────────────────────────────── */
    public static final String HELPMESSAGE =
            "§eCommands: §7/bu or /bazaarutils opens the settings GUI\n" +
            "§6----------------------------------------\n" +
            "§7/bu tax <amount> – set Bazaar tax\n" +
            "§7/bu customorder … – manage custom orders\n" +
            "§6----------------------------------------";

    /* ────────────────────────────────────────────────────────────────
       Small task-scheduler executed every client tick
       ──────────────────────────────────────────────────────────────── */
    private static final class ScheduledTask {
        int ticksLeft;
        final Runnable action;
        ScheduledTask(int ticks, Runnable run) { ticksLeft = ticks; action = run; }
    }
    private static final LinkedList<ScheduledTask> TASKS = new LinkedList<>();

    /** register tick-handler once */
    @Override public void subscribe() {
        MinecraftForge.EVENT_BUS.register(new Object() {
            @SubscribeEvent public void onTick(TickEvent.ClientTickEvent e) {
                if (e.phase != TickEvent.Phase.END) return;
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
            }
        });
    }

    /** schedule a Runnable for <code>ticks</code> client-ticks later */
    public static void tickExecuteLater(int ticks, Runnable action) {
        synchronized (TASKS) { TASKS.add(new ScheduledTask(ticks, action)); }
    }

    /* ────────────────────────────────────────────────────────────────
       Chat + log helpers                                              */
    /* ──────────────────────────────────────────────────────────────── */

    private static void sendRawChat(String raw) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null) mc.thePlayer.addChatMessage(new ChatComponentText(raw));
    }

    private static void sendStyledChat(String base,
                                       ClickEvent click,
                                       HoverEvent hover) {
        ChatComponentText comp = new ChatComponentText(base);
        ChatStyle style = new ChatStyle();
        style.setChatClickEvent(click);
        style.setChatHoverEvent(hover);
        comp.setChatStyle(style);
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null) mc.thePlayer.addChatMessage(comp);
    }

    public static void notifyAll(String msg) { notifyAll(msg, notificationTypes.GUI); }

    public static void notifyAll(String msg, notificationTypes type) {
        if (!type.isEnabled() && !BUConfig.get().developer.allMessages) return;

        String prefix = EnumChatFormatting.GOLD + "[Bazaar Utils] " + EnumChatFormatting.RESET;
        sendRawChat(prefix + msg);
        LogManager.getLogger(getCallingClassName()).info(msg);
    }

    public static void notifyError(String msg, Throwable t) {
        String prefix = EnumChatFormatting.RED + "[Bazaar-Utils Error] " + EnumChatFormatting.RESET;
        sendStyledChat(prefix + msg,
                new ClickEvent(ClickEvent.Action.OPEN_URL, "https://discord.gg/xDKjvm5hQd"),
                new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new ChatComponentText("Open Discord support server")));
        if (t != null)
            LogManager.getLogger(getCallingClassName()).error(msg, t);
        else
            LogManager.getLogger(getCallingClassName()).error(msg);
    }

    /* clickable green command helper */
    public static void notifyChatCommand(String text, String command) {
        sendStyledChat(EnumChatFormatting.GREEN + text + EnumChatFormatting.RESET,
                new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + command),
                new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new ChatComponentText("Run /" + command)));
    }

    /* ────────────────────────────────────────────────────────────────
       Misc helpers
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

    public static double pretty(double d) { return Math.round(d * 100) / 100.0; }

    public static void writeFile(Object obj) {
        try {
            Files.writeString(Path.of("bazaar_data.json"), Objects.toString(obj));
            notifyAll("Data written to bazaar_data.json");
        } catch (Exception e) {
            notifyError("Failed to write file", e);
        }
    }

    /* add + persist watched-item */
    public static void addWatchedItem(ItemData d) {
        if (d == null) return;
        BUConfig.get().watchedItems.add(d);
        BUConfig.HANDLER.save();
        notifyAll("Added item: " + d.getGeneralInfo(), notificationTypes.ITEMDATA);
        ItemData.update();
    }

    /* utility */
    private static String getCallingClassName() {
        StackTraceElement[] st = Thread.currentThread().getStackTrace();
        return st.length > 3 ? st[3].getClassName() : "Unknown";
    }
}
