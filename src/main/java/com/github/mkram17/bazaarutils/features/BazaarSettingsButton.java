package com.github.mkram17.bazaarutils.features;

import com.github.mkram17.bazaarutils.BazaarUtils;
import com.github.mkram17.bazaarutils.config.BUConfig;
import com.github.mkram17.bazaarutils.misc.ItemSlotButtonWidget;
import com.github.mkram17.bazaarutils.mixin.AccessorHandledScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A tiny 18 × 18 cog button that opens the Bazaar-Utils settings GUI.
 * <p>Forge 1.8.9-only – no Fabric/YACL classes.</p>
 */
public final class BazaarSettingsButton {

    /* ───────────────────────── textures ───────────────────────── */
    private static final ResourceLocation TEX_BASE =
            new ResourceLocation(BazaarUtils.MODID,
                    "textures/widget/widget_settings_base.png");
    private static final ResourceLocation TEX_HOVER =
            new ResourceLocation(BazaarUtils.MODID,
                    "textures/widget/widget_settings_hover.png");

    /* ───────────────────────── public builder ──────────────────── */
    public static List<ItemSlotButtonWidget> getWidget() {

        Minecraft mc = Minecraft.getMinecraft();

        // 1) only while *any* Bazaar screen is open
        if (!BazaarUtils.GUI.inBazaar())        return Collections.emptyList();
        if (mc.currentScreen == null)           return Collections.emptyList();

        // 2) we need a handled-screen (for safe co-ordinates)
        if (!(mc.currentScreen instanceof AccessorHandledScreen))
            return Collections.emptyList();

        /* ----------------------------------------------------------
           Safe drawing area inside the vanilla container
           ---------------------------------------------------------- */
        GuiContainer container = (GuiContainer) mc.currentScreen;
        ItemSlotButtonWidget.ScreenWidgetDimensions dims =
                ItemSlotButtonWidget.getSafeScreenDimensions(container);

        final int SIZE    = 18;      // square 18-pixel button
        final int PAD     = 4;
        final int x       = dims.x - SIZE - PAD;   // to the *left* of BG
        final int y       = dims.y + PAD;          // align to top

        /* ----------------------------------------------------------
           Build the cog-button with two textures
           ---------------------------------------------------------- */
        ItemSlotButtonWidget cog = ItemSlotButtonWidget.textured(
                x, y, SIZE,
                TEX_BASE, TEX_HOVER,
                () -> mc.displayGuiScreen(
                        BUConfig.get().createGUI(mc.currentScreen)),
                "Bazaar-Utils Settings");

        List<ItemSlotButtonWidget> out = new ArrayList<>(1);
        out.add(cog);
        return out;
    }

    /* prevent instantiation */
    private BazaarSettingsButton() {}
}
