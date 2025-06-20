package com.github.mkram17.bazaarutils.events;

/**
 * Event object posted (manually) when the vanilla sign-editing screen opens.
 *
 * <p>The concrete screen class differs between mappings / versions
 * ( Fabric · Forge · MCP, 1.8.9 ↔ 1.20.x, etc.).  
 * To avoid compile-time breakage we store it as a plain {@code Object}.</p>
 *
 * <p>Other code that actually needs to interact with the screen should
 * <code>instanceof</code>–check and cast accordingly.</p>
 */
public final class SignOpenEvent {

    /* ------------------------------------------------------------------
       Immutable context
       ------------------------------------------------------------------ */
    private final Object signScreen;   // vanilla sign GUI – kept generic

    /* ------------------------------------------------------------------
       Mutable “cancelled” flag
       ------------------------------------------------------------------ */
    private boolean cancelled = false;

    /* ------------------------------------------------------------------
       Ctor
       ------------------------------------------------------------------ */
    public SignOpenEvent(Object screen) {
        this.signScreen = screen;
    }

    /* ------------------------------------------------------------------
       Accessors
       ------------------------------------------------------------------ */
    /** Return the raw sign-editing screen instance (cast as needed). */
    public Object getSignScreen() {
        return signScreen;
    }

    /** Mark the event as cancelled so listeners can suppress default behaviour. */
    public void setCancelled(boolean flag) {
        this.cancelled = flag;
    }

    /** @return {@code true} if some listener cancelled the event */
    public boolean isCancelled() {
        return cancelled;
    }
}
