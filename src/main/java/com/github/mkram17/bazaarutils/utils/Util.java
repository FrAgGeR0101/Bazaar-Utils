package com.github.mkram17.bazaarutils.utils;

import com.github.mkram17.bazaarutils.config.BUConfig;
import com.github.mkram17.bazaarutils.events.BUListener;
import com.github.mkram17.bazaarutils.misc.ItemData;
import net.minecraft.client.Minecraft;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Forge-1.8.9 utility collection – chat, clipboard, tick-tasks, file I/O.
 */
public final class Util implements BUListener {

    /* ───────────────────────── notifications ────────────────────── */

    public enum NotificationType {
        GUI, FEATURE, BAZAARDATA, COMMAND, ITEMDATA;

        /** Tiny helper – can be expanded into proper per-type toggles later. */
        public boolean isEnabled() { return true; }
    }

    public static final String HELPMESSAGE =
            "§eCommands: §7/bu or /bazaarutils opens the settings GUI\n" +
            "§6----------------------------------------\n" +
            "§7/bu tax <amount> – set Bazaar tax\n" +
            "§7/bu customorder … – manage custom orders\n" +
            "§6----------------------------------------";

    /* ───────────────────────── tick scheduler ───────────────────── */

    private static final class ScheduledTask {
        int ticksLeft; final Runnable run;
        ScheduledTask(int t, Runnable r) { ticksLeft = t; run = r; }
    }
    private static final Deque<ScheduledTask> TASKS = new ArrayDeque<>();

    @Override public void subscribe() {
        MinecraftForge.EVENT_BUS.register(new Object() {
            @SubscribeEvent public void onTick(TickEvent.ClientTickEvent e) {
                if (e.phase != TickEvent.Phase.END) return;
                synchronized (TASKS) {
                    Iterator<ScheduledTask> it = TASKS.iterator();
                    while (it.hasNext()) {
                        ScheduledTask t = it.next();
                        if (--t.ticksLeft <= 0) { t.run.run(); it.remove(); }
                    }
                }
            }
        });
    }

    public static void tickExecuteLater(int ticks, Runnable r) {
        synchronized (TASKS) { TASKS.add(new ScheduledTask(ticks, r)); }
    }

    /* ───────────────────── chat / log helpers ───────────────────── */

    private static void raw(String s) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null) mc.thePlayer.addChatMessage(new ChatComponentText(s));
    }

    private static void styled(String base, ClickEvent click, HoverEvent hover) {
        ChatComponentText comp = new ChatComponentText(base);
        ChatStyle style = new ChatStyle();
        style.setChatClickEvent(click);
        style.setChatHoverEvent(hover);
        comp.setChatStyle(style);
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null) mc.thePlayer.addChatMessage(comp);
    }

    public static void notifyAll(String msg) { notifyAll(msg, NotificationType.GUI); }

    public static void notifyAll(String msg, NotificationType type) {
        boolean dev = BUConfig.get().isDeveloperMode();
        if (!type.isEnabled() && !dev) return;

        String prefix = EnumChatFormatting.GOLD + "[Bazaar Utils] " + EnumChatFormatting.RESET;
        raw(prefix + msg);
        LogManager.getLogger(callingClass()).info(msg);
    }

    public static void notifyError(String msg, Throwable t) {
        String prefix = EnumChatFormatting.RED + "[Bazaar-Utils Error] " + EnumChatFormatting.RESET;
        styled(prefix + msg,
               new ClickEvent(ClickEvent.Action.OPEN_URL,"https://discord.gg/xDKjvm5hQd"),
               new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                              new ChatComponentText("Open Discord support server")));
        LogManager.getLogger(callingClass()).error(msg, t);
    }

    public static void notifyChatCommand(String text, String cmd) {
        styled(EnumChatFormatting.GREEN + text + EnumChatFormatting.RESET,
               new ClickEvent(ClickEvent.Action.RUN_COMMAND, '/' + cmd),
               new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                              new ChatComponentText("Run /" + cmd)));
    }

    /* ───────────────────────── misc helpers ─────────────────────── */

    public static void sendCommand(String cmd) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null) mc.thePlayer.sendChatMessage('/' + cmd);
    }

    public static void copyToClipboard(String s) {
        try {
            Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
            cb.setContents(new StringSelection(s), null);
        } catch (Exception ignored) { /* headless env? */ }
    }

    public static String removeFormatting(String s) { return s.replaceAll("§.", ""); }

    public static double pretty(double d) { return Math.round(d * 100) / 100.0; }

    public static void writeFile(Object o) {
        try {
            Files.write(Paths.get("bazaar_data.json"),
                        Objects.toString(o).getBytes(StandardCharsets.UTF_8));
            notifyAll("Wrote bazaar_data.json");
        } catch (Exception e) { notifyError("File write failed", e); }
    }

    /* add & persist watched item */
    public static void addWatchedItem(ItemData d) {
        if (d == null) return;
        BUConfig.get().getWatchedItems().add(d);
        BUConfig.save();
        notifyAll("Added item: " + d.getGeneralInfo(), NotificationType.ITEMDATA);
        ItemData.update();
    }

    /* util */
    private static String callingClass() {
        StackTraceElement[] st = Thread.currentThread().getStackTrace();
        return st.length > 4 ? st[4].getClassName() : "Unknown";
    }
}
