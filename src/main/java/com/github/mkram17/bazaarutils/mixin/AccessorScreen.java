package com.github.mkram17.bazaarutils.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Simple accessor that exposes the protected {@code mc} field of {@link GuiScreen}.
 * Only needed by legacy helpers; no Fabric classes involved.
 */
@Mixin(GuiScreen.class)
public interface AccessorGuiScreen {

    /** Vanilla 1.8.9 field name is {@code mc}. */
    @Accessor("mc")
    Minecraft getMinecraft();
}
