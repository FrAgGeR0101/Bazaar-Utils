package com.github.mkram17.bazaarutils.features;

import com.github.mkram17.bazaarutils.BazaarUtils;
import com.github.mkram17.bazaarutils.events.*;
import com.github.mkram17.bazaarutils.misc.CustomItemButton;
import com.github.mkram17.bazaarutils.misc.ItemData;
import com.github.mkram17.bazaarutils.utils.GUIUtils;
import com.github.mkram17.bazaarutils.utils.SoundUtil;
import com.github.mkram17.bazaarutils.utils.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.common.MinecraftForge;

/**
 * “Flip order” helper shown in the Bazaar order-options GUI.
 * Pure Forge 1.8.9 – no Fabric, Lombok or YACL classes required.
 */
public final class FlipHelper extends CustomItemButton implements BUListener {

    /* ────────────────────────── tunables / runtime ───────────────────────── */
    private boolean enabled;
    private final Item replaceItem;

    private ItemData item;                  // matching watched item
    private boolean  waitingForSign = false;
    private boolean  inCancelDlg   = false;

    private double   flipPrice     = 0;     // coins per unit
    private double   orderPrice    = -1;    // parsed from lore
    private int      orderFilled   = -1;    // parsed from lore

    /* ────────────────────────── construction ─────────────────────────────── */
    public FlipHelper(boolean enabled, int slotNumber, Item pane) {
        this.enabled    = enabled;
        this.slotNumber = slotNumber;
        this.replaceItem= pane;

        /* use Forge event-bus – BUListener.subscribe() hooks runtime bus */
        MinecraftForge.EVENT_BUS.register(this);
    }

    /* ---------------------------------------------------------------------- */
    /*  BUListener hook (no-op – registration done in ctor)                   */
    /* ---------------------------------------------------------------------- */
    @Override public void subscribe() { /* already on Forge bus */ }

    /* ---------------------------------------------------------------------- */
    /*  Chest finished loading → analyse GUI                                  */
    /* ---------------------------------------------------------------------- */
    public void onChestLoaded(ChestLoadedEvent ev) {
        if (!enabled || !BazaarUtils.GUI.inFlipGui()) return;

        inCancelDlg = isCancelDialogue(ev);          // confirm-cancel GUI?

        item = findFlipItem(ev);
        if (item != null) {
            /* flip price: 0.1 coin below best competitor on opposite side   */
            flipPrice = Util.pretty(item.getMarketOppositePrice() - 0.1);
        }
    }

    /* ---------------------------------------------------------------------- */
    /*  Player clicked inside the flip-options GUI                            */
    /* ---------------------------------------------------------------------- */
    public void onSlotClick(SlotClickEvent ev) {
        if (!enabled || !BazaarUtils.GUI.inFlipGui()) return;

        Slot s = ev.getSlot();
        if (s == null || s.slotNumber != slotNumber) return;

        SoundUtil.playSound("random.click", 0.5f);
        GUIUtils.clickSlot(15, 0);                   // open sign (slot 15)
        waitingForSign = true;
        ev.setCancelled(true);                       // block vanilla click
    }

    /* ---------------------------------------------------------------------- */
    /*  Sign opened → fill in new under-cut price                             */
    /* ---------------------------------------------------------------------- */
    public void onSignOpen(SignOpenEvent ev) {
        if (!waitingForSign || item == null || flipPrice <= 0) return;
        waitingForSign = false;

        GUIUtils.setSignText(Double.toString(flipPrice), true);
        item.flip(flipPrice);                        // update watched list
    }

    /* ---------------------------------------------------------------------- */
    /*  Replace the dummy glass-pane in slot <slotNumber>                      */
    /* ---------------------------------------------------------------------- */
    public void onReplaceItem(ReplaceItemEvent ev) {
        if (ev.getSlotId() != slotNumber ||
            !enabled ||
            !BazaarUtils.GUI.inFlipGui() ||
            inCancelDlg) return;

        ItemStack out = new ItemStack(replaceItem, 1);

        String name, size;
        if (flipPrice == 0) {
            name = EnumChatFormatting.DARK_PURPLE + "No competing sell offers";
            size = "ANY";
        } else if (item == null) {
            name = EnumChatFormatting.DARK_PURPLE + "Order not found";
            size = "???";
        } else {
            name = EnumChatFormatting.DARK_PURPLE +
                   "Flip for " + Util.pretty(flipPrice) + " coins";
            size = Util.pretty(flipPrice) + "";
        }

        out.setStackDisplayName(name);
        out.setTagCompound(new net.minecraft.nbt.NBTTagCompound());
        out.getTagCompound().setString("bu_size", size);

        ev.setReplacement(out);
    }

    /* ====================================================================== */
    /*  Helper methods                                                        */
    /* ====================================================================== */

    /** Scan the chest for the “Flip Order” stack and match against watch-list. */
    private ItemData findFlipItem(ChestLoadedEvent ev) {
        for (ItemStack st : ev.getItemStacks()) {
            if (st == null) continue;
            if (!st.getDisplayName().contains("Flip Order")) continue;

            parseLore(st);                            // fills orderPrice/orderFilled

            /* naive match: same price ± rounding, same vol, same side         */
            for (ItemData it : BUConfig.get().getWatchedItems()) {
                if (it.isSimilarPrice(orderPrice) &&
                    it.getVolume() == orderFilled &&
                    it.getPriceType() == ItemData.PriceType.INSTASELL) return it;
            }
        }
        return null;
    }

    /** Extract price per unit + filled volume from the tooltip. */
    private void parseLore(ItemStack st) {
        orderPrice   = -1;
        orderFilled  = -1;

        for (String raw : st.getTooltip(Minecraft.getMinecraft().thePlayer, false)) {
            String s = Util.removeFormatting(raw);

            if (s.startsWith("Price per unit")) {               // “Price per unit: 123.4”
                String num = s.replaceAll("[^0-9.]", "");
                orderPrice = Double.parseDouble(num);
            } else if (s.startsWith("Filled")) {                // “Filled: 128/128”
                int idx = s.indexOf('/');
                if (idx > 0) {
                    orderFilled = Integer.parseInt(
                            s.substring(7, idx).replace(",", ""));
                }
            }
        }
    }

    /** Detect whether the current chest is the “Cancel order?” dialogue. */
    private boolean isCancelDialogue(ChestLoadedEvent ev) {
        if (!GUIUtils.containerTitle().contains("Order options")) return false;
        if (ev.getItemStacks().size() <= 11) return false;        // safety

        ItemStack cancel = ev.getItemStacks().get(11);            // cancel button
        for (String t : cancel.getTooltip(Minecraft.getMinecraft().thePlayer, false))
            if (t.contains("Cannot cancel")) return false;
        return true;
    }
}
