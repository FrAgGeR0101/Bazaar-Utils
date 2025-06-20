package com.github.mkram17.bazaarutils.config;

import com.github.mkram17.bazaarutils.events.BUListener;
import com.github.mkram17.bazaarutils.features.*;
import com.github.mkram17.bazaarutils.features.restrictsell.RestrictSell;
import com.github.mkram17.bazaarutils.misc.ItemData;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.gui.GuiScreen;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Tiny JSON-backed config scaffolding – **only what the rest of the
 * 1.8.9 code base needs to compile and run**.
 */
public final class BUConfig {

    /* ─────────────────────────── persistence ─────────────────────────── */

    private static final Path FILE = Path.of("config", "bazaarutils.json");
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    /* ─────────────────────────── serialised fields ───────────────────── */

    /*  PUBLIC so Gson can fill them – code also accesses them directly   */
    public List<Bookmark>    bookmarks     = new ArrayList<>();
    public List<CustomOrder> customOrders  = new ArrayList<>();
    public List<ItemData>    watchedItems  = new ArrayList<>();

    public StashMessages  stashMessages = new StashMessages(false);
    public RestrictSell   restrictSell  = new RestrictSell(true, 3, new ArrayList<>());

    public List<BUListener> serializedEvents = new ArrayList<>();

    /* simple scalars */
    private double  bzTax         = 0.01;      // 1 %
    private boolean developerMode = false;

    /* ─────────────────────────── singleton plumbing ──────────────────── */

    private static BUConfig INSTANCE = new BUConfig();
    public  static BUConfig get() { return INSTANCE; }

    /* ─────────────────────────── JSON helpers ────────────────────────── */

    public static void load() {
        try {
            if (Files.notExists(FILE)) { save(); return; }
            String json = Files.readString(FILE);
            BUConfig cfg = GSON.fromJson(json, BUConfig.class);
            if (cfg != null) INSTANCE = cfg;
        } catch (Exception e) {
            System.err.println("[Bazaar-Utils] Bad config – using defaults: " + e);
        }
    }

    public static void save() { HANDLER.save(); }

    /* legacy alias used all over the code base */
    public static final class ConfigHandler {
        public void save() {
            try {
                Files.createDirectories(FILE.getParent());
                Files.writeString(FILE, GSON.toJson(INSTANCE));
            } catch (Exception e) {
                System.err.println("[Bazaar-Utils] Couldn’t write config: " + e);
            }
        }
        public void load() { /* unused – BUConfig.load() is called */ }
    }
    public static final ConfigHandler HANDLER = new ConfigHandler();

    /* ─────────────────────────── getters / setters ───────────────────── */

    /* lists */
    public List<Bookmark>      getBookmarks()     { return bookmarks;    }
    public List<CustomOrder>   getCustomOrders()  { return customOrders; }
    public List<ItemData>      getWatchedItems()  { return watchedItems; }

    public List<BUListener>    getSerializedEvents() { return serializedEvents; }

    /* scalars */
    public double  getBzTax()                    { return bzTax; }
    public void    setBzTax(double v)            { bzTax = v;    }

    public boolean isDeveloperMode()             { return developerMode; }
    public void    setDeveloperMode(boolean v)   { developerMode = v;    }

    public StashMessages getStashMessages()      { return stashMessages; }
    public RestrictSell  getRestrictSell()       { return restrictSell;  }

    /* ─────────────────────────── tiny stubs used by GUI code ─────────── */

    /** YACL-style GUI factory placeholder – simply returns the parent. */
    public GuiScreen createGUI(GuiScreen parent) { return parent; }

    /** Boolean-option controller stub (YACL replacement). */
    public static Object createBooleanController() { return null; }

    /* hide ctor */
    private BUConfig() {}
}
