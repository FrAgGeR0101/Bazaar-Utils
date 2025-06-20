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
import java.util.Iterator;
import java.util.List;

import static com.github.mkram17.bazaarutils.BazaarUtils.EVENT_BUS;

public final class RestrictSell implements BUListener {

    /* ──────────────────────────────────── rule types ──────────────────────────────────── */
    public enum Rule { PRICE, VOLUME, NAME }

    /* ───────────────────────────────────── state ─────────────────────────────────────── */
    private boolean enabled               = true;
    private int     safetyClicksRequired  = 3;

    private final List<RestrictSellControl> rules = new ArrayList<>();
    private int  safetyClicks = 0;
    private boolean locked    = false;

    private static final int SELL_SLOT = 47;           // Hypixel insta-sell slot

    /* ────────────────────────── public helper to add rules ──────────────────────────── */
    public void addPriceRule (double maxPrice)  { rules.add(new RestrictSellControl(Rule.PRICE , maxPrice)); }
    public void addVolumeRule(double maxVol)    { rules.add(new RestrictSellControl(Rule.VOLUME, maxVol)); }
    public void addNameRule  (String name)      { rules.add(new RestrictSellControl(name)); }

    /* ───────────────────────────────── BUListener ───────────────────────────────────── */
    @Override public void subscribe() {
        EVENT_BUS.subscribe(this);
        EVENT_BUS.subscribe((ChestLoadedEvent ev) -> safetyClicks = 0);   // reset every GUI
    }

    /* ─────────────────────────── ReplaceItemEvent hook ─────────────────────────────── */
    @SuppressWarnings("unused")
    public void onReplaceItem(ReplaceItemEvent ev) {

        if (!enabled)                          return;
        if (ev.getSlotId() != SELL_SLOT)       return;
        if (!BazaarUtils.GUI.inBazaar())       return;

        ItemStack original = ev.getOriginal();
        if (original == null || !original.hasTagCompound()) return;

        ParsedSellData sd = parseLore(original.getTagCompound());
        if (sd == null) return;

        locked = violatesAnyRule(sd);
        if (!locked) return;

        /* replace slot 47 with red glass pane showing remaining clicks */
        ItemStack block = new ItemStack(Blocks.stained_glass_pane, 1, 14);
        int left = safetyClicksRequired - safetyClicks;
        block.setStackDisplayName("§cSELL BLOCKED §7(" + left + " clicks)");
        ev.setReplacement(block);

        if (++safetyClicks >= safetyClicksRequired) {   // user confirmed
            enabled      = false;
            safetyClicks = 0;
        }
        warnUser(sd, left);
    }

    /* ───────────────────────────── rule evaluation ─────────────────────────────────── */
    private boolean violatesAnyRule(ParsedSellData d) {

        for (RestrictSellControl r : rules) {
            if (!r.isEnabled()) continue;

            switch (r.getRule()) {
                case PRICE:
                    if (d.totalPrice > r.getAmount()) return true;
                    break;

                case VOLUME:
                    for (Stack s : d.stacks)
                        if (s.volume() > r.getAmount()) return true;
                    break;

                case NAME:
                    for (Stack s : d.stacks)
                        if (s.name().equalsIgnoreCase(r.getName())) return true;
                    break;

                default: break;   // safety – should never hit
            }
        }
        return false;
    }

    /* ───────────────────────────── parsing helpers  … unchanged … ───────────────────── */
    private static class ParsedSellData {
        double totalPrice;
        final List<Stack> stacks = new ArrayList<>();
    }
    private record Stack(int volume, String name) {}

    private ParsedSellData parseLore(NBTTagCompound root) { /* — unchanged — */ /* … */ return null; }

    /* ───────────────────────────── user feedback … unchanged … ─────────────────────── */
    private static void warnUser(ParsedSellData d, int left) { /* … unchanged … */ }
}
