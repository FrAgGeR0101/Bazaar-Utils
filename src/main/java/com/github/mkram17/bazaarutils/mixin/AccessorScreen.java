package com.github.mkram17.bazaarutils.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the protected {@code mc} field of {@link GuiScreen}.
 * Pure-vanilla (Forge 1.8.9) – no Fabric classes here.
 */
@Mixin(GuiScreen.class)
public interface AccessorGuiScreen {

    /* field name is exactly “mc” in 1.8.9 */
    @Accessor("mc")
    Minecraft getMinecraft();
}
