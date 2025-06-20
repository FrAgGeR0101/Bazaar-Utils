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
 * Ultra-light JSON config – only the members that the old
 * Forge-1.8.9 code base references.  No Cloth / YACL / modern APIs.
 */
public final class BUConfig {

    /* ───────────────────────── persistence ───────────────────────── */

    private static final Path FILE = Paths.get("config", "bazaarutils.json");
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    /* ───────────────────── serialised fields ────────────────────── */

    /** visible lists – referenced directly from many classes */
    public List<Bookmark>        bookmarks      = new ArrayList<>();
    public List<CustomOrder>     customOrders   = new ArrayList<>();
    public List<ItemData>        watchedItems   = new ArrayList<>();

    /** feature sub-objects (simple no-arg constructors) */
    public StashMessages stashMessages = new StashMessages(false);
    public RestrictSell  restrictSell  = new RestrictSell();

    /** Orbit listeners that were persisted previously */
    public List<BUListener> serializedEvents = new ArrayList<>();

    /** run-time widgets injected by mixins */
    public final List<ItemSlotButtonWidget> widgets = new ArrayList<>();

    /** scalars that old maths expressions use */
    public double  bzTax = 0.01;               // 1 %
    public boolean firstLoad            = true;
    public boolean updatedMajorVersion  = false;

    /** developer sub-object – code calls developer.allMessages */
    public final Developer developer = new Developer();
    public static final class Developer { public boolean allMessages = false; }

    /* extra flags used only by StashMessages */
    private boolean developerMode   = false;
    private boolean removeStashMsgs = false;
    private boolean stashTipShown   = false;

    /* ───────────────────── singleton plumbing ───────────────────── */

    private static BUConfig INSTANCE = new BUConfig();
    public  static BUConfig get() { return INSTANCE; }

    private BUConfig() {}                         // hide public ctor

    /* ───────────────────────── JSON I/O ─────────────────────────── */

    public static void load() {
        try {
            if (Files.notExists(FILE)) { save(); return; }
            String json = new String(Files.readAllBytes(FILE), StandardCharsets.UTF_8);
            BUConfig tmp = GSON.fromJson(json, BUConfig.class);
            if (tmp != null) INSTANCE = tmp;
        } catch (Exception e) {
            System.err.println("[Bazaar-Utils] Bad config – defaults used ("+e+')');
        }
    }
    public static void save() { HANDLER.save(); }

    /** legacy alias so calls like BUConfig.HANDLER.save() keep compiling */
    public static final class ConfigHandler {
        public void save() {
            try {
                Files.createDirectories(FILE.getParent());
                Files.write(FILE,
                        GSON.toJson(INSTANCE).getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                System.err.println("[Bazaar-Utils] Cannot write config ("+e+')');
            }
        }
        public void load() { /* unused – call BUConfig.load() instead */ }
    }
    public static final ConfigHandler HANDLER = new ConfigHandler();

    /* ───────────────────── helper accessors ────────────────────── */

    /* lists */
    public List<Bookmark>      getBookmarks()        { return bookmarks; }
    public List<CustomOrder>   getCustomOrders()     { return customOrders; }
    public List<ItemData>      getWatchedItems()     { return watchedItems; }
    public List<BUListener>    getSerializedEvents() { return serializedEvents; }

    /* widgets – MixinHandledScreen hooks into this */
    public static List<ItemSlotButtonWidget> getWidgets() {
        return get().widgets;
    }

    /* plain scalars + flags */
    public double  getBzTax()                        { return bzTax; }
    public void    setBzTax(double v)                { bzTax = v;    }

    public boolean isDeveloperMode()                 { return developerMode; }
    public void    setDeveloperMode(boolean v)       { developerMode = v;    }

    public boolean isRemoveStashMessages()           { return removeStashMsgs; }
    public void    setRemoveStashMessages(boolean v) { removeStashMsgs = v;   }

    public boolean isStashTipShown()                 { return stashTipShown; }
    public void    setStashTipShown(boolean v)       { stashTipShown = v;     }

    public StashMessages getStashMessages()          { return stashMessages; }
    public RestrictSell  getRestrictSell()           { return restrictSell;  }

    /* ─────────────── GUI / option stubs ─────────────── */

    /** YACL-style factory – just bounce back to the parent screen */
    public GuiScreen createGUI(GuiScreen parent) { return parent; }
    /** boolean controller placeholder used in Option builders */
    public static Object createBooleanController() { return null; }

    /** old shortcut: /bu → BUConfig.openGUI() */
    public static void openGUI() {
        Minecraft mc = Minecraft.getMinecraft();
        mc.displayGuiScreen(get().createGUI(mc.currentScreen));
    }
}
