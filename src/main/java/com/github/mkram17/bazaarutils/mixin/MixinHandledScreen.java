// MixinHandledScreen.java
package com.github.mkram17.bazaarutils.mixin;

import com.github.mkram17.bazaarutils.BazaarUtils;
import com.github.mkram17.bazaarutils.config.BUConfig;
import com.github.mkram17.bazaarutils.events.SlotClickEvent;
import com.github.mkram17.bazaarutils.features.StashHelper;
import com.github.mkram17.bazaarutils.features.restrictsell.RestrictSell;
import com.github.mkram17.bazaarutils.misc.ItemSlotButtonWidget;
import com.github.mkram17.bazaarutils.misc.ModCompatibilityHelper;
import com.github.mkram17.bazaarutils.utils.Util;
import com.moulberry.mixinconstraints.annotations.IfModLoaded;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//used for SlotClickEvent, register keybinds in chests, block slot clicks
@Mixin(value = HandledScreen.class, priority = 999)
public abstract class MixinHandledScreen<T extends ScreenHandler> extends Screen {


	protected MixinHandledScreen(Text title) {
		super(title);
	}

	@Inject(method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V",at = @At("HEAD"),cancellable = true)
	private void onHandleMouseClick(Slot slot, int slotId, int button, SlotActionType actionType, CallbackInfo ci) {
		if (slot == null) return;

		//for insta sell rules
		RestrictSell sell = BUConfig.get().restrictSell;
		if (sell.isSlotLocked(slotId)) {
			if (sell.getSafetyClicks() < 3) {
				sell.addSafetyClick();
				Util.notifyAll(sell.getMessage());
				ci.cancel();
			} else {
				sell.resetSafetyClicks();
			}
		}

		HandledScreen<?> screen = (HandledScreen<?>) (Object) this;
		SlotClickEvent event = new SlotClickEvent(screen, slot, slotId, button, actionType);
		BazaarUtils.eventBus.post(event);
//		Util.notifyAll("Mouse Click Posted");
// Use the accessor to safely get the client instance
		MinecraftClient client = ((AccessorScreen) screen).getClient();
		if (event.isCancelled()) {
			ci.cancel();
			return;
		}

		if (event.usePickblockInstead) {
			assert client != null && client.player != null;
			client.interactionManager.clickSlot(
					screen.getScreenHandler().syncId,
					slotId,
					2,
					SlotActionType.PICKUP,
					client.player
			);
			ci.cancel();
		}
	}

	@IfModLoaded(ModCompatibilityHelper.AMECS_MODID)
	@Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
	public void onkeyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
		StashHelper keyBinding = (StashHelper) BazaarUtils.keybinds.getFirst();
		if (!keyBinding.isPressed() && keyBinding.getDefaultKey().getCode() == keyCode && keyBinding.getDefaultModifiers().getAlt()) {
			Util.notifyAll("Stash helper pressed", Util.notificationTypes.FEATURE);
			if(keyBinding.getTicksBetweenPresses() > 10)
				keyBinding.setPressed(true);
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void bazaarutils$addConfiguredButtons(CallbackInfo ci) {


		int buttonsAdded = 0;
		for (ItemSlotButtonWidget button : BUConfig.getWidgets()) {
			this.addDrawableChild(button);
			buttonsAdded++;
		}
	}

}