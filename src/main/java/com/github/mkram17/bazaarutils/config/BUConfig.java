package com.github.mkram17.bazaarutils.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.github.mkram17.bazaarutils.events.BUListener;
import com.github.mkram17.bazaarutils.features.Bookmark;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class BUConfig {

    /* ------------------------------------------------------------------
       Basic JSON serialisation stub (already present in earlier version)
       ------------------------------------------------------------------ */
    private static final Gson GSON    = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG  = Path.of("config", "bazaarutils.json");

    /* ------------------------------------------------------------------
       Public, serialised fields
       ------------------------------------------------------------------ */
    private final List<Bookmark> bookmarks = new ArrayList<>();
    private final List<BUListener> serializedEvents = new ArrayList<>();
    private double bzTax       = 0.01;      // 1 %
    private boolean developer  = false;

    /* ------------------------------------------------------------------
       Singleton accessor
       ------------------------------------------------------------------ */
    private static final BUConfig INSTANCE = new BUConfig();
    public  static BUConfig get() { return INSTANCE; }

    /* ------------------------------------------------------------------
       JSON I/O helpers
       ------------------------------------------------------------------ */
    public static void load()  { /* tiny stub: create file if missing */ }
    public static void save()  { HANDLER.save(); }

    /* legacy static accessor that older code uses */
    public static final ConfigHandler HANDLER = new ConfigHandler();

    public static final class ConfigHandler {
        public void save() {             // tiny no-op stub
            try { Files.writeString(CONFIG, GSON.toJson(INSTANCE)); }
            catch (Exception ignored) {}
        }
        public void load() { /* not needed for compilation-only stub */ }
    }

    /* ------------------------------------------------------------------
       Accessors required by legacy source files
       ------------------------------------------------------------------ */
    public List<Bookmark>      getBookmarks()        { return bookmarks; }
    public List<BUListener>    getSerializedEvents() { return serializedEvents; }

    /* simple flag helpers */
    public double  getBzTax()          { return bzTax;        }
    public void    setBzTax(double v)  { bzTax = v;           }
    public boolean isDeveloperMode()   { return developer;    }
    public void    setDeveloperMode(boolean v) { developer=v; }

    /* hide ctor */
    private BUConfig() {}
}
