package com.github.mkram17.bazaarutils.config;

import com.github.mkram17.bazaarutils.events.BUListener;
import com.github.mkram17.bazaarutils.features.StashHelper;
import com.github.mkram17.bazaarutils.features.StashMessages;
import com.github.mkram17.bazaarutils.features.restrictsell.RestrictSell;
import com.github.mkram17.bazaarutils.features.Bookmark;
import com.github.mkram17.bazaarutils.features.CustomOrder;
import com.github.mkram17.bazaarutils.misc.ItemData;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.gui.GuiScreen;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * **Very small JSON-backed config holder** that exposes every field or helper
 * older source files reference.  It is _not_ a full-featured config system –
 * just enough to let the code base compile and run.
 *
 * – Pure Forge 1.8.9 (no Cloth / YACL)  
 * – Reads / writes a single <code>config/bazaarutils.json</code> file  
 */
public final class BUConfig {

    /* ──────────────────────────────────────────────────────────
       Persistence helpers
       ───────────────────────────────────────────────────────── */
    private static final Gson GSON   = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();
    private static final Path FILE   = Path.of("config", "bazaarutils.json");

    /* ──────────────────────────────────────────────────────────
       Serializable fields (keep **public** for Gson)
       ───────────────────────────────────────────────────────── */
    public List<Bookmark>        bookmarks        = new ArrayList<>();
    public List<CustomOrder>     customOrders     = new ArrayList<>();
    public List<ItemData>        watchedItems     = new ArrayList<>();
    public StashMessages         stashMessages    = new StashMessages(false);
    public RestrictSell          restrictSell     = new RestrictSell(true, 3, new ArrayList<>());
    public List<BUListener>      serializedEvents = new ArrayList<>();

    /** Bazaar tax (0.01 == 1 %) */
    private double  bzTax = 0.01;

    /** Developer-mode toggle */
    private boolean developerMode = false;

    /* ──────────────────────────────────────────────────────────
       Singleton plumbing
       ───────────────────────────────────────────────────────── */
    private static BUConfig INSTANCE = new BUConfig();

    public static BUConfig get() { return INSTANCE; }

    /* ──────────────────────────────────────────────────────────
       Very small JSON loader / writer
       ───────────────────────────────────────────────────────── */
    public static void load() {
        try {
            if (Files.notExists(FILE)) {          // first run → create file
                save();
                return;
            }
            String json = Files.readString(FILE);
            INSTANCE = GSON.fromJson(json, BUConfig.class);
            if (INSTANCE == null) INSTANCE = new BUConfig(); // corrupted file
        } catch (Exception e) {
            System.err.println("[Bazaar-Utils] Failed to load config: " + e);
            INSTANCE = new BUConfig();
        }
    }

    public static void save() { HANDLER.save(); }

    /* Old static alias kept for legacy calls (e.g. BUConfig.HANDLER.save()) */
    public static final class ConfigHandler {
        public void save() {
            try {
                Files.createDirectories(FILE.getParent());
                Files.writeString(FILE, GSON.toJson(INSTANCE));
            } catch (Exception e) {
                System.err.println("[Bazaar-Utils] Failed to save config: " + e);
            }
        }
        public void load() { /* not required – BUConfig.load() is used */ }
    }
    public static final ConfigHandler HANDLER = new ConfigHandler();

    /* ──────────────────────────────────────────────────────────
       Tiny helpers referenced from other classes
       ───────────────────────────────────────────────────────── */
    // GUI factory stub – just return the parent to keep calls compiling.
    public GuiScreen createGUI(GuiScreen parent) { return parent; }

    // “boolean controller” stub for the various Option-builders.
    @SuppressWarnings("unused")
    public static <T> Object createBooleanController() { return null; }

    /* ─────────────── getters / setters used by the rest of the code ─────────────── */
    public List<Bookmark>   getBookmarks()          { return bookmarks; }
    public List<BUListener> getSerializedEvents()   { return serializedEvents; }

    public double  getBzTax()               { return bzTax; }
    public void    setBzTax(double v)       { bzTax = v; }

    public boolean isDeveloperMode()        { return developerMode; }
    public void    setDeveloperMode(boolean v) { developerMode = v; }

    public StashMessages  getStashMessages() { return stashMessages; }
    public RestrictSell   getRestrictSell()  { return restrictSell; }

    /* hide ctor */
    private BUConfig() {}
}
