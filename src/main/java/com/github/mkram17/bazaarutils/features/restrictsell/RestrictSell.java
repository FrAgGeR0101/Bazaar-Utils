package com.github.mkram17.bazaarutils.features.restrictsell;

import com.github.mkram17.bazaarutils.BazaarUtils;
import com.github.mkram17.bazaarutils.events.BUListener;
import com.github.mkram17.bazaarutils.events.ChestLoadedEvent;
import com.github.mkram17.bazaarutils.events.ReplaceItemEvent;
import com.github.mkram17.bazaarutils.utils.Util;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;

import java.util.ArrayList;
import java.util.List;


/**
 * Forge-1.8.9 implementation of the “Restrict Sell” helper.
 * Pure vanilla classes – no Fabric, Lombok, Orbit, records, or switch-arrow syntax.
 */
public final class RestrictSell implements BUListener {

    /* ───────────────── rule kinds ───────────────── */
    public enum Rule { PRICE, VOLUME, NAME }

    /* ──────────────── state ──────────────── */
    private boolean enabled              = true;
    private int     safetyClicksRequired = 3;

    private final List<RestrictSellControl> rules = new ArrayList<>();
    private int     safetyClicks = 0;
    private boolean locked       = false;

    private static final int SELL_SLOT = 47;          // Hypixel insta-sell slot

    /* ───────── public helpers ───────── */
    public void addPriceRule (double maxPrice) { rules.add(new RestrictSellControl(Rule.PRICE , maxPrice)); }
    public void addVolumeRule(double maxVol)   { rules.add(new RestrictSellControl(Rule.VOLUME, maxVol));  }
    public void addNameRule  (String name)     { rules.add(new RestrictSellControl(name));                 }

    /* ───────── BUListener ───────── */
    @Override public void subscribe() {
        BazaarUtils.eventBus.subscribe(this);
        BazaarUtils.eventBus.subscribe((ChestLoadedEvent ev) -> safetyClicks = 0);   // reset each GUI
    }

    /* ───────── ReplaceItemEvent hook ───────── */
    @SuppressWarnings("unused")
    public void onReplaceItem(ReplaceItemEvent ev) {

        if (!enabled)                    return;
        if (ev.getSlotId() != SELL_SLOT) return;
        if (!BazaarUtils.GUI.inBazaar()) return;

        ItemStack original = ev.getOriginal();
        if (original == null || !original.hasTagCompound()) return;

        ParsedSellData sd = parseLore(original.getTagCompound());
        if (sd == null) return;                       // malformed-lore

        locked = violatesAnyRule(sd);
        if (!locked) return;                          // allow vanilla button

        /* replace slot-47 with a red pane showing remaining clicks */
        ItemStack pane = new ItemStack(Blocks.stained_glass_pane, 1, 14);
        int left = safetyClicksRequired - safetyClicks;
        pane.setStackDisplayName("§cSELL BLOCKED §7(" + left + " clicks)");
        ev.setReplacement(pane);

        if (++safetyClicks >= safetyClicksRequired) { // user confirmed
            enabled      = false;
            safetyClicks = 0;
        }
        warnUser(sd, left);
    }

    /* ───────── rule evaluation ───────── */
    private boolean violatesAnyRule(ParsedSellData d) {

        for (RestrictSellControl r : rules) {
            if (!r.isEnabled()) continue;

            switch (r.getRule()) {
                case PRICE:
                    if (d.totalPrice > r.getAmount()) return true;
                    break;

                case VOLUME:
                    for (Stack s : d.stacks)
                        if (s.volume > r.getAmount()) return true;
                    break;

                case NAME:
                    for (Stack s : d.stacks)
                        if (s.name.equalsIgnoreCase(r.getName())) return true;
                    break;

                default: break;
            }
        }
        return false;
    }

    /* ───────── lore-parsing helpers ───────── */

    private static final class ParsedSellData {
        double totalPrice = 0;
        final List<Stack> stacks = new ArrayList<>();
    }
    private static final class Stack {
        final int    volume;
        final String name;
        Stack(int v, String n) { volume = v; name = n; }
    }

    /** Extract price & per-item lines from Hypixel insta-sell button lore. */
    private ParsedSellData parseLore(NBTTagCompound root) {

        if (!root.hasKey("display")) return null;
        NBTTagCompound disp = root.getCompoundTag("display");
        if (!disp.hasKey("Lore"))    return null;

        ParsedSellData out = new ParsedSellData();

        for (int i = 0; i < disp.getTagList("Lore", 8).tagCount(); i++) {   // 8 == String
            String s = disp.getTagList("Lore", 8).getStringTagAt(i).replace("§", "");

            if (s.startsWith("Price: ")) {
                String num = s.substring(7, s.indexOf(" coins")).replace(",", "");
                out.totalPrice = Double.parseDouble(num);
            }

            if (s.contains("x ")) { // e.g. " 128x Cobblestone"
                int    vol = Integer.parseInt(s.substring(0, s.indexOf('x')).trim());
                String nm  = s.substring(s.indexOf('x') + 2).trim();
                out.stacks.add(new Stack(vol, nm));
            }
        }
        return out;
    }

    /* ───────── user feedback ───────── */
    private static void warnUser(ParsedSellData d, int left) {
        StringBuilder msg = new StringBuilder("§eSell blocked – ")
                .append(left).append(" click").append(left == 1 ? "" : "s")
                .append(" to confirm.  (Total ")
                .append(Util.pretty(d.totalPrice)).append(" coins)");

        if (net.minecraft.client.Minecraft.getMinecraft().thePlayer != null)
            net.minecraft.client.Minecraft.getMinecraft()
                    .thePlayer.addChatMessage(new ChatComponentText(msg.toString()));
    }
}
