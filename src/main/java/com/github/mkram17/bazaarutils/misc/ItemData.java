/* … import section unchanged … */

public final class ItemData {

    /* ───────────────────────── enums ───────────────────────── */
    /* (PriceType / Status remain unchanged)                     */

    /* ───────────────────── data fields ─────────────────────── */
    /* … fields unchanged … */

    /* ───────────────────── constructor ─────────────────────── */
    /* … ctor unchanged … */

    /* ───────────────────── getters / setters … ─────────────── */
    /* … unchanged … */

    /* ───────────── legacy helpers moved inside class ───────── */

    /** flip from buy- to sell-side (or vice-versa) and reset progress */
    public void flip(double newUnitPrice) {
        priceType    = priceType.getOpposite();
        price        = newUnitPrice;
        amountFilled = 0;
        status       = Status.SET;
    }

    /** historical static search used by ItemUpdater / FlipHelper */
    public static ItemData findItem(ItemData probe, List<ItemData> list) {
        if (probe == null) return null;
        for (ItemData d : list) {
            if (d.name.equalsIgnoreCase(probe.name) &&
                d.priceType == probe.priceType         &&
                d.volume    == probe.volume)
                return d;
        }
        return null;
    }

    /** public array replica referenced by a few old files */
    public static final Status[] statuses = Status.values();

    /* ───────────── helper logic / update routines ──────────── */
    /* keep the rest of the original class (refreshMarketPrices,
       detectOutdated, etc.) exactly as it already is.           */
}
