package com.github.mkram17.bazaarutils.utils;

import com.github.mkram17.bazaarutils.config.BUConfig;
import com.github.mkram17.bazaarutils.events.BUListener;
import com.github.mkram17.bazaarutils.misc.ItemData;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import org.apache.logging.log4j.LogManager;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Generic helpers: chat notifications, clipboard, file-I/O, tick scheduling …
 */
public final class Util implements BUListener {

    /* ────────────────────────────────────────────────────────────────
       Developer-notification helper
       ──────────────────────────────────────────────────────────────── */
    public enum NotificationType {
        GUI, FEATURE, BAZAARDATA, COMMAND, ITEMDATA;

        /** Is this notification category enabled in the config? */
        public boolean isEnabled() {
            return BUConfig.get().developer.isDevMessageEnabled(this);
        }
    }

    /* ───────────────────────────────
       Small static info / help texts
       ─────────────────────────────── */
    public static final String HELPMESSAGE =
            "§eCommands: §7/bu or /bazaarutils opens the settings GUI\n" +
            "§6----------------------------------------\n" +
            "§7/bu tax <amount> – set Bazaar tax\n" +
            "§7/bu customorder … – manage custom orders\n" +
            "§6----------------------------------------";

    public static final Text DISCORD_LINK = Text.literal("Discord server")
            .styled(style -> style
                    .withBold(true)
                    .withClickEvent(ClickEvent.openUrl(URI.create("https://discord.gg/xDKjvm5hQd")))
                    .withHoverEvent(HoverEvent.showText(Text.literal("Click to join the Discord!"))));

    public static final Text CHANGELOG_LINK = Text.literal("Click to see changelog")
            .styled(style -> style
                    .withBold(true)
                    .withColor(Formatting.GREEN)
                    .withClickEvent(ClickEvent.openUrl(URI.create("https://modrinth.com/mod/bazaar-utils/changelog")))
                    .withHoverEvent(HoverEvent.showText(Text.literal("Latest update notes"))));

    /* ───────────────────────────────
       Simple in-game scheduler
       ─────────────────────────────── */
    private static final LinkedList<ScheduledTask> TASKS = new LinkedList<>();

    private static final class ScheduledTask {
        int     ticksLeft;
        Runnable action;

        ScheduledTask(int ticksLeft, Runnable action) {
            this.ticksLeft = ticksLeft;
            this.action    = action;
        }
    }

    /* ------------------------------------------------------------------
       BUListener
       ------------------------------------------------------------------ */
    @Override
    public void subscribe() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
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

    /** Schedule a task for execution n client-ticks later. */
    public static void tickExecuteLater(int ticks, Runnable action) {
        synchronized (TASKS) { TASKS.add(new ScheduledTask(ticks, action)); }
    }

    /* ───────────────────────────────
       Chat + log helpers
       ─────────────────────────────── */
    public static void notifyAll(String msg) {
        notifyAll(msg, NotificationType.GUI);
    }

    public static void notifyAll(String msg, NotificationType type) {
        if (!type.isEnabled() && !BUConfig.get().developer.allMessages) return;

        MutableText txt = Text.literal("[Bazaar Utils] ").formatted(Formatting.GOLD)
                              .append(Text.literal(msg).formatted(Formatting.WHITE));

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) mc.player.sendMessage(txt, false);

        LogManager.getLogger(getCallingClassName()).info(msg);
    }

    public static void notifyError(String message, Throwable t) {
        Text txt = Text.literal("[Bazaar-Utils Error] " + message)
                       .formatted(Formatting.RED)
                       .styled(s -> s
                               .withClickEvent(ClickEvent.openUrl(URI.create("https://discord.gg/xDKjvm5hQd")))
                               .withHoverEvent(HoverEvent.showText(Text.literal("Click for Discord support"))));

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) mc.player.sendMessage(txt, false);

        if (t != null)
            LogManager.getLogger(getCallingClassName()).error(message, t);
        else
            LogManager.getLogger(getCallingClassName()).error(message);
    }

    /** Green clickable chat message that runs a command. */
    public static void notifyChatCommand(Text txt, String command) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        Text styled = txt.copy().styled(s -> s
                .withClickEvent(ClickEvent.runCommand("/" + command))
                .withHoverEvent(HoverEvent.showText(Text.literal("Run /" + command)))
                .withColor(Formatting.GREEN));

        mc.player.sendMessage(styled, false);
    }

    /* ───────────────────────────────
       Generic helpers
       ─────────────────────────────── */
    public static void sendCommand(String cmd) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) mc.player.networkHandler.sendChatCommand(cmd);
    }

    public static void copyToClipboard(String str) {
        Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
        cb.setContents(new StringSelection(str), null);
    }

    public static String removeFormatting(String s) {
        return s.replaceAll("§.", "").replace(",", "").trim();
    }

    public static int parseNumber(String input) {
        String up = input.toUpperCase(Locale.ROOT);
        double v = Double.parseDouble(up.replaceAll("[^0-9.]", ""));
        if (up.endsWith("K")) return (int)(v * 1_000);
        if (up.endsWith("M")) return (int)(v * 1_000_000);
        if (up.endsWith("B")) return (int)(v * 1_000_000_000);
        return (int)v;
    }

    public static double truncate(double d) {
        return Math.round(d * 100) / 100.0;
    }

    public static double pretty(double d) {
        String s = String.valueOf(d).replaceAll("\\.0$", "")
                                    .replaceAll("(\\.\\d*?)0+$", "$1");
        return Double.parseDouble(s);
    }

    public static void writeFile(Object content) {
        try {
            Files.writeString(Path.of("bazaar_data.json"), content.toString());
            notifyAll("Data written to bazaar_data.json");
        } catch (Exception e) {
            notifyError("Failed writing file", e);
        }
    }

    /* Small chat component helpers ----------------------------------- */
    public static int findComponentIndex(List<Text> components, String needle) {
        for (int i = 0; i < components.size(); i++)
            if (components.get(i).getString().contains(needle)) return i;
        return -1;
    }

    public static String findComponentWith(List<Text> components, String needle) {
        for (Text c : components)
            if (c.getString().contains(needle)) return c.getString();
        return null;
    }

    /* ───────────────────────────────
       Simple item-watch helpers
       ─────────────────────────────── */
    public static void addWatchedItem(ItemData item) {
        if (item == null) return;

        BUConfig.get().watchedItems.add(item);
        notifyAll("Added item: " + item.getGeneralInfo(), NotificationType.ITEMDATA);
        BUConfig.HANDLER.save();
        ItemData.update();
    }

    /* ---------------------------------------------------------------- */
    private static String getCallingClassName() {
        StackTraceElement[] st = Thread.currentThread().getStackTrace();
        return st.length > 3 ? st[3].getClassName() : "UnknownClass";
    }

    /* ---------------------------------------------------------------- */
    @FunctionalInterface
    public interface LengthJudger { int judgeLength(char c); }
}
