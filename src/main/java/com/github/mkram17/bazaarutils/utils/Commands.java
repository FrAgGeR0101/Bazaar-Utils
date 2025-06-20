package com.github.mkram17.bazaarutils.utils;

import com.github.mkram17.bazaarutils.config.BUConfig;
import com.github.mkram17.bazaarutils.features.CustomOrder;
import com.github.mkram17.bazaarutils.features.restrictsell.RestrictSell;
import com.github.mkram17.bazaarutils.features.restrictsell.RestrictSellControl;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.ClientCommandHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Simplified Forge-1.8.9 client command:
 *
 * <pre>
 * /bazaarutils          – opens GUI
 * /bazaarutils help
 * /bazaarutils tax <1-25>           (percent)
 * /bazaarutils developer            (toggle)
 *
 * /bazaarutils custom add  <amount> <slot>
 * /bazaarutils custom remove <index>
 *
 * /bazaarutils rule   add    price|volume|name <value>
 * /bazaarutils rule   remove <index>
 * </pre>
 */
public final class Commands {

    /* ------------------------------------------------------------------ */
    /*  Installation                                                      */
    /* ------------------------------------------------------------------ */

    public static void register() {
        ClientCommandHandler.instance.registerCommand(new BUCommand());
        // short alias
        ClientCommandHandler.instance.registerCommand(
                new Alias("bu", "bazaarutils"));
    }

    /* ------------------------------------------------------------------ */
    /*  Main command implementation                                       */
    /* ------------------------------------------------------------------ */

    private static final class BUCommand implements ICommand {

        @Override public String getCommandName() { return "bazaarutils"; }

        @Override public String getCommandUsage(ICommandSender s) {
            return "/bazaarutils [sub …]  –  /bazaarutils help";
        }

        @Override public List<String> getCommandAliases() { return Collections.emptyList(); }

        @Override public void processCommand(ICommandSender sender, String[] args) {

            /* ---------- no args → open GUI ---------- */
            if (args.length == 0) {
                BUConfig.openGUI();
                return;
            }

            String sub = args[0].toLowerCase();

            switch (sub) {
                case "help" -> Util.notifyAll(Util.HELPMESSAGE);

                case "tax"  -> handleTax(args);
                case "developer" -> toggleDeveloper();

                case "custom" -> handleCustom(args);
                case "rule"   -> handleRule(args);

                default -> Util.notifyAll("Unknown sub-command.  /bazaarutils help");
            }
        }

        /* ----- /bazaarutils tax <1-25> ----- */
        private static void handleTax(String[] a) {
            if (a.length != 2) { Util.notifyAll("Usage: /bu tax 1-25"); return; }
            try {
                double p = Double.parseDouble(a[1]);
                if (p < 1 || p > 25) throw new NumberFormatException();
                BUConfig.get().bzTax = p / 100.0;
                BUConfig.HANDLER.save();
                Util.notifyAll("Bazaar tax set to " + p + "%");
            } catch (NumberFormatException e) {
                Util.notifyAll("Invalid tax value.");
            }
        }

        /* ----- /bazaarutils developer ----- */
        private static void toggleDeveloper() {
            BUConfig.get().developerMode = !BUConfig.get().developerMode;
            BUConfig.HANDLER.save();
            Util.notifyAll("Developer mode " +
                    (BUConfig.get().developerMode ? "enabled" : "disabled") +
                    ". Restart required.");
        }

        /* ----- /bazaarutils custom … ----- */
        private static void handleCustom(String[] a) {
            if (a.length < 2) { Util.notifyAll("Usage: /bu custom …"); return; }

            switch (a[1]) {
                case "add" -> {
                    if (a.length != 4) { Util.notifyAll("Usage: /bu custom add <amount> <slot>"); return; }
                    try {
                        int amount = Integer.parseInt(a[2]);
                        int slot   = Integer.parseInt(a[3]);
                        if (amount < 1 || amount > 71679 || slot < 1 || slot > 36)
                            throw new NumberFormatException();
                        CustomOrder co = new CustomOrder(true, amount, slot - 1,
                                                         CustomOrder.getNextColoredPane());
                        BUConfig.get().customOrders.add(co);
                        BUConfig.HANDLER.save();
                        Util.notifyAll("Added custom-order: " + amount + " @ slot " + slot);
                    } catch (NumberFormatException e) {
                        Util.notifyAll("Invalid numbers.");
                    }
                }
                case "remove" -> {
                    if (a.length != 3) { Util.notifyAll("Usage: /bu custom remove <index>"); return; }
                    int idx = Integer.parseInt(a[2]) - 1;
                    if (idx < 0 || idx >= BUConfig.get().customOrders.size()) {
                        Util.notifyAll("No such custom-order.");
                        return;
                    }
                    CustomOrder rem = BUConfig.get().customOrders.remove(idx);
                    BUConfig.HANDLER.save();
                    Util.notifyAll("Removed custom-order " + rem.getOrderAmount());
                }
                default -> Util.notifyAll("Usage: /bu custom add|remove …");
            }
        }

