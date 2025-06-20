package com.github.mkram17.bazaarutils.features;

import com.github.mkram17.bazaarutils.BazaarUtils;
import com.github.mkram17.bazaarutils.config.BUConfig;
import com.github.mkram17.bazaarutils.events.ChestLoadedEvent;
import com.github.mkram17.bazaarutils.events.ReplaceItemEvent;
import com.github.mkram17.bazaarutils.events.SlotClickEvent;
import com.github.mkram17.bazaarutils.misc.CustomItemButton;
import com.github.mkram17.bazaarutils.misc.ItemSlotButtonWidget;
import com.github.mkram17.bazaarutils.misc.ModCompatibilityHelper;
import com.github.mkram17.bazaarutils.mixin.AccessorHandledScreen;
import com.github.mkram17.bazaarutils.utils.GUIUtils;
import com.github.mkram17.bazaarutils.utils.SoundUtil;
import com.github.mkram17.bazaarutils.utils.Util;
import lombok.Getter;
import lombok.Setter;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

//TODO when itemStack cant be found, do not show bookmark option
public class Bookmark extends CustomItemButton {
    @Getter @Setter
    public String name;
    @Getter @Setter
    public ItemStack bookmarkedItem;
    protected boolean inCorrectGui = false;
    private static final int SIGN_SLOT_NUMBER = 45;

    private static final Identifier BASE = Identifier.tryParse(BazaarUtils.MODID, "widget/widget_bookmark_base");
    private static final Identifier HOVER = Identifier.tryParse(BazaarUtils.MODID, "widget/widget_bookmark_hover");
    public static final ButtonTextures SLOT_BUTTON_TEXTURES = new ButtonTextures(
            BASE,
            HOVER);

    @EventHandler
    protected void checkGui(ChestLoadedEvent event) {
            BazaarUtils.eventBus.unsubscribe(this);
    }

    public Bookmark(String name, ItemStack bookmarkedItem) {
        this.name = name;
        this.slotNumber = 0;
        this.bookmarkedItem = bookmarkedItem;
        changeVisuals(isBookmarked(this.name));
        this.replacementItem.set(BazaarUtils.CUSTOM_SIZE_COMPONENT, "★");
        inCorrectGui = true;

        BazaarUtils.eventBus.subscribe(this);
    }

