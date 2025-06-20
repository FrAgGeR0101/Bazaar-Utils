package com.github.mkram17.bazaarutils.features;

import com.github.mkram17.bazaarutils.BazaarUtils;
import com.github.mkram17.bazaarutils.events.*;
import com.github.mkram17.bazaarutils.misc.CustomItemButton;
import com.github.mkram17.bazaarutils.misc.ItemData;
import com.github.mkram17.bazaarutils.utils.GUIUtils;
import com.github.mkram17.bazaarutils.utils.SoundUtil;
import com.github.mkram17.bazaarutils.utils.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

import static com.github.mkram17.bazaarutils.BazaarUtils.EVENT_BUS;

/**
 * “Flip order” helper shown in the Bazaar order-options GUI.
 * <p>Works on Forge 1.8.9 without any Fabric, Lombok or YACL classes.</p>
 */
public final class FlipHelper extends CustomItemButton implements BUListener {

    /* ------------------------------------------------------------------ */
    /*  Tunables & runtime state                                          */
    /* ------------------------------------------------------------------ */
    private boolean enabled;
    private final Item replaceItem;

    private ItemData item;                 // matching watched-item
    private boolean  waitingForSign    = false;
    private boolean  inCancelDialogue  = false;

    private double   flipPrice         = 0;
    private double   orderPrice        = -1;
    private int      orderVolFilled    = -1;

    /* ------------------------------------------------------------------ */
    /*  Construction / config                                             */
    /* ------------------------------------------------------------------ */
    public FlipHelper(boolean enabled, int slotNumber, Item pane) {
        this.enabled     = enabled;
        this.slotNumber  = slotNumber;
        this.replaceItem = pane;

        EVENT_BUS.subscribe(this);
    }

    /* ------------------------------------------------------------- */
    /*  Plain getters / setters (no Lombok)                         */
    /* ------------------------------------------------------------- */
    public boolean isEnabled()              { return enabled; }
    public void    setEnabled(boolean b)    { enabled = b;    }
    public Item    getReplaceItem()         { return replaceItem; }

    /* ------------------------------------------------------------------ */
    /*  Event-style callback registrations                                */
    /* ------------------------------------------------------------------ */
    @Override
    public void subscribe() {
        /* already subscribed in constructor */
    }

    /* ------------------------------------------------------------------ */
    /*  Chest-load (GUI opened)                                           */
    /* ------------------------------------------------------------------ */
    public void onChestLoaded(ChestLoadedEvent ev) {

        if (!enabled || !BazaarUtils.GUI.inFlipGui()) return;

        inCancelDialogue = isCancelDialogue(ev);

        /* find the “Flip Order” item inside the chest */
        item = findFlipItem(ev);
        if (item != null) flipPrice = item.getFlipPrice();
    }

    /* ------------------------------------------------------------------ */
    /*  Slot click inside flip GUI                                        */
    /* ------------------------------------------------------------------ */
    public void onSlotClick(SlotClickEvent ev) {
        if (!enabled ||
            !BazaarUtils.GUI.inFlipGui() ||
            ev.slot.getIndex() != slotNumber) return;

        SoundUtil.playSound(BUTTON_SOUND, BUTTON_VOLUME);

        /* click the sign (slot 15) to open it */
        GUIUtils.clickSlot(15, 0);
        waitingForSign = true;
        ev.setCancelled(true);
    }

    /* ------------------------------------------------------------------ */
    /*  Sign opened –- insert new price                                   */
    /* ------------------------------------------------------------------ */
    public void onSignOpen(SignOpenEvent e) {
        if (!waitingForSign || item == null) return;
        waitingForSign = false;

        if (flipPrice == 0) return;                      // no competitor yet

        String txt = Util.pretty(flipPrice) + "";
        GUIUtils.setSignText(txt, true);

        item.flip(flipPrice);
    }

    /* ------------------------------------------------------------------ */
    /*  Replace dummy button in the GUI                                   */
    /* ------------------------------------------------------------------ */
    public void onReplaceItem(ReplaceItemEvent ev) {
        if (ev.getSlotId() != slotNumber ||
            !enabled ||
            !BazaarUtils.GUI.inFlipGui() ||
            inCancelDialogue) return;

        ItemStack stack = new ItemStack(replaceItem, 1);

        String name;
        String size;

        if (flipPrice == 0) {
            name = EnumChatFormatting.DARK_PURPLE +
                   "No competing sell offers";
            size = "ANY";
        } else if (item == null) {
            name = EnumChatFormatting.DARK_PURPLE + "Order not found";
            size = "???";
        } else {
            name = EnumChatFormatting.DARK_PURPLE +
                   "Flip for " + Util.pretty(flipPrice) + " c";
            size = Util.pretty(flipPrice) + "";
        }

        stack.setStackDisplayName(name);
        stack.setTagCompound(new net.minecraft.nbt.NBTTagCompound());
        stack.getTagCompound().setString("bu_size", size);

        ev.setReplacement(stack);
    }

    /* ================================================================== */
    /*  Helper methods                                                    */
    /* ================================================================== */

    /** Parse the container looking for the “Flip Order” stack. */
    private ItemData findFlipItem(ChestLoadedEvent ev) {

        for (ItemStack st : ev.getItemStacks()) {
            if (st == null) continue;

            String name = st.getDisplayName();
            if (!name.contains("Flip Order")) continue;

            parseLore(st);                               // fills orderPrice/Vol

            ItemData it = ItemData.findItem(
                    null,
                    orderPrice,
                    orderVolFilled,
                    ItemData.PriceType.INSTASELL);

            if (it != null) return it;
        }
        return null;
    }

    /** Extract order price & filled volume from tooltip lines. */
    private void parseLore(ItemStack st) {
        orderPrice      = -1;
        orderVolFilled  = -1;

        Minecraft mc = Minecraft.getMinecraft();
        for (String s : st.getTooltip(mc.thePlayer, false)) {
            String clean = Util.removeFormatting(s);

            if (clean.startsWith("Price per unit")) {
                /* “Price per unit: 1 234.5 coins” */
                String num = clean.replaceAll("[^0-9.]", "");
                orderPrice = Double.parseDouble(num);
            } else if (clean.startsWith("Filled")) {
                /* “Filled: 128/128” */
                int idx = clean.indexOf('/');
                if (idx > 0) {
                    String part = clean.substring(7, idx).replace(",", "");
                    orderVolFilled = Integer.parseInt(part);
                }
            }
        }
    }

    /** Detect the “Cancel order?” confirmation chest. */
    private boolean isCancelDialogue(ChestLoadedEvent ev) {
        if (!BazaarUtils.GUI.getContainerName().contains("Order options"))
            return false;

        /* slot 11 is the cancel-button – read first lore line */
        if (ev.getItemStacks().size() <= 11) return false;

        ItemStack s = ev.getItemStacks().get(11);
        for (String t : s.getTooltip(Minecraft.getMinecraft().thePlayer, false))
            if (t.contains("Cannot cancel")) return false;

        return true;
    }
}
