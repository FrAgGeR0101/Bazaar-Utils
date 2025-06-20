package com.github.mkram17.bazaarutils.misc;

import com.github.mkram17.bazaarutils.BazaarUtils;
import com.github.mkram17.bazaarutils.config.BUConfig;
import com.github.mkram17.bazaarutils.data.BazaarData;
import com.github.mkram17.bazaarutils.events.OutdatedItemEvent;
import com.github.mkram17.bazaarutils.utils.Util;

import java.util.*;
import java.util.function.Function;

/**
 * In-memory representation of one watched Bazaar order / item.
 * Pure Forge 1.8.9 – no Fabric / Lombok.
 */
public final class ItemData {

    /* ───────────────────── enums ───────────────────── */

    /** BUY- / SELL-side discriminator */
    public enum PriceType {
        INSTASELL,   // we own items – we sell to existing buy orders
        INSTABUY;    // we pay instantly – we receive items immediately

        /* cached opposite for quick flip() */
        private PriceType opposite;

        static {                 // one-time init block
            INSTASELL.opposite = INSTABUY;
            INSTABUY.opposite  = INSTASELL;
        }
        public PriceType getOpposite() { return opposite; }

        public String getHumanName() {
            return this == INSTASELL ? "buy order" : "sell order";
        }
    }

    /** Simple life-cycle state */
    public enum Status { SET, FILLED }

    /* ───────────────── data fields ───────────────── */

    private final String   name;
    private final String   productId;
    private final int      volume;           // total amount placed

    private double    price;                 // unit price we placed
    private PriceType priceType;
    private Status    status = Status.SET;

    /* live market data – updated by update() */
    private double marketPrice;
    private double marketOppositePrice;
    private double maximumRounding;          // Hypixel rounding quirks

    /* progress tracking */
    private int amountClaimed = 0;
    private int amountFilled  = 0;

    /* thread-safe container with *currently* outdated items */
    private static final List<ItemData> OUTDATED =
            Collections.synchronizedList(new ArrayList<>());

    /* ─────────────── constructor ─────────────── */

    public ItemData(String naturalName,
                    double totalCoins,       // full price paid/received
                    PriceType side,
                    int vol) {

        this.name       = naturalName;
        this.volume     = vol;
        this.priceType  = side;
        this.productId  = BazaarData.findProductId(naturalName);

        this.price            = Math.round((totalCoins / vol) * 100) / 100.0;
        this.maximumRounding  = calcMaxRounding(totalCoins, vol);

        if (productId == null) {
            Util.notifyAll("Unknown product id for “" + naturalName + "”.",
                           Util.notificationTypes.ITEMDATA);
        }
    }

    /* ─────────────── getters ⸺ setters ─────────────── */

    public String    getName()          { return name; }
    public String    getProductId()     { return productId; }
    public int       getVolume()        { return volume; }
    public double    getPrice()         { return price; }
    public PriceType getPriceType()     { return priceType; }
    public Status    getStatus()        { return status; }
    public int       getAmountClaimed() { return amountClaimed; }
    public int       getAmountFilled()  { return amountFilled; }
    public double    getMarketPrice()   { return marketPrice; }
    public double    getMaximumRounding(){ return maximumRounding; }

    public void setPrice        (double p)        { price = p; }
    public void setPriceType    (PriceType pt)    { priceType = pt; }
    public void setStatus       (Status s)        { status = s; }
    public void setAmountClaimed(int v)           { amountClaimed = v; }
    public void setAmountFilled (int v)           { amountFilled  = v; }
    public void setMaximumRounding(double r)      { maximumRounding = r; }

    /* ─────────────── legacy helpers ─────────────── */

    /** Flip from buy- to sell-side (or vice-versa) and reset progress. */
    public void flip(double newUnitPrice) {
        priceType    = priceType.getOpposite();
        price        = newUnitPrice;
        amountFilled = 0;
        status       = Status.SET;
    }

