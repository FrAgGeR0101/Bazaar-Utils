package com.github.mkram17.bazaarutils.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;

/**
 * A small square GUI-button that shows an {@link ItemStack} and calls a
 * “press action” when clicked.  
 * Completely self-contained – no Fabric, no Lombok, no modern classes.
 */
public final class ItemSlotButtonWidget extends GuiButton {

    /* ------------------------------------------------------------------ */
    /*  Callback interface (Forge 1.8.9 has no built-in functional type)  */
    /* ------------------------------------------------------------------ */
    public interface PressAction { void onPress(ItemSlotButtonWidget btn); }

    /* ------------------------------------------------------------------ */
    /*  Immutable runtime data                                            */
    /* ------------------------------------------------------------------ */
    private final ItemStack  icon;
    private final PressAction onPress;

    /* ------------------------------------------------------------------ */
    /*  Construction                                                      */
    /* ------------------------------------------------------------------ */
    public ItemSlotButtonWidget(int x, int y,
                                int size,
                                ItemStack icon,
                                PressAction onPress) {

        /* Forge-1.8.9 GuiButton ctor: id, x, y, width, height, text       */
        super(-1, x, y, size, size, "");
        this.icon    = icon.copy();
        this.onPress = onPress;
    }

    /* ------------------------------------------------------------------ */
    /*  Drawing                                                           */
    /* ------------------------------------------------------------------ */
    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!visible) return;

        // background: simple semi-transparent rectangle
        int colour = 0x80FFFFFF;                       // 50 % white
        drawRect(xPosition, yPosition,
                 xPosition + width, yPosition + height,
                 colour);

        // render the item icon in the centre
        if (!icon.isEmpty()) {
            RenderHelper.enableGUIStandardItemLighting();
            RenderItem renderer = mc.getRenderItem();
            int iconX = xPosition + (width  - 16) / 2;
            int iconY = yPosition + (height - 16) / 2;
            renderer.renderItemAndEffectIntoGUI(icon, iconX, iconY);
            RenderHelper.disableStandardItemLighting();
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Mouse interaction                                                 */
    /* ------------------------------------------------------------------ */
    @Override
    public boolean mousePressed(Minecraft mc, int mx, int my) {
        boolean inside = super.mousePressed(mc, mx, my);
        if (inside && enabled && onPress != null)
            onPress.onPress(this);
        return inside;
    }

    /* ------------------------------------------------------------------ */
    /*  Helper: safe area inside a {@link GuiContainer}                   */
    /* ------------------------------------------------------------------ */
    public static class ScreenWidgetDimensions {
        public final int x, y, backgroundWidth;
        public ScreenWidgetDimensions(int x, int y, int bw) {
            this.x = x; this.y = y; this.backgroundWidth = bw;
        }
    }

    /** Derive <code>guiLeft</code>, <code>guiTop</code> and <code>xSize</code>. */
    public static ScreenWidgetDimensions getSafeScreenDimensions(GuiContainer gui) {
        return new ScreenWidgetDimensions(gui.guiLeft, gui.guiTop, gui.xSize);
    }

    /* ------------------------------------------------------------------ */
    /*  Simple getters                                                    */
    /* ------------------------------------------------------------------ */
    public ItemStack getIcon() { return icon; }
}
