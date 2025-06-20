package com.github.mkram17.bazaarutils.events;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;

/**
 * Fired when a chest / generic container GUI is fully populated.
 *
 * <p><b>How to use on 1.8.9:</b><br>
 * Forge does not have Fabric’s {@code ScreenEvents.AFTER_INIT}.  Build and
 * post this event from the point where you finish constructing your GUI:</p>
 *
 * <pre>{@code
 * ChestLoadedEvent ev = new ChestLoadedEvent();
 * ev.setLowerChestInventory(inventory);
 * ev.getItemStacks().addAll(collectedStacks);
 * ev.setContainerName(guiTitle);
 * BazaarUtils.eventBus.post(ev);
 * }</pre>
 */
public class ChestLoadedEvent implements BUListener {

    /* ------------------------------------------------------------------
     *  Event data
     * ------------------------------------------------------------------ */

    /** Vanilla lower-inventory reference (kept as {@code Object} for parity) */
    private Object lowerChestInventory;

    /** Immutable list reference – add stacks with {@link #getItemStacks()} */
    private final List<ItemStack> itemStacks = new ArrayList<>();

    /** The translated title of the open container */
    private String containerName = "";

    /* ------------------------------------------------------------------
     *  Getters & setters
     * ------------------------------------------------------------------ */

    public Object getLowerChestInventory()      { return lowerChestInventory; }
    public void   setLowerChestInventory(Object inv) { this.lowerChestInventory = inv; }

    /** Direct access: {@code getItemStacks().add(stack);} */
    public List<ItemStack> getItemStacks()      { return itemStacks; }

    public String getContainerName()            { return containerName; }
    public void   setContainerName(String name) { this.containerName = name; }

    /* ------------------------------------------------------------------
     *  Convenience helpers
     * ------------------------------------------------------------------ */

    /** @return {@code true} when opened screen is the “Order options” menu. */
    public boolean inFlipMenu() {
        return containerName != null && containerName.contains("Order options");
    }

    /* ------------------------------------------------------------------
     *  Cancel support (optional – Orbit–free)
     * ------------------------------------------------------------------ */

    private boolean cancelled = false;

    /** Mark the event as cancelled inside your own handlers. */
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }

    /** @return {@code true} if some handler cancelled this event. */
    public boolean isCancelled()               { return cancelled; }

    /* ------------------------------------------------------------------
     *  BUListener implementation
     * ------------------------------------------------------------------ */

    @Override
    public void subscribe() {
        /* No automatic GUI hook on Forge-1.8.9.
           Post this event manually at the proper time (see class javadoc). */
    }
}
