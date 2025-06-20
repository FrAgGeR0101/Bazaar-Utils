package com.github.mkram17.bazaarutils.mixin;

import com.github.mkram17.bazaarutils.BazaarUtils;
import com.github.mkram17.bazaarutils.config.BUConfig;
import com.github.mkram17.bazaarutils.events.SlotClickEvent;
import com.github.mkram17.bazaarutils.features.StashHelper;
import com.github.mkram17.bazaarutils.features.restrictsell.RestrictSell;
import com.github.mkram17.bazaarutils.misc.ItemSlotButtonWidget;
import com.github.mkram17.bazaarutils.utils.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Forge 1.8.9 mixin that:
 * <ul>
 *   <li>publishes a {@link SlotClickEvent} for every slot interaction,</li>
 *   <li>enforces {@link RestrictSell} “insta-sell” rules,</li>
 *   <li>passes clicks to {@link StashHelper} when its key-bind is active,</li>
 *   <li>adds Bazaar-Utils overlay buttons once the GUI is initialised.</li>
 * </ul>
 * <p>All references are strictly 1.8.9 classes—no modern Text API,
 * ScreenHandler, SlotActionType, etc.</p>
 */
@Mixin(GuiContainer.class)
public abstract class MixinHandledScreen {

    /* ────────────────────────────────────────────────────────────────
       1)  Emit SlotClickEvent + Restrict-Sell guard
       ─────────────────────────────────────────────────────────────── */
    @Inject(
        method = "handleMouseClick(Lnet/minecraft/inventory/Slot;IILjava/lang/String;)V",
        at     = @At("HEAD"),
        cancellable = true)
    private void bazaarutils$onHandleMouseClick(
            Slot slot, int slotId, int clickedButton, String clickType,
            CallbackInfo ci) {

        if (slot == null) return;

        /* Restrict-Sell ------------------------------------------------ */
        RestrictSell rs = BUConfig.get().restrictSell;
        if (rs != null && rs.isSlotLocked(slotId)) {
            if (rs.getSafetyClicks() < 3) {
                rs.addSafetyClick();
                Util.notifyAll(rs.getMessage());
                ci.cancel();
                return;
            } else {
                rs.resetSafetyClicks();
            }
        }

        /* SlotClickEvent ---------------------------------------------- */
        GuiContainer self = (GuiContainer) (Object) this;
        SlotClickEvent ev = new SlotClickEvent(
                self, slot, slotId, clickedButton, clickType);
        BazaarUtils.EVENT_BUS.post(ev);

        if (ev.isCancelled()) {
            ci.cancel();
            return;
        }

        /* “Pick-block instead” fallback (used by Flip-Helper) */
        if (ev.usePickblockInstead()) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.playerController != null && mc.thePlayer != null) {
                Container c = mc.thePlayer.openContainer;
                mc.playerController.windowClick(
                        c.windowId, slotId, 2, 0, mc.thePlayer); // button=2 → pick-block
            }
            ci.cancel();
        }
    }

    /* ────────────────────────────────────────────────────────────────
       2)  Key-press hook for StashHelper (only if Amecs is present)
       ─────────────────────────────────────────────────────────────── */
    @Inject(method = "keyTyped", at = @At("HEAD"), cancellable = true)
    private void bazaarutils$onKeyTyped(char chr, int code, CallbackInfoReturnable<Boolean> cir) {
        if (BazaarUtils.STASH_HELPER == null) return;

        StashHelper kb = BazaarUtils.STASH_HELPER;
        /* default Amecs binding: ALT+V (code stored in StashHelper) */
        if (!kb.isPressed() && kb.matchesKey(code)) {
            if (kb.getTicksBetweenPresses() > 10) {
                kb.setPressed(true);
                Util.notifyAll("§e[Stash-Helper] closing GUI + /pickupstash",
                        Util.NotificationType.FEATURE);
            }
            cir.setReturnValue(Boolean.TRUE);   // consume key
        }
    }

    /* ────────────────────────────────────────────────────────────────
       3)  After the container is set up, add BU overlay buttons
       ─────────────────────────────────────────────────────────────── */
    @Inject(method = "initGui", at = @At("TAIL"))
    private void bazaarutils$addButtons(CallbackInfo ci) {
        int added = 0;
        for (ItemSlotButtonWidget w : BUConfig.getWidgets()) {
            // GuiContainer#buttonList is public in 1.8.9
            ((GuiContainer)(Object)this).buttonList.add(w);
            added++;
        }
        if (added > 0 && Minecraft.getMinecraft().thePlayer != null) {
            Minecraft.getMinecraft().thePlayer.addChatMessage(
                    new ChatComponentText(EnumChatFormatting.DARK_GRAY +
                            "[Bazaar-Utils] added " + added + " overlay button" +
                            (added == 1 ? "" : "s")));
        }
    }
}
