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
 * Small “settings cog” shown on every Bazaar GUI.
 * Forge 1.8.9 rewrite – no Fabric classes.
 */
public final class BazaarSettingsButton {

    /* ------------------------------------------------------------------ */
    /*  texture locations (16 × 16 PNGs in assets/…/textures/widget/)     */
    /* ------------------------------------------------------------------ */
    private static final ResourceLocation BASE  =
            new ResourceLocation(BazaarUtils.MODID, "textures/widget/widget_settings_base.png");
    private static final ResourceLocation HOVER =
            new ResourceLocation(BazaarUtils.MODID, "textures/widget/widget_settings_hover.png");

    /* ------------------------------------------------------------------ */
    /*  Public helper – returns a list with at most one widget            */
    /* ------------------------------------------------------------------ */
    public static List<ItemSlotButtonWidget> getWidget() {

        Minecraft mc = Minecraft.getMinecraft();

        /* Only render while the player is in *any* Bazaar screen */
        if (!GUIUtils.inBazaar())            return Collections.emptyList();
        if (mc.currentScreen == null)        return Collections.emptyList();

        /* Need handled-screen access for safe co-ordinates */
        if (!(mc.currentScreen instanceof AccessorHandledScreen))
            return Collections.emptyList();
        AccessorHandledScreen screen = (AccessorHandledScreen) mc.currentScreen;

        /* ------------------------------------------------------------------
           Determine safe position – to the left of the vanilla container
           ------------------------------------------------------------------ */
        ItemSlotButtonWidget.ScreenWidgetDimensions dims =
                ItemSlotButtonWidget.getSafeScreenDimensions(
                        screen,
                        GUIUtils.getContainerName());

        final int SIZE    = 18;          // 18×18 px button
        final int PADDING = 4;
        final int x       = dims.x() - SIZE - PADDING;
        final int y       = dims.y() + PADDING;

        /* ------------------------------------------------------------------
           Build the widget (simple textured button with tooltip)
           ------------------------------------------------------------------ */
        ItemSlotButtonWidget btn = new ItemSlotButtonWidget(
                x, y, SIZE, SIZE,
                BASE, HOVER,
                b -> mc.displayGuiScreen(
                        BUConfig.get().createGUI(mc.currentScreen)),
                null,                     // icon slot (unused for cog)
                "Bazaar-Utils Settings"); // tooltip

        List<ItemSlotButtonWidget> out = new ArrayList<>(1);
        out.add(btn);
        return out;
    }

    /* Prevent instantiation */
    private BazaarSettingsButton() {}
}
