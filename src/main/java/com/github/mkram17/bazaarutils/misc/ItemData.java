package com.github.mkram17.bazaarutils.misc;

import com.github.mkram17.bazaarutils.BazaarUtils;
import com.github.mkram17.bazaarutils.config.BUConfig;
import com.github.mkram17.bazaarutils.data.BazaarData;
import com.github.mkram17.bazaarutils.events.OutdatedItemEvent;
import com.github.mkram17.bazaarutils.utils.Util;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;


//TODO figure out how to handle rounding with price
//TODO use last viewed item in bazaar to help with finding accurate price instead of just chat message
@Slf4j
public class ItemData {
    public String getProductID() {
        return productId;
    }

    @Getter
    private final String name;
    private final String productId;

    public int getIndex(){return BUConfig.get().watchedItems.indexOf(this);}

    public String getGeneralInfo(){
        String str = "(name: " + name + "[" + getIndex() + "]" + ", price:" + price + ", volume: " + volume;
        if(amountClaimed != 0)
            str += ", amount claimed: " + amountClaimed;
        str += ", type: " + priceType;
        if(status == statuses.FILLED)
            str += ", status: " + status;
        str +=  ")";
        return str;
    }

    @Getter
    public enum priceTypes{INSTASELL,INSTABUY;
        private priceTypes opposite;
        static {
            INSTASELL.opposite = INSTABUY;
            INSTABUY.opposite = INSTASELL;
        }
        public String getString(){
            return switch (this) {
                case INSTASELL -> "buy order";
                case INSTABUY -> "sell order";
            };
        }
    }
    public enum statuses{SET,FILLED}

    //insta sell and insta buy
    @Setter
    @Getter
    private double price;
    @Setter
    @Getter
    private priceTypes priceType;
    //the sell or buy price of lowest/highest offer
    @Getter
    private double marketPrice;
    //the price of the opposite type of order
    private double marketOppositePrice;
    @Setter
    @Getter
    private statuses status;
    @Getter
    private final int volume;

    @Setter
    @Getter
    private int amountClaimed = 0;
    @Setter
    @Getter
    private int amountFilled = 0;
    @Getter @Setter
    private double maximumRounding;

    @Getter
    private static List<ItemData> outdated = new ArrayList<>(Collections.emptyList());

//    @Param fullPrice is the price per unit * volume
    //When finding item price, it can round to the nearest coin sometimes, so maximumRounding is used to determine if the price is similar enough to be considered a match
    public ItemData(String name, Double fullPrice, priceTypes priceType, int volume) {
        this.name = name;
        this.priceType = priceType;
        this.productId = BazaarData.findProductId(name);
        this.volume = volume;
        this.price = fullPrice/volume;
        price = (double) Math.round(price * 100) / 100;
        this.status = statuses.SET;
        this.maximumRounding = getMaxRounding(fullPrice, volume);

        if(productId == null){
            Util.notifyAll("Could not find product id for item: " + name, Util.notificationTypes.ITEMDATA);
        }
    }

    private static double getMaxRounding(double fullPrice, int volume){
        if(fullPrice < 10000)
            return 0;
        else{
            return (Math.ceil((.9 / volume) * 10))/10;
        }
    }


    public static void update(){
        updateMarketPrices();
        findOutdated();
    }

    private static void updateMarketPrices(){
        for(ItemData item: BUConfig.get().watchedItems) {
            double oldPrice = item.marketPrice;
            item.marketPrice = Util.getPrettyNumber(BazaarData.findItemPrice(item.productId, item.priceType));
            item.marketOppositePrice = Util.getPrettyNumber(BazaarData.findItemPrice(item.productId, item.priceType.getOpposite()));
            if(oldPrice != item.marketPrice)
                Util.notifyAll(item.getGeneralInfo() + " has new market price: " + item.getMarketPrice(), Util.notificationTypes.BAZAARDATA);
        }
    }

