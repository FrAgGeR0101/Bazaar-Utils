package com.github.mkram17.bazaarutils.misc;

import com.github.mkram17.bazaarutils.utils.Util;
import com.google.gson.*;
import de.hysky.skyblocker.config.SkyblockerConfig;
import de.hysky.skyblocker.config.SkyblockerConfigManager;
import lombok.Getter;
import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModCompatibilityHelper {
    private static final String REI_MOD_ID = "roughlyenoughitems";
    private static final String SKYBLOCKER_MOD_ID = "skyblocker";
    private static final String REI_CONFIG_FILENAME = "roughlyenoughitems/config.json5";
    private static final String REI_CONFIG_SECTION = "appearance";
    private static final String REI_CONFIG_FIELD = "horizontalEntriesBoundariesColumns";
    private static final int HORIZONTALENTRIESBOUNDARIESCOLUMS_VALUE = 16;
    public static final String AMECS_MODID = "amecs-reborn";
    public static final String FIRMAMENT_MODID = "firmament";
    @Getter
    private static boolean amecsReborn = false;

    private static final Gson GSON_WRITER = new GsonBuilder().setPrettyPrinting().create();

    public static void initializePatches(){
        if (FabricLoader.getInstance().isModLoaded(REI_MOD_ID)) {
            Util.notifyAll("Bazaar utils: REI detected. Attempting to modify REI config.");
            modifyReiConfigWithGson();
        }
        if(FabricLoader.getInstance().isModLoaded(AMECS_MODID))
            amecsReborn = true;
    }

    private static void modifyReiConfigWithGson() {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        Path reiConfigFile = configDir.resolve(REI_CONFIG_FILENAME);

        if (!Files.exists(reiConfigFile)) {
            Util.notifyError("Could not find REI config file at: " + reiConfigFile, null);
            return;
        }

        JsonObject rootObject = null;

        try (BufferedReader reader = Files.newBufferedReader(reiConfigFile, StandardCharsets.UTF_8)) {
            JsonElement rootElement = JsonParser.parseReader(reader);

            if (rootElement.isJsonObject()) {
                rootObject = rootElement.getAsJsonObject();
            } else {
                Util.notifyError("REI config root is not a JSON object: " + reiConfigFile, null);
                return;
            }

        } catch (JsonSyntaxException e) {
            Util.notifyError("Failed to parse REI config file (likely due to non-standard JSON5 features like comments that Gson couldn't handle, or actual syntax errors): " + reiConfigFile, e);
            return;
        } catch (IOException e) {
            Util.notifyError("Failed to read REI config file: " + reiConfigFile, e);
            return;
        }

        try {
            if (rootObject != null && rootObject.has(REI_CONFIG_SECTION) && rootObject.get(REI_CONFIG_SECTION).isJsonObject()) {
                JsonObject appearanceObject = rootObject.getAsJsonObject(REI_CONFIG_SECTION);

                if (appearanceObject.has(REI_CONFIG_FIELD)) {
                    JsonElement currentValue = appearanceObject.get(REI_CONFIG_FIELD);
                    Util.notifyAll("Current REI value for '" + REI_CONFIG_SECTION + "." + REI_CONFIG_FIELD + "': " + currentValue, Util.notificationTypes.GUI);
                } else {
                    Util.notifyError("Key '" + REI_CONFIG_SECTION + "." + REI_CONFIG_FIELD + "' not found in REI config.", null);
                }

                appearanceObject.addProperty(REI_CONFIG_FIELD, HORIZONTALENTRIESBOUNDARIESCOLUMS_VALUE);
                Util.notifyAll("Set REI value for '" + REI_CONFIG_SECTION + "." + REI_CONFIG_FIELD + "' to: " + HORIZONTALENTRIESBOUNDARIESCOLUMS_VALUE, Util.notificationTypes.GUI);

            } else {
                Util.notifyError("REI config structure unexpected. Missing '" + REI_CONFIG_SECTION + "' object.", null);
                return;
            }
        } catch (Exception e) {
            Util.notifyError("Error modifying the JSON structure in memory.", e);
            return;
        }

        try (BufferedWriter writer = Files.newBufferedWriter(reiConfigFile, StandardCharsets.UTF_8)) {
            GSON_WRITER.toJson(rootObject, writer);
            Util.notifyAll("Successfully saved modified REI config (comments removed): " + reiConfigFile, Util.notificationTypes.GUI);
        } catch (IOException e) {
            Util.notifyError("Failed to write modified REI config file: " + reiConfigFile, e);
        }
    }

    //true == success, false == failure
    public static boolean tryDisableSkyblockerBazaarOverlay() {
        if (FabricLoader.getInstance().isModLoaded(SKYBLOCKER_MOD_ID)) {
            try {
                SkyblockerConfig skyblockerConfig = SkyblockerConfigManager.get();
                boolean currentValue = skyblockerConfig.uiAndVisuals.searchOverlay.enableBazaar;
                Util.notifyAll("Skyblocker Bazaar Overlay current state: " + currentValue, Util.notificationTypes.GUI);

                if (currentValue) {
                    //TODO test to make sure this works instead of .save()
                    SkyblockerConfigManager.update((x) -> x.uiAndVisuals.searchOverlay.enableBazaar = false);
                    Util.notifyAll("Attempting to disable Skyblocker Bazaar Overlay...", Util.notificationTypes.GUI);

                    Util.notifyAll("Disabled Skyblocker Bazaar search overlay.", Util.notificationTypes.GUI);
                    return true;
                } else {
                    Util.notifyAll("Skyblocker Bazaar Overlay already disabled.", Util.notificationTypes.GUI);
                    return true;
                }
            } catch (NoClassDefFoundError | NoSuchFieldError | Exception e) {
                Util.notifyError("Failed to access or modify Skyblocker config setting.", e);
                return false;
            }
        } else {
            System.out.println("Skyblocker not loaded, cannot change its config.");
            return false;
        }
    }

    public static boolean tryEnableSkyblockerBazaarOverlay() {
        if (FabricLoader.getInstance().isModLoaded(SKYBLOCKER_MOD_ID)) {
            try {
                SkyblockerConfig skyblockerConfig = SkyblockerConfigManager.get();
                if (!skyblockerConfig.uiAndVisuals.searchOverlay.enableBazaar) {
                    System.out.println("Attempting to enable Skyblocker Bazaar Overlay...");
                    SkyblockerConfigManager.update((x) -> x.uiAndVisuals.searchOverlay.enableBazaar = true);


                    Util.notifyAll("Enabled Skyblocker Bazaar search overlay.", Util.notificationTypes.GUI);
                    return true;
                } else {
                    System.out.println("Skyblocker Bazaar Overlay already enabled.");
                    return true;
                }

            } catch (NoClassDefFoundError | NoSuchFieldError | Exception e) {
                Util.notifyError("Failed to access or modify Skyblocker config setting (enable attempt).", e);
                return false;
            }
        } else {
            System.out.println("Skyblocker not loaded, cannot enable its config setting.");
            return false;
        }
    }
    
}
