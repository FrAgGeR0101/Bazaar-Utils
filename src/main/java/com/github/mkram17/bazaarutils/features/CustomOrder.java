package com.github.mkram17.bazaarutils.features;

import com.github.mkram17.bazaarutils.BazaarUtils;
import com.github.mkram17.bazaarutils.config.BUConfig;
import com.github.mkram17.bazaarutils.events.ReplaceItemEvent;
import com.github.mkram17.bazaarutils.events.SignOpenEvent;
import com.github.mkram17.bazaarutils.events.SlotClickEvent;
import com.github.mkram17.bazaarutils.misc.CustomItemButton;
import com.github.mkram17.bazaarutils.utils.GUIUtils;
import com.github.mkram17.bazaarutils.utils.SoundUtil;
import dev.isxander.yacl3.api.YaclStubs.ConfigCategory;
import dev.isxander.yacl3.api.YaclStubs.Option;
import dev.isxander.yacl3.api.YaclStubs.OptionGroup;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.EnumChatFormatting;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * “Buy N” quick-order button shown in the Bazaar buy-order GUI.
 * <p>
 * Fully self-contained version for Forge 1.8.9 – no Lombok, no YACL
 * runtime dependency (tiny stubs live in {@code dev.isxander.yacl3.api.YaclStubs}).
 */
public final class CustomOrder extends CustomItemButton {
    /* ------------------------------------------------------------- */
    /*  Static helpers                                               */
    /* ------------------------------------------------------------- */

    /** Five-colour cycle (purple, blue, orange, black, black). */
    public static final Map<Integer, Item> COLOR_MAP = new HashMap<>(
            java.util.Map.of(
                    0, Items.purple_stained_glass_pane,
                    1, Items.blue_stained_glass_pane,
                    2, Items.orange_stained_glass_pane,
                    3, Items.black_stained_glass_pane,
                    4, Items.black_stained_glass_pane));

    /** Next colour when user adds another custom-order. */
    public static Item nextPaneColour() {
        int idx = BUConfig.get().customOrders.size();
        return COLOR_MAP.get(idx % 5);
    }

    /* ------------------------------------------------------------- */
    /*  Instance data                                                */
    /* ------------------------------------------------------------- */

    private boolean enabled;
    private int     orderAmount;
    private Item    icon;

    private boolean waitingForSign = false;

    /* ------------------------------------------------------------- */
    /*  Construction                                                 */
    /* ------------------------------------------------------------- */

    public CustomOrder(boolean enabled, int amount, int slot, Item pane) {
        this.enabled     = enabled;
        this.orderAmount = amount;
        this.slotNumber  = slot;
        this.icon        = pane;

        BazaarUtils.eventBus.subscribe(this);
    }

    /* ------------------------------------------------------------- */
    /*  Orbit event-handlers                                         */
    /* ------------------------------------------------------------- */

    @EventHandler
    public void onReplaceItem(ReplaceItemEvent ev) {
        if (!enabled)                                             return;
        if (!(BazaarUtils.gui.inBuyOrderScreen() ||
              BazaarUtils.gui.inInstaBuy()))                     return;
        if (ev.getSlotId() != slotNumber)                        return;

        ItemStack stack = new ItemStack(icon, 1);
        stack.setStackDisplayName(EnumChatFormatting.DARK_PURPLE +
                                  "Buy " + orderAmount);
        ev.setReplacement(stack);
    }

    @EventHandler
    public void onSlotClick(SlotClickEvent ev) {
        if (!enabled)                                             return;
        if (!(BazaarUtils.gui.inBuyOrderScreen() ||
              BazaarUtils.gui.inInstaBuy()))                     return;
        if (ev.slot.getIndex() != slotNumber)                    return;

        SoundUtil.playClick();
        openSign();
        ev.setCancelled(true);
    }

    @EventHandler
    public void onSignOpen(SignOpenEvent ev) {
        if (!waitingForSign) return;
        GUIUtils.setSignText(Integer.toString(orderAmount), true);
        waitingForSign = false;
    }

    /* ------------------------------------------------------------- */
    /*  GUI helpers                                                  */
    /* ------------------------------------------------------------- */

    private void openSign() {
        final int SIGN_SLOT = 16;          // Hypixel hard-coded slot
        GUIUtils.clickSlot(SIGN_SLOT, 0);
        waitingForSign = true;
    }

    /* ------------------------------------------------------------- */
    /*  Lightweight config-GUI stubs                                 */
    /* ------------------------------------------------------------- */

    /** Tiny YACL-style option (noop on Forge). */
    public Option<Boolean> createOption() {
        return Option.<Boolean>createBuilder()
                .name("Buy " + (orderAmount == 71680 ? "Max" : orderAmount))
                .description("Quick-buy button for " + orderAmount + " items.")
                .binding(enabled,
                         () -> enabled,
                         v  -> enabled = v)
                .build();
    }

    /** Populates the “Custom Buy Amounts” category in BUConfig. */
    public static void addOptionsTo(OptionGroup.Builder group) {
        if (BUConfig.get().customOrders.isEmpty())
            BUConfig.get().customOrders.add(
                    new CustomOrder(true, 71680, 17, nextPaneColour()));

        BUConfig.get().customOrders.forEach(o -> group.option(o.createOption()));
    }

    /** Stub category builder so BUConfig compiles without YACL runtime. */
    public static ConfigCategory.Builder createCategory() {
        return ConfigCategory.createBuilder().name("Buy Amount Options");
    }

    /* ------------------------------------------------------------- */
    /*  Plain getters / setters                                      */
    /* ------------------------------------------------------------- */

    public boolean isEnabled()           { return enabled;      }
    public void    setEnabled(boolean b) { enabled = b;         }
    public int     getOrderAmount()      { return orderAmount;  }
    public Item    getIcon()             { return icon;         }

    /* ------------------------------------------------------------- */
    /*  Removal helper                                               */
    /* ------------------------------------------------------------- */

    public void remove() {
        BUConfig.get().customOrders.remove(this);
        BUConfig.HANDLER.save();
        BazaarUtils.eventBus.unsubscribe(this);
    }

    @Override public void subscribe() { /* subscribed in ctor */ }
}
