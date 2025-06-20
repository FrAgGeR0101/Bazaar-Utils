package com.github.mkram17.bazaarutils.config;

import com.github.mkram17.bazaarutils.events.BUListener;
import com.github.mkram17.bazaarutils.utils.Util;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Minimal config holder – no Fabric API, no YACL, no Java-17 features. */
public class BUConfig {

    /* ───────────────────────── serialization ───────────────────────── */
    private static final File  FILE = new File("config/bazaarutils-189.json");
    private static final Gson  GSON = new GsonBuilder()
                                            .setPrettyPrinting()
                                            .create();

    public  static BUConfig INSTANCE = new BUConfig();
    public  static BUConfig get()    { return INSTANCE; }

    /** call once during mod initialisation */
    public static void load() {
        try (FileReader r = new FileReader(FILE)) {
            INSTANCE = GSON.fromJson(r, BUConfig.class);
        } catch (Exception ignored) { /* first run or damaged file */ }
    }

    /** call when you change a field and want to persist it */
    public static void save() {
        try {
            FILE.getParentFile().mkdirs();
            try (FileWriter w = new FileWriter(FILE)) {
                GSON.toJson(INSTANCE, w);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    /* ───────────────────────── config values ───────────────────────── */

    public String  modVersion = "";   // set at runtime
    public boolean firstLoad  = true;
    public double  bzTax      = 1.125;

    // TODO: add more simple fields if you need them
    public final Developer developer = new Developer();

    /* ────────────────── helper: collect BUListener instances ────────── */
    public List<BUListener> getSerializedEvents() {
        List<BUListener> list = new ArrayList<>();
        for (Field f : getClass().getDeclaredFields()) {
            try {
                Object v = f.get(this);
                if (v instanceof BUListener)                    list.add((BUListener) v);
                else if (v instanceof Collection<?>) {
                    for (Object o : (Collection<?>) v)
                        if (o instanceof BUListener)            list.add((BUListener) o);
                }
            } catch (IllegalAccessException e) {
                Util.notifyError("Reflection error on " + f.getName(), e);
            }
        }
        return list;
    }

    /* ───────────────────── developer flags & helpers ────────────────── */
    public static class Developer {
        public boolean allMessages        = false;
        public boolean errorMessages      = false;
        public boolean guiMessages        = false;
        public boolean featureMessages    = false;
        public boolean bazaarDataMessages = false;
        public boolean commandMessages    = false;
        public boolean itemDataMessages   = false;
    }

    /** true if a particular developer-message category should be shown */
    public boolean isDevMessageEnabled(Util.notificationTypes t) {
        switch (t) {
            case GUI:        return developer.guiMessages;
            case FEATURE:    return developer.featureMessages;
            case BAZAARDATA: return developer.bazaarDataMessages;
            case COMMAND:    return developer.commandMessages;
            case ITEMDATA:   return developer.itemDataMessages;
            default:         return developer.allMessages;
        }
    }
}
