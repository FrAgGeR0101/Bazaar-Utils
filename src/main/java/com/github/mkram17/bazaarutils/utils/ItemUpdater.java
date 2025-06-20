package com.github.mkram17.bazaarutils.utils;

import com.github.mkram17.bazaarutils.BazaarUtils;
import com.github.mkram17.bazaarutils.config.BUConfig;
import com.github.mkram17.bazaarutils.events.BUListener;
import com.github.mkram17.bazaarutils.events.ChestLoadedEvent;
import com.github.mkram17.bazaarutils.misc.ItemData;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.github.mkram17.bazaarutils.BazaarUtils.EVENT_BUS;

/**
 * Parses the “Co-op Bazaar Orders” chest GUI and keeps {@link BUConfig#watchedItems}
 * in sync.  Pure Forge 1.8.9 – no Fabric classes, no Lombok, no Orbit.
 */
public final class ItemUpdater implements BUListener {

    /* live references while the GUI is open */
    private List<ItemStack> chestStacks   = new ArrayList<>();
    private List<ItemStack> orderEntries  = new ArrayList<>();

    /* ------------------------------------------------------------ */
    /*  BUListener                                                  */
    /* ------------------------------------------------------------ */

    @Override
    public void subscribe() {
        EVENT_BUS.subscribe(this);      // functional interface – no annotations needed
    }

    /** Called from the (vanilla) ChestLoadedEvent mixin helper. */
    @SuppressWarnings("unused")   // called via functional-interface registration
    public void onChestLoaded(ChestLoadedEvent ev) {
        if (!BazaarUtils.GUI.inBuyOrders()) return;

        chestStacks  = ev.getItemStacks();
        orderEntries = locateOrderItems(chestStacks);

        updateWatchedItems(orderEntries);
    }

    /* ------------------------------------------------------------ */
    /*  Main update routine                                         */
    /* ------------------------------------------------------------ */

    private void updateWatchedItems(List<ItemStack> orders) {
        List<ItemData> found = new ArrayList<>();

        for (ItemStack st : orders) {
            if (st == null || !st.hasTagCompound()) continue;

            ParsedOrder o = parseOrder(st);
            if (o == null) continue;                     // malformed lore

            /* Build a temporary ItemData mirroring the order’s info. */
            ItemData tmp = new ItemData(
                    o.name,
                    o.unitPrice * o.totalVol,
                    o.sellOrder ? ItemData.priceTypes.INSTABUY
                                 : ItemData.priceTypes.INSTASELL,
                    o.totalVol);
            tmp.setMaximumRounding(0.0);                 // exact price now
            if (o.filledVol >= 0) tmp.setAmountFilled(o.filledVol);
            if (o.claimedVol >= 0) tmp.setAmountClaimed(o.claimedVol);
            if (o.filledVol == o.totalVol) tmp.setStatus(ItemData.statuses.FILLED);

            /* Try to merge with existing watch-list entry. */
            ItemData existing = ItemData.findItem(tmp, BUConfig.get().watchedItems);
            if (existing == null) {
                Util.addWatchedItem(tmp);                // new entry
            } else {
                sync(existing, tmp);                     // update fields
            }
            found.add(tmp);
        }

        /* Remove watch-list items that are no longer present */
        purgeMissing(found);

        BUConfig.HANDLER.save();
        ItemData.update();
    }

    /* Copy changed fields existing ← fresh */
    private static void sync(ItemData ex, ItemData nu) {
        if (ex.getPrice()         != nu.getPrice())         ex.setPrice(nu.getPrice());
        if (ex.getStatus()        != nu.getStatus())        ex.setStatus(nu.getStatus());
        if (ex.getAmountFilled()  != nu.getAmountFilled())  ex.setAmountFilled(nu.getAmountFilled());
        if (ex.getAmountClaimed() != nu.getAmountClaimed()) ex.setAmountClaimed(nu.getAmountClaimed());
        if (ex.getMaximumRounding()!= 0.0)                  ex.setMaximumRounding(0.0);
    }

    /* Remove items the GUI no longer shows */
    private static void purgeMissing(List<ItemData> current) {
        Iterator<ItemData> it = BUConfig.get().watchedItems.iterator();
        while (it.hasNext()) {
            ItemData w = it.next();
            if (ItemData.findItem(w, current) == null) {
                it.remove();
                Util.notifyAll("Removed " + w.getGeneralInfo(), Util.notificationTypes.ITEMDATA);
            }
        }
    }

