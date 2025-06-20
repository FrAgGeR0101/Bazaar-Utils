package com.github.mkram17.bazaarutils.features;

import com.github.mkram17.bazaarutils.BazaarUtils;
import com.github.mkram17.bazaarutils.data.BazaarData;
import com.github.mkram17.bazaarutils.events.BUListener;
import com.github.mkram17.bazaarutils.utils.SoundUtil;
import com.github.mkram17.bazaarutils.utils.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.EnumChatFormatting;

import java.awt.Desktop;
import java.net.URI;

/**
 * Very small helper that adds a “CTRL + SHIFT click for price chart” line
 * to Bazaar item tool-tips.  A click opens the corresponding
 * <a href="https://skyblock.finance">skyblock.finance</a> page.
 *
 * <p>Written for pure Forge 1.8.9 – no Fabric, Lombok, or modern APIs.</p>
 */
public final class PriceCharts implements BUListener {

    /* ------------------------------------------------------------------ */
    /*  User flag (serialised in your JSON config elsewhere)              */
    /* ------------------------------------------------------------------ */
    private boolean showOutsideBazaar = false;

    public boolean isShowOutsideBazaar()              { return showOutsideBazaar; }
    public void    setShowOutsideBazaar(boolean flag) { showOutsideBazaar = flag; }

    /* ------------------------------------------------------------------ */
    /*  Core logic                                                         */
    /* ------------------------------------------------------------------ */

    private static final String TAG_KEY   = "BU_financeId";
    private static final String FINANCE   = "https://skyblock.finance/items/";

    /** Inject one extra line into the vanilla tooltip list. */
    public void addTooltip(ItemStack stack, java.util.List<String> lines) {

        if (stack == null || stack.stackSize == 0) return;        // no isEmpty in 1.8.9
        if (!shouldShow())                    return;

        /* Best-effort product-id lookup via display-name                */
        String cleanName = Util.removeFormatting(stack.getDisplayName());
        String productId = BazaarData.findProductId(cleanName);
        if (productId == null) return;                            // not a Bazaar item

        lines.add(EnumChatFormatting.GOLD + "" + EnumChatFormatting.BOLD +
                  "CTRL+SHIFT-click for price-chart");

        /* remember the id inside the stack so the click-handler can use it */
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) stack.setTagCompound(tag = new NBTTagCompound());
        tag.setTag(TAG_KEY, new NBTTagString(productId));
    }

    /** Called from the global SlotClickEvent mix-in. */
    public void onSlotClick(Slot slot, boolean ctrl, boolean shift) {

        if (!ctrl || !shift)               return;
        if (!shouldShow())                 return;

        ItemStack st = slot.getStack();
        if (st == null || st.stackSize == 0) return;

        NBTTagCompound tag = st.getTagCompound();
        if (tag == null || !tag.hasKey(TAG_KEY)) return;

        openBrowser(FINANCE + tag.getString(TAG_KEY));
        SoundUtil.notifyMultipleTimes(2);                // quick audio feedback
    }

    /* ------------------------------------------------------------------ */
    /*  Helper utilities                                                  */
    /* ------------------------------------------------------------------ */

    private boolean shouldShow() {
        return BazaarUtils.GUI.inBazaar() || showOutsideBazaar;
    }

    private static void openBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported())
                Desktop.getDesktop().browse(new URI(url));
        } catch (Exception ignored) { /* silent */ }
    }

    /* ------------------------------------------------------------------ */
    /*  BUListener                                                        */
    /* ------------------------------------------------------------------ */

    @Override
    public void subscribe() {
        BazaarUtils.EVENT_BUS.subscribe(this);
    }
}
