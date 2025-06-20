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
import java.nio.file.Paths;                  // Java-8 compatible
import java.util.ArrayList;
import java.util.List;

/**
 * Minimal JSON-backed config that only exposes what the old
 * Forge-1.8.9 code expects – nothing more, nothing less.
 */
public final class BUConfig {

    /* ───────────────── persistence ───────────────── */

    private static final Path FILE = Paths.get("config", "bazaarutils.json");
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    /* ─────────────── serialised data ─────────────── */

    public List<Bookmark>        bookmarks      = new ArrayList<>();
    public List<CustomOrder>     customOrders   = new ArrayList<>();
    public List<ItemData>        watchedItems   = new ArrayList<>();

    public StashMessages stashMessages = new StashMessages(false);
    public RestrictSell  restrictSell  = new RestrictSell();     // no-arg ctor

    public List<BUListener> serializedEvents = new ArrayList<>();

    /** run-time widgets (mixin reads this) */
    public final List<ItemSlotButtonWidget> widgets = new ArrayList<>();

    /** tax is used directly in a few maths expressions – keep public */
    public double  bzTax = 0.01;        // 1 %

    /* “first-load” & version flags used in JoinMessages */
    public boolean firstLoad            = true;
    public boolean updatedMajorVersion  = false;

    /* developer sub-object – older code calls `developer.allMessages` */
    public final Developer developer = new Developer();
    public static final class Developer {
        /** if true, Util will log _every_ message */
        public boolean allMessages = false;
    }

    /* simple internal flags */
    private boolean developerMode   = false;
    private boolean removeStashMsgs = false;
    private boolean stashTipShown   = false;

    /* ─────────────── singleton plumbing ───────────── */

    private static BUConfig INSTANCE = new BUConfig();
    public  static BUConfig get() { return INSTANCE; }

    /* ─────────────────── JSON I/O ─────────────────── */

    public static void load() {
        try {
            if (Files.notExists(FILE)) { save(); return; }
            String json = new String(Files.readAllBytes(FILE), StandardCharsets.UTF_8);
            BUConfig tmp = GSON.fromJson(json, BUConfig.class);
            if (tmp != null) INSTANCE = tmp;
        } catch (Exception e) {
            System.err.println("[Bazaar-Utils] Bad config – defaults applied (" + e + ')');
        }
    }
    public static void save() { HANDLER.save(); }

    /** legacy alias kept so older calls compile unchanged */
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
        public void load() { /* use BUConfig.load() instead */ }
    }
    public static final ConfigHandler HANDLER = new ConfigHandler();

    /* ─────────── accessor helpers used elsewhere ─────────── */

    public List<Bookmark>      getBookmarks()        { return bookmarks; }
    public List<CustomOrder>   getCustomOrders()     { return customOrders; }
    public List<ItemData>      getWatchedItems()     { return watchedItems; }
    public List<BUListener>    getSerializedEvents() { return serializedEvents; }

    /** mixin helper */
    public static List<ItemSlotButtonWidget> getWidgets() {
        return get().widgets;
    }

    /* plain flags */
    public boolean isDeveloperMode()                 { return developerMode; }
    public void    setDeveloperMode(boolean v)       { developerMode = v;    }

    public double  getBzTax()                        { return bzTax; }
    public void    setBzTax(double v)                { bzTax = v;    }

    public boolean isRemoveStashMessages()           { return removeStashMsgs; }
    public void    setRemoveStashMessages(boolean v) { removeStashMsgs = v; }

    public boolean isStashTipShown()                 { return stashTipShown; }
    public void    setStashTipShown(boolean v)       { stashTipShown = v;   }

    public StashMessages getStashMessages()          { return stashMessages; }
    public RestrictSell  getRestrictSell()           { return restrictSell;  }

    /* ───────────── GUI / option stubs ───────────── */

    /** Old YACL callers reference this – we just return parent to keep flow. */
    public GuiScreen createGUI(GuiScreen parent) { return parent; }

    /** Option builder placeholder (boolean controller) */
    public static Object createBooleanController() { return null; }

    /** Old command shortcut: `/bu` → `BUConfig.openGUI()` */
    public static void openGUI() {
        Minecraft mc = Minecraft.getMinecraft();
        mc.displayGuiScreen(get().createGUI(mc.currentScreen));
    }

    /* private ctor */
    private BUConfig() {}
}