    public void flipItem(double newPrice){
        this.priceType = this.priceType.getOpposite();
        this.price = newPrice;
        this.amountFilled = 0;
        this.status = statuses.SET;
    }

    //TODO some error with maximum rounding or finding the price. either finding price can round down by .1 accidentally or maximum rounding calculation is wrong
    public boolean isSimilarPrice(double price) {
        return Math.abs(getPrice() - price) <= maximumRounding;
    }

    //run by ex: getVariables((item) -> item.getPrice()) orItemData.getVariables(ItemData::getPrice);
    public static <T> ArrayList<T> getVariables(Function<ItemData, T> variable){
        ArrayList<T> variables = new ArrayList<>();
        for(ItemData item : BUConfig.get().watchedItems){
            variables.add(variable.apply(item));
        }
        return variables;
    }

    //TODO refactor of match finding -- it can definitely be improved
    private static ArrayList<ItemData> findExactMatches(String name, Double price, Integer volume, priceTypes priceType){
        ArrayList<ItemData> itemList = new ArrayList<>();
        for(ItemData item : BUConfig.get().watchedItems){
            if((price == null || item.isSimilarPrice(price)) &&
                    (volume == null || Math.abs(item.getVolume() - volume) <= (0.05 * volume)) &&
                    (name == null || name.equalsIgnoreCase(item.getName())) &&
                    (priceType == null || priceType == item.getPriceType())){
                itemList.add(item);
            }
        }
        return itemList;
    }
    private static ArrayList<ItemData> findLooseVolumeMatches(String name, Double price, Integer volume, priceTypes priceType){
        ArrayList<ItemData> itemList = new ArrayList<>();
        for(ItemData item : BUConfig.get().watchedItems){
            if((price == null || item.isSimilarPrice(price)) &&
                    (volume == null || Math.abs(item.getVolume() - volume) <= (0.05 * volume) || Math.abs(item.getVolume()-item.getAmountClaimed() - volume) <= (0.05 * volume)) &&
                    (name == null || name.equalsIgnoreCase(item.getName())) &&
                    (priceType == null || priceType == item.getPriceType())){
                itemList.add(item);
            }
        }
        return itemList;
    }

    public static ItemData findItem(String name, Double price, Integer volume, priceTypes priceType) {
        ArrayList<ItemData> itemList = findExactMatches(name, price, volume, priceType);
        if(itemList.isEmpty())
            itemList = findLooseVolumeMatches(name, price, volume, priceType);

        if (itemList.isEmpty()) {
            Util.notifyAll("Could not find item with info: [name: " + name + ", price: " + price + ", volume: " + volume + "]", Util.notificationTypes.ITEMDATA);
            return null;
        }
        if (itemList.size() > 1) {
            ItemData bestMatch = itemList.getFirst();
            for (ItemData duplicate : itemList) {
                Util.notifyAll("Duplicate item: " + duplicate.getGeneralInfo(), Util.notificationTypes.ITEMDATA);
                if (volume == null) {
                    continue;
                }
                if (Math.abs(duplicate.getVolume() - volume) < Math.abs(bestMatch.getVolume() - volume)) {
                    bestMatch = duplicate;
                }
            }
            return bestMatch;
        }

        return itemList.getFirst();
    }
    public static ItemData findItem(ItemData matchingItem, List<ItemData> list) {
        String name = matchingItem.getName();
        double price = matchingItem.getPrice();
        int volume = matchingItem.getVolume();
        ItemData.priceTypes priceType = matchingItem.getPriceType();
        ArrayList<ItemData> itemList = new ArrayList<>();
        for(ItemData item : list){
            if(item.isSimilarPrice(price) &&
                    item.getVolume() == volume &&
                    name.equalsIgnoreCase(item.getName()) &&
                    priceType == item.getPriceType()){
                itemList.add(item);
            }
        }
        if (itemList.isEmpty()) {
            return null;
        }
        if (itemList.size() > 1) {
            itemList.forEach(duplicate -> {
                Util.notifyAll("Duplicate item: " + duplicate.getGeneralInfo(), Util.notificationTypes.ITEMDATA);
            });
        }
        return itemList.getFirst();
    }

