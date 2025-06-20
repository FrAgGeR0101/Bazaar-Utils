package com.github.mkram17.bazaarutils.features;

import com.github.mkram17.bazaarutils.utils.GUIUtils;
import com.github.mkram17.bazaarutils.utils.Util;
import de.siphalor.amecs.api.AmecsKeyBinding;
import de.siphalor.amecs.api.KeyModifiers;
import lombok.Getter;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

//TODO make using amecs optional
public class StashHelper extends AmecsKeyBinding {
    @Getter
    private transient int ticksBetweenPresses;

    public StashHelper() {
        super("Pick Up Stash", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_V, "Bazaar utils", new KeyModifiers(true, false, false, false));
        ticksBetweenPresses = 0;
    }

    @Override
    public void onPressed(){
        if(ticksBetweenPresses>10) {
            ticksBetweenPresses = 0;
            GUIUtils.closeHandledScreen();
            Util.sendCommand("pickupstash");
        }
    }

    public void registerTickCounter() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ticksBetweenPresses++;
        });
    }

    public String getUsage(){
        return getModifierString(getDefaultModifiers().getValue()) +"+" + getDefaultKey().getLocalizedText().getString();
    }

    private static String getModifierString(boolean[] modifiers) {
        String modifierString = "";
        if(modifiers[0])
            modifierString += "ALT";
        if(modifiers[1])
            modifierString += "CTRL";
        if(modifiers[2])
            modifierString += "SHIFT";
        return modifierString;
    }


}
