package com.github.mkram17.bazaarutils.misc;

import com.github.mkram17.bazaarutils.events.ChestLoadedEvent;
import com.github.mkram17.bazaarutils.events.ReplaceItemEvent;
import com.github.mkram17.bazaarutils.events.SlotClickEvent;
import com.github.mkram17.bazaarutils.config.BUConfig;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class CustomItemButton {
    //TODO make flip helper and custom order use this instead of their own settings variables when possible
    protected int slotNumber;
    @Getter @Setter
    protected transient ItemStack replacementItem;
    protected static final RegistryEntry<SoundEvent> BUTTON_SOUND = SoundEvents.UI_BUTTON_CLICK;
    protected static final float BUTTON_VOLUME = .2f;

    protected void checkGui(ChestLoadedEvent event) {

    }

    protected boolean shouldReplaceItem(ReplaceItemEvent event) {
        return event.getSlotId() == slotNumber;
    }

    protected boolean shouldUseSlot(SlotClickEvent event) {
        return (event.slotId == slotNumber);
    }

    public Option<Boolean> createOption(String name, String description, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        return Option.<Boolean>createBuilder()
                .name(Text.literal(name))
                .binding(true,
                        getter,
                        setter)
                .description(OptionDescription.of(Text.literal(description)))
                .controller(BUConfig::createBooleanController)
                .build();
    }
}
