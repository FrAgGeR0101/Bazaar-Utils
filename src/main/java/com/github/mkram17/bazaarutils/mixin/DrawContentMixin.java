package com.github.mkram17.bazaarutils.mixin;

import com.github.mkram17.bazaarutils.BazaarUtils;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

//used to change stack size String
@Mixin(DrawContext.class)
public abstract class DrawContentMixin {
//? if >= 1.21.4 {
    @ModifyVariable(
            method = "drawStackCount",
            at = @At("HEAD"),
            ordinal = 0
    )
    private String modifyStackCountString(String text, TextRenderer textRenderer, ItemStack stack, int x, int y) {
        String customData = stack.get(BazaarUtils.CUSTOM_SIZE_COMPONENT);
        double dataSize;
        if (customData != null) {
            boolean hasNumber = customData.matches(".*\\d.*");

            if(hasNumber)
                dataSize = Double.parseDouble(customData);
            else
                return customData;

            if(dataSize >= 1_000_000)
                return (((int) dataSize) / 1_000_000) + "m";
            if(dataSize >= 1_000)
                return (((int) dataSize) / 1_000) + "k";
            return customData;
        }

        return text;
    }
    //?} else {
        /*@ModifyVariable(
                method = "drawItemInSlot(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/item/ItemStack;IILjava/lang/String;)V",
                at = @At(
                        value = "INVOKE",
                        target = "Lnet/minecraft/client/font/TextRenderer;getWidth(Ljava/lang/String;)I"
                ),
                ordinal = 1
        )
        private String modifyStackCountString(String originalString, TextRenderer textRenderer, ItemStack stack) {
            if (!stack.getComponents().contains(BazaarUtils.CUSTOM_SIZE_COMPONENT))
                return originalString;
            String customData = stack.get(BazaarUtils.CUSTOM_SIZE_COMPONENT);

            if (customData != null) {
                try {
                    double dataSize = Double.parseDouble(customData);

                    if (dataSize >= 1_000_000) {
                        return ((int) (dataSize / 1_000_000)) + "m";
                    }
                    if (dataSize >= 1_000) {
                        return ((int) (dataSize / 1_000)) + "k";
                    }

                    return customData;
                } catch (NumberFormatException e) {
                    return customData;
                }
            }
            return originalString;
        }
    *///?}

}