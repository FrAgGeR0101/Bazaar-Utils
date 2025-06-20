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

/**
 * Represents a *single* watched item (or order) in Bazaar-Utils.
 */
@Slf4j
public class ItemData {

    /* ──────────────────────────────────────────────────────────────────
       ENUMS
       ────────────────────────────────────────────────────────────────── */

    /** BUY-order ⇄ SELL-order discriminator */
    public enum PriceType {
        INSTASELL,   // we placed an insta-sell ⇒ we own items, selling to a buy-order
        INSTABUY;    // we placed an insta-buy  ⇒ we pay instantly, receiving items

        private PriceType opposite;

        static {
            INSTASELL.opposite = INSTABUY;
            INSTABUY.opposite  = INSTASELL;
        }

        /** @return the opposite side of the market (buy ↔ sell) */
        public PriceType getOpposite() { return opposite; }

        /** Human-readable name used in chat messages */
        public String getString() {
            return (this == INSTASELL) ? "buy order" : "sell order";
        }
    }

    /** Per-order state machine */
    public enum Status { SET, FILLED }

    /* ──────────────────────────────────────────────────────────────────
       BASIC DATA
       ────────────────────────────────────────────────────────────────── */

    @Getter private final String  name;
    @Getter private final String  productId;      // internal Bazaar ID
    @Getter private final int     volume;

    @Getter @Setter private double     price;     // unit price we placed
    @Getter @Setter private PriceType  priceType; // insta-buy or insta-sell
    @Getter @Setter private Status     status;

    /** Lowest opposite-side price – used for flip suggestions */
    private double marketOppositePrice;
    /** Current best price on *our* side of the market */
    @Getter private double marketPrice;

    /* ────────── misc bookkeeping ────────── */
    @Getter @Setter private int    amountClaimed  = 0;
    @Getter @Setter private int    amountFilled   = 0;
    @Getter @Setter private double maximumRounding;

    /* ────────────────────────────────────────────────────────────────── */

    /** Thread-safe list containing the *current* outdated orders */
    @Getter private static final List<ItemData> outdated =
            new ArrayList<>();

    /* ──────────────────────────────────────────────────────────────────
       CTOR
       ────────────────────────────────────────────────────────────────── */

    public ItemData(String name,
                    double fullPrice,          // coin total = unit × volume
                    PriceType priceType,
                    int volume) {

        this.name       = name;
        this.priceType  = priceType;
        this.productId  = BazaarData.findProductId(name);
        this.volume     = volume;
        this.price      = Math.round((fullPrice / volume) * 100) / 100.0;
        this.status     = Status.SET;
        this.maximumRounding = calcMaxRounding(fullPrice, volume);

        if (productId == null) {
            Util.notifyAll("Could not find product id for item: " + name,
                           Util.notificationTypes.ITEMDATA);
        }
    }

    /* ──────────────────────────────────────────────────────────────────
       SMALL HELPERS
       ────────────────────────────────────────────────────────────────── */

    /** Index inside BUConfig list – useful for chat debug messages */
    public int getIndex() {
        return BUConfig.get().watchedItems.indexOf(this);
    }

    /** Compact human-readable description – only for debugging */
    public String getGeneralInfo() {
        StringBuilder sb = new StringBuilder("(name: ")
                .append(name).append("[").append(getIndex()).append("]")
                .append(", price: ").append(price)
                .append(", volume: ").append(volume);

        if (amountClaimed != 0) sb.append(", amount claimed: ").append(amountClaimed);
        sb.append(", type: ").append(priceType);
        if (status == Status.FILLED) sb.append(", status: FILLED");
        sb.append(")");
        return sb.toString();
    }

    private static double calcMaxRounding(double fullPrice, int volume) {
        // small orders rarely suffer rounding issues
        return (fullPrice < 10_000)
                ? 0
                : Math.ceil((0.9 / volume) * 10) / 10.0;
    }

    /** True when a given chat/GUI price is within rounding tolerance */
    public boolean isSimilarPrice(double other) {
        return Math.abs(price - other) <= maximumRounding;
    }

    /* ──────────────────────────────────────────────────────────────────
       STATIC UPDATE HELPERS
       ────────────────────────────────────────────────────────────────── */

    public static void update() {
        updateMarketPrices();
        findOutdated();
    }

    private static void updateMarketPrices() {
        for (ItemData item : BUConfig.get().watchedItems) {
            double old = item.marketPrice;
            item.marketPrice = Util.getPrettyNumber(
                    BazaarData.findItemPrice(item.productId, item.priceType));
            item.marketOppositePrice = Util.getPrettyNumber(
                    BazaarData.findItemPrice(item.productId,
                                             item.priceType.getOpposite()));

            if (old != item.marketPrice) {
                Util.notifyAll(item.getGeneralInfo() +
                               " has new market price: " + item.marketPrice,
                               Util.notificationTypes.BAZAARDATA);
            }
        }
    }

    /* ──────────────────────────────────────────────────────────────────
       MUTATORS
       ────────────────────────────────────────────────────────────────── */

    /** Flip an order to the opposite side at a new price */
    public void flipItem(double newUnitPrice) {
        this.priceType = priceType.getOpposite();
        this.price     = newUnitPrice;
        this.amountFilled = 0;
        this.status    = Status.SET;
    }

    /** Mark order as completely filled */
    public void setFilled() {
        this.amountFilled = volume;
        this.status       = Status.FILLED;
    }

