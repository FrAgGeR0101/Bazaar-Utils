package com.github.mkram17.bazaarutils.features;

import com.github.mkram17.bazaarutils.BazaarUtils;
import com.github.mkram17.bazaarutils.config.BUConfig;
import com.github.mkram17.bazaarutils.events.BUListener;
import com.github.mkram17.bazaarutils.events.ReplaceItemEvent;
import com.github.mkram17.bazaarutils.events.SignOpenEvent;
import com.github.mkram17.bazaarutils.events.SlotClickEvent;
import com.github.mkram17.bazaarutils.misc.CustomItemButton;
import com.github.mkram17.bazaarutils.utils.GUIUtils;
import com.github.mkram17.bazaarutils.utils.SoundUtil;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionGroup;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.mkram17.bazaarutils.BazaarUtils.eventBus;

//TODO low priority -- add number formating with commas (NumberFormat class?) for the tooltips to make large numbers easier to read
@NoArgsConstructor
public class CustomOrder extends CustomItemButton implements BUListener {
    public static final Map<Integer, Item> COLORMAP = new HashMap<>(Map.of(0, Items.PURPLE_STAINED_GLASS_PANE, 1, Items.BLUE_STAINED_GLASS_PANE, 2, Items.ORANGE_STAINED_GLASS_PANE, 3, Items.BLACK_STAINED_GLASS_PANE, 4, Items.BLACK_STAINED_GLASS_PANE));
    private boolean buySignClicked = false;

    @Getter @Setter
    private boolean enabled;
    @Getter @Setter
    private int orderAmount;
    @Getter
    private Item item;

    public CustomOrder(boolean enabled, int orderAmount, int slotNumber, Item item) {
        this.enabled = enabled;
        this.orderAmount = orderAmount;
        this.slotNumber = slotNumber;
        this.item = item;
        eventBus.subscribe(this);
    }
    public static Item getNextColoredPane(){
        int size = BUConfig.get().customOrders.size();
        return CustomOrder.COLORMAP.get(size % 5);
    }

    public static ConfigCategory.Builder createOrdersCategory(){
        return ConfigCategory.createBuilder()
                .name(Text.literal("Buy Amount Options"));
    }

    @EventHandler
    public void replaceItemEvent(ReplaceItemEvent event) {
        if (!(BazaarUtils.gui.inBuyOrderScreen() || BazaarUtils.gui.inInstaBuy()) || !isEnabled())
            return;

        if (event.getSlotId() != slotNumber)
            return;

        ItemStack itemStack = new ItemStack(getItem(), 1);
        itemStack.set(BazaarUtils.CUSTOM_SIZE_COMPONENT, String.valueOf(getOrderAmount()));

        itemStack.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Buy " + getOrderAmount()).formatted(Formatting.DARK_PURPLE));
        event.setReplacement(itemStack);
    }

    @EventHandler
    public void onSlotClicked(SlotClickEvent event) {
        if (!(BazaarUtils.gui.inBuyOrderScreen() || BazaarUtils.gui.inInstaBuy()) || !isEnabled())
            return;

        if (event.slot.getIndex() != slotNumber)
            return;
        SoundUtil.playSound(BUTTON_SOUND, BUTTON_VOLUME);

        openSign();
    }

    @EventHandler
    private void onSignOpened(SignOpenEvent event) {
        if (!buySignClicked) return;
        GUIUtils.setSignText(Integer.toString(getOrderAmount()), true);
        buySignClicked = false;
    }

    public void openSign() {
        int signSlotId = 16;
        GUIUtils.clickSlot(signSlotId, 0);
        buySignClicked = true;
    }

    public Option<Boolean> createOption() {
        return super.createOption(
                getOrderAmount() == 71680 ? "Buy Max Button" : "Buy " + getOrderAmount() + " Button",
                "Buy order button for " + getOrderAmount() + " of an item.",
                this::isEnabled,
                this::setEnabled
        );
    }
    public static void buildOptions(OptionGroup.Builder builder){
        List<CustomOrder> customOrders = BUConfig.get().customOrders;
        if(customOrders.isEmpty())
            customOrders.add(new CustomOrder(true, 71680, 17, CustomOrder.COLORMAP.get(0)));

        for (CustomOrder order : customOrders) {
            builder.option(order.createOption());
        }
    }

    public void remove(){
        if (BUConfig.get().customOrders.contains(this)) {
            BUConfig.get().customOrders.remove(this);
            BUConfig.HANDLER.save();
            eventBus.unsubscribe(this);
        }
    }

    @Override
    public void subscribe() {
        eventBus.subscribe(this);
    }
}