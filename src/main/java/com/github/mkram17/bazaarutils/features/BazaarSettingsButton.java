package com.github.mkram17.bazaarutils.features;

import com.github.mkram17.bazaarutils.BazaarUtils;
import com.github.mkram17.bazaarutils.config.BUConfig;
import com.github.mkram17.bazaarutils.misc.ItemSlotButtonWidget;
import com.github.mkram17.bazaarutils.mixin.AccessorHandledScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Small “cog” button that opens the Bazaar-Utils config screen from any
 * Bazaar GUI.
 */
public class BazaarSettingsButton {

    /* ------------------------------------------------------------------ */
    /*  widget textures                                                   */
    /* ------------------------------------------------------------------ */
    private static final Identifier BASE  =
            Identifier.tryParse(BazaarUtils.MODID, "widget/widget_settings_base");
    private static final Identifier HOVER =
            Identifier.tryParse(BazaarUtils.MODID, "widget/widget_settings_hover");

    public static final ButtonTextures SLOT_BUTTON_TEXTURES =
            new ButtonTextures(BASE, HOVER);

    /* ------------------------------------------------------------------ */
    /*  public API                                                        */
    /* ------------------------------------------------------------------ */

    /**
     * Build the widget(s) for the *current* GUI.  
     * Returns an empty list when the player is not inside a Bazaar screen.
     */
    public static List<ItemSlotButtonWidget> getWidget() {
        MinecraftClient mc = MinecraftClient.getInstance();

        /* Only draw on Bazaar containers */
        if (!BazaarUtils.gui.inBazaar()) return Collections.emptyList();
        if (mc.currentScreen == null)    return Collections.emptyList();

        /* Classic instanceof + cast (works on every Java 17/21 tool-chain) */
        if (!(mc.currentScreen instanceof AccessorHandledScreen))
            return Collections.emptyList();
        AccessorHandledScreen screen = (AccessorHandledScreen) mc.currentScreen;

        /* Safe drawing area inside the vanilla container texture */
        String title = mc.currentScreen.getTitle().getString();
        ItemSlotButtonWidget.ScreenWidgetDimensions dims =
                ItemSlotButtonWidget.getSafeScreenDimensions(screen, title);

        final int size    = 18;
        final int spacing = 4;
        final int x       = dims.x() - size - spacing;   // place to the *left* of the container
        final int y       = dims.y() + spacing;          // aligned with top

        ItemSlotButtonWidget cog = new ItemSlotButtonWidget(
                x, y, size, size,
                SLOT_BUTTON_TEXTURES,
                b -> mc.setScreen(BUConfig.get().createGUI(mc.currentScreen)),
                /* icon stack */ null,
                Text.literal("Bazaar-Utils Settings"));

        List<ItemSlotButtonWidget> out = new ArrayList<>(1);
        out.add(cog);
        return out;
    }
}
