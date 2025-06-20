package com.github.mkram17.bazaarutils.mixin;

import com.github.mkram17.bazaarutils.BazaarUtils;
import com.github.mkram17.bazaarutils.events.ReplaceItemEvent;
import net.minecraft.inventory.InventoryBasic;     // ← vanilla 1.8.9 class
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fires a {@link ReplaceItemEvent} every time an inventory slot is read.
 * Works on Forge 1.8.9 – no Fabric classes required.
 */
@Mixin(InventoryBasic.class)
public abstract class MixinInventoryBasic {

    /** underlying array in vanilla 1.8.9 */
    @Shadow @Final
    private ItemStack[] inventoryContents;

    /**
     * Intercept {@code getStackInSlot(int)}.
     */
    @Inject(method = "getStackInSlot", at = @At("HEAD"), cancellable = true)
    private void onGetStack(int slot, CallbackInfoReturnable<ItemStack> cir) {
        if (slot < 0 || slot >= inventoryContents.length) return;

        ReplaceItemEvent ev =
            new ReplaceItemEvent(inventoryContents[slot],
                                 (InventoryBasic) (Object) this,
                                 slot);

        BazaarUtils.EVENT_BUS.post(ev);

        if (ev.getReplacement() != ev.getOriginal()) {
            cir.setReturnValue(ev.getReplacement());
        }
    }
}
