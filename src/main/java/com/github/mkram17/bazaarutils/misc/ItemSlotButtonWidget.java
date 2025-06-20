package com.github.mkram17.bazaarutils.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.util.ResourceLocation;
import net.minecraft.item.ItemStack;

/**
 * A tiny 18 × 18 square button that can either:
 * <ul>
 *   <li>render an {@link ItemStack} icon, <b>or</b></li>
 *   <li>show two 16 × 16 textures (normal/hover)</li>
 * </ul>
 *
 * Pure Forge 1.8.9 – no Fabric/YACL/Lombok required.
 */
public final class ItemSlotButtonWidget extends GuiButton {

    /* ───────────────────────── functional helper ───────────────────── */
    public interface PressAction { void onPress(ItemSlotButtonWidget btn); }

    /* ───────────────────────── immutable data ──────────────────────── */
    private final ItemStack        icon;        // optional (may be null)
    private final ResourceLocation texBase;     // optional (may be null)
    private final ResourceLocation texHover;    // optional (may be null)
    private final PressAction      onPress;
    private final String           tooltip;     // unused in core logic

    /* ────────────────────────── constructors ───────────────────────── */

    /** Icon-based variant (original behaviour). */
    public ItemSlotButtonWidget(int x, int y, int size,
                                ItemStack icon,
                                PressAction onPress) {
        this(x, y, size, icon, null, null, onPress, null);
    }

    /** Internal shared ctor, also used by the textured factory. */
    private ItemSlotButtonWidget(int x, int y, int size,
                                 ItemStack icon,
                                 ResourceLocation base,
                                 ResourceLocation hover,
                                 PressAction onPress,
                                 String tooltip) {
        super(-1, x, y, size, size, "");
        this.icon     = icon == null ? null : icon.copy();
        this.texBase  = base;
        this.texHover = hover == null ? base : hover;
        this.onPress  = onPress;
        this.tooltip  = tooltip;
    }

    /* ───────────────────────── factory for textured buttons ────────── */
    public static ItemSlotButtonWidget textured(int x, int y, int size,
                                                ResourceLocation base,
                                                ResourceLocation hover,
                                                Runnable      click,
                                                String        tooltip) {
        return new ItemSlotButtonWidget(
                x, y, size,
                null, base, hover,
                b -> { if (click != null) click.run(); }, tooltip);
    }

    /* ───────────────────────── drawing ─────────────────────────────── */
    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!visible) return;

        /* hover detection via superclass helper                          */
        this.hovered = mouseX >= xPosition && mouseY >= yPosition &&
                       mouseX <  xPosition + width &&
                       mouseY <  yPosition + height;

        /* ----------------------------------------------------------------
           1) TEXTURED variant (cog, arrows, ...)
           ---------------------------------------------------------------- */
        if (texBase != null) {
            mc.getTextureManager().bindTexture(hovered ? texHover : texBase);
            // drawTexturedModalRect(u,v): we reuse GuiButton helper
            drawTexturedModalRect(xPosition, yPosition, 0, 0, width, height);
        }

        /* ----------------------------------------------------------------
           2) ICON variant  (original behaviour)
           ---------------------------------------------------------------- */
        if (icon != null && icon.stackSize > 0) {
            RenderHelper.enableGUIStandardItemLighting();
            RenderItem ri = mc.getRenderItem();
            int ix = xPosition + (width  - 16) / 2;
            int iy = yPosition + (height - 16) / 2;
            ri.renderItemAndEffectIntoGUI(icon, ix, iy);
            RenderHelper.disableStandardItemLighting();
        }
    }

    /* ───────────────────────── mouse click ─────────────────────────── */
    @Override
    public boolean mousePressed(Minecraft mc, int mx, int my) {
        boolean inside = super.mousePressed(mc, mx, my);
        if (inside && enabled && onPress != null) onPress.onPress(this);
        return inside;
    }

    /* ─────────────────── safe-area helper for containers ───────────── */
    public static final class ScreenWidgetDimensions {
        public final int x, y, backgroundWidth;
        public ScreenWidgetDimensions(int x, int y, int bw) {
            this.x = x; this.y = y; this.backgroundWidth = bw;
        }
    }

    /** Reflection helper that works on obfuscated 1.8.9 jars as well. */
    public static ScreenWidgetDimensions getSafeScreenDimensions(GuiContainer gui) {
        try {
            int left =  (Integer) getField(gui, "guiLeft",  "field_147003_i");
            int top  =  (Integer) getField(gui, "guiTop",   "field_147009_r");
            int size =  (Integer) getField(gui, "xSize",    "field_146999_f");
            return new ScreenWidgetDimensions(left, top, size);
        } catch (Exception ignored) {
            // fallback: vanilla 176 × 166 container
            return new ScreenWidgetDimensions((gui.width  - 176) / 2,
                                              (gui.height - 166) / 2,
                                              176);
        }
    }

    private static Object getField(Object o, String mcp, String srg) throws Exception {
        try {
            java.lang.reflect.Field f = o.getClass().getField(mcp);  return f.get(o);
        } catch (NoSuchFieldException e) {
            java.lang.reflect.Field f = o.getClass().getField(srg);  return f.get(o);
        }
    }

    /* ───────────────────────── simple accessor ─────────────────────── */
    public ItemStack getIcon() { return icon; }

    /* tooltip getter (future use) */
    public String getTooltipText() { return tooltip; }
}
