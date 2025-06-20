package com.github.mkram17.bazaarutils.features.restrictsell;

import com.github.mkram17.bazaarutils.BazaarUtils;
import com.github.mkram17.bazaarutils.events.BUListener;
import com.github.mkram17.bazaarutils.events.ChestLoadedEvent;
import com.github.mkram17.bazaarutils.events.ReplaceItemEvent;
import com.github.mkram17.bazaarutils.utils.Util;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.github.mkram17.bazaarutils.BazaarUtils.EVENT_BUS;

/**
 * Forge-1 .8 .9 implementation of the “Restrict Sell” helper.
 * <br>– No Fabric, Lombok, Orbit, YACL, or Text API – everything is
 * plain vanilla-MC classes, so it compiles on any 1 .8 .9 tool-chain.
 */
public final class RestrictSell implements BUListener {

    /* ──────────────────────────────────── rule types ──────────────────────────────────── */
    public enum Rule { PRICE, VOLUME, NAME }

    /* ───────────────────────────────────── state ─────────────────────────────────────── */
    private boolean enabled               = true;
    private int     safetyClicksRequired  = 3;

    private final List<RestrictSellControl> rules = new ArrayList<>();
    private int  safetyClicks = 0;
    private boolean locked    = false;                 // true ⇒ slot-47 replaced

    private static final int SELL_SLOT = 47;           // Hypixel insta-sell slot-id

    /* ───────────────────────────────── constructors ──────────────────────────────────── */
    public RestrictSell() { /* defaults ok */ }

    /* ───────────────────────────────── public API ────────────────────────────────────── */
    public void addPriceRule (double maxPrice)        { rules.add(new RestrictSellControl(Rule.PRICE , maxPrice)); }
    public void addVolumeRule(double maxVolume)       { rules.add(new RestrictSellControl(Rule.VOLUME, maxVolume));}
    public void addNameRule  (String itemName)        { rules.add(new RestrictSellControl(itemName));             }

    /* ───────────────────────────── BUListener hook ──────────────────────────────────── */
    @Override public void subscribe() {
        EVENT_BUS.subscribe(this);
        EVENT_BUS.subscribe((ChestLoadedEvent ev) -> safetyClicks = 0); // reset per GUI
    }

    /* ───────────────────────── ReplaceItemEvent handler ─────────────────────────────── */
    @SuppressWarnings("unused") // called via functional-interface subscription
    public void onReplaceItem(final ReplaceItemEvent ev) {

        if (!enabled)                           return;
        if (ev.getSlotId() != SELL_SLOT)        return;
        if (!BazaarUtils.GUI.inBazaar())        return;

        ItemStack original = ev.getOriginal();
        if (original == null || !original.hasTagCompound()) return;

        ParsedSellData sd = parseLore(original.getTagCompound());
        if (sd == null) return;                 // malformed lore

        locked = violatesAnyRule(sd);
        if (!locked) return;                    // no rule hit → vanilla button

        /*  ============  slot is blocked  ============ */
        ItemStack block = new ItemStack(Blocks.stained_glass_pane, 1, 14); // red pane
        int left = safetyClicksRequired - safetyClicks;
        block.setStackDisplayName("§cSELL BLOCKED §7(" + left + " clicks)");
        ev.setReplacement(block);

        if (++safetyClicks >= safetyClicksRequired) {
            // user confirmed – allow one sell and reset
            enabled      = false;  // disable until next GUI open
            safetyClicks = 0;
        }

        warnUser(sd, left);
    }

    /* ───────────────────────────── parsing helpers ──────────────────────────────────── */
    private static class ParsedSellData {
        double totalPrice;
        final List<Stack> stacks = new ArrayList<>();
    }
    private record Stack(int volume, String name) {}

    private ParsedSellData parseLore(NBTTagCompound root) {

        if (!root.hasKey("display")) return null;
        NBTTagCompound disp = root.getCompoundTag("display");
        if (!disp.hasKey("Lore"))    return null;

        ParsedSellData out = new ParsedSellData();

        for (int i = 0; i < disp.getTagList("Lore", 8).tagCount(); i++) { // 8 = String
            String s = disp.getTagList("Lore", 8).getStringTagAt(i).replace("§", "");

            if (s.startsWith("Price: ")) {
                String num = s.substring(7, s.indexOf(" coins")).replace(",", "");
                out.totalPrice = Double.parseDouble(num);
            }
            if (s.contains("x ")) {                                       // “ 128x Cobblestone”
                int vol  = Integer.parseInt(s.substring(0, s.indexOf('x')).trim());
                String nm = s.substring(s.indexOf('x') + 2).trim();
                out.stacks.add(new Stack(vol, nm));
            }
        }
        return out;
    }

    /* ───────────────────────────── rule evaluation ──────────────────────────────────── */
    private boolean violatesAnyRule(ParsedSellData d) {

        for (RestrictSellControl r : rules) {
            if (!r.isEnabled()) continue;

            switch (r.getRule()) {
                case PRICE  -> { if (d.totalPrice > r.getAmount()) return true; }
                case VOLUME -> {
                    for (Stack s : d.stacks)
                        if (s.volume() > r.getAmount()) return true;
                }
                case NAME   -> {
                    for (Stack s : d.stacks)
                        if (s.name().equalsIgnoreCase(r.getName())) return true;
                }
            }
        }
        return false;
    }

    /* ───────────────────────────── user feedback ───────────────────────────────────── */
    private static void warnUser(ParsedSellData d, int left) {
        StringBuilder msg = new StringBuilder("§eSell blocked");
        msg.append(" – ").append(left).append(" click");
        if (left != 1) msg.append('s');
        msg.append(" to confirm.  (Total ").append(Util.pretty(d.totalPrice)).append(" coins)");

        if (net.minecraft.client.Minecraft.getMinecraft().thePlayer != null)
            net.minecraft.client.Minecraft.getMinecraft()
                    .thePlayer.addChatMessage(new ChatComponentText(msg.toString()));
    }
}
