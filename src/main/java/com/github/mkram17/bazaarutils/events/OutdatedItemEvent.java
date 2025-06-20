package com.github.mkram17.bazaarutils.events;

import com.github.mkram17.bazaarutils.misc.ItemData;

/**
 * Event fired when a watched Bazaar order becomes “outdated”.  
 * <p>
 * The event is <em>cancellable</em>: listeners may call
 * {@link #setCancelled(boolean)} to stop further processing.
 */
public class OutdatedItemEvent implements Cancellable {

    /* ------------------------------------------------------------------
       immutable event data
       ------------------------------------------------------------------ */
    private final ItemData item;

    /* ------------------------------------------------------------------
       ctor
       ------------------------------------------------------------------ */
    public OutdatedItemEvent(ItemData item) {
        this.item = item;
    }

    /* ------------------------------------------------------------------
       public API
       ------------------------------------------------------------------ */
    /** @return the order that has gone stale */
    public ItemData getItem() {
        return item;
    }

    /* ------------------------------------------------------------------
       Cancellable implementation
       ------------------------------------------------------------------ */
    private boolean cancelled = false;

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }
}

/* ======================================================================
   Helper interface (kept package-private, so no extra file is required)
   ====================================================================== */
interface Cancellable {
    void setCancelled(boolean cancel);
    boolean isCancelled();
}
