package com.github.mkram17.bazaarutils.utils;

import com.github.mkram17.bazaarutils.BazaarUtils;
import com.github.mkram17.bazaarutils.events.BUListener;
import com.github.mkram17.bazaarutils.events.ChestLoadedEvent;
import com.github.mkram17.bazaarutils.events.SignOpenEvent;
import com.github.mkram17.bazaarutils.features.Bookmark;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiEditSign;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.init.Items;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Vanilla-only GUI helper used throughout Bazaar-Utils.
 * <p>No Fabric, no Lombok, no mixins – compatible with Forge 1.8.9.</p>
 */
public final class GUIUtils implements BUListener {

    /* ───────────────────────────── state ───────────────────────────── */

    private GuiType currentType = GuiType.NONE;

    /** Tracks the lower inventory when a chest GUI opens (for flip-menu check). */
    private Object lowerChestInventory;

    /** The item-list extracted from the last opened chest GUI. */
    private final List<ItemStack> itemStacks = new ArrayList<>();

    /** `true` while the player is inside the “flip” options GUI. */
    private boolean inFlipMenu = false;

    /** Bookmark whose item-button is currently shown (may be <code>null</code>). */
    private Bookmark currentBookmark;

    /* ───────────────────────────── enum ────────────────────────────── */

    private enum GuiType { NONE, CHEST, SIGN }

    /* ───────────────────────────── ctor ───────────────────────────── */

    public GUIUtils() {
        /* listen for game events via the simple Orbit bus */
        BazaarUtils.EVENT_BUS.subscribe(this);
    }

    /* ───────────────────────── BUListener impl. ───────────────────── */

    @Override
    public void subscribe() {
        /* already subscribed in the constructor */
    }

    /* ───────────────────────── chest / sign hooks ─────────────────── */

    /** Called externally whenever a sign GUI opens. */
    public void onSignOpen(SignOpenEvent ev) {
        currentType = GuiType.SIGN;
    }

    /** Called externally whenever a chest GUI finished initialising. */
    public void onChestLoaded(ChestLoadedEvent ev) {
        currentType           = GuiType.CHEST;
        lowerChestInventory   = ev.getLowerChestInventory();
        itemStacks.clear();
        itemStacks.addAll(ev.getItemStacks());

        /* figure out whether this chest is the flip-options menu */
        inFlipMenu = checkFlipMenu();

        /* keep bookmark instance in sync */
        currentBookmark = null;
        if (inBuyOrderScreen() || inInstaBuy() || inAnyItemScreen()) {
            String name = Bookmark.findName(ev);
            currentBookmark = Bookmark.isBookmarked(name)
                    ? Bookmark.findMatchingBookmark(name)
                    : new Bookmark(name, new ItemStack(Items.barrier));
            if (currentBookmark != null) BazaarUtils.EVENT_BUS.subscribe(currentBookmark);
        }
    }

    /* ───────────────────────────── GUI tests ──────────────────────── */

    public static String containerTitle() {
        GuiScreen scr = Minecraft.getMinecraft().currentScreen;
        return (scr != null) ? EnumChatFormatting.getTextWithoutFormattingCodes(scr.getTitle().getFormattedText())
                             : null;
    }

    /* “How many do you want?” (classic buy-order amount screen) */
    public boolean inBuyOrderScreen() {
        String t = containerTitle();
        return t != null && t.contains("How many do you want?");
    }

    /* “➜ Insta...” screen shown right after clicking Insta-Buy */
    public boolean inInstaBuy() {
        String t = containerTitle();
        return t != null && t.contains("➜ Insta");
    }

    /* Co-op orders list */
    public boolean inBuyOrders() {
        String t = containerTitle();
        return t != null && t.contains("Co-op Bazaar Orders");
    }

    /** Any Bazaar-related screen? */
    public boolean inBazaar() {
        String t = containerTitle();
        return t != null && (inBuyOrderScreen() || inFlipMenu || inInstaBuy() ||
                             t.contains("Bazaar") || inBuyOrders() || t.contains("➜"));
    }

    /** Inside any item-specific GUI? */
    public boolean inAnyItemScreen() {
        String t = containerTitle();
        return t != null && !t.contains("Bazaar") &&
               (t.contains("➜") || inBuyOrderScreen() || inInstaBuy());
    }

    /** True while in the flip-options chest GUI. */
    public boolean inFlipGui() { return inFlipMenu; }

    /* ───────────────────────── flip check helper ──────────────────── */

    private boolean checkFlipMenu() {
        if (!(Minecraft.getMinecraft().currentScreen instanceof GuiChest)) return false;
        if (lowerChestInventory == null) return false;
        String title = containerTitle();
        if (title == null || !title.contains("Order options")) return false;

        /* slot 13 is the “Confirm” / “Cancel” glass-pane */
        try {
            // lowerChestInventory = IInventory in vanilla – we avoid the generic type to stay 1.8.9-friendly
            Object inv = lowerChestInventory;
            ItemStack stack = (ItemStack) inv.getClass().getMethod("getStackInSlot", int.class).invoke(inv, 13);

            String name = stack.getDisplayName();
            return !name.contains("Cancel Order");
        } catch (Exception ignored) { }
        return false;
    }

    /* ─────────────────────── slot-click helper ────────────────────── */

    public static void clickSlot(int slot, int mouseButton) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer p = mc.thePlayer;
        if (p == null) return;

        mc.playerController.windowClick(
                p.openContainer.windowId,
                slot,
                mouseButton,
                0,          // 0 = CLICK, 1 = SHIFT, 2 = HOTBAR
                p);
    }

    /* ─────────────────────── sign-text helper ─────────────────────── */

    public static void setSignText(String text, boolean closeAfter) {
        GuiScreen scr = Minecraft.getMinecraft().currentScreen;
        if (!(scr instanceof GuiEditSign)) return;

        GuiEditSign signGui = (GuiEditSign) scr;
        try {
            /* reflect into private fields of GuiEditSign / TileEntitySign */
            Field tileField = GuiEditSign.class.getDeclaredField("tileSign");
            tileField.setAccessible(true);
            Object tileSign = tileField.get(signGui);

            Field linesF = tileSign.getClass().getDeclaredField("signText");
            linesF.setAccessible(true);
            String[] lines = (String[]) linesF.get(tileSign);

            String[] newLines = text.split("\n", 4);
            System.arraycopy(newLines, 0, lines, 0, newLines.length);

            if (closeAfter) Minecraft.getMinecraft().displayGuiScreen(null);
        } catch (Exception e) {
            Util.notifyError("Failed to set sign text", e);
        }
    }

    /* ─────────────────────—— misc public helpers ──────────────────── */

    public Object   getLowerChestInventory()             { return lowerChestInventory; }
    public List<ItemStack> getItemStacks()                { return Collections.unmodifiableList(itemStacks); }
    public Bookmark getCurrentBookmark()                  { return currentBookmark; }
    public GuiType  getCurrentGuiType()                   { return currentType; }

    /* ---------------------------------------------------------------- */
    /*  tiny debug helper                                               */
    /* ---------------------------------------------------------------- */

    private static void log(String s) {
        Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(
                EnumChatFormatting.DARK_GRAY + "[GUIUtils] " + EnumChatFormatting.RESET + s));
    }
}
