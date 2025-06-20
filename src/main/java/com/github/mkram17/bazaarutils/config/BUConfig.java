package com.github.mkram17.bazaarutils.config;

import com.github.mkram17.bazaarutils.events.BUListener;
import com.github.mkram17.bazaarutils.features.*;
import com.github.mkram17.bazaarutils.features.restrictsell.RestrictSell;
import com.github.mkram17.bazaarutils.misc.ItemData;
import com.github.mkram17.bazaarutils.misc.ItemSlotButtonWidget;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.gui.GuiScreen;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;                          // Java-8 friendly
import java.util.ArrayList;
import java.util.List;

/**
 * **Ultra-light JSON config** that only exposes the fields/methods
 * legacy 1.8.9 code still references.  No Cloth / YACL – just Gson.
 */
public final class BUConfig {

    /* ─────────────────── persistence ─────────────────── */

    private static final Path FILE = Paths.get("config", "bazaarutils.json");
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    /* ─────────────────── serialised data ─────────────── */

    public List<Bookmark>        bookmarks      = new ArrayList<>();
    public List<CustomOrder>     customOrders   = new ArrayList<>();
    public List<ItemData>        watchedItems   = new ArrayList<>();

    public StashMessages stashMessages = new StashMessages(false);
    public RestrictSell  restrictSell  = new RestrictSell();          // no-arg ctor

    public List<BUListener> serializedEvents = new ArrayList<>();

    /* tiny widget store – used by the mixin that adds buttons dynamically */
    public final List<ItemSlotButtonWidget> widgets = new ArrayList<>();

    /* simple scalars (made *public* where older code accessed the field directly) */
    public       double   bzTax               = 0.01;   // 1 %
    public       boolean  firstLoad           = true;   // shown on 1st join
    public       boolean  updatedMajorVersion = false;

    private boolean developerMode   = false;
    private boolean removeStashMsgs = false;
    private boolean stashTipShown   = false;

    /* ─────────────────── singleton plumbing ──────────── */

    private static BUConfig INSTANCE = new BUConfig();
    public  static BUConfig get() { return INSTANCE; }

    /* ─────────────────── JSON I/O ─────────────────────── */

    public static void load() {
        try {
            if (Files.notExists(FILE)) { save(); return; }

            String json = new String(Files.readAllBytes(FILE), StandardCharsets.UTF_8);
            BUConfig cfg = GSON.fromJson(json, BUConfig.class);
            if (cfg != null) INSTANCE = cfg;
        } catch (Exception e) {
            System.err.println("[Bazaar-Utils] Bad config – using defaults (" + e + ')');
        }
    }
    public static void save() { HANDLER.save(); }

    /* legacy alias still referenced in a few spots */
    public static final class ConfigHandler {
        public void save() {
            try {
                Files.createDirectories(FILE.getParent());
                Files.write(FILE, GSON.toJson(INSTANCE).getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                System.err.println("[Bazaar-Utils] Cannot write config (" + e + ')');
            }
        }
        public void load() { /* unused – call BUConfig.load() instead */ }
    }
    public static final ConfigHandler HANDLER = new ConfigHandler();

    /* ─────────────────── accessors used elsewhere ────── */

    /* lists */
    public List<Bookmark>      getBookmarks()         { return bookmarks; }
    public List<CustomOrder>   getCustomOrders()      { return customOrders; }
    public List<ItemData>      getWatchedItems()      { return watchedItems; }
    public List<BUListener>    getSerializedEvents()  { return serializedEvents; }

    /* widgets (mixin looks this up statically) */
    public static List<ItemSlotButtonWidget> getWidgets() { return get().widgets; }

    /* simple scalars – methods preferred by newer code */
    public double  getBzTax()                 { return bzTax; }
    public void    setBzTax(double v)         { bzTax = v;    }

    public boolean isDeveloperMode()          { return developerMode; }
    public void    setDeveloperMode(boolean v){ developerMode = v;    }

    /* stash-message helpers */
    public boolean isRemoveStashMessages()            { return removeStashMsgs; }
    public void    setRemoveStashMessages(boolean v)  { removeStashMsgs = v;    }

    public boolean isStashTipShown()                  { return stashTipShown; }
    public void    setStashTipShown(boolean v)        { stashTipShown = v;     }

    public StashMessages getStashMessages()           { return stashMessages; }
    public RestrictSell  getRestrictSell()            { return restrictSell;  }

    /* ─────────────────── GUI / option stubs ─────────── */

    /** Old YACL screens call this – we just return the parent. */
    public GuiScreen createGUI(GuiScreen parent) { return parent; }

    /** Placeholder for Option builders that expect a boolean controller. */
    public static Object createBooleanController() { return null; }

    /* prevent external instantiation */
    private BUConfig() {}
}
