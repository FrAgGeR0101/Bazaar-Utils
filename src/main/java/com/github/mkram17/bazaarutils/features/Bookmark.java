package com.github.mkram17.bazaarutils.features;

import com.github.mkram17.bazaarutils.BazaarUtils;
import com.github.mkram17.bazaarutils.config.BUConfig;
import com.github.mkram17.bazaarutils.events.ReplaceItemEvent;
import com.github.mkram17.bazaarutils.events.SlotClickEvent;
import com.github.mkram17.bazaarutils.misc.CustomItemButton;
import com.github.mkram17.bazaarutils.misc.ItemSlotButtonWidget;
import com.github.mkram17.bazaarutils.utils.GUIUtils;
import com.github.mkram17.bazaarutils.utils.SoundUtil;
import com.github.mkram17.bazaarutils.utils.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Items;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Simple bookmark button that appears in every Bazaar item GUI.
 * Pure Forge 1.8.9 (no Fabric, Lombok or modern APIs).
 */
public final class Bookmark extends CustomItemButton {

    /* ------------------------------------------------------------------ */
    /*  Static resources                                                  */
    /* ------------------------------------------------------------------ */

    private static final ResourceLocation TEX_BASE  =
            new ResourceLocation(BazaarUtils.MODID,
                                 "textures/widget/widget_bookmark_base.png");
    private static final ResourceLocation TEX_HOVER =
            new ResourceLocation(BazaarUtils.MODID,
                                 "textures/widget/widget_bookmark_hover.png");

    /* ------------------------------------------------------------------ */
    /*  Per-instance state                                                */
    /* ------------------------------------------------------------------ */

    private final ItemStack icon;          // icon shown on the cog-bar
    private String  name;                  // item name being bookmarked
    private boolean activeGui = false;     // true ← right Bazaar GUI open

    /* ------------------------------------------------------------------ */
    /*  Construction                                                      */
    /* ------------------------------------------------------------------ */

    public Bookmark(String name, ItemStack icon) {
        this.name      = name;
        this.icon      = icon.copy();
        this.slotNumber = 0;               // assigned later by buildWidgets()
    }

    /* ------------------------------------------------------------------ */
    /*  Replace-item logic                                                */
    /* ------------------------------------------------------------------ */

    @Override protected boolean shouldReplaceItem(ReplaceItemEvent ev) {
        if (!activeGui)                       return false;
        if (ev.getSlotId() != slotNumber)     return false;

        ev.setReplacement(getReplacement());
        return true;
    }

    @Override protected boolean shouldUseSlot(SlotClickEvent ev) {
        return activeGui && ev.getSlotId() == slotNumber;
    }

    /* handle the (invisible) pane click inside the chest */
    @Override public void handleSlotClick() {
        SoundUtil.playClick();
        toggleBookmark();
        BUConfig.save();
    }

    /* ------------------------------------------------------------------ */
    /*  GUI widget clicks                                                 */
    /* ------------------------------------------------------------------ */

    /** Left-click on the cog-bar bookmark button → rename. */
    public void onWidgetLeftClick() {
        SoundUtil.playClick();
        GUIUtils.clickSlot(45, 0);                // open sign
        GUIUtils.setSignText(name, true);
    }

    /** SHIFT-click on the cog-bar bookmark button → delete bookmark. */
    public void onWidgetShiftClick() {
        BUConfig.get().getBookmarks().remove(this);
        BUConfig.save();
        SoundUtil.playClick();
    }

    /* ------------------------------------------------------------------ */
    /*  Bookmark toggle helpers                                           */
    /* ------------------------------------------------------------------ */

    private void toggleBookmark() {
        List<Bookmark> list = BUConfig.get().getBookmarks();

        if (list.contains(this)) {
            list.remove(this);
            Util.notifyAll("Removed bookmark: " + name);
        } else {
            list.add(this);
            Util.notifyAll("Added bookmark: " + name);
        }
    }

    private ItemStack getReplacement() {
        boolean on = BUConfig.get().getBookmarks().contains(this);

        ItemStack pane = new ItemStack(Items.stained_glass_pane, 1, on ? 10 : 14);
        pane.setStackDisplayName(
                (on ? "Remove " : "Bookmark ") + EnumChatFormatting.GOLD + name);
        return pane;
    }

    /* ------------------------------------------------------------------ */
    /*  Static helpers                                                    */
    /* ------------------------------------------------------------------ */

    public static boolean exists(String name) {
        return BUConfig.get().getBookmarks().stream()
                       .anyMatch(b -> b.name.equalsIgnoreCase(name));
    }

    /** Called every time a Bazaar container opens to build all buttons. */
    public static List<ItemSlotButtonWidget> buildWidgets() {

        if (!GUIUtils.inBazaar()) return Collections.emptyList();

        final int SIZE = 18, PAD = 4;
        int x = 176 + 7;       // vanilla container width + left margin
        int y = 17  + PAD;

        List<ItemSlotButtonWidget> out = new ArrayList<>();

        for (Bookmark bm : BUConfig.get().getBookmarks()) {

            ItemSlotButtonWidget w = new ItemSlotButtonWidget(
                    x, y, SIZE, SIZE,
                    TEX_BASE, TEX_HOVER,
                    () -> {
                        boolean shift = org.lwjgl.input.Keyboard.isKeyDown(
                                org.lwjgl.input.Keyboard.KEY_LSHIFT);
                        if (shift) bm.onWidgetShiftClick();
                        else       bm.onWidgetLeftClick();
                    },
                    bm.icon.stackSize == 0 ? new ItemStack(Items.barrier) : bm.icon,
                    bm.name);

            /* remember the slot-number inside the bookmark instance */
            bm.slotNumber = w.getSlotIndex();

            out.add(w);
            y += SIZE + PAD;
        }
        return out;
    }
}
