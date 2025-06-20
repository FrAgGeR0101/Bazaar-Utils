package com.github.mkram17.bazaarutils.events;

import net.minecraft.client.gui.GuiEditSign;

/**
 * Fired whenever the vanilla sign–editing GUI is opened.
 *
 * <p>No Lombok, no Orbit, no Fabric – just a tiny, self-contained POJO
 * that other parts of the mod can populate / query.</p>
 */
public final class SignOpenEvent {

    /* ------------------------------------------------------------------
       Immutable context
       ------------------------------------------------------------------ */
    private final GuiEditSign signGui;

    /* ------------------------------------------------------------------
       Mutable “cancelled” flag
       ------------------------------------------------------------------ */
    private boolean cancelled = false;

    /* ------------------------------------------------------------------
       Ctor
       ------------------------------------------------------------------ */
    public SignOpenEvent(GuiEditSign signGui) {
        this.signGui = signGui;
    }

    /* ------------------------------------------------------------------
       Accessors
       ------------------------------------------------------------------ */
    public GuiEditSign getSignGui() {
        return signGui;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
