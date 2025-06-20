package com.github.mkram17.bazaarutils.mixin;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Re-writes the stack-count string that {@link RenderItem} overlays on an item
 * icon.  Works on vanilla/Forge 1.8.9 – no Fabric classes.
 *
 * <p>The mod stores an optional custom size in the item’s NBT tag
 * {@code bu_size}.  If present we abbreviate it (<code>1 234 → 1k</code>,
 * <code>3 400 000 → 3m</code>) or pass the raw string through when it is not
 * numeric (“★”, “⃠ ”, etc.).</p>
 */
@Mixin(RenderItem.class)
public abstract class DrawContentMixin {

    /**
     * Intercept the local&nbsp;{@code String s} just after it is assigned in
     * {@code RenderItem.renderItemOverlayIntoGUI}.
     */
    @ModifyVariable(
            method = "renderItemOverlayIntoGUI"
                    + "(Lnet/minecraft/client/gui/FontRenderer;"
                    + "Lnet/minecraft/item/ItemStack;II)V",
            at = @At(value = "STORE", ordinal = 0), // first assignment of the string
            index = 4                                // local slot of the String "s"
    )
    private String bazaarutils$customStackSize(String original,
                                               FontRenderer fr,
                                               ItemStack stack,
                                               int x, int y) {

        /* nothing special stored → keep vanilla string */
        if (!stack.hasTagCompound() ||
            !stack.getTagCompound().hasKey("bu_size"))
            return original;

        String tag = stack.getTagCompound().getString("bu_size");

        /* non-numeric tags (“★”, “⃠”, “ANY” …) are rendered as-is */
        try {
            double v = Double.parseDouble(tag);

            if (v >= 1_000_000) return (int) (v / 1_000_000) + "m";
            if (v >= 1_000)     return (int) (v / 1_000)     + "k";
            return tag;                               // small numbers – full value
        } catch (NumberFormatException ignore) {
            return tag;                               // symbolic text
        }
    }
}
