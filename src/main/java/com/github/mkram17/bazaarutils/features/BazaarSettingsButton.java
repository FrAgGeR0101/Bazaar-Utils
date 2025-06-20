package com.github.mkram17.bazaarutils.features;

import com.github.mkram17.bazaarutils.BazaarUtils;
import com.github.mkram17.bazaarutils.config.BUConfig;
import com.github.mkram17.bazaarutils.misc.ItemSlotButtonWidget;
import com.github.mkram17.bazaarutils.mixin.AccessorHandledScreen;
import com.github.mkram17.bazaarutils.utils.GUIUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Small cog-shaped button shown on every Bazaar GUI.
 * Pure Forge-1.8.9 implementation (no Fabric classes).
 */
public final class BazaarSettingsButton {

    /* ------------------------------------------------------------------ */
    /*  Widget textures (assets/<modid>/textures/widget/ … )              */
    /* ------------------------------------------------------------------ */
    private static final ResourceLocation BASE  =
            new ResourceLocation(BazaarUtils.MODID,
                                 "textures/widget/widget_settings_base.png");
    private static final ResourceLocation HOVER =
            new ResourceLocation(BazaarUtils.MODID,
                                 "textures/widget/widget_settings_hover.png");

    /* ------------------------------------------------------------------ */
    /*  Build button(s) for the *current* GUI                             */
    /* ------------------------------------------------------------------ */
    public static List<ItemSlotButtonWidget> getWidget() {

        Minecraft mc = Minecraft.getMinecraft();

        /* Only draw while inside *any* Bazaar screen */
        if (!BazaarUtils.GUI.inBazaar())   return Collections.emptyList();
        if (mc.currentScreen == null)      return Collections.emptyList();

        /* Need access to HandledScreen internals */
        if (!(mc.currentScreen instanceof AccessorHandledScreen))
            return Collections.emptyList();
        AccessorHandledScreen screen = (AccessorHandledScreen) mc.currentScreen;

        /* Safe drawing-area co-ordinates inside the vanilla container */
        ItemSlotButtonWidget.ScreenWidgetDimensions dims =
                ItemSlotButtonWidget.getSafeScreenDimensions(
                        screen,
                        GUIUtils.containerTitle());

        final int SIZE    = 18;        // 18×18 pixel button
        final int PADDING = 4;
        final int x       = dims.x - SIZE - PADDING;   // left of container
        final int y       = dims.y + PADDING;          // top-aligned

        /* Build the textured cog button */
        ItemSlotButtonWidget cog = new ItemSlotButtonWidget(
                x, y, SIZE, SIZE,
                BASE, HOVER,
                () -> mc.displayGuiScreen(
                        BUConfig.get().createGUI(mc.currentScreen)),
                "Bazaar-Utils Settings");

        List<ItemSlotButtonWidget> out = new ArrayList<>(1);
        out.add(cog);
        return out;
    }

    /* Prevent instantiation */
    private BazaarSettingsButton() {}
}
