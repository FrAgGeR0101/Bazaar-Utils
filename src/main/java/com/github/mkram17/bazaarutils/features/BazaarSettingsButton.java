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

    /* widget textures (stored in assets/.../textures/widget/) */
    private static final Identifier BASE  =
            Identifier.tryParse(BazaarUtils.MODID, "widget/widget_settings_base");
    private static final Identifier HOVER =
            Identifier.tryParse(BazaarUtils.MODID, "widget/widget_settings_hover");

    public static final ButtonTextures SLOT_BUTTON_TEXTURES =
            new ButtonTextures(BASE, HOVER);

    /**
     * Build the widget for the current GUI, or an empty list when the
     * player is not inside a Bazaar container.
     */
    public static List<ItemSlotButtonWidget> getWidget() {
        MinecraftClient mc = MinecraftClient.getInstance();

        /* Only draw on Bazaar screens */
        if (!BazaarUtils.gui.inBazaar()) return Collections.emptyList();

        /* The current screen must be a handled screen */
        if (!(mc.currentScreen instanceof AccessorHandledScreen screen))
            return Collections.emptyList();

        /* Safe area of the vanilla container texture */
        String title = mc.currentScreen.getTitle().getString();
        ItemSlotButtonWidget.ScreenWidgetDimensions dims =
                ItemSlotButtonWidget.getSafeScreenDimensions(screen, title);

        int size    = 18;
        int spacing = 4;
        int x       = dims.x() - size - spacing;        // left of container
        int y       = dims.y() + spacing;               // top-aligned

        ItemSlotButtonWidget btn = new ItemSlotButtonWidget(
                x, y, size, size,
                SLOT_BUTTON_TEXTURES,
                b -> mc.setScreen(BUConfig.get().createGUI(mc.currentScreen)),
                null,
                Text.literal("Bazaar-Utils Settings")
        );

        List<ItemSlotButtonWidget> list = new ArrayList<>(1);
        list.add(btn);
        return list;
    }
}