    /* ──────────────────────────────────────────────────────────────────
       PRICE / MATCHING HELPERS
       ────────────────────────────────────────────────────────────────── */

    public double getFlipPrice() {
        updateMarketPrices(); // refresh once
        if (marketOppositePrice == 0) return 0;

        return (priceType == PriceType.INSTABUY)
                ? marketOppositePrice + 0.1
                : marketOppositePrice - 0.1;
    }

    public boolean isOutdated() {
        if (status == Status.FILLED) return false;

        if (priceType == PriceType.INSTABUY) {
            return price - maximumRounding > marketPrice;
        } else { // INSTASELL
            return price + maximumRounding < marketPrice;
        }
    }

    /* ──────────────────────────────────────────────────────────────────
       FIND/SEARCH HELPERS
       ────────────────────────────────────────────────────────────────── */

    public static <T> List<T> getVariables(Function<ItemData, T> fn) {
        List<T> list = new ArrayList<>(BUConfig.get().watchedItems.size());
        BUConfig.get().watchedItems.forEach(item -> list.add(fn.apply(item)));
        return list;
    }

    private static List<ItemData> findExactMatches(String name,
                                                   Double price,
                                                   Integer volume,
                                                   PriceType type) {
        List<ItemData> out = new ArrayList<>();
        for (ItemData item : BUConfig.get().watchedItems) {
            if ((price   == null || item.isSimilarPrice(price)) &&
                (volume  == null || Math.abs(item.volume - volume) <= 0.05 * volume) &&
                (name    == null || name.equalsIgnoreCase(item.name)) &&
                (type    == null || type == item.priceType))
                out.add(item);
        }
        return out;
    }

    private static List<ItemData> findLooseVolumeMatches(String name,
                                                         Double price,
                                                         Integer volume,
                                                         PriceType type) {
        List<ItemData> out = new ArrayList<>();
        for (ItemData item : BUConfig.get().watchedItems) {
            boolean volumeClose =
                    volume == null ||
                    Math.abs(item.volume - volume) <= 0.05 * volume ||
                    Math.abs(item.volume - item.amountClaimed - volume) <= 0.05 * volume;

            if ((price == null || item.isSimilarPrice(price)) &&
                volumeClose &&
                (name == null || name.equalsIgnoreCase(item.name)) &&
                (type == null || type == item.priceType))
                out.add(item);
        }
        return out;
    }

    /** Find the *best* matching item in the current watch-list */
    public static ItemData findItem(String name,
                                    Double price,
                                    Integer volume,
                                    PriceType type) {

        List<ItemData> matches = findExactMatches(name, price, volume, type);
        if (matches.isEmpty())
            matches = findLooseVolumeMatches(name, price, volume, type);

        if (matches.isEmpty()) {
            Util.notifyAll("Could not find item with info: [name: " + name +
                           ", price: " + price + ", volume: " + volume + "]",
                           Util.notificationTypes.ITEMDATA);
            return null;
        }
        if (matches.size() == 1) return matches.getFirst();

        /* multiple matches → choose the closest volume */
        ItemData best = matches.getFirst();
        for (ItemData d : matches) {
            Util.notifyAll("Duplicate item: " + d.getGeneralInfo(),
                           Util.notificationTypes.ITEMDATA);
            if (volume != null &&
                Math.abs(d.volume - volume) < Math.abs(best.volume - volume))
                best = d;
        }
        return best;
    }

    /** Search *within an arbitrary list* */
    public static ItemData findItem(ItemData probe, List<ItemData> list) {
        return list.stream()
                   .filter(d ->
                       d.isSimilarPrice(probe.price) &&
                       d.volume == probe.volume &&
                       d.name.equalsIgnoreCase(probe.name) &&
                       d.priceType == probe.priceType)
                   .findFirst()
                   .orElse(null);
    }

    /* ──────────────────────────────────────────────────────────────────
       OUTDATED-ITEM HANDLING
       ────────────────────────────────────────────────────────────────── */

    private static void findOutdated() {
        List<ItemData> previous = new ArrayList<>(outdated);
        outdated.clear();

        /* rebuild list */
        BUConfig.get().watchedItems.stream()
                .filter(ItemData::isOutdated)
                .forEach(outdated::add);

        if (outdated.isEmpty()) return;

        List<ItemData> stillOutdated = new ArrayList<>(previous);

        for (ItemData nowOutdated : outdated) {
            ItemData match = findItem(nowOutdated, stillOutdated);
            if (match != null) {
                stillOutdated.remove(match); // already reported earlier
            } else {
                // new outdated item → fire event
                BazaarUtils.eventBus.post(new OutdatedItemEvent(nowOutdated));
            }
        }

        /* items that disappeared from ‘outdated’ list */
        for (ItemData recovered : stillOutdated) {
            if (BUConfig.get().watchedItems.contains(recovered) &&
                recovered.status != Status.FILLED) {

                Text msg = Text.literal("[Bazaar Utils] ").formatted(Formatting.GOLD)
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

    /* ──────────────────────────────────────────────────────────────────
       UTILITIES
       ────────────────────────────────────────────────────────────────── */

    /** Remove this item from the watched-list and persist the config */
    public void removeFromWatchedItems() {
        BUConfig.get().watchedItems.remove(this);
        BUConfig.HANDLER.save();
        ItemData.update();
    }

    /** Static helper for external classes */
    public static void removeFromWatchedItems(ItemData item) {
        BUConfig.get().watchedItems.remove(item);
        BUConfig.HANDLER.save();
        ItemData.update();
    }
}