    private static void findOutdated() {
        List<ItemData> previousOutdatedItems = new ArrayList<>(outdated);
        outdated.clear();
        for (ItemData item : BUConfig.get().watchedItems) {
            if (item.isOutdated()) {
                outdated.add(item);
            }
        }

        if (outdated.isEmpty()) {
            Util.notifyAll("No outdated items found.", Util.notificationTypes.ITEMDATA);
            return;
        }

        List<ItemData> availableOldOutdated = new ArrayList<>(previousOutdatedItems);

        for (ItemData currentNewOutdatedItem : outdated) {
            boolean foundMatchInOldList = false;
            ItemData matchedOldItem = null;

            for (ItemData oldItem : availableOldOutdated) {
                if (currentNewOutdatedItem.getName().equals(oldItem.getName()) &&
                        Math.abs(currentNewOutdatedItem.getPrice() - oldItem.getPrice()) <= currentNewOutdatedItem.maximumRounding &&
                        currentNewOutdatedItem.getVolume() == oldItem.getVolume() &&
                        currentNewOutdatedItem.getPriceType() == oldItem.getPriceType()) {
                    foundMatchInOldList = true;
                    matchedOldItem = oldItem;
                    break;
                }
            }

            if (foundMatchInOldList) {
                availableOldOutdated.remove(matchedOldItem);
            } else {
                BazaarUtils.eventBus.post(new OutdatedItemEvent(currentNewOutdatedItem));
            }
        }

        for (ItemData noLongerOutdatedItem : availableOldOutdated) {
            if (BUConfig.get().watchedItems.contains(noLongerOutdatedItem) && noLongerOutdatedItem.getStatus() != statuses.FILLED) {
                Text amount = Text.literal(noLongerOutdatedItem.getVolume() + "x ").formatted(Formatting.BOLD).formatted(Formatting.DARK_PURPLE);
                Text itemName = Text.literal(noLongerOutdatedItem.getName().formatted(Formatting.BOLD).formatted(Formatting.GOLD));
                MutableText message = Text.literal("[Bazaar Utils] ").formatted(Formatting.GOLD)
                        .append(Text.literal("Your " + noLongerOutdatedItem.getPriceType().getString() + " for ").formatted(Formatting.WHITE))
                        .append(amount)
                        .append(itemName)
                        .append(Text.literal( " is no longer outdated.").formatted(Formatting.WHITE));
                if(MinecraftClient.getInstance().player != null)
                    MinecraftClient.getInstance().player.sendMessage(message, false);
                else
                    Util.notifyError("Could not send no longer outdated notif because player is null.", null);
            }
        }

    }

    private boolean isOutdated(){
        if(status == statuses.FILLED)
            return false;
        if (priceType == priceTypes.INSTABUY) {
            return this.price-maximumRounding > this.marketPrice;
        } else {
            return this.price+maximumRounding < this.marketPrice;
        }
    }

    public double getFlipPrice(){
        updateMarketPrices();
        if(marketOppositePrice == 0)
            return 0;
        if (priceType == priceTypes.INSTABUY) {
            return (marketOppositePrice + .1);
        } else {
            return (marketOppositePrice - .1);
        }
    }

    public void setFilled(){
        amountFilled = volume;
        status = statuses.FILLED;
    }

    public static void removeFromWatchedItems(ItemData item){
        BUConfig.get().watchedItems.remove(item);
        BUConfig.HANDLER.save();
        ItemData.update();
    }
    public void removeFromWatchedItems(){
        if(!BUConfig.get().watchedItems.remove(this))
            Util.notifyAll("Error removing " + name + " from watched items. Item couldn't be found.");
        BUConfig.HANDLER.save();
        ItemData.update();
    }
}
