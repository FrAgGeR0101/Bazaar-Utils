package com.github.mkram17.bazaarutils.features;


import com.github.mkram17.bazaarutils.BazaarUtils;
import com.github.mkram17.bazaarutils.events.*;
import com.github.mkram17.bazaarutils.utils.GUIUtils;
import com.github.mkram17.bazaarutils.utils.SoundUtil;
import com.github.mkram17.bazaarutils.utils.Util;
import com.github.mkram17.bazaarutils.misc.CustomItemButton;
import com.github.mkram17.bazaarutils.misc.ItemData;
import dev.isxander.yacl3.api.Option;
import lombok.Getter;
import lombok.Setter;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import static com.github.mkram17.bazaarutils.BazaarUtils.eventBus;

//TODO switch to finding market price without finding the ItemData first. Then, ItemUpdater should handle fixing it. Or just do it that way for redundancy.
public class FlipHelper extends CustomItemButton implements BUListener {
    public double flipPrice;
    private double orderPrice = -1;
    private int orderVolumeFilled = -1;
    private ItemData item;
    private boolean shouldAddToSign = false;
    private boolean inCancelOrderScreen = false;
    @Getter @Setter
    private boolean enabled;
    @Getter
    private Item replaceItem;

    public FlipHelper(boolean enabled, int slotNumber, Item item) {
        this.enabled = enabled;
        this.slotNumber = slotNumber;
        this.replaceItem = item;
    }
    @EventHandler
    public void guiChestOpenedEvent(ChestLoadedEvent e) {
        if(isEnabled() && BazaarUtils.gui.inFlipGui) {
            item = getFlipItem(e);

            inCancelOrderScreen = false;
            ItemStack cancelItem = e.getItemStacks().get(11);
            if(!cancelItem.getComponentChanges().get(DataComponentTypes.LORE).get().styledLines().get(0).getString().contains("Cannot cancel"))
                inCancelOrderScreen = true;

            flipPrice = item.getFlipPrice();
        }
    }

    public void handleFlip() {
        if(item != null && flipPrice != 0 && BazaarUtils.gui.wasLastChestFlip()) {
            GUIUtils.setSignText(Double.toString(Util.getPrettyNumber(flipPrice)), true);
            item.flipItem(flipPrice);
        }
    }

    //not an event, just uses data from the event
    public ItemData getFlipItem(ChestLoadedEvent e){

        for (ItemStack itemStack : e.getItemStacks()) {

            if (itemStack == null) continue;

            String displayName = itemStack.getName().getString();
            LoreComponent lore = itemStack.getComponents().get(DataComponentTypes.LORE);

            if(displayName.contains("Flip Order")) {
                getItemInfo(lore);

                //method updates item var
                if (matchFound()) {
                    return item;
                }
            }
        }
        return null;
    }

    public void getItemInfo(LoreComponent lore) {
        //get order volume and price of item that is being flipped
        try {
            String orderPrice = lore.lines().get(3).getSiblings().get(1).getString();
            orderPrice = orderPrice.substring(0, orderPrice.indexOf(" coins")).replace(",","");
            this.orderPrice = Double.parseDouble(orderPrice);
            String orderVolume = lore.lines().get(1).getSiblings().get(1).getString().replace(",","");
            this.orderVolumeFilled = Integer.parseInt(orderVolume);

        } catch (Exception ex) {
            Util.notifyAll("Error while trying to find order price or volume in Flip Helper");
            ex.printStackTrace();
        }
    }

    public boolean matchFound() {
        item = ItemData.findItem(null, orderPrice, orderVolumeFilled, ItemData.priceTypes.INSTASELL);
        if (item != null) {
            if (item.getStatus() == ItemData.statuses.FILLED) {
                Util.notifyAll("Found match.", Util.notificationTypes.ITEMDATA);
                return true;
            }else {
                Util.notifyAll("found match, but isnt filled", Util.notificationTypes.GUI);
                return true;
            }
        }
        return false;
    }

    @EventHandler
    public void onSlotClicked(SlotClickEvent event) {
        if (!BazaarUtils.gui.inFlipGui || !isEnabled() || event.slot.getIndex() != slotNumber)
            return;
        SoundUtil.playSound(BUTTON_SOUND, BUTTON_VOLUME);
        GUIUtils.clickSlot(15,0);
        if (item != null)
            shouldAddToSign = true;
    }
    @EventHandler
    private void onSignOpen(SignOpenEvent e){
        if(!shouldAddToSign) return;
        handleFlip();
        shouldAddToSign = false;
    }

    @EventHandler
    public void replaceItemEvent(ReplaceItemEvent event) {
        if(!(event.getSlotId() == slotNumber)) return;
        if(!BazaarUtils.gui.inFlipGui || !isEnabled() || inCancelOrderScreen) return;

        ItemStack itemStack = new ItemStack(getReplaceItem(), 1);
        if(flipPrice == 0) {
            itemStack.set(DataComponentTypes.CUSTOM_NAME, Text.literal("There are no competing sell offers.").formatted(Formatting.DARK_PURPLE));
            itemStack.set(BazaarUtils.CUSTOM_SIZE_COMPONENT, "ANY");
        } else if(item == null) {
            itemStack.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Could not find order").formatted(Formatting.DARK_PURPLE));
            itemStack.set(BazaarUtils.CUSTOM_SIZE_COMPONENT, "???");
        }else {
            itemStack.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Flip order for " + Util.getPrettyNumber(flipPrice) + " coins").formatted(Formatting.DARK_PURPLE));
            itemStack.set(BazaarUtils.CUSTOM_SIZE_COMPONENT, String.valueOf(Util.getPrettyNumber(flipPrice)));
        }
        event.setReplacement(itemStack);
    }

    public Option<Boolean> createOption() {
        return super.createOption("Flip Helper", "Button in flip order menu to undercut market prices for items.", () -> isEnabled(), newVal -> setEnabled(newVal));
    }

    @Override
    public void subscribe() {
        eventBus.subscribe(this);
    }
}

