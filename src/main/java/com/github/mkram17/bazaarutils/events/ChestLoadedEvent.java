package com.github.mkram17.bazaarutils.events;

import lombok.Getter;
import meteordevelopment.orbit.ICancellable;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Forge-1.8.9 version of ChestLoadedEvent.
 *
 * The original Fabric callback (ScreenEvents.AFTER_INIT, GenericContainerScreen, etc.)
 * does not exist on 1.8.9, so this class is now a simple data-carrier that other
 * parts of the mod can fill and post manually:
 *
 *     ChestLoadedEvent ev = new ChestLoadedEvent();
 *     // ...populate fields…
 *     BazaarUtils.eventBus.post(ev);
 */
public class ChestLoadedEvent implements ICancellable, BUListener {

    /* ------------------------------------------------------------------
       Event data
       ------------------------------------------------------------------ */
    @Getter
    private Object lowerChestInventory;          // kept for API parity

    @Getter
    private List<ItemStack> itemStacks = new ArrayList<>();

    @Getter
    private String containerName = "";

    /* ------------------------------------------------------------------
       BUListener
       ------------------------------------------------------------------ */
    @Override
    public void subscribe() {
        // No automatic GUI hook on 1.8.9 – post this event manually
    }

    /* ------------------------------------------------------------------
       Helpers
       ------------------------------------------------------------------ */
    public boolean inFlipMenu() {
        return containerName != null && containerName.contains("Order options");
    }

    /* ------------------------------------------------------------------
       ICancellable implementation
       ------------------------------------------------------------------ */
    private boolean cancelled = false;

    @Override
    public void setCancelled(boolean b) {
        this.cancelled = b;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }
}
