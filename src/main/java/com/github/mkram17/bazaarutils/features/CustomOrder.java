package com.github.mkram17.bazaarutils.features;

import com.github.mkram17.bazaarutils.BazaarUtils;
import com.github.mkram17.bazaarutils.config.BUConfig;
import com.github.mkram17.bazaarutils.events.ReplaceItemEvent;
import com.github.mkram17.bazaarutils.events.SignOpenEvent;
import com.github.mkram17.bazaarutils.events.SlotClickEvent;
import com.github.mkram17.bazaarutils.misc.CustomItemButton;
import com.github.mkram17.bazaarutils.utils.GUIUtils;
import com.github.mkram17.bazaarutils.utils.SoundUtil;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * “Buy N” quick-order button for the Bazaar buy-order GUI.
 * Pure Forge-1.8.9 (no Fabric / Lombok / Orbit).
 */
public final class CustomOrder extends CustomItemButton {

    /* ───────────────────────── static helpers ───────────────────────── */

    /**
     * Five-colour cycle – we just take the block-item of stained_glass_pane
     * (meta is handled by the resource-pack, so we don’t care here).
     */
    private static final Item GLASS_PANE =
            Item.getItemFromBlock(Blocks.stained_glass_pane);

    private static final Map<Integer, Item> COLOR_MAP = new HashMap<>();
    static {
        for (int i = 0; i < 5; i++) COLOR_MAP.put(i, GLASS_PANE);
    }

    /** Returns the next colour in the cycle when the user adds a button. */
    public static Item nextPaneColour() {
        int idx = BUConfig.get().getCustomOrders().size();
        return COLOR_MAP.get(idx % 5);
    }

    /* ───────────────────────── instance data ───────────────────────── */

    private boolean enabled;
    private final int  orderAmount;
    private final Item icon;

    /** Helper flag so we know the next Sign-GUI belongs to us. */
    private boolean waitingForSign = false;

    /* ───────────────────────── life-cycle ──────────────────────────── */

    public CustomOrder(boolean enabled, int amount, int slot, Item pane) {
        this.enabled      = enabled;
        this.orderAmount  = amount;
        this.slotNumber   = slot;
        this.icon         = pane;

        /* register to Forge’s event-bus (not Fabric’s) */
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(this);
    }

    /* ───────────────────────── Forge event hooks ───────────────────── */

    @SubscribeEvent
    public void onReplaceItem(ReplaceItemEvent ev) {
        if (!enabled)                                      return;
        if (!(BazaarUtils.GUI.inBuyOrderScreen() ||
              BazaarUtils.GUI.inInstaBuy()))               return;
        if (ev.getSlotId() != slotNumber)                  return;

        ItemStack stack = new ItemStack(icon, 1);
        stack.setStackDisplayName(EnumChatFormatting.DARK_PURPLE +
                                  "Buy " + orderAmount);
        ev.setReplacement(stack);
    }

    @SubscribeEvent
    public void onSlotClick(SlotClickEvent ev) {
        if (!enabled)                                      return;
        if (!(BazaarUtils.GUI.inBuyOrderScreen() ||
              BazaarUtils.GUI.inInstaBuy()))               return;
        /* 1.8.9 Slot has a public ‘slotNumber’ – no getIndex() */
        if (ev.slot.slotNumber != slotNumber)              return;

        SoundUtil.playClick();
        openSign();
        ev.setCancelled(true);
    }

    @SubscribeEvent
    public void onSignOpen(SignOpenEvent ev) {
        if (!waitingForSign) return;
        GUIUtils.setSignText(Integer.toString(orderAmount), true);
        waitingForSign = false;
    }

    /* ───────────────────────── GUI helpers ─────────────────────────── */

    /** Click vanilla “sign” slot and wait for Sign-GUI. */
    private void openSign() {
        final int SIGN_SLOT = 16;            // Hypixel constant
        GUIUtils.clickSlot(SIGN_SLOT, 0);
        waitingForSign = true;
    }

    /* ───────────────────────── config-GUI stubs ────────────────────── */

    /** Create a Boolean option (YACL stub) */
    public Option<Boolean> createOption() {
        return Option.<Boolean>builder()
                .name(orderAmount == 71680 ? "Buy Max Button"
                                           : "Buy " + orderAmount + " Button")
                .description("Quick-buy button for " + orderAmount + " items.")
                .binding(enabled, () -> enabled, v -> enabled = v)
                .build();
    }

    /** Add every CustomOrder to the “Custom Buy Amounts” group. */
    public static void addOptionsTo(OptionGroup.Builder grp) {
        if (BUConfig.get().getCustomOrders().isEmpty())
            BUConfig.get().getCustomOrders()
                    .add(new CustomOrder(true, 71680, 17, nextPaneColour()));

        BUConfig.get().getCustomOrders()
                .forEach(o -> grp.option(o.createOption()));
    }

    public static ConfigCategory.Builder createCategory() {
        return ConfigCategory.builder().name("Buy Amount Options");
    }

    /* ───────────────────────── simple getters ─────────────────────── */

    public boolean isEnabled()          { return enabled; }
    public void    setEnabled(boolean b){ enabled = b;    }
    public int     getOrderAmount()     { return orderAmount; }
    public Item    getIcon()            { return icon;    }

    /* ───────────────────────── removal helper ─────────────────────── */

    public void remove() {
        BUConfig.get().getCustomOrders().remove(this);
        BUConfig.HANDLER.save();
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(this);
    }

    /* =================================================================
       Tiny YACL-style stubs – just enough for compilation
       ================================================================= */
    // @formatter:off
    public static final class Option<T> {
        public interface Getter<T> { T get(); }
        public interface Setter<T> { void set(T v); }
        public static <T> Builder<T> builder() { return new Builder<>(); }
        public static final class Builder<T> {
            public Builder<T> name(String s){ return this; }
            public Builder<T> description(String d){ return this; }
            public Builder<T> binding(T def, Getter<T> g, Setter<T> s){ return this; }
            public Option<T> build(){ return new Option<>(); }
        }
    }
    public static final class OptionGroup {
        public static Builder builder(){ return new Builder(); }
        public static final class Builder {
            public Builder name(String s){ return this; }
            public Builder option(Object o){ return this; }
            public OptionGroup build(){ return new OptionGroup(); }
        }
    }
    public static final class ConfigCategory {
        public static Builder builder(){ return new Builder(); }
        public static final class Builder {
            public Builder name(String s){ return this; }
            public Builder option(Object o){ return this; }
            public Builder group(Object o){ return this; }
            public ConfigCategory build(){ return new ConfigCategory(); }
        }
    }
    // @formatter:on
}
