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
import net.minecraft.item.ItemStack;
import net.minecraft.init.Items;
import net.minecraft.util.ResourceLocation;
import java.util.ArrayList;
import java.util.List;

/**
 * Vanilla Forge-1.8.9 bookmark button—no Fabric, Lombok, or Orbit.
 */
public final class Bookmark extends CustomItemButton {

    /* ------------------------------------------------------------------ */
    /*  Static data                                                       */
    /* ------------------------------------------------------------------ */

    private static final int SIGN_SLOT_NUMBER = 45;

    /** 16×16 PNGs inside <tt>assets/bazaarutils/textures/widget/</tt> */
    private static final ResourceLocation TEX_BASE  =
            new ResourceLocation(BazaarUtils.MODID, "textures/widget/widget_bookmark_base.png");
    private static final ResourceLocation TEX_HOVER =
            new ResourceLocation(BazaarUtils.MODID, "textures/widget/widget_bookmark_hover.png");

    /** Minimal replacement for ButtonTextures (does not exist on 1.8.9). */
    public static final class ButtonTextures {
        public final ResourceLocation normal, hover;
        public ButtonTextures(ResourceLocation n, ResourceLocation h) {
            this.normal = n; this.hover = h;
        }
    }
    public static final ButtonTextures SLOT_BUTTON_TEXTURES =
            new ButtonTextures(TEX_BASE, TEX_HOVER);

    /* ------------------------------------------------------------------ */
    /*  Per-instance data                                                 */
    /* ------------------------------------------------------------------ */

    private String    name;
    private ItemStack bookmarkedItem;
    private boolean   guiActive = false;   // true only while the correct GUI is open

    /* ------------------------------------------------------------------ */
    /*  Construction                                                      */
    /* ------------------------------------------------------------------ */

    public Bookmark(String name, ItemStack icon) {
        this.name           = name;
        this.slotNumber     = 0;           // overwritten later by GUI logic
        this.bookmarkedItem = icon.copy();

        changeVisuals(isBookmarked(name));
        replacementItem.setStackDisplayName("★");

        guiActive = true;
    }

    /* ------------------------------------------------------------------ */
    /*  Public getters                                                    */
    /* ------------------------------------------------------------------ */

    public String    getName()           { return name; }
    public ItemStack getBookmarkedItem() { return bookmarkedItem; }

    /* ------------------------------------------------------------------ */
    /*  Replace / click logic (called by GUI helper classes)              */
    /* ------------------------------------------------------------------ */

    /** Called from {@link ReplaceItemEvent} dispatcher. */
    @Override
    protected boolean shouldReplaceItem(ReplaceItemEvent ev) {
        if (!guiActive)                             return false;
        if (ev.getSlotId() != slotNumber)           return false;

        changeVisuals(isBookmarked(name));          // refresh
        ev.setReplacement(replacementItem);
        return true;
    }

    /** Called from {@link SlotClickEvent} dispatcher. */
    @Override
    protected boolean shouldUseSlot(SlotClickEvent ev) {
        return guiActive && ev.slotId == slotNumber;
    }

    /** Toggle bookmark when the invisible glass-pane is clicked. */
    public void handleSlotClick() {
        SoundUtil.playClick();
        toggleBookmark();
        BUConfig.HANDLER.save();
    }

    /* ------------------------------------------------------------------ */
    /*  Widget interactions                                               */
    /* ------------------------------------------------------------------ */

    public void onWidgetLeftClick() {
        SoundUtil.playClick();
        GUIUtils.clickSlot(SIGN_SLOT_NUMBER, 0);
        GUIUtils.setSignText(name, true);
    }

    public void onWidgetShiftClick() {
        BUConfig.get().bookmarks.remove(this);
        BUConfig.HANDLER.save();
    }

    /* ------------------------------------------------------------------ */
    /*  Bookmark management                                               */
    /* ------------------------------------------------------------------ */

    private void toggleBookmark() {
        if (isBookmarked(name)) {
            changeVisuals(false);
            BUConfig.get().bookmarks.remove(this);
        } else {
            changeVisuals(true);
            BUConfig.get().bookmarks.add(this);
        }
    }

    private void changeVisuals(boolean bookmarked) {
        if (bookmarked) {
            replacementItem = new ItemStack(Items.dye, 1, 10);          // green pane in 1.8.9
            replacementItem.setStackDisplayName("Remove " + name + " Bookmark");
        } else {
            replacementItem = new ItemStack(Items.dye, 1, 14);          // red pane
            replacementItem.setStackDisplayName("Bookmark " + name);
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Static helpers                                                    */
    /* ------------------------------------------------------------------ */

    public static boolean isBookmarked(String n) { return find(n) != null; }

    public static Bookmark find(String n) {
        for (Bookmark b : BUConfig.get().bookmarks)
            if (b.name.equalsIgnoreCase(n)) return b;
        return null;
    }

    /** Build all bookmark widgets for the currently open Bazaar GUI. */
    public static List<ItemSlotButtonWidget> buildWidgets() {
        List<ItemSlotButtonWidget> list = new ArrayList<>();
        if (Minecraft.getMinecraft().currentScreen == null) return list;
        if (!Util.removeFormatting(Minecraft.getMinecraft()
                                            .currentScreen.getTitle().getFormattedText())
                  .startsWith("Bazaar")) return list;

        final int size = 18, pad = 4;
        int x = 176 + 7;                 // vanilla container width + margin
        int y = 17 + pad;

        for (Bookmark bm : BUConfig.get().bookmarks) {
            ItemSlotButtonWidget w = new ItemSlotButtonWidget(
                    x, y, size, size,
                    SLOT_BUTTON_TEXTURES,
                    () -> {
                        if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_LSHIFT))
                            bm.onWidgetShiftClick();
                        else
                            bm.onWidgetLeftClick();
                    },
                    bm.bookmarkedItem.isEmpty()
                            ? new ItemStack(Items.barrier)
                            : bm.bookmarkedItem,
                    bm.name);

            list.add(w);
            y += size + pad;
        }
        return list;
    }
}