    @EventHandler
    protected void replaceItemEvent(ReplaceItemEvent event) {
        try {
            if (!inCorrectGui || !super.shouldReplaceItem(event))
                return;

            if (replacementItem == null)
                changeVisuals(isBookmarked(name));

            event.setReplacement(replacementItem);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @EventHandler
    private void onBookmarkClick(SlotClickEvent event){
        if(!inCorrectGui || !super.shouldUseSlot(event))
            return;
        SoundUtil.playSound(BUTTON_SOUND, BUTTON_VOLUME);
        switchBookmarked();
        bookmarkedItem = findItem(name, event);
        BUConfig.HANDLER.save();
    }

    public void onWidgetLeftClick(){
        SoundUtil.playSound(BUTTON_SOUND, BUTTON_VOLUME);
        ModCompatibilityHelper.tryDisableSkyblockerBazaarOverlay();
        GUIUtils.clickSlot(SIGN_SLOT_NUMBER, 0);
        GUIUtils.setSignText(name, true);
        Util.tickExecuteLater(4, ModCompatibilityHelper::tryEnableSkyblockerBazaarOverlay);
    }

    //requires cookie?
    public void alternateOnWidgetLeftClick(){
        GUIUtils.closeHandledScreen();
        Util.sendCommand("bz " + name);
    }


    public void onWidgetShiftClick(){
        BUConfig.get().bookmarks.remove(this);
        BUConfig.HANDLER.save();
    }

    private void switchBookmarked(){
        if(isBookmarked(name)) {
            changeVisuals(false);
            BUConfig.get().bookmarks.remove(this);
        }else {
            changeVisuals(true);
            BUConfig.get().bookmarks.add(this);
        }
        BUConfig.HANDLER.save();
    }

    private void changeVisuals(boolean bookmarked){
        if(bookmarked) {
            replacementItem = new ItemStack(Items.GREEN_STAINED_GLASS_PANE, 1);
            replacementItem.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Remove " + name + " Bookmark"));
            replacementItem.set(BazaarUtils.CUSTOM_SIZE_COMPONENT, "⃠ ");
        }
        else {
            replacementItem = new ItemStack(Items.RED_STAINED_GLASS_PANE, 1);
            replacementItem.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Bookmark " + name));
            replacementItem.set(BazaarUtils.CUSTOM_SIZE_COMPONENT, "★");
        }
    }

    public static String findName(ChestLoadedEvent e){
        String containerName = GUIUtils.getContainerName();
        String name = findNameFromContainer();
        if(containerName.length() > 30){
            for(ItemStack stack : e.getItemStacks()){
                if(stack == null) continue;
                if (!stack.isEmpty() && stack.getName().getString().startsWith(name)) {
                    //? if >= 1.21.4 {
                    return stack.getCustomName().getString();
                    //?} else {
                    /*return stack.getComponentChanges().get(DataComponentTypes.CUSTOM_NAME).get().getString();
                    *///?}
                }
            }
        }
        return name;
    }

    private static String findNameFromContainer(){
        String containerName = GUIUtils.getContainerName();
        assert containerName != null;
        if(BazaarUtils.gui.inInstaBuy()) {
            return containerName.substring(0, containerName.indexOf("➜")-1);
        }
        if(BazaarUtils.gui.inBuyOrderScreen()){
            containerName = BazaarUtils.gui.getPreviousScreenName();
            return containerName.substring(containerName.indexOf("➜")+2);
        }
        if(BazaarUtils.gui.inAnyItemScreen())
            return containerName.substring(containerName.indexOf("➜")+2);
        return "?";
    }


    private static ItemStack findItem(String name, SlotClickEvent event){
        ScreenHandler handler = event.handledScreen.getScreenHandler();

        for(Slot slot : handler.slots){
            ItemStack itemStack = slot.getStack();
            if(itemStack == null) continue;

            if (!itemStack.isEmpty() && itemStack.getName().getString().startsWith(name)) {
                return itemStack;
            }
        }
        for(Slot slot : handler.slots){
            ItemStack itemStack = slot.getStack();

            if (!itemStack.isEmpty() && itemStack.getName().getString().contains(name)) {
                return itemStack;
            }
        }
        return null;
    }

    public static boolean isBookmarked(String name){
        return findMatchingBookmark(name) != null;
    }

    public static Bookmark findMatchingBookmark(String name){
        for(Bookmark bookmark : BUConfig.get().bookmarks) {
            if(bookmark.getName().equalsIgnoreCase(name))
                return bookmark;
        }
        return null;
    }

    public static List<ItemSlotButtonWidget> getWidgets(){
        List<ItemSlotButtonWidget> widgets = new ArrayList<>();
        String screenTitle = MinecraftClient.getInstance().currentScreen.getTitle().getString();
        boolean isTargetScreen = screenTitle.startsWith("Bazaar");

        if(!(MinecraftClient.getInstance().currentScreen instanceof AccessorHandledScreen screen) || !isTargetScreen)
            return Collections.emptyList();


        ItemSlotButtonWidget.ScreenWidgetDimensions dimensions = ItemSlotButtonWidget.getSafeScreenDimensions(screen, screenTitle);

            int buttonSize = 18;
            int spacing = 4;
            int buttonX = dimensions.x() + dimensions.backgroundWidth() + spacing;
            int currentButtonY = dimensions.y() + spacing;

            List<Bookmark> bookmarks = BUConfig.get().bookmarks;

            for (int i = 0; i < bookmarks.size(); i++) {
                ItemStack configuredItem = bookmarks.get(i).getBookmarkedItem();

                final int buttonIndex = i;
                final ItemStack itemForButton = (configuredItem == null) ? Items.BARRIER.getDefaultStack() : configuredItem;
                final Bookmark bookmark = bookmarks.get(buttonIndex);

                ItemSlotButtonWidget button = new ItemSlotButtonWidget(
                        buttonX,
                        currentButtonY,
                        buttonSize, buttonSize,
                        SLOT_BUTTON_TEXTURES,
                        (btn) -> {
                            if (Screen.hasShiftDown()) {
                                Util.notifyAll("Removed " + bookmark.getName() + " bookmark from shift-click. Open Bazaar again to display changes.");
                                bookmark.onWidgetShiftClick();
                            } else {
                                bookmark.onWidgetLeftClick();
                            }

                        },
                        itemForButton,
                        Text.of(bookmark.getName())
                );

                widgets.add(button);
                currentButtonY += buttonSize + spacing;
            }

        return widgets;
    }

}
