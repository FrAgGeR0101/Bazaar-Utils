package com.github.mkram17.bazaarutils.features;

import com.github.mkram17.bazaarutils.BazaarUtils;
import com.github.mkram17.bazaarutils.config.BUConfig;
import com.github.mkram17.bazaarutils.events.BUListener;
import com.github.mkram17.bazaarutils.misc.CustomItemButton;          // for the tiny Option-stub
import net.minecraft.client.Minecraft;                               // 1.8.9 class
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

import java.awt.Desktop;
import java.net.URI;

/**
 * Lightweight Forge-1.8.9 “Price-Charts” helper.<br>
 * <br>
 * – Adds a clickable tooltip line to Bazaar items (only while inside the
 *   Bazaar unless the optional config toggle is enabled).<br>
 * – CTRL + SHIFT click on an item opens its skyblock.finance page.<br>
 * <br>
 * All Fabric / YACL / Orbit dependencies have been stripped so the file
 * compiles on a pure Forge 1.8.9 environment.
 */
public final class PriceCharts implements BUListener {

    /* ------------------------------------------------------------------ */
    /*  User-configurable toggle                                          */
    /* ------------------------------------------------------------------ */

    private boolean showOutsideBazaar = false;          // config field

    public boolean isShowOutsideBazaar()        { return showOutsideBazaar; }
    public void    setShowOutsideBazaar(boolean v) { showOutsideBazaar = v; }

    /* ------------------------------------------------------------------ */
    /*  Tooltip + click logic (very small)                                */
    /* ------------------------------------------------------------------ */

    private static final String FINANCE_URL = "https://skyblock.finance/items/";

    /** Insert one extra tooltip line when we are allowed to show it. */
    public void addTooltip(ItemStack stack, java.util.List<String> lines) {
        if (stack == null || stack.isEmpty()) return;
        if (!shouldShow())                    return;

        String productId = BazaarUtils.GUI.getProductIdForStack(stack);
        if (productId == null) return;                    // no Bazaar item

        lines.add(EnumChatFormatting.GOLD + "" + EnumChatFormatting.BOLD +
                  "CTRL+SHIFT-click for price-chart");
        stack.setTagInfo("BU_financeId", new net.minecraft.nbt.NBTTagString(productId));
    }

    /** Called from the generic slot-click hook in {@link com.github.mkram17.bazaarutils.events.SlotClickEvent}. */
    public void onSlotClick(net.minecraft.inventory.Slot slot, boolean ctrlDown, boolean shiftDown) {
        if (!ctrlDown || !shiftDown)                   return;
        if (!shouldShow())                             return;

        ItemStack stack = slot.getStack();
        if (stack == null || stack.isEmpty())          return;
        if (!stack.hasTagCompound())                   return;
        if (!stack.getTagCompound().hasKey("BU_financeId")) return;

        String id = stack.getTagCompound().getString("BU_financeId");
        openInBrowser(FINANCE_URL + id);
    }

    private static void openInBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported())
                Desktop.getDesktop().browse(new URI(url));
        } catch (Exception ignored) { /* best-effort */ }
    }

    private boolean shouldShow() {
        return BazaarUtils.GUI.inBazaar() || showOutsideBazaar;
    }

    /* ------------------------------------------------------------------ */
    /*  Config-GUI helper (uses the stub from CustomItemButton)           */
    /* ------------------------------------------------------------------ */

    public CustomItemButton.Option<Boolean> createOption() {
        return CustomItemButton.Option.<Boolean>builder()
                .name("Price-chart tooltip outside Bazaar")
                .description("Show the CTRL+SHIFT tooltip everywhere, not " +
                             "just inside Bazaar item screens.")
                .binding(false, this::isShowOutsideBazaar, this::setShowOutsideBazaar)
                .build();
    }

    /* ------------------------------------------------------------------ */
    /*  BUListener                                                        */
    /* ------------------------------------------------------------------ */

    @Override
    public void subscribe() {
        // Hook into the central tooltip + click callbacks that already exist
        BazaarUtils.EVENT_BUS.subscribe(this);
    }
}
