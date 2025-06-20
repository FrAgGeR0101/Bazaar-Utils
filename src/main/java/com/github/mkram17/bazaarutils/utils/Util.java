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
 * Generic helpers (chat notifications, clipboard, file-IO, tick scheduling …).
 */
public final class Util implements BUListener {

    /* ------------------------------------------------------------------ */
    /*  developer-notification helpers                                   */
    /* ------------------------------------------------------------------ */

    public enum NotificationType {
        GUI, FEATURE, BAZAARDATA, COMMAND, ITEMDATA;

        /** Whether this notification category is enabled in the config. */
        public boolean isEnabled() {
            return BUConfig.get().developer.isDevMessageEnabled(this);
        }
    }

    /* The global task-queue executed from END_CLIENT_TICK -------------- */
    private static final LinkedList<ScheduledTask> TASKS = new LinkedList<>();

    /* Simple help / info strings -------------------------------------- */
    public static final String HELPMESSAGE = """
            §eCommands: §7/bu or /bazaarutils opens the settings GUI
            §6----------------------------------------
            §7/bu tax <amount> – set Bazaar tax
            §7/bu customorder … – manage custom orders
            §6----------------------------------------""";

    public static final Text DISCORD_LINK  = Text.literal("Discord server")
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

    /* ------------------------------------------------------------------ */
    /*  BUListener implementation                                        */
    /* ------------------------------------------------------------------ */

    @Override
    public void subscribe() {
        subscribeTicks();
    }

    /* ------------------------------------------------------------------ */
    /*  Chat + log helpers                                                */
    /* ------------------------------------------------------------------ */

    public static void notifyAll(String message) {
        notifyAll(message, NotificationType.GUI);
    }

    public static void notifyAll(String message, NotificationType type) {

        if (!type.isEnabled() && !BUConfig.get().developer.allMessages) return;

        MutableText txt = Text.literal("[Bazaar Utils] ")
                               .formatted(Formatting.GOLD)
                               .append(Text.literal(message).formatted(Formatting.WHITE));

        if (MinecraftClient.getInstance().player != null)
            MinecraftClient.getInstance().player.sendMessage(txt, false);

        LogManager.getLogger(getCallingClassName())
                  .info("[Bazaar-Utils] " + message);
    }

    public static void notifyError(String message, Throwable t) {
        Text txt = Text.literal("[Bazaar-Utils Error] " + message)
                .formatted(Formatting.RED)
                .styled(s -> s.withClickEvent(ClickEvent.openUrl(
                                URI.create("https://discord.gg/xDKjvm5hQd")))
                              .withHoverEvent(HoverEvent.showText(Text.literal("Click for Discord support"))));

        if (MinecraftClient.getInstance().player != null)
            MinecraftClient.getInstance().player.sendMessage(txt, false);

        if (t != null) {
            LogManager.getLogger(getCallingClassName()).error(message, t);
        } else {
            LogManager.getLogger(getCallingClassName()).error(message);
        }
    }

    /** Send a green clickable chat-message that runs the given command. */
    public static void notifyChatCommand(Text text, String command) {
        if (MinecraftClient.getInstance().player == null) return;

        Text styled = text.copy().styled(s -> s
                .withClickEvent(ClickEvent.runCommand("/" + command))
                .withHoverEvent(HoverEvent.showText(Text.literal("Run /" + command)))
                .withColor(Formatting.GREEN));

        MinecraftClient.getInstance().player.sendMessage(styled, false);
    }

    /* ------------------------------------------------------------------ */
    /*  Config / data helpers                                             */
    /* ------------------------------------------------------------------ */

    public static void addWatchedItem(ItemData item) {
        if (item == null) return;

        BUConfig.get().watchedItems.add(item);
        notifyAll("Added item: " + item.getGeneralInfo(), NotificationType.ITEMDATA);
        BUConfig.HANDLER.save();
        ItemData.update();
    }

    /* ------------------------------------------------------------------ */
    /*  Tick-scheduler                                                    */
    /* ------------------------------------------------------------------ */

    private record ScheduledTask(int ticksLeft, Runnable action) {}

    private static void subscribeTicks() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            synchronized (TASKS) {
                Iterator<ScheduledTask> it = TASKS.iterator();
                List<ScheduledTask> updated = new ArrayList<>();
                while (it.hasNext()) {
                    ScheduledTask t = it.next();
                    if (t.ticksLeft() <= 1) {
                        t.action().run();
                        it.remove();
                    } else {
                        updated.add(new ScheduledTask(t.ticksLeft() - 1, t.action()));
                        it.remove();
                    }
                }
                TASKS.addAll(updated);
            }
        });
    }

    /** Schedule a task for N client ticks in the future. */
    public static void tickExecuteLater(int ticks, Runnable action) {
        synchronized (TASKS) {
            TASKS.add(new ScheduledTask(ticks, action));
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Misc helpers                                                      */
    /* ------------------------------------------------------------------ */

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

    public static double truncate(double v) {
        return Math.round(v * 100) / 100.0;
    }

    public static double pretty(double v) {
        return truncate(Double.parseDouble(String.valueOf(v)
                                           .replaceAll("\\.0$", "")
                                           .replaceAll("(\\.\\d*?)0+$", "$1")));
    }

    public static void writeFile(Object content) {
        try {
            Files.writeString(Path.of("bazaar_data.json"), content.toString());
            notifyAll("Data written to bazaar_data.json");
        } catch (Exception e) {
            notifyError("Failed to write file", e);
        }
    }

    /* -------------------------------------------------------- */

    private static String getCallingClassName() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        return stack.length > 3
                ? stack[3].getClassName()
                : "UnknownClass";
    }

    /* -------------------------------------------------------- */
    /*  Functional interface                                    */
    /* -------------------------------------------------------- */

    @FunctionalInterface
    public interface LengthJudger {
        int judgeLength(char c);
    }
}
