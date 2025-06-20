package com.github.mkram17.bazaarutils.misc;

import com.github.mkram17.bazaarutils.BazaarUtils;
import com.github.mkram17.bazaarutils.config.BUConfig;
import com.github.mkram17.bazaarutils.data.BazaarData;
import com.github.mkram17.bazaarutils.events.OutdatedItemEvent;
import com.github.mkram17.bazaarutils.utils.Util;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.apache.logging.log4j.LogManager;

import java.util.*;
import java.util.function.Function;

/**
 * In-memory representation of one watched item / order.
 * Completely Lombok-free version for 1.20.4.
 */
public class ItemData {

    /* ───────────────────────── enums ───────────────────────── */

    /** BUY-/SELL side of the market */
    public enum PriceType {
        INSTASELL,   // we own items → selling to buy orders
        INSTABUY;    // we pay instantly → receiving items

        private PriceType opposite;
        static {
            INSTASELL.opposite = INSTABUY;
            INSTABUY.opposite  = INSTASELL;
        }
        public PriceType getOpposite()     { return opposite; }
        public String    getString()       { return this == INSTASELL ? "buy order" : "sell order"; }
    }

    /** Order status */
    public enum Status { SET, FILLED }

    /* ───────────────────── instance fields ─────────────────── */

    private final String  name;
    private final String  productId;
    private final int     volume;

    private double    price;          // unit price
    private PriceType priceType;
    private Status    status = Status.SET;

    private double marketPrice;           // best price on our side
    private double marketOppositePrice;   // best price on opposite side
    private double maximumRounding;       // tolerance for rounding errors

    private int amountClaimed = 0;
    private int amountFilled  = 0;

    /* outdated-item tracking (static, thread-safe) */
    private static final List<ItemData> OUTDATED = Collections.synchronizedList(new ArrayList<>());

    /* ───────────────────────── ctor ────────────────────────── */

    public ItemData(String name, double fullPrice, PriceType priceType, int volume) {
        this.name      = name;
        this.priceType = priceType;
        this.productId = BazaarData.findProductId(name);
        this.volume    = volume;
        this.price     = Math.round((fullPrice / volume) * 100) / 100.0;
        this.maximumRounding = calcMaxRounding(fullPrice, volume);

        if (productId == null) {
            Util.notifyAll("Could not find product-id for " + name, Util.NotificationType.ITEMDATA);
        }
    }

    /* ───────────────── getters / setters ───────────────────── */

    public String   getName()               { return name; }
    public String   getProductId()          { return productId; }
    public int      getVolume()             { return volume; }
    public double   getPrice()              { return price; }
    public void     setPrice(double p)      { price = p; }
    public PriceType getPriceType()         { return priceType; }
    public void     setPriceType(PriceType t){ priceType = t; }
    public Status   getStatus()             { return status; }
    public void     setStatus(Status s)     { status = s; }

    public int      getAmountClaimed()      { return amountClaimed; }
    public void     setAmountClaimed(int v) { amountClaimed = v; }
    public int      getAmountFilled()       { return amountFilled; }
    public void     setAmountFilled(int v)  { amountFilled = v; }

    public double   getMarketPrice()        { return marketPrice; }
    public double   getMaxRounding()        { return maximumRounding; }

    /* ───────────────── small helpers ───────────────────────── */

    public int getIndex() { return BUConfig.get().watchedItems.indexOf(this); }

    public String getGeneralInfo() {
        return "(name: " + name + "[" + getIndex() + "], price: " + price +
               ", volume: " + volume + ", type: " + priceType +
               (amountClaimed != 0 ? ", claimed: " + amountClaimed : "") +
               (status == Status.FILLED ? ", status: FILLED" : "") + ")";
    }

    private static double calcMaxRounding(double full, int vol) {
        return full < 10_000 ? 0 : Math.ceil((0.9 / vol) * 10) / 10.0;
    }

    public boolean isSimilarPrice(double other) {
        return Math.abs(price - other) <= maximumRounding;
    }

    /* ──────────────── periodic update hooks ────────────────── */

    public static void update() {
        updateMarketPrices();
        identifyOutdated();
    }

    private static void updateMarketPrices() {
        for (ItemData it : BUConfig.get().watchedItems) {
            double old = it.marketPrice;
            it.marketPrice = Util.pretty(
                    BazaarData.findItemPrice(it.productId, it.priceType));
            it.marketOppositePrice = Util.pretty(
                    BazaarData.findItemPrice(it.productId, it.priceType.getOpposite()));

            if (old != it.marketPrice) {
                Util.notifyAll(it.getGeneralInfo() + " → new market: " + it.marketPrice,
                               Util.NotificationType.BAZAARDATA);
            }
        }
    }

    /* ───────────────── state changes ───────────────────────── */

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

    /* ──────────────── outdated-item logic ─────────────────── */

    public boolean isOutdated() {
        if (status == Status.FILLED) return false;
        return priceType == PriceType.INSTABUY
                ? price - maximumRounding > marketPrice
                : price + maximumRounding < marketPrice;
    }

    private static void identifyOutdated() {
        List<ItemData> previous = new ArrayList<>(OUTDATED);
        OUTDATED.clear();

        BUConfig.get().watchedItems.stream()
                                   .filter(ItemData::isOutdated)
                                   .forEach(OUTDATED::add);

        if (OUTDATED.isEmpty()) return;

        /* Check new vs old list */
        List<ItemData> oldRemaining = new ArrayList<>(previous);

        for (ItemData n : OUTDATED) {
            ItemData match = findInList(n, oldRemaining);
            if (match != null) {
                oldRemaining.remove(match);          // already alerted
            } else {
                BazaarUtils.eventBus.post(new OutdatedItemEvent(n));  // new alert
            }
        }

        /* Items no longer outdated */
        for (ItemData recovered : oldRemaining) {
            if (BUConfig.get().watchedItems.contains(recovered) &&
                recovered.status != Status.FILLED) {

                Text msg = Text.literal("[Bazaar Utils] ")
                               .formatted(Formatting.GOLD)
                               .append(Text.literal("Your " + recovered.priceType.getString() +
                               " for ").formatted(Formatting.WHITE))
                               .append(Text.literal(recovered.volume + "x ")
                                       .formatted(Formatting.BOLD, Formatting.DARK_PURPLE))
                               .append(Text.literal(recovered.name)
                                       .formatted(Formatting.BOLD, Formatting.GOLD))
                               .append(Text.literal(" is no longer outdated.")
                                       .formatted(Formatting.WHITE));

                MinecraftClient client = MinecraftClient.getInstance();
                if (client.player != null) client.player.sendMessage(msg, false);
            }
        }
    }

    /* ─────────────── generic search helpers ───────────────── */

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

    /* ───────────── static removal helpers ─────────────────── */

    public void removeFromWatchedItems() {
        removeFromWatchedItems(this);
    }

    public static void removeFromWatchedItems(ItemData it) {
        BUConfig.get().watchedItems.remove(it);
        BUConfig.HANDLER.save();
        update();
    }

    /* ────────────────── debug log helper ───────────────────── */

    private static void debug(String msg) {
        LogManager.getLogger(ItemData.class).debug(msg);
    }
}