        /* ----- /bazaarutils rule … ----- */
        private static void handleRule(String[] a) {
            if (a.length < 3) { Util.notifyAll("Usage: /bu rule …"); return; }

            RestrictSell r = BUConfig.get().restrictSell;

            switch (a[1]) {
                case "add" -> {
                    if (a.length < 4) { Util.notifyAll("Usage: /bu rule add price|volume|name <value>"); return; }
                    switch (a[2]) {
                        case "price", "volume" -> {
                            try {
                                double v = Double.parseDouble(a[3]);
                                if ("price".equals(a[2]))
                                    r.addRule(RestrictSell.restrictBy.PRICE , v);
                                else
                                    r.addRule(RestrictSell.restrictBy.VOLUME, v);
                                BUConfig.HANDLER.save();
                                Util.notifyAll("Added rule " + a[2].toUpperCase() + " " + v);
                            } catch (NumberFormatException e) {
                                Util.notifyAll("Number expected.");
                            }
                        }
                        case "name"  -> {
                            String name = String.join(" ", java.util.Arrays.copyOfRange(a, 3, a.length));
                            r.addRule(RestrictSell.restrictBy.NAME, name);
                            BUConfig.HANDLER.save();
                            Util.notifyAll("Added NAME rule: " + name);
                        }
                        default -> Util.notifyAll("Unknown rule type.");
                    }
                }
                case "remove" -> {
                    if (a.length != 3) { Util.notifyAll("Usage: /bu rule remove <index>"); return; }
                    int idx = Integer.parseInt(a[2]) - 1;
                    List<RestrictSellControl> list = r.getControls();
                    if (idx < 0 || idx >= list.size()) { Util.notifyAll("No such rule."); return; }
                    RestrictSellControl c = list.remove(idx);
                    BUConfig.HANDLER.save();
                    Util.notifyAll("Removed rule " + c.getRule() +
                                   (c.getRule() == RestrictSell.restrictBy.NAME
                                        ? ": " + c.getName()
                                        : ": " + c.getAmount()));
                }
                default -> Util.notifyAll("Usage: /bu rule add|remove …");
            }
        }

        /* ------------------------------------------------------------------ */
        /*  Standard ICommand boiler-plate                                    */
        /* ------------------------------------------------------------------ */

        @Override public boolean canCommandSenderUseCommand(ICommandSender s) { return true; }
        @Override public List<String> addTabCompletionOptions(ICommandSender s, String[] a, BlockPos p) { return Collections.emptyList(); }
        @Override public int compareTo(ICommand o) { return getCommandName().compareTo(o.getCommandName()); }
    }

    /* ------------------------------------------------------------------ */
    /*  Simple alias handler                                              */
    /* ------------------------------------------------------------------ */

    private static final class Alias implements ICommand {
        private final String name, redirect;
        Alias(String n, String r) { name = n; redirect = r; }

        @Override public String getCommandName() { return name; }
        @Override public String getCommandUsage(ICommandSender s) { return "/" + name; }
        @Override public void processCommand(ICommandSender s, String[] a) {
            // fake “/redirect …” by re-building the full command line
            StringBuilder sb = new StringBuilder('/').append(redirect);
            for (String arg : a) sb.append(' ').append(arg);
            // send through the player so history & macros still work
            if (s instanceof EntityPlayer p) p.sendChatMessage(sb.toString());
        }

        /* minimal boiler-plate */
        @Override public boolean canCommandSenderUseCommand(ICommandSender s) { return true; }
        @Override public List<String> addTabCompletionOptions(ICommandSender s,String[] a,BlockPos p){return Collections.emptyList();}
        @Override public List<String> getCommandAliases(){return Collections.emptyList();}
        @Override public int compareTo(ICommand o){return name.compareTo(o.getCommandName());}
    }
}
