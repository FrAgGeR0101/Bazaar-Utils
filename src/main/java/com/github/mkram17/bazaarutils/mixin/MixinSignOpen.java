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
 * Emits a {@link SignOpenEvent} whenever the vanilla sign-editing GUI
 * ({@link GuiEditSign}) is opened.  
 * <p>Plain 1.8.9 classes only – no Java 17 pattern matching.</p>
 */
@Mixin(GuiScreen.class)
public abstract class MixinSignOpen {

    /** Called when any {@link GuiScreen} is first initialised. */
    @Inject(method = "initGui", at = @At("HEAD"))
    private void bazaarutils$onInitGui(CallbackInfo ci) {
        if ((Object) this instanceof GuiEditSign) {
            GuiEditSign sign = (GuiEditSign) (Object) this;   // classic cast for 1.8.9
            BazaarUtils.EVENT_BUS.post(new SignOpenEvent(sign));
        }
    }
}
