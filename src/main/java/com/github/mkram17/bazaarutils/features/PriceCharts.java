package com.github.mkram17.bazaarutils.features;

import com.github.mkram17.bazaarutils.BazaarUtils;
import com.github.mkram17.bazaarutils.config.BUConfig;
import com.github.mkram17.bazaarutils.data.BazaarData;
import com.github.mkram17.bazaarutils.events.BUListener;
import com.github.mkram17.bazaarutils.events.SlotClickEvent;
import com.github.mkram17.bazaarutils.utils.GUIUtils;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import lombok.Getter;
import lombok.Setter;
import meteordevelopment.orbit.EventHandler;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConfirmLinkScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

public class PriceCharts implements ItemTooltipCallback, BUListener {
    private static String productID;
    @Getter @Setter
    private boolean showOutsideBazaar = false;

    @Override
    public void getTooltip(ItemStack stack, Item.TooltipContext tooltip, TooltipType tooltipType, List<Text> lines) {
        if (stack == null || stack.isEmpty() || !shouldShow() || stack.getItem().getName().getString().contains("GLASS_PANE"))
            return;
        productID = null;

        String itemName = stack.getName().getString();

        if(itemName.indexOf("x") == itemName.length()-2)
            itemName = itemName.substring(0, itemName.indexOf("x")-1);

        productID = BazaarData.findProductId(itemName);
        if(productID == null) {
            stack.set(BazaarUtils.CUSTOM_SHOWPRICECHART_COMPONENT, false);
            //for checking items missing from bazaar-resources product id conversions
//            if(stack.getComponentChanges() != null && stack.getComponentChanges().get(DataComponentTypes.CUSTOM_DATA) != null) {
//                productID = stack.getComponentChanges().get(DataComponentTypes.CUSTOM_DATA).toString();
//                productID = productID.substring(productID.indexOf("\"")+1, productID.lastIndexOf("\""));
//                if(productID.contains(itemName.toUpperCase()) ||
//                        (itemName.contains(" ") && productID.contains(itemName.substring(0, itemName.indexOf(" ")).toUpperCase())) ||
//                        (itemName.contains("-") && productID.contains(itemName.substring(0, itemName.indexOf("-")).toUpperCase()))) {
//                    Util.notifyAll("New product id detected: " + productID + "name: " + itemName, Util.notificationTypes.BAZAARDATA);
//                }
//            }
            return;
        }
        stack.set(BazaarUtils.CUSTOM_SHOWPRICECHART_COMPONENT, true);
        MutableText text = Text.literal("CTRL+SHIFT click for price charts & other info")
                .formatted(Formatting.GOLD)
                .formatted(Formatting.BOLD);

        MutableText poweredBy = Text.literal("Powered by skyblock.finance")
                .formatted(Formatting.GRAY);

        lines.add(Text.literal(""));
        lines.add(text);
        lines.add(poweredBy);
    }

    @EventHandler
    private void onClick(SlotClickEvent e){
        if(!shouldShow() || Boolean.FALSE.equals(e.slot.getStack().get(BazaarUtils.CUSTOM_SHOWPRICECHART_COMPONENT)) || e.isCancelled() ||  !(Screen.hasShiftDown() && Screen.hasControlDown()))
            return;

        String link = "https://skyblock.finance/items/"+productID;
        MinecraftClient.getInstance().setScreen(new ConfirmLinkScreen((confirmed) -> {
            if (confirmed) {
                try {
                    net.minecraft.util.Util.getOperatingSystem().open(new URI(link));
                } catch (URISyntaxException exception) {
                    throw new RuntimeException(exception);
                }
            }
            MinecraftClient.getInstance().setScreen(null);
        }, link, true));
        e.cancel();
    }

    @Override
    public void subscribe() {
        ItemTooltipCallback.EVENT.register(this);
        BazaarUtils.eventBus.subscribe(this);
    }

    private boolean shouldShow(){
        return (BazaarUtils.gui.inBazaar() || showOutsideBazaar) && !GUIUtils.getContainerName().contains("Bazaar");
    }

    public Option<Boolean> createOption() {
        return Option.<Boolean>createBuilder()
                .name(Text.literal("Price Charts Option Outside Bazaar"))
                .description(OptionDescription.of(Text.literal("Usually the option to CTRL+SHIFT click an item to see the price charts and other information on skyblock.finance is only shown inside the Bazaar while in an item view. This enables the feature outside of the Bazaar as well.")))
                .binding(false,
                        this::isShowOutsideBazaar,
                        this::setShowOutsideBazaar)
                .controller(BUConfig::createBooleanController)
                .build();
    }
}