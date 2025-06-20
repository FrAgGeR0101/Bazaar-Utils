package com.github.mkram17.bazaarutils.events;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

/**
 * Event fired right before an item stack is rendered in a slot so other
 * parts of the mod may replace it with a custom stack.
 *
 * <p>This class is only a plain data-holder – post it on
 * {@code BazaarUtils.eventBus} and let listeners modify
 * {@link #replacement} or set {@link #cancelled}.</p>
 */
public class ReplaceItemEvent {

    /* ------------------------------------------------------------------
       immutable event data
       ------------------------------------------------------------------ */
    private final ItemStack  original;     // stack currently inside the slot
    private final IInventory inventory;    // parent container
    private final int        slotId;       // slot index (0-44 etc.)

    /* ------------------------------------------------------------------
       mutable part
       ------------------------------------------------------------------ */
    private ItemStack replacement;         // what should be rendered instead
    private boolean   cancelled;           // listener may cancel further use

    /* ------------------------------------------------------------------
       ctor
       ------------------------------------------------------------------ */
    public ReplaceItemEvent(ItemStack original,
                            IInventory inventory,
                            int slotId) {
        this.original    = original;
        this.inventory   = inventory;
        this.slotId      = slotId;
        this.replacement = original;   // default: keep the original
    }

    /* ------------------------------------------------------------------
       getters / setters
       ------------------------------------------------------------------ */
    public ItemStack  getOriginal()   { return original; }
    public IInventory getInventory()  { return inventory; }
    public int        getSlotId()     { return slotId;   }

    public ItemStack  getReplacement(){ return replacement; }
    public void       setReplacement(ItemStack stack) {
        this.replacement = stack;
    }

    public boolean isCancelled()      { return cancelled; }
    public void    setCancelled(boolean c) { this.cancelled = c; }
}
