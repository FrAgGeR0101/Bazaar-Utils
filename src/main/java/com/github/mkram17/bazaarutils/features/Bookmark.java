package com.github.mkram17.bazaarutils.features;

import com.github.mkram17.bazaarutils.BazaarUtils;
import com.github.mkram17.bazaarutils.config.BUConfig;
import com.github.mkram17.bazaarutils.events.ChestLoadedEvent;
import com.github.mkram17.bazaarutils.events.ReplaceItemEvent;
import com.github.mkram17.bazaarutils.events.SlotClickEvent;
import com.github.mkram17.bazaarutils.misc.CustomItemButton;
import com.github.mkram17.bazaarutils.misc.ItemSlotButtonWidget;
import com.github.mkram17.bazaarutils.misc.ModCompatibilityHelper;
import com.github.mkram17.bazaarutils.mixin.AccessorHandledScreen;
import com.github.mkram17.bazaarutils.utils.GUIUtils;
import com.github.mkram17.bazaarutils.utils.SoundUtil;
import com.github.mkram17.bazaarutils.utils.Util;
import lombok.Getter;
import lombok.Setter;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Bookmark button rendered on the right-hand side of the Bazaar GUI. */
public class Bookmark extends CustomItemButton {

    /* ------------------------------------------------------------------ */
    /*  instance data                                                     */
    /* ------------------------------------------------------------------ */
    @Getter @Setter private String     name;
    @Getter @Setter private ItemStack  bookmarkedItem;

    private static final int SIGN_SLOT_NUMBER = 45;      // slot for the buy-order sign
    private boolean inCorrectGui        = false;

    /* widget textures (16×16 PNGs inside assets/…/textures/widget/) */
    private static final Identifier BASE  =
            Identifier.tryParse(BazaarUtils.MODID, "widget/widget_bookmark_base");
    private static final Identifier HOVER =
            Identifier.tryParse(BazaarUtils.MODID, "widget/widget_bookmark_hover");

    public  static final ButtonTextures SLOT_BUTTON_TEXTURES =
            new ButtonTextures(BASE, HOVER);

    /* ------------------------------------------------------------------ */
    /*  life-cycle                                                        */
    /* ------------------------------------------------------------------ */

    /** Construct a new bookmark button for the current GUI. */
    public Bookmark(String name, ItemStack bookmarkedItem) {
        this.name          = name;
        this.slotNumber    = 0;           // overwritten later by GUI logic
        this.bookmarkedItem = bookmarkedItem;

        changeVisuals(isBookmarked(name));
        this.replacementItem.set(BazaarUtils.CUSTOM_SIZE_COMPONENT, "★");

        this.inCorrectGui = true;
        BazaarUtils.eventBus.subscribe(this);
    }

    /** Unsubscribe as soon as a ChestLoadedEvent fires (GUI finished). */
    @EventHandler
    protected void checkGui(ChestLoadedEvent event) {
        BazaarUtils.eventBus.unsubscribe(this);
    }

    /* ------------------------------------------------------------------ */
    /*  Orbit event handlers                                              */
    /* ------------------------------------------------------------------ */

    @EventHandler
    protected void replaceItemEvent(ReplaceItemEvent event) {
        if (!inCorrectGui || !super.shouldReplaceItem(event))
            return;

        if (replacementItem == null)
            changeVisuals(isBookmarked(name));

        event.setReplacement(replacementItem);
    }

    @EventHandler
    private void onBookmarkClick(SlotClickEvent event) {
        if (!inCorrectGui || !super.shouldUseSlot(event))
            return;

        SoundUtil.playSound(BUTTON_SOUND, BUTTON_VOLUME);
        switchBookmarked();
        bookmarkedItem = findItem(name, event);
        BUConfig.HANDLER.save();
    }

    /* ------------------------------------------------------------------ */
    /*  click behaviour                                                   */
    /* ------------------------------------------------------------------ */

    /** Normal click: write the item name to the Bazaar search sign. */
    public void onWidgetLeftClick() {
        SoundUtil.playSound(BUTTON_SOUND, BUTTON_VOLUME);

        ModCompatibilityHelper.tryDisableSkyblockerBazaarOverlay();
        GUIUtils.clickSlot(SIGN_SLOT_NUMBER, 0);
        GUIUtils.setSignText(name, true);
        Util.tickExecuteLater(4, ModCompatibilityHelper::tryEnableSkyblockerBazaarOverlay);
    }

    /** Alt-click (requires cookie): open the item directly. */
    public void alternateOnWidgetLeftClick() {
        GUIUtils.closeHandledScreen();
        Util.sendCommand("bz " + name);
    }

    /** Shift-click on the widget itself → delete bookmark. */
    public void onWidgetShiftClick() {
        BUConfig.get().bookmarks.remove(this);
        BUConfig.HANDLER.save();
    }

    /* ------------------------------------------------------------------ */
    /*  bookmark management                                               */
    /* ------------------------------------------------------------------ */

