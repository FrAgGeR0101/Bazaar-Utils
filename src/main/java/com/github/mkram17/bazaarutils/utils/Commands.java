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

import java.util.Collections;
import java.util.List;

/**
 * Forge-1.8.9 client command:
 *
 * /bazaarutils
 * /bazaarutils help
 * /bazaarutils tax &lt;1-25&gt;
 * /bazaarutils developer
 *
 * /bazaarutils custom add    &lt;amount&gt; &lt;slot&gt;
 * /bazaarutils custom remove &lt;index&gt;
 *
 * /bazaarutils rule add    price|volume|name &lt;value&gt;
 * /bazaarutils rule remove &lt;index&gt;
 */
public final class Commands {

    /* ------------------------------------------------------------------ */
    /*  Installation                                                      */
    /* ------------------------------------------------------------------ */

    public static void register() {
        ClientCommandHandler.instance.registerCommand(new BUCommand());
        ClientCommandHandler.instance.registerCommand(
                new AliasCommand("bu", "bazaarutils"));
    }

    /* ------------------------------------------------------------------ */
    /*  Main bazaarutils command                                          */
    /* ------------------------------------------------------------------ */

    private static final class BUCommand implements ICommand {

        @Override public String getCommandName() { return "bazaarutils"; }
        @Override public String getCommandUsage(ICommandSender s) { return "/bazaarutils …"; }
        @Override public List<String> getCommandAliases() { return Collections.emptyList(); }
        @Override public boolean canCommandSenderUseCommand(ICommandSender s) { return true; }
        @Override public int compareTo(ICommand o) { return getCommandName().compareTo(o.getCommandName()); }
        @Override public List<String> addTabCompletionOptions(ICommandSender s,String[]a,BlockPos p){return Collections.emptyList();}

        /* -------------------------------------------------------------- */

        @Override
        public void processCommand(ICommandSender sender, String[] a) {

            /* no args → open GUI */
            if (a.length == 0) { BUConfig.openGUI(); return; }

            String sub = a[0].toLowerCase();

            switch (sub) {
                case "help":
                    Util.notifyAll(Util.HELPMESSAGE);
                    break;

                case "tax":
                    handleTax(a);         // /bu tax <percent>
                    break;

                case "developer":
                    toggleDeveloper();
                    break;

                case "custom":
                    handleCustom(a);      // /bu custom …
                    break;

                case "rule":
                    handleRule(a);        // /bu rule …
                    break;

                default:
                    Util.notifyAll("Unknown sub-command, use /bazaarutils help");
            }
        }

        /* -------------------------- tax -------------------------- */

        private static void handleTax(String[] a) {
            if (a.length != 2) { Util.notifyAll("Usage: /bu tax <1-25>"); return; }
            try {
                double p = Double.parseDouble(a[1]);
                if (p < 1 || p > 25) throw new NumberFormatException();
                BUConfig.get().setBzTax(p / 100.0);
                BUConfig.HANDLER.save();
                Util.notifyAll("Bazaar tax set to " + p + "%");
            } catch (NumberFormatException e) {
                Util.notifyAll("Invalid number.");
            }
        }

        /* ---------------------- developer ----------------------- */

        private static void toggleDeveloper() {
            boolean now = !BUConfig.get().isDeveloperMode();
            BUConfig.get().setDeveloperMode(now);
            BUConfig.HANDLER.save();
            Util.notifyAll("Developer mode " + (now ? "enabled" : "disabled") +
                           " (restart required)");
        }

        /* ------------------------- custom ----------------------- */

        private static void handleCustom(String[] a) {
            if (a.length < 2) { Util.notifyAll("Usage: /bu custom add|remove …"); return; }

            if ("add".equals(a[1])) {
                if (a.length != 4) { Util.notifyAll("Usage: /bu custom add <amount> <slot>"); return; }
                try {
                    int amount = Integer.parseInt(a[2]);
                    int slot   = Integer.parseInt(a[3]);
                    if (amount < 1 || amount > 71679 || slot < 1 || slot > 36)
                        throw new NumberFormatException();

                    CustomOrder co = new CustomOrder(true, amount, slot - 1,
                                                     CustomOrder.getNextColoredPane());
                    BUConfig.get().getCustomOrders().add(co);
                    BUConfig.HANDLER.save();
                    Util.notifyAll("Added custom-order " + amount + " @ slot " + slot);
                } catch (NumberFormatException e) {
                    Util.notifyAll("Invalid numbers.");
                }
                return;
            }

            if ("remove".equals(a[1])) {
                if (a.length != 3) { Util.notifyAll("Usage: /bu custom remove <index>"); return; }
                int idx = Integer.parseInt(a[2]) - 1;
                List<CustomOrder> list = BUConfig.get().getCustomOrders();
                if (idx < 0 || idx >= list.size()) {
                    Util.notifyAll("No such custom-order.");
                    return;
                }
                CustomOrder removed = list.remove(idx);
                BUConfig.HANDLER.save();
                Util.notifyAll("Removed custom-order " + removed.getOrderAmount());
                return;
            }

            Util.notifyAll("Usage: /bu custom add|remove …");
        }