    /* ------------------------------------------------------------ */
    /*  Helper – locate order slots between black-pane separators   */
    /* ------------------------------------------------------------ */

    private static boolean isBlackPane(ItemStack s) {
        return s != null
                && s.getItem() == Item.getItemFromBlock(Blocks.stained_glass_pane)
                && s.getMetadata() == 15;                 // colour id for black
    }

    private static List<ItemStack> locateOrderItems(List<ItemStack> slots) {
        int first = -1, last = -1;

        /* Skip initial black panes */
        int idx = 0;
        while (idx < slots.size() && isBlackPane(slots.get(idx))) idx++;
        first = idx;

        /* Find triple-pane terminator */
        while (idx + 2 < slots.size()) {
            if (isBlackPane(slots.get(idx)) &&
                isBlackPane(slots.get(idx + 1)) &&
                isBlackPane(slots.get(idx + 2))) {
                last = idx;
                break;
            }
            idx++;
        }
        if (first < 0 || last < 0 || first >= last) return new ArrayList<>();

        List<ItemStack> out = new ArrayList<>();
        for (int i = first; i < last; i++)
            if (!isBlackPane(slots.get(i)))
                out.add(slots.get(i));

        /* If very first slot is an arrow ➜ the page is empty */
        if (!out.isEmpty() && out.get(0).getItem() == Items.arrow) out.clear();
        return out;
    }

    /* ------------------------------------------------------------ */
    /*  Lore-parsing helpers                                         */
    /* ------------------------------------------------------------ */

    private static final class ParsedOrder {
        String  name = "";
        boolean sellOrder;
        double  unitPrice;
        int     totalVol   = 0;
        int     filledVol  = -1;    // -1 = not filled yet
        int     claimedVol = -1;
    }

    /** Extract all useful information from the Lore of one order-item. */
    private static ParsedOrder parseOrder(ItemStack stack) {
        ParsedOrder po = new ParsedOrder();

        /* ---------- display name ---------- */
        String disp = stack.getDisplayName().replaceAll("§.", "");
        if (disp.startsWith("BUY "))  { po.sellOrder = false; po.name = disp.substring(4); }
        if (disp.startsWith("SELL ")) { po.sellOrder = true;  po.name = disp.substring(5); }

        /* ---------- lore lines ---------- */
        List<String> lore = getLoreLines(stack);

        // volume / price
        if (lore.size() >= 3) {
            String volStr = strip(lore.get(2));     // “   128x  ”
            try { po.totalVol = Util.parseNumber(volStr); } catch (Exception ignored) {}
        }

        // per-unit price
        for (String l : lore)
            if (l.contains("per unit")) {
                po.unitPrice = Double.parseDouble(Util.extractTextAfterWord(l, "unit:")
                                                        .replace(",", "").trim());
                break;
            }

        // filled / claimed
        for (String l : lore) {
            String s = strip(l);
            if (s.startsWith("Filled")) {                   // “Filled: 64/128”
                int slash = s.indexOf('/');
                if (slash > 0) {
                    po.filledVol = Util.parseNumber(s.substring(8, slash));
                    // totalVol was set already
                }
            }
            if (s.contains("to claim")) {                   // “… 3200 coins to claim!”
                String num = s.substring(9, s.indexOf(' ')).replace(",", "");
                try { po.claimedVol = Integer.parseInt(num); } catch (Exception ignored) {}
            }
        }
        return po;
    }

    /* Grab lore → List<String> (colour codes stripped) */
    private static List<String> getLoreLines(ItemStack st) {
        List<String> out = new ArrayList<>();
        if (!st.hasTagCompound()) return out;

        NBTTagCompound root = st.getTagCompound();
        if (root == null || !root.hasKey("display")) return out;

        NBTTagCompound disp = root.getCompoundTag("display");
        if (!disp.hasKey("Lore")) return out;

        for (int i = 0; i < disp.getTagList("Lore", 8 /*String*/).tagCount(); i++) {
            String s = disp.getTagList("Lore", 8).getStringTagAt(i)
                           .replace("§", "");
            out.add(s);
        }
        return out;
    }

    private static String strip(String s) { return s.replace("§", ""); }
}
