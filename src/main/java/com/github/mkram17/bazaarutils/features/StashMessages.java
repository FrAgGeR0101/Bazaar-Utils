package com.github.mkram17.bazaarutils.features.restrictsell;

import com.github.mkram17.bazaarutils.BazaarUtils;
import com.github.mkram17.bazaarutils.events.BUListener;
import com.github.mkram17.bazaarutils.events.ReplaceItemEvent;
import com.github.mkram17.bazaarutils.utils.Util;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.github.mkram17.bazaarutils.BazaarUtils.eventBus;

/**
 * Prevents accidental bulk-selling via the “Insta-Sell” button.<br>
 * Rules can be based on <b>total price</b>, <b>total volume</b> or
 * specific <b>item-names</b>.  When a rule triggers, the button shows
 * the remaining “safety clicks” that must be performed before the sell
 * goes through.
 */
public final class RestrictSell implements BUListener {

    /* ────────────────────────────────────────────────────────── */
    /*  rule-type                                                */
    /* ────────────────────────────────────────────────────────── */

    public enum Rule { PRICE, VOLUME, NAME }

    /* ────────────────────────────────────────────────────────── */
    /*  config / state                                           */
    /* ────────────────────────────────────────────────────────── */

    private boolean              enabled;
    private final int            safetyClicksRequired;
    private final List<RestrictSellControl> controls;

    private static final int SELL_BUTTON_SLOT = 47;

    /* live state while a Bazaar GUI is open */
    private boolean locked         = false;
    private int     safetyClicks   = 0;

    /* ────────────────────────────────────────────────────────── */
    /*  construction                                             */
    /* ────────────────────────────────────────────────────────── */

    public RestrictSell(boolean on, int clicks, List<RestrictSellControl> rules) {
        this.enabled               = on;
        this.safetyClicksRequired  = clicks;
        this.controls              = (rules == null) ? new ArrayList<>() : rules;
    }

    /* ────────────────────────────────────────────────────────── */
    /*  BUListener                                               */
    /* ────────────────────────────────────────────────────────── */

    @Override
    public void subscribe() {
        ScreenEvents.AFTER_INIT.register((c,s,w,h) -> safetyClicks = 0);
        eventBus.subscribe(this);
    }

    /* ────────────────────────────────────────────────────────── */
    /*  public helpers                                           */
    /* ────────────────────────────────────────────────────────── */

    public boolean isEnabled()               { return enabled; }
    public void    setEnabled(boolean b)     { enabled = b;    }

    public boolean isSlotLocked(int slotId) {
        return enabled && locked &&
               BazaarUtils.gui.inBazaar() &&
               slotId == SELL_BUTTON_SLOT;
    }

    public void addSafetyClick()  { safetyClicks++; }
    public void resetSafetyClicks(){ safetyClicks  = 0; }

    /* ────────────────────────────────────────────────────────── */
    /*  core logic                                               */
    /* ────────────────────────────────────────────────────────── */

    @meteordevelopment.orbit.EventHandler
    private void onReplaceItem(ReplaceItemEvent ev) {
        if (!enabled) return;
        if (ev.getSlotId() != SELL_BUTTON_SLOT)                 return;
        if (!BazaarUtils.gui.inBazaar())                        return;
        if (ev.getOriginal() == null)                           return;
        if (ev.getOriginal().getComponentChanges() == null)     return;

        /* Parse the button’s lore                                     */
        var lore = ev.getOriginal().get(DataComponentTypes.LORE);
        if (lore == null || lore.lines().size() < 6) return;    // still “Loading …”

        List<Text> lines = lore.lines();
        int numItems = lines.size() - 8;                        // lore layout (Hypixel)

        List<SellItem> items = extractItems(lines, numItems);

        String coinLine  = lines.get(5 + numItems).getString(); // “Total: 123 coins”
        double totalCost = Double.parseDouble(
                coinLine.substring(coinLine.indexOf(": ") + 2,
                                   coinLine.indexOf(" coins"))
                        .replace(",", ""));

        locked = isSellLocked(items, totalCost);

        if (locked) {
            ItemStack repl = ev.getOriginal().copy();
            if (safetyClicks < safetyClicksRequired) {
                repl.set(BazaarUtils.CUSTOM_SIZE_COMPONENT,
                         String.valueOf(safetyClicksRequired - safetyClicks));
            }
            ev.setReplacement(repl);
        }
    }

    /* Extract list of items (name + volume) from Hypixel lore */
    private static List<SellItem> extractItems(List<Text> lore, int n) {
        if (n <= 0) return Collections.emptyList();

        List<SellItem> out = new ArrayList<>(n);
        for (int i = 4; i < 4 + n; i++) {
            List<Text> comps = lore.get(i).getSiblings();
            if (comps.size() < 4) {
                Util.notifyError("RestrictSell: could not parse item row", null);
                continue;
            }
            int    volume = Integer.parseInt(comps.get(1).getString().replace(",", ""));
            String name   = comps.get(3).getString().trim();
            out.add(new SellItem(volume, name));
        }
        return out;
    }

    /* ------------------------------------------------------------------ */

    private boolean isSellLocked(List<SellItem> items, double total) {

        /* price-limit rules */
        for (RestrictSellControl c : controls)
            if (c.isEnabled() &&
                c.getRule() == Rule.PRICE &&
                total > c.getAmount())
                return true;

        /* per-item checks (volume / name) */
        for (SellItem si : items) {
            for (RestrictSellControl c : controls) {
                if (!c.isEnabled()) continue;

                if (c.getRule() == Rule.VOLUME &&
                    si.volume > c.getAmount()) return true;

                if (c.getRule() == Rule.NAME   &&
                    si.name.equalsIgnoreCase(c.getName())) return true;
            }
        }
        return false;
    }

    /* ────────────────────────────────────────────────────────── */
    /*  small record helper                                       */
    /* ────────────────────────────────────────────────────────── */

    /** A single line (“ 123 × Enchanted Diamond ”) in the sell-GUI. */
    private record SellItem(int volume, String name) {}
}
