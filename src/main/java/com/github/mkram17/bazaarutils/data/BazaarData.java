package com.github.mkram17.bazaarutils.data;

import com.github.mkram17.bazaarutils.BazaarUtils;
import com.github.mkram17.bazaarutils.config.BUConfig;
import com.github.mkram17.bazaarutils.misc.ItemData;
import com.github.mkram17.bazaarutils.utils.Util;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.hypixel.api.reply.skyblock.SkyBlockBazaarReply;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;

import java.io.InputStreamReader;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Periodically fetches Hypixel Bazaar data (via the Hypixel-Java API) and
 * offers a few static helper-methods for price-look-ups and product-ID
 * conversions.  All Fabric-only classes have been replaced by their
 * 1.8.9 Forge counterparts (<em>Minecraft</em>, <em>IResourceManager</em>,
 * <em>ResourceLocation</em> …).
 */
public final class BazaarData {

    /* ────────────────────────────────────────────────────────── */
    private static final String RESOURCE_JSON = "bazaar-resources.json";

    private static final ScheduledExecutorService EXEC =
            Executors.newSingleThreadScheduledExecutor();

    private static SkyBlockBazaarReply reply;          // most-recent API payload
    private static int  callCounter     = 0;           // API call statistics
    private static int  exceptionCount  = 0;
    private static int  periodSeconds   = 1;
    private static boolean skipNextCall = false;

    /* ────────────────────────────────────────────────────────── */

    private BazaarData() {}           // static-only utility

    /* ────────────────────────────────────────────────────────── */
    /*  public API                                               */
    /* ────────────────────────────────────────────────────────── */

    public static void startScheduler() {
        EXEC.scheduleAtFixedRate(BazaarData::fetchBazaar,
                                 3,       // initial delay
                                 1,       // tick every second
                                 TimeUnit.SECONDS);
    }

    /** Current best buy / sell price (or –1 when unavailable). */
    public static double findItemPrice(String productId, ItemData.PriceType type) {

        if (reply == null) return -1.0;

        try {
            SkyBlockBazaarReply.Product prod = reply.getProduct(productId);
            if (prod == null) return -1.0;

            if (type == ItemData.PriceType.INSTABUY) {
                return prod.getBuySummary().isEmpty() ? 0.0
                        : prod.getBuySummary().getFirst().getPricePerUnit();
            } else { // INSTASELL
                return prod.getSellSummary().isEmpty() ? 0.0
                        : prod.getSellSummary().getFirst().getPricePerUnit();
            }
        } catch (Exception ex) {
            Util.notifyError("Bazaar price lookup failed for " + productId, ex);
            return -1.0;
        }
    }

    /** Convert human-readable item-name to Hypixel product-ID. */
    public static String findProductId(String naturalName) {

        JsonObject conv = loadResourceJson(RESOURCE_JSON)
                .getAsJsonObject("bazaarConversions");

        for (String key : conv.keySet()) {
            if (conv.get(key).getAsString().equalsIgnoreCase(naturalName))
                return key;
        }
        return null;
    }

    /* ────────────────────────────────────────────────────────── */
    /*  internal helpers                                          */
    /* ────────────────────────────────────────────────────────── */

    private static void fetchBazaar() {
        if (skipNextCall || (callCounter % periodSeconds) != 0) {
            skipNextCall = false;
            return;
        }

        APIUtils.API.getSkyBlockBazaar().whenComplete((rep, thr) -> {

            callCounter++;
            if (callCounter % 10 == 0 || callCounter < 5) skipNextCall = true;

            if (thr != null) {
                exceptionCount++;
                if (exceptionCount % 5 == 0) periodSeconds++;
                Util.notifyError("Hypixel API error while fetching bazaar", thr);
                return;
            }

            reply = rep;
            if (!BUConfig.get().watchedItems.isEmpty()) ItemData.update();
        });
    }

    /* ------------------------------------------------------------------ */

    private static JsonObject loadResourceJson(String path) {
        try {
            IResourceManager mgr = Minecraft.getMinecraft().getResourceManager();
            ResourceLocation  id  = new ResourceLocation(BazaarUtils.MODID, path);
            Optional<IResource> res = Optional.ofNullable(mgr.getResource(id));

            if (res.isPresent()) {
                try (InputStreamReader r = new InputStreamReader(res.get().getInputStream())) {
                    return JsonParser.parseReader(r).getAsJsonObject();
                }
            }
        } catch (Exception e) {
            Util.notifyError("Could not load resource " + path, e);
        }
        return new JsonObject();
    }
}
