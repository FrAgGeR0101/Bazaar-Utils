package com.github.mkram17.bazaarutils.config;

import com.github.mkram17.bazaarutils.events.BUListener;
import com.github.mkram17.bazaarutils.features.*;
import com.github.mkram17.bazaarutils.features.restrictsell.RestrictSell;
import com.github.mkram17.bazaarutils.misc.ItemData;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.gui.GuiScreen;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;                     // ← Java-8 compatible
import java.util.ArrayList;
import java.util.List;

/** ultra-light JSON config – only what the rest of 1 .8 .9 needs */
public final class BUConfig {

    /* ───────────────── persistence ───────────────── */
    private static final Path FILE = Paths.get("config", "bazaarutils.json");
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    /* ───────────────── serialised data ───────────── */
    public List<Bookmark>    bookmarks      = new ArrayList<>();
    public List<CustomOrder> customOrders   = new ArrayList<>();
    public List<ItemData>    watchedItems   = new ArrayList<>();

    public StashMessages stashMessages   = new StashMessages(false);
    public RestrictSell  restrictSell    = new RestrictSell();      // ← no-arg ctor

    public List<BUListener> serializedEvents = new ArrayList<>();

    /* simple scalars */
    private double  bzTax            = 0.01;   // 1 %
    private boolean developerMode    = false;
    private boolean removeStashMsgs  = false;
    private boolean stashTipShown    = false;

    /* ───────────────── singleton ─────────────────── */
    private static BUConfig INSTANCE = new BUConfig();
    public  static BUConfig get() { return INSTANCE; }

    /* ───────────────── JSON I/O ───────────────────── */
    public static void load() {
        try {
            if (Files.notExists(FILE)) { save(); return; }
            String json = new String(Files.readAllBytes(FILE), StandardCharsets.UTF_8);
            BUConfig cfg = GSON.fromJson(json, BUConfig.class);
            if (cfg != null) INSTANCE = cfg;
        } catch (Exception e) {
            System.err.println("[Bazaar-Utils] bad config → defaults ("+e+")");
        }
    }
    public static void save() { HANDLER.save(); }

    /* legacy alias */
    public static final class ConfigHandler {
        public void save() {
            try {
                Files.createDirectories(FILE.getParent());
                Files.write(FILE, GSON.toJson(INSTANCE).getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                System.err.println("[Bazaar-Utils] cannot write config ("+e+")");
            }
        }
        public void load() {}   // unused
    }
    public static final ConfigHandler HANDLER = new ConfigHandler();

    /* ───────── getters / setters needed elsewhere ───────── */

    /* lists */
    public List<Bookmark>      getBookmarks()       { return bookmarks;    }
    public List<CustomOrder>   getCustomOrders()    { return customOrders; }
    public List<ItemData>      getWatchedItems()    { return watchedItems; }
    public List<BUListener>    getSerializedEvents(){ return serializedEvents; }

    /* simple scalars */
    public double  getBzTax()                 { return bzTax; }
    public void    setBzTax(double v)         { bzTax = v;    }

    public boolean isDeveloperMode()          { return developerMode; }
    public void    setDeveloperMode(boolean v){ developerMode = v;    }

    /* stash-message helpers used by StashMessages */
    public boolean isRemoveStashMessages()    { return removeStashMsgs; }
    public void    setRemoveStashMessages(boolean v){ removeStashMsgs = v; }

    public boolean isStashTipShown()          { return stashTipShown; }
    public void    setStashTipShown(boolean v){ stashTipShown = v; }

    public StashMessages getStashMessages()   { return stashMessages; }
    public RestrictSell  getRestrictSell()    { return restrictSell;  }

    /* GUI / option stubs (keep the compiler happy) */
    public GuiScreen createGUI(GuiScreen parent){ return parent; }
    public static Object createBooleanController(){ return null; }

    private BUConfig() {}
}
