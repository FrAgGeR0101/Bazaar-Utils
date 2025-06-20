package com.github.mkram17.bazaarutils.events;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;

/**
 * Simple POJO representing a slot-click inside any handled screen.
 * <p>
 * Designed for Forge 1.8.9: no Lombok, no Orbit, no Fabric classes.
 */
public class SlotClickEvent {

    /* ------------------------------------------------------------------
       Click types (small superset of what the mod actually uses)
       ------------------------------------------------------------------ */
    public enum SlotActionType {
        PICKUP,
        QUICK_MOVE,
        CLONE,
        THROW,
        QUICK_CRAFT,
        SWAP,
        PICKUP_ALL
    }

    /* ------------------------------------------------------------------
       Immutable context data
       ------------------------------------------------------------------ */
    private final GuiContainer   handledScreen;
    private final Slot           slot;
    private final int            slotId;
    private final int            clickedButton;
    private final SlotActionType clickType;

    /* ------------------------------------------------------------------
       Mutable flags
       ------------------------------------------------------------------ */
    private boolean usePickblockInstead = false;
    private boolean cancelled           = false;

    /* ------------------------------------------------------------------
       Ctor
       ------------------------------------------------------------------ */
    public SlotClickEvent(GuiContainer screen,
                          Slot slot,
                          int slotId,
                          int clickedButton,
                          SlotActionType clickType) {

        this.handledScreen  = screen;
        this.slot           = slot;
        this.slotId         = slotId;
        this.clickedButton  = clickedButton;
        this.clickType      = clickType;
    }

    /* ------------------------------------------------------------------
       Accessors
       ------------------------------------------------------------------ */
    public GuiContainer   getHandledScreen() { return handledScreen; }
    public Slot           getSlot()          { return slot; }
    public int            getSlotId()        { return slotId; }
    public int            getClickedButton() { return clickedButton; }
    public SlotActionType getClickType()     { return clickType; }

    public boolean isUsePickblockInstead()   { return usePickblockInstead; }
    public boolean isCancelled()             { return cancelled; }

    /* ------------------------------------------------------------------
       Mutators
       ------------------------------------------------------------------ */
    /** Instructs calling code to send a PICKBLOCK packet instead. */
    public void usePickblockInstead() {
        this.usePickblockInstead = true;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