    private void switchBookmarked() {
        if (isBookmarked(name)) {
            changeVisuals(false);
            BUConfig.get().bookmarks.remove(this);
        } else {
            changeVisuals(true);
            BUConfig.get().bookmarks.add(this);
        }
        BUConfig.HANDLER.save();
    }

    private void changeVisuals(boolean bookmarked) {
        if (bookmarked) {
            replacementItem = new ItemStack(Items.GREEN_STAINED_GLASS_PANE, 1);
            replacementItem.setStackDisplayName("Remove " + name + " Bookmark");
            replacementItem.set(BazaarUtils.CUSTOM_SIZE_COMPONENT, "⃠ ");
        } else {
            replacementItem = new ItemStack(Items.RED_STAINED_GLASS_PANE, 1);
            replacementItem.setStackDisplayName("Bookmark " + name);
            replacementItem.set(BazaarUtils.CUSTOM_SIZE_COMPONENT, "★");
        }
    }

    /* ------------------------------------------------------------------ */
    /*  find matching item / name helper                                  */
    /* ------------------------------------------------------------------ */

    public static String findName(ChestLoadedEvent e) {
        String containerName = GUIUtils.getContainerName();
        String name          = findNameFromContainer();

        // Extra check for very long container titles
        if (containerName.length() > 30) {
            for (ItemStack stack : e.getItemStacks()) {
                if (stack == null) continue;
                if (!stack.isEmpty() &&
                    stack.getDisplayName().startsWith(name)) {
                    return stack.getDisplayName(); // 1.8.9 equivalent
                }
            }
        }
        return name;
    }

    private static String findNameFromContainer() {
        String containerName = GUIUtils.getContainerName();
        if (containerName == null) return "?";

        if (BazaarUtils.gui.inInstaBuy())
            return containerName.substring(0, containerName.indexOf("➜") - 1);

        if (BazaarUtils.gui.inBuyOrderScreen()) {
            containerName = BazaarUtils.gui.getPreviousScreenName();
            return containerName.substring(containerName.indexOf("➜") + 2);
        }

        if (BazaarUtils.gui.inAnyItemScreen())
            return containerName.substring(containerName.indexOf("➜") + 2);

        return "?";
    }

    private static ItemStack findItem(String name, SlotClickEvent event) {
        ScreenHandler handler = event.handledScreen.getScreenHandler();

        for (Slot slot : handler.slots) {
            ItemStack stack = slot.getStack();
            if (stack == null) continue;
            if (!stack.isEmpty() && stack.getDisplayName().startsWith(name))
                return stack;
        }
        for (Slot slot : handler.slots) {
            ItemStack stack = slot.getStack();
            if (!stack.isEmpty() && stack.getDisplayName().contains(name))
                return stack;
        }
        return null;
    }

    /* ------------------------------------------------------------------ */
    /*  static helpers                                                    */
    /* ------------------------------------------------------------------ */

    public static boolean isBookmarked(String name) {
        return findMatchingBookmark(name) != null;
    }

    public static Bookmark findMatchingBookmark(String name) {
        for (Bookmark bm : BUConfig.get().bookmarks)
            if (bm.getName().equalsIgnoreCase(name))
                return bm;
        return null;
    }

    /**
     * Build all widget instances that should be drawn on the current GUI.
     */
    public static List<ItemSlotButtonWidget> getWidgets() {
        List<ItemSlotButtonWidget> widgets = new ArrayList<>();

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.currentScreen == null) return widgets;

        String title = mc.currentScreen.getTitle().getString();
        if (title == null || !title.startsWith("Bazaar")) return widgets;

        if (!(mc.currentScreen instanceof AccessorHandledScreen screen))
            return widgets;

        /* Safe area inside the vanilla container texture */
        ItemSlotButtonWidget.ScreenWidgetDimensions dims =
                ItemSlotButtonWidget.getSafeScreenDimensions(screen, title);

        int size     = 18;
        int spacing  = 4;
        int x        = dims.x() + dims.backgroundWidth() + spacing;
        int y        = dims.y() + spacing;

        for (Bookmark bm : BUConfig.get().bookmarks) {
            ItemStack icon = (bm.getBookmarkedItem() == null || bm.getBookmarkedItem().isEmpty())
                    ? new ItemStack(Items.BARRIER)
                    : bm.getBookmarkedItem();

            ItemSlotButtonWidget btn = new ItemSlotButtonWidget(
                    x, y, size, size,
                    SLOT_BUTTON_TEXTURES,
                    b -> {
                        if (Screen.hasShiftDown()) {
                            Util.notifyAll("Removed " + bm.getName() +
                                           " bookmark (shift-click). " +
                                           "Open Bazaar again to refresh.");
                            bm.onWidgetShiftClick();
                        } else {
                            bm.onWidgetLeftClick();
                        }
                    },
                    icon,
                    Text.of(bm.getName()));

            widgets.add(btn);
            y += size + spacing;
        }

        return widgets;
    }
}