    /** Historical static search helper still used by ItemUpdater. */
    public static ItemData findItem(ItemData probe, List<ItemData> list) {
        if (probe == null) return null;
        for (ItemData d : list) {
            if (d.name.equalsIgnoreCase(probe.name) &&
                d.priceType == probe.priceType &&
                d.volume    == probe.volume)
                return d;
        }
        return null;
    }

    /** Public replica of Status.values() – referenced by very old code. */
    public static final Status[] statuses = Status.values();

    /* ─────────────── misc helpers ─────────────── */

    public String getGeneralInfo() {
        return "(name:" + name + '[' + getIndex() + "], price:" + price +
               ", vol:" + volume + ", type:" + priceType +
               (amountClaimed != 0 ? ", claimed:" + amountClaimed : "") +
               (status == Status.FILLED ? ", FILLED" : "") + ')';
    }

    /** index inside persisted list (-1 = not present) */
    public int getIndex() {
        return BUConfig.get().getWatchedItems().indexOf(this);
    }

    private static double calcMaxRounding(double total, int vol) {
        return total < 10_000 ? 0 : Math.ceil((0.9 / vol) * 10) / 10.0;
    }

    public boolean isSimilarPrice(double other) {
        return Math.abs(price - other) <= maximumRounding;
    }

    /* ───────────── periodic updates ───────────── */

    public static void update() {
        refreshMarketPrices();
        detectOutdated();
    }

    private static void refreshMarketPrices() {
        for (ItemData it : BUConfig.get().getWatchedItems()) {
            double old = it.marketPrice;

            it.marketPrice = Util.pretty(
                    BazaarData.findItemPrice(it.productId, it.priceType));
            it.marketOppositePrice = Util.pretty(
                    BazaarData.findItemPrice(it.productId,
                                             it.priceType.getOpposite()));

            if (old != it.marketPrice) {
                Util.notifyAll(it.getGeneralInfo() +
                               " → new market " + it.marketPrice,
                               Util.notificationTypes.BAZAARDATA);
            }
        }
    }

    /* ───────────── outdated-item detection ───────────── */

    public boolean isOutdated() {
        if (status == Status.FILLED) return false;
        return priceType == PriceType.INSTABUY
               ? price - maximumRounding > marketPrice
               : price + maximumRounding < marketPrice;
    }

    private static void detectOutdated() {

        List<ItemData> previous = new ArrayList<>(OUTDATED);
        OUTDATED.clear();

        BUConfig.get().getWatchedItems().stream()
                 .filter(ItemData::isOutdated)
                 .forEach(OUTDATED::add);

        if (OUTDATED.isEmpty()) return;

        List<ItemData> still = new ArrayList<>(previous);

        for (ItemData n : OUTDATED) {
            ItemData match = findIn(still, n);
            if (match != null) {
                still.remove(match);               // already reported
            } else {
                BazaarUtils.EVENT_BUS.post(new OutdatedItemEvent(n)); // new alert
            }
        }

        /* recovered items */
        for (ItemData rec : still) {
            if (BUConfig.get().getWatchedItems().contains(rec) &&
                rec.status != Status.FILLED) {

                Util.notifyAll(rec.volume + "× " + rec.name +
                               " (" + rec.priceType.getHumanName() +
                               ") is no longer outdated.",
                               Util.notificationTypes.ITEMDATA);
            }
        }
    }

    /* ───────────── search helpers ───────────── */

    private static ItemData findIn(List<ItemData> list, ItemData probe) {
        return list.stream().filter(d ->
            d.isSimilarPrice(probe.price) &&
            d.volume == probe.volume &&
            d.name.equalsIgnoreCase(probe.name) &&
            d.priceType == probe.priceType).findFirst().orElse(null);
    }

    public static <T> List<T> getVariables(Function<ItemData, T> map) {
        List<T> out = new ArrayList<>();
        BUConfig.get().getWatchedItems().forEach(i -> out.add(map.apply(i)));
        return out;
    }

    /* ───────────── list-management helpers ───────────── */

    public void removeFromWatchedItems() { removeFromWatchedItems(this); }

    public static void removeFromWatchedItems(ItemData it) {
        BUConfig.get().getWatchedItems().remove(it);
        BUConfig.HANDLER.save();
        update();
    }
}
