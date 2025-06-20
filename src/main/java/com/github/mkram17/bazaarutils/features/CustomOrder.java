package com.github.mkram17.bazaarutils.features;

import com.github.mkram17.bazaarutils.BazaarUtils;
import com.github.mkram17.bazaarutils.config.BUConfig;
import com.github.mkram17.bazaarutils.events.ReplaceItemEvent;
import com.github.mkram17.bazaarutils.events.SignOpenEvent;
import com.github.mkram17.bazaarutils.events.SlotClickEvent;
import com.github.mkram17.bazaarutils.misc.CustomItemButton;
import com.github.mkram17.bazaarutils.utils.GUIUtils;
import com.github.mkram17.bazaarutils.utils.SoundUtil;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * “Buy N” quick-order button for the Bazaar buy-order GUI.
 *
 * <p>No Lombok, YACL or Orbit required – only the standard Forge
 * event-bus plus tiny builder stubs for the config-GUI.</p>
 */
public final class CustomOrder extends CustomItemButton {

    /* ───────────────────────── static helpers ───────────────────────── */

    /** Five-colour cycle used for the glass-pane icons. */
    private static final Map<Integer, Item> COLOR_MAP = new HashMap<>();
    static {
        COLOR_MAP.put(0, Items.stained_glass_pane); // purple  (meta handled by texture-pack)
        COLOR_MAP.put(1, Items.stained_glass_pane); // blue
        COLOR_MAP.put(2, Items.stained_glass_pane); // orange
        COLOR_MAP.put(3, Items.stained_glass_pane); // black
        COLOR_MAP.put(4, Items.stained_glass_pane); // black
    }

    /** Returns the next colour in the cycle when the user adds a button. */
    public static Item nextPaneColour() {
        int idx = BUConfig.get().customOrders.size();
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

        /* Register to Forge’s global event-bus */
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(this);
    }

    /* ───────────────────────── Forge event handlers ────────────────── */

    @SubscribeEvent
    public void onReplaceItem(ReplaceItemEvent ev) {
        if (!enabled)                                     return;
        if (!(BazaarUtils.GUI.inBuyOrderScreen() ||
              BazaarUtils.GUI.inInstaBuy()))              return;
        if (ev.getSlotId() != slotNumber)                 return;

        ItemStack stack = new ItemStack(icon, 1);
        stack.setStackDisplayName(EnumChatFormatting.DARK_PURPLE +
                                  "Buy " + orderAmount);
        ev.setReplacement(stack);
    }

    @SubscribeEvent
    public void onSlotClick(SlotClickEvent ev) {
        if (!enabled)                                     return;
        if (!(BazaarUtils.GUI.inBuyOrderScreen() ||
              BazaarUtils.GUI.inInstaBuy()))              return;
        if (ev.slot.getIndex() != slotNumber)             return;

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

    /** Clicks the vanilla “sign” slot and waits for the Sign-GUI. */
    private void openSign() {
        final int SIGN_SLOT = 16;                     // Hypixel hard-coded slot
        GUIUtils.clickSlot(SIGN_SLOT, 0);
        waitingForSign = true;
    }

    /* ───────────────────────── config-GUI helpers ──────────────────── */
    /*  (minimal YACL-style stubs so existing BUConfig code compiles)   */

    /** Build a Boolean option for the config screen. */
    public Option<Boolean> createOption() {
        return Option.<Boolean>builder()
                .name(orderAmount == 71680 ? "Buy Max Button"
                                           : "Buy " + orderAmount + " Button")
                .description("Quick-buy button for " + orderAmount + " items.")
                .binding(enabled,
                         () -> enabled,
                         v  -> enabled = v)
                .build();
    }

    /** Adds every CustomOrder to the “Custom Buy Amounts” group. */
    public static void addOptionsTo(OptionGroup.Builder grp) {
        if (BUConfig.get().customOrders.isEmpty())
            BUConfig.get().customOrders.add(
                    new CustomOrder(true, 71680, 17, nextPaneColour()));

        BUConfig.get().customOrders.forEach(o -> grp.option(o.createOption()));
    }

    /** Category header shown in BUConfig’s GUI. */
    public static ConfigCategory.Builder createCategory() {
        return ConfigCategory.builder().name("Buy Amount Options");
    }

    /* ───────────────────────── plain getters ───────────────────────── */

    public boolean isEnabled()            { return enabled;     }
    public void    setEnabled(boolean b)  { enabled = b;        }
    public int     getOrderAmount()       { return orderAmount; }
    public Item    getIcon()              { return icon;        }

    /* ───────────────────────── removal helper ─────────────────────── */

    public void remove() {
        BUConfig.get().customOrders.remove(this);
        BUConfig.HANDLER.save();
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(this);
    }

    /* =================================================================
       Tiny YACL stub classes – just enough for the code to compile while
       you port away from Fabric’s config library.
       ================================================================= */

    // @formatter:off
    public static final class Option<T> {
        public interface Getter<T> { T get(); }
        public interface Setter<T> { void set(T v); }

        /* Builder pattern ------------------------------------------------ */
        public static <T> Builder<T> builder() { return new Builder<>(); }
        public static final class Builder<T> {
            private String  name = "";
            private String  desc = "";
            private T       def;
            private Getter<T> getter;
            private Setter<T> setter;
            public Builder<T> name(String n)              { name  = n;  return this; }
            public Builder<T> description(String d)       { desc  = d;  return this; }
            public Builder<T> binding(T def, Getter<T> g, Setter<T> s) {
                this.def = def; getter = g; setter = s; return this;
            }
            public Option<T> build() { return new Option<>(); }
        }
    }

    public static final class OptionGroup {
        public static Builder builder() { return new Builder(); }
        public static final class Builder {
            public Builder name(String n)                     { return this; }
            public Builder option(Object ignored)             { return this; }
            public OptionGroup build()                        { return new OptionGroup(); }
        }
    }

    public static final class ConfigCategory {
        public static Builder builder() { return new Builder(); }
        public static final class Builder {
            public Builder name(String n)                     { return this; }
            public Builder option(Object ignored)             { return this; }
            public Builder group(Object ignored)              { return this; }
            public ConfigCategory build()                     { return new ConfigCategory(); }
        }
    }
    // @formatter:on
}