        /* -------------------------- rule ------------------------ */

        private static void handleRule(String[] a) {
            if (a.length < 3) { Util.notifyAll("Usage: /bu rule add|remove …"); return; }

            RestrictSell rs = BUConfig.get().getRestrictSell();

            /* -------- add -------- */
            if ("add".equals(a[1])) {
                if (a.length < 4) {
                    Util.notifyAll("Usage: /bu rule add price|volume|name <value>");
                    return;
                }
                switch (a[2].toLowerCase()) {
                    case "price":
                    case "volume":
                        try {
                            double v = Double.parseDouble(a[3]);
                            if ("price".equals(a[2]))
                                rs.addRule(RestrictSell.restrictBy.PRICE , v);
                            else
                                rs.addRule(RestrictSell.restrictBy.VOLUME, v);
                            BUConfig.HANDLER.save();
                            Util.notifyAll("Added rule " + a[2].toUpperCase() + " " + v);
                        } catch (NumberFormatException e) {
                            Util.notifyAll("Number expected.");
                        }
                        break;

                    case "name":
                        String name = joinFrom(a, 3);
                        rs.addRule(RestrictSell.restrictBy.NAME, name);
                        BUConfig.HANDLER.save();
                        Util.notifyAll("Added NAME rule: " + name);
                        break;

                    default:
                        Util.notifyAll("Unknown rule type.");
                }
                return;
            }

            /* -------- remove -------- */
            if ("remove".equals(a[1])) {
                if (a.length != 3) { Util.notifyAll("Usage: /bu rule remove <index>"); return; }
                int idx = Integer.parseInt(a[2]) - 1;
                List<RestrictSellControl> list = rs.getControls();
                if (idx < 0 || idx >= list.size()) { Util.notifyAll("No such rule."); return; }
                RestrictSellControl c = list.remove(idx);
                BUConfig.HANDLER.save();
                Util.notifyAll("Removed rule " + c.getRule() +
                               (c.getRule() == RestrictSell.restrictBy.NAME
                                    ? ": " + c.getName()
                                    : ": " + c.getAmount()));
                return;
            }

            Util.notifyAll("Usage: /bu rule add|remove …");
        }

        /* helper to join remaining args (for NAME rule) */
        private static String joinFrom(String[] a, int idx) {
            StringBuilder sb = new StringBuilder();
            for (int i = idx; i < a.length; i++) {
                if (i > idx) sb.append(' ');
                sb.append(a[i]);
            }
            return sb.toString();
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Simple alias ( /bu → /bazaarutils )                               */
    /* ------------------------------------------------------------------ */

    private static final class AliasCommand implements ICommand {
        private final String alias, target;

        AliasCommand(String a, String t) { alias = a; target = t; }

        @Override public String getCommandName()  { return alias; }
        @Override public String getCommandUsage(ICommandSender s){return '/' + alias;}
        @Override public List<String> getCommandAliases(){return Collections.emptyList();}
        @Override public boolean canCommandSenderUseCommand(ICommandSender s){return true;}
        @Override public int compareTo(ICommand o){return alias.compareTo(o.getCommandName());}
        @Override public List<String> addTabCompletionOptions(ICommandSender s,String[]a,BlockPos p){return Collections.emptyList();}

        @Override
        public void processCommand(ICommandSender s, String[] a) {
            StringBuilder sb = new StringBuilder('/').append(target);
            for (String arg : a) sb.append(' ').append(arg);
            if (s instanceof EntityPlayer p) p.sendChatMessage(sb.toString());
        }
    }
}
