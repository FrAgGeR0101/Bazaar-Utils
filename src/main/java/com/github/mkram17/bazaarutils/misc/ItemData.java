package com.github.mkram17.bazaarutils.misc;

import com.github.mkram17.bazaarutils.BazaarUtils;
import com.github.mkram17.bazaarutils.config.BUConfig;
import com.github.mkram17.bazaarutils.data.BazaarData;
import com.github.mkram17.bazaarutils.events.OutdatedItemEvent;
import com.github.mkram17.bazaarutils.utils.Util;

import java.util.*;
import java.util.function.Function;

/**
 * In-memory representation of a single watched order / item.
 * 100 % Lombok-free and using Minecraft-1.8.9 classes only.
 */
public class ItemData {

    /* ───────────────────────── enums ───────────────────────── */

    /** BUY- / SELL-side discriminator */
    public enum PriceType {
        INSTASELL,        // we own items → sell to existing buy orders
        INSTABUY;         // we pay instantly → receive items immediately

        private PriceType opposite;

        static {
            INSTASELL.opposite = INSTABUY;
            INSTABUY.opposite  = INSTASELL;
        }
        public PriceType getOpposite() { return opposite; }

        public String getHumanName() {
            return this == INSTASELL ? "buy order" : "sell order";
        }
    }

    /** Book-keeping state */
    public enum Status { SET, FILLED }

    /* ───────────────────── data fields ─────────────────────── */

    private final String   name;
    private final String   productId;
    private final int      volume;

    private double    price;          // unit price we placed
    private PriceType priceType;
    private Status    status          = Status.SET;

    /* live market data (refreshed via update()) */
    private double marketPrice;
    private double marketOppositePrice;
    private double maximumRounding;   // tolerance for Hypixel rounding quirks

    /* progress-tracking */
    private int amountClaimed = 0;
    private int amountFilled  = 0;

    /* global list of currently outdated items (thread-safe) */
    private static final List<ItemData> OUTDATED =
            Collections.synchronizedList(new ArrayList<>());

    /* ───────────────────── constructor ─────────────────────── */

    public ItemData(String name,
                    double fullPrice,      // total coins (unit × volume)
                    PriceType priceType,
                    int volume) {

        this.name      = name;
        this.priceType = priceType;
        this.productId = BazaarData.findProductId(name);
        this.volume    = volume;

        this.price     = Math.round((fullPrice / volume) * 100) / 100.0;
        this.maximumRounding = calcMaxRounding(fullPrice, volume);

        if (productId == null) {
            Util.notifyAll("Could not find product id for “" + name + "”.",
                           Util.notificationTypes.ITEMDATA);
        }
    }

    /* ───────────────────── getters ─────────────────────────── */

    public String    getName()         { return name; }
    public String    getProductId()    { return productId; }
    public int       getVolume()       { return volume; }
    public double    getPrice()        { return price; }
    public PriceType getPriceType()    { return priceType; }
    public Status    getStatus()       { return status; }
    public int       getAmountClaimed(){ return amountClaimed; }
    public int       getAmountFilled() { return amountFilled; }
    public double    getMarketPrice()  { return marketPrice; }

    /* ──────────────────── mutators ─────────────────────────── */

    public void setPrice(double p)        { price = p; }
    public void setPriceType(PriceType t) { priceType = t; }
    public void setStatus(Status s)       { status = s; }
    public void setAmountClaimed(int v)   { amountClaimed = v; }
    public void setAmountFilled(int v)    { amountFilled  = v; }

    /* ─────────────────── helpers ───────────────────────────── */

    public String getGeneralInfo() {
        return "(name: " + name + "[" + getIndex() + "], price: " + price +
               ", vol: " + volume + ", type: " + priceType +
               (amountClaimed != 0 ? ", claimed: " + amountClaimed : "") +
               (status == Status.FILLED ? ", FILLED" : "") + ")";
    }

    public int getIndex() {
        return BUConfig.get().watchedItems.indexOf(this);
    }

    private static double calcMaxRounding(double full, int vol) {
        return full < 10_000 ? 0 : Math.ceil((0.9 / vol) * 10) / 10.0;
    }

    public boolean isSimilarPrice(double other) {
        return Math.abs(price - other) <= maximumRounding;
    }

    /* ──────────────── periodic updates ─────────────────────── */

    public static void update() {
        refreshMarketPrices();
        identifyOutdated();
    }

    private static void refreshMarketPrices() {
        for (ItemData it : BUConfig.get().watchedItems) {
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

    /* ─────────────── state-change helpers ──────────────────── */

    /** flip to opposite side at a new unit-price */
    public void flip(double newUnitPrice) {
        priceType = priceType.getOpposite();
        price     = newUnitPrice;
        amountFilled = 0;
        status    = Status.SET;
    }

    public void markFilled() {
        amountFilled = volume;
        status = Status.FILLED;
    }

    /* ───────────── outdated-item detection ─────────────────── */

    public boolean isOutdated() {
        if (status == Status.FILLED) return false;
        return priceType == PriceType.INSTABUY
                ? price - maximumRounding > marketPrice
                : price + maximumRounding < marketPrice;
    }

    private static void identifyOutdated() {
        List<ItemData> previous = new ArrayList<>(OUTDATED);
        OUTDATED.clear();

        // Rebuild list
        BUConfig.get().watchedItems.stream()
                                   .filter(ItemData::isOutdated)
                                   .forEach(OUTDATED::add);

        if (OUTDATED.isEmpty()) return;

        List<ItemData> stillOutdated = new ArrayList<>(previous);

        for (ItemData n : OUTDATED) {
            ItemData match = findInList(n, stillOutdated);
            if (match != null) {
                stillOutdated.remove(match); // already reported before
            } else {
                BazaarUtils.eventBus.post(new OutdatedItemEvent(n)); // new alert
            }
        }

        // Items that recovered
        for (ItemData recovered : stillOutdated) {
            if (BUConfig.get().watchedItems.contains(recovered) &&
                recovered.status != Status.FILLED) {

                Util.notifyAll(recovered.getVolume() + "× " + recovered.getName() +
                               " (" + recovered.getPriceType().getHumanName() +
                               ") is no longer outdated.",
                               Util.notificationTypes.ITEMDATA);
            }
        }
    }

    /* ───────────── generic search helpers ──────────────────── */

    private static ItemData findInList(ItemData probe, List<ItemData> list) {
        return list.stream().filter(d ->
                d.isSimilarPrice(probe.price) &&
                d.volume == probe.volume &&
                d.name.equalsIgnoreCase(probe.name) &&
                d.priceType == probe.priceType).findFirst().orElse(null);
    }

    public static <T> List<T> getVariables(Function<ItemData, T> mapper) {
        List<T> out = new ArrayList<>();
        BUConfig.get().watchedItems.forEach(i -> out.add(mapper.apply(i)));
        return out;
    }

    /* ───────────── list-management helpers ─────────────────── */

    public void removeFromWatchedItems() { removeFromWatchedItems(this); }

    public static void removeFromWatchedItems(ItemData it) {
        BUConfig.get().watchedItems.remove(it);
        BUConfig.HANDLER.save();
        update();
    }
}
