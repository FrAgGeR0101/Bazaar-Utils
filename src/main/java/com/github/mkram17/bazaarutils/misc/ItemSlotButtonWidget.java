package com.github.mkram17.bazaarutils.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;

/**
 * Small square button that renders an ItemStack and runs a callback on click.
 * Pure Forge 1.8.9 – no Fabric / Lombok / modern APIs required.
 */
public final class ItemSlotButtonWidget extends GuiButton {

    /* ------------------------------------------------------------------ */
    /*  Functional callback (Forge 1.8.9 has no built-in functional types)*/
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
    public ItemSlotButtonWidget(int x, int y, int size,
                                ItemStack icon,
                                PressAction onPress) {

        /* GuiButton(id,x,y,width,height,text) – we never need an id       */
        super(-1, x, y, size, size, "");
        this.icon    = icon == null ? null : icon.copy();
        this.onPress = onPress;
    }

    /* ------------------------------------------------------------------ */
    /*  Drawing                                                           */
    /* ------------------------------------------------------------------ */
    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!visible) return;

        /* simple semi-transparent white slot background                   */
        drawRect(xPosition, yPosition,
                 xPosition + width, yPosition + height,
                 0x80FFFFFF);

        /* render the item icon if present                                 */
        if (icon != null && icon.stackSize > 0) {
            RenderHelper.enableGUIStandardItemLighting();
            RenderItem ri = mc.getRenderItem();
            int ix = xPosition + (width  - 16) / 2;
            int iy = yPosition + (height - 16) / 2;
            ri.renderItemAndEffectIntoGUI(icon, ix, iy);
            RenderHelper.disableStandardItemLighting();
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Mouse interaction                                                 */
    /* ------------------------------------------------------------------ */
    @Override
    public boolean mousePressed(Minecraft mc, int mx, int my) {
        boolean inside = super.mousePressed(mc, mx, my);
        if (inside && enabled && onPress != null) onPress.onPress(this);
        return inside;
    }

    /* ------------------------------------------------------------------ */
    /*  Helper ­– safe co-ordinates inside a GuiContainer                 */
    /* ------------------------------------------------------------------ */
    public static final class ScreenWidgetDimensions {
        public final int x, y, backgroundWidth;
        public ScreenWidgetDimensions(int x, int y, int bw) {
            this.x = x; this.y = y; this.backgroundWidth = bw;
        }
    }

    /**
     * Reflectively obtain {@code guiLeft}, {@code guiTop} and {@code xSize}
     * from any {@link GuiContainer}.  Works on obfuscated 1.8.9 jars as well
     * (fallback SRG names are provided).
     */
    public static ScreenWidgetDimensions getSafeScreenDimensions(GuiContainer gui) {

        try {
            int left =  (Integer) getField(gui, "guiLeft",  "field_147003_i");
            int top  =  (Integer) getField(gui, "guiTop",   "field_147009_r");
            int size =  (Integer) getField(gui, "xSize",    "field_146999_f");

            return new ScreenWidgetDimensions(left, top, size);
        } catch (Exception e) {
            /* fallback: standard 176×166 vanilla container                */
            return new ScreenWidgetDimensions( (gui.width  - 176) / 2,
                                               (gui.height - 166) / 2,
                                               176);
        }
    }

    /* reflect helper with unobfuscated + SRG fallback names               */
    private static Object getField(Object obj, String mcp, String srg) throws Exception {
        try {
            java.lang.reflect.Field f = obj.getClass().getField(mcp);
            return f.get(obj);
        } catch (NoSuchFieldException e) {
            java.lang.reflect.Field f = obj.getClass().getField(srg);
            return f.get(obj);
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Simple accessor                                                   */
    /* ------------------------------------------------------------------ */
    public ItemStack getIcon() { return icon; }
}
