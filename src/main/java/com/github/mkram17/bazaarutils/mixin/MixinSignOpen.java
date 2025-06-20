package com.github.mkram17.bazaarutils.mixin;

import com.github.mkram17.bazaarutils.BazaarUtils;
import com.github.mkram17.bazaarutils.events.SignOpenEvent;
import net.minecraft.client.gui.GuiEditSign;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fires a {@link SignOpenEvent} whenever the vanilla sign-editing GUI
 * ({@link GuiEditSign}) is opened.  Uses 1.8.9 classes only.
 */
@Mixin(GuiScreen.class)
public abstract class MixinSignOpen {

    /** `GuiScreen.initGui()` is called once when the screen opens. */
    @Inject(method = "initGui", at = @At("HEAD"))
    private void bazaarutils$onInitGui(CallbackInfo ci) {
        if ((Object) this instanceof GuiEditSign sign) {
            BazaarUtils.EVENT_BUS.post(new SignOpenEvent(sign));
        }
    }
}
