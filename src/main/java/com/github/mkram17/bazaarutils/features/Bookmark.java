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
import java.util.List;

/** Bookmark button rendered on the right-hand side of the Bazaar GUI. */
public class Bookmark extends CustomItemButton {

    /* ────────────────────────────── constants ────────────────────────────── */
    private static final int SIGN_SLOT_NUMBER = 45;

    private static final Identifier BASE  =
            Identifier.tryParse(BazaarUtils.MODID, "widget/widget_bookmark_base");
    private static final Identifier HOVER =
            Identifier.tryParse(BazaarUtils.MODID, "widget/widget_bookmark_hover");

    public static final ButtonTextures SLOT_BUTTON_TEXTURES =
            new ButtonTextures(BASE, HOVER);

    /* ─────────────────────────── per-instance data ───────────────────────── */
    @Getter @Setter private String    name;
    @Getter @Setter private ItemStack bookmarkedItem;
    private             boolean       inCorrectGui = false;

    /* ────────────────────────────── life-cycle ───────────────────────────── */
    public Bookmark(String name, ItemStack bookmarkedItem) {
        this.name           = name;
        this.slotNumber     = 0;
        this.bookmarkedItem = bookmarkedItem;

        changeVisuals(isBookmarked(name));
        replacementItem.set(BazaarUtils.CUSTOM_SIZE_COMPONENT, "★");

        inCorrectGui = true;
        BazaarUtils.eventBus.subscribe(this);
    }

    @EventHandler
    private void onGuiReady(ChestLoadedEvent e) {
        BazaarUtils.eventBus.unsubscribe(this);           // one-shot subscription
    }

    /* ───────────────────────── Orbit event handlers ──────────────────────── */
    @EventHandler
    private void onReplaceItem(ReplaceItemEvent e) {
        if (!inCorrectGui || !shouldReplaceItem(e)) return;

        if (replacementItem == null)
            changeVisuals(isBookmarked(name));

        e.setReplacement(replacementItem);
    }

    @EventHandler
    private void onSlotClick(SlotClickEvent e) {
        if (!inCorrectGui || !shouldUseSlot(e)) return;

        SoundUtil.playSound(BUTTON_SOUND, BUTTON_VOLUME);
        toggleBookmark();
        bookmarkedItem = findMatchingStack(name, e);
        BUConfig.HANDLER.save();
    }

    /* ───────────────────────────── widget actions ────────────────────────── */
    public void onWidgetLeftClick() {
        SoundUtil.playSound(BUTTON_SOUND, BUTTON_VOLUME);

        ModCompatibilityHelper.tryDisableSkyblockerBazaarOverlay();
        GUIUtils.clickSlot(SIGN_SLOT_NUMBER, 0);
        GUIUtils.setSignText(name, true);
        Util.tickExecuteLater(4, ModCompatibilityHelper::tryEnableSkyblockerBazaarOverlay);
    }

    public void alternateOnWidgetLeftClick() {
        GUIUtils.closeHandledScreen();
        Util.sendCommand("bz " + name);
    }

    public void onWidgetShiftClick() {
        BUConfig.get().bookmarks.remove(this);
        BUConfig.HANDLER.save();
    }

    /* ───────────────────────── bookmark helpers ──────────────────────────── */
    private void toggleBookmark() {
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
            replacementItem = new ItemStack(Items.GREEN_STAINED_GLASS_PANE);
            replacementItem.setStackDisplayName("Remove " + name + " Bookmark");
            replacementItem.set(BazaarUtils.CUSTOM_SIZE_COMPONENT, "⃠ ");
        } else {
            replacementItem = new ItemStack(Items.RED_STAINED_GLASS_PANE);
            replacementItem.setStackDisplayName("Bookmark " + name);
            replacementItem.set(BazaarUtils.CUSTOM_SIZE_COMPONENT, "★");
        }
    }

    /* ─────────────────────── find item / name helpers ───────────────────── */
    private static ItemStack findMatchingStack(String wanted, SlotClickEvent ev) {
        ScreenHandler h = ev.handledScreen.getScreenHandler();

        // 1 × exact prefix
        for (Slot s : h.slots) {
            ItemStack st = s.getStack();
            if (!st.isEmpty() && st.getDisplayName().startsWith(wanted))
                return st;
        }
        // 2 × contains
        for (Slot s : h.slots) {
            ItemStack st = s.getStack();
            if (!st.isEmpty() && st.getDisplayName().contains(wanted))
                return st;
        }
        return ItemStack.EMPTY;
    }

    /* ─────────────────────────── static helpers ─────────────────────────── */
    public static boolean isBookmarked(String n) { return findMatchingBookmark(n) != null; }

    public static Bookmark findMatchingBookmark(String n) {
        for (Bookmark b : BUConfig.get().bookmarks)
            if (b.getName().equalsIgnoreCase(n)) return b;
        return null;
    }

    /** Build widgets for the *current* GUI state. */
    public static List<ItemSlotButtonWidget> getWidgets() {
        List<ItemSlotButtonWidget> out = new ArrayList<>();

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.currentScreen == null) return out;
        if (!mc.currentScreen.getTitle().getString().startsWith("Bazaar")) return out;

        /* classic cast – no pattern-matching → always in scope */
        if (!(mc.currentScreen instanceof AccessorHandledScreen))
            return out;
        AccessorHandledScreen screen = (AccessorHandledScreen) mc.currentScreen;

        var dims = ItemSlotButtonWidget.getSafeScreenDimensions(
                screen, mc.currentScreen.getTitle().getString());

        final int btn = 18, pad = 4;
        int x = dims.x() + dims.backgroundWidth() + pad;
        int y = dims.y() + pad;

        for (Bookmark bm : BUConfig.get().bookmarks) {
            ItemStack icon = bm.getBookmarkedItem();
            if (icon == null || icon.isEmpty()) icon = new ItemStack(Items.BARRIER);

            ItemSlotButtonWidget w = new ItemSlotButtonWidget(
                    x, y, btn, btn,
                    SLOT_BUTTON_TEXTURES,
                    b -> {
                        if (Screen.hasShiftDown()) {
                            Util.notifyAll("Removed " + bm.getName() + " bookmark (shift-click)", Util.notificationTypes.GUI);
                            bm.onWidgetShiftClick();
                        } else {
                            bm.onWidgetLeftClick();
                        }
                    },
                    icon,
                    Text.of(bm.getName()));

            out.add(w);
            y += btn + pad;
        }
        return out;
    }
}
