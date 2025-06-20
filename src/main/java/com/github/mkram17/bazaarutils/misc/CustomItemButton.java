package com.github.mkram17.bazaarutils.misc;

import com.github.mkram17.bazaarutils.config.BUConfig;
import com.github.mkram17.bazaarutils.events.ChestLoadedEvent;
import com.github.mkram17.bazaarutils.events.ReplaceItemEvent;
import com.github.mkram17.bazaarutils.events.SlotClickEvent;
import net.minecraft.item.ItemStack;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Base-class for every “injected” Bazaar-Utils button that replaces a slot
 * inside Hypixel’s Bazaar GUI.
 *
 * <p>Forge 1.8.9 edition – no Lombok, no YACL run-time.  A tiny stub
 * version of the YACL {@code Option} builder is included at the bottom
 * so the configuration screens in {@code BUConfig} can keep compiling
 * until you migrate to a proper Forge config UI.</p>
 */
public abstract class CustomItemButton {

    /* ───────────────────────── slot info ───────────────────────── */

    /** Absolute slot index inside the container to be replaced. */
    protected int slotNumber;

    /** Cached replacement stack (built lazily by subclasses). */
    protected transient ItemStack replacementItem;

    /* ───────────────────────── sound constants ─────────────────── */

    /** 1.8.9 click-sound identifier – played via {@code SoundUtil}. */
    public static final String BUTTON_SOUND  = "random.click";
    public static final float  BUTTON_VOLUME = 0.20f;

    /* ───────────────────────── GUI helpers ─────────────────────── */

    /** Subclasses may override to (un)subscribe themselves when a new
     *  chest screen finishes loading. */
    protected void checkGui(ChestLoadedEvent ev) { /* optional */ }

    /** True when we should replace the original slot stack. */
    protected boolean shouldReplaceItem(ReplaceItemEvent ev) {
        return ev.getSlotId() == slotNumber;
    }

    /** True when we should react to a click on this slot. */
    protected boolean shouldUseSlot(SlotClickEvent ev) {
        return ev.slotId == slotNumber;
    }

    /* ───────────────────────── tiny “Option” stub ───────────────── */

    /**
     * Convenience helper for building a Boolean toggle in the config GUI.
     *<p>
     * Uses a lightweight builder stub so the rest of the codebase can keep
     * its familiar YACL-style calls without pulling the full library into
     * a Forge 1.8.9 project.
     */
    public Option<Boolean> createOption(String name,
                                        String description,
                                        Supplier<Boolean> getter,
                                        Consumer<Boolean> setter) {

        return Option.<Boolean>builder()
                     .name(name)
                     .description(description)
                     .binding(true, getter, setter)
                     .build();
    }

    /* =================================================================
       The next three inner classes are **minimal stubs** that mimic the
       public API of YACL’s Option/OptionGroup/ConfigCategory builders.
       They are *no-ops* – just enough to keep the compiler happy while
       you migrate the actual configuration GUI to Forge.
       ================================================================= */

    public static final class Option<T> {

        /** Functional getter used by the binding. */
        public interface Getter<T> { T get(); }
        /** Functional setter used by the binding. */
        public interface Setter<T> { void set(T v); }

        /* ───────────── builder factory ───────────── */
        public static <T> Builder<T> builder() { return new Builder<>(); }

        /* ───────────── builder  impl  ───────────── */
        public static final class Builder<T> {
            private String    name = "";
            private String    desc = "";
            private T         def;
            private Getter<T> getter;
            private Setter<T> setter;

            public Builder<T> name(String n)              { name  = n;  return this; }
            public Builder<T> description(String d)       { desc  = d;  return this; }
            public Builder<T> binding(T def,
                                      Getter<T> g,
                                      Setter<T> s)        { this.def = def; getter = g; setter = s; return this; }

            /** Ignored on Forge – kept for source compatibility. */
            public Builder<T> controller(Object __)       { return this; }

            public Option<T> build()                      { return new Option<>(); }
        }
    }

    public static final class OptionGroup {
        public static Builder builder() { return new Builder(); }
        public static final class Builder {
            public Builder name(String __)                    { return this; }
            public Builder description(String __)             { return this; }
            public Builder option(Object __)                  { return this; }
            public OptionGroup build()                        { return new OptionGroup(); }
        }
    }

    public static final class ConfigCategory {
        public static Builder builder() { return new Builder(); }
        public static final class Builder {
            public Builder name(String __)                    { return this; }
            public Builder option(Object __)                  { return this; }
            public Builder group(Object __)                   { return this; }
            public ConfigCategory build()                     { return new ConfigCategory(); }
        }
    }
}
