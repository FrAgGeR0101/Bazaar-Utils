package com.github.mkram17.bazaarutils.config;

import com.github.mkram17.bazaarutils.events.BUListener;
import com.github.mkram17.bazaarutils.features.*;
import com.github.mkram17.bazaarutils.features.restrictsell.RestrictSell;
import com.github.mkram17.bazaarutils.misc.ItemData;
import com.github.mkram17.bazaarutils.misc.ItemSlotButtonWidget;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Ultra-light JSON config – only what ancient 1.8.9 sources expect.
 */
public final class BUConfig {

    /* ─────────────── persistence ─────────────── */
    private static final Path FILE = Paths.get("config", "bazaarutils.json");
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    /* ─────────────── serialised fields ─────────────── */
    public List<Bookmark>     bookmarks      = new ArrayList<>();
    public List<CustomOrder>  customOrders   = new ArrayList<>();
    public List<ItemData>     watchedItems   = new ArrayList<>();

    public StashMessages stashMessages = new StashMessages(false);
    public RestrictSell  restrictSell  = new RestrictSell();     // no-arg ctor

    public List<BUListener> serializedEvents = new ArrayList<>();

    /* Live run-time widgets (MixinHandledScreen reads them) */
    public final List<ItemSlotButtonWidget> widgets = new ArrayList<>();

    /* Misc top-level scalars referenced directly */
    public double  bzTax = 0.01;          // 1 %
    public boolean firstLoad           = true;
    public boolean updatedMajorVersion = false;

    /* Developer-mode container (old code: developer.allMessages) */
    public final Developer developer = new Developer();
    public static final class Developer { public boolean allMessages = false; }

    /* Simple internal flags */
    private boolean developerMode   = false;
    private boolean removeStashMsgs = false;
    private boolean stashTipShown   = false;

    /* ─────────────── singleton ─────────────── */
    private static BUConfig INSTANCE = new BUConfig();
    public  static BUConfig get() { return INSTANCE; }

    /* ─────────────── JSON I/O ─────────────── */
    public static void load() {
        try {
            if (Files.notExists(FILE)) { save(); return; }
            String json = new String(Files.readAllBytes(FILE), StandardCharsets.UTF_8);
            BUConfig tmp = GSON.fromJson(json, BUConfig.class);
            if (tmp != null) INSTANCE = tmp;
        } catch (Exception e) {
            System.err.println("[Bazaar-Utils] Bad config – defaults used (" + e + ')');
        }
    }
    public static void save() { HANDLER.save(); }

    /* Legacy alias (old source uses BUConfig.HANDLER.save()) */
    public static final class ConfigHandler {
        public void save() {
            try {
                Files.createDirectories(FILE.getParent());
                Files.write(FILE,
                            GSON.toJson(INSTANCE).getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                System.err.println("[Bazaar-Utils] Cannot write config (" + e + ')');
            }
        }
        public void load() {/* unused – call BUConfig.load() */}
    }
    public static final ConfigHandler HANDLER = new ConfigHandler();

    /* ─────────────── helpers used elsewhere ─────────────── */

    /* lists */
    public List<Bookmark>      getBookmarks()         { return bookmarks; }
    public List<CustomOrder>   getCustomOrders()      { return customOrders; }
    public List<ItemData>      getWatchedItems()      { return watchedItems; }
    public List<BUListener>    getSerializedEvents()  { return serializedEvents; }

    /* widgets for the HandledScreen mixin */
    public static List<ItemSlotButtonWidget> getWidgets() {
        return get().widgets;
    }

    /* simple flag / value accessors */
    public double  getBzTax()                        { return bzTax; }
    public void    setBzTax(double v)                { bzTax = v;    }

    public boolean isDeveloperMode()                 { return developerMode; }
    public void    setDeveloperMode(boolean v)       { developerMode = v; }

    /* stash message helpers expected by StashMessages */
    public boolean isRemoveStashMessages()           { return removeStashMsgs; }
    public void    setRemoveStashMessages(boolean v) { removeStashMsgs = v; }

    public boolean isStashTipShown()                 { return stashTipShown; }
    public void    setStashTipShown(boolean v)       { stashTipShown = v; }

    public StashMessages getStashMessages()          { return stashMessages; }
    public RestrictSell  getRestrictSell()           { return restrictSell;  }

    /* YACL-style stubs – simply bounce back the parent screen */
    public GuiScreen createGUI(GuiScreen parent) { return parent; }
    public static Object createBooleanController() { return null; }

    /* Command helper – old code calls BUConfig.openGUI() */
    public static void openGUI() {
        Minecraft mc = Minecraft.getMinecraft();
        mc.displayGuiScreen(get().createGUI(mc.currentScreen));
    }

    private BUConfig() {}
}
