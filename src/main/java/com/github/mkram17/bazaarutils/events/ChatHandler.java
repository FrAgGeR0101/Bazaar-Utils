package com.github.mkram17.bazaarutils.events;

import com.github.mkram17.bazaarutils.config.BUConfig;
import com.github.mkram17.bazaarutils.misc.ItemData;
import com.github.mkram17.bazaarutils.utils.Util;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses Bazaar-related chat messages on Forge 1.8.9 and converts them
 * into {@link ItemData} updates / events.
 *
 * All Fabric-only classes have been removed – the file now compiles and
 * runs on Forge without extra dependencies.
 */
public class ChatHandler implements BUListener {

    /* ───────────────────────── enums ───────────────────────── */
    private enum MsgType { BUYORDER, SELLORDER, FILLED, CLAIMED }

    /* ───────────────── BUListener ───────────────────────────── */
    @Override
    public void subscribe() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    /* ───────────── event hook (Forge 1.8.9) ─────────────────── */
    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent e) {
        IChatComponent  msg      = e.message;
        String          raw      = msg.getUnformattedText();
        List<IChatComponent> sib = msg.getSiblings();

        /* ignore anything that is not Bazaar-related */
        if (!raw.contains("[Bazaar]")) return;
        if (!sib.isEmpty()) {
            String s1 = sib.get(1).getUnformattedText();
            if (s1.contains("escrow")     || s1.contains("Submitting") ||
                s1.contains("Executing")  || s1.contains("Claiming")  ||
                (sib.size() >= 5 && sib.get(2).getUnformattedText().contains("Cancelled")))
                return;
        }

        /* ──────────────── message classification ─────────────── */
        MsgType type = null;
        if (sib.isEmpty() && raw.contains("was filled!")) type = MsgType.FILLED;
        if (sib.size() >= 3 && sib.get(2).getUnformattedText().contains("Buy Order Setup!"))
            type = MsgType.BUYORDER;
        if (sib.size() >= 3 && sib.get(2).getUnformattedText().contains("Sell Offer Setup!"))
            type = MsgType.SELLORDER;
        if (sib.size() >= 3 && sib.get(2).getUnformattedText().contains("Claimed"))
            type = MsgType.CLAIMED;

        /* ─────────────── Buy / Sell order setup ──────────────── */
        if (type == MsgType.BUYORDER || type == MsgType.SELLORDER) {
            String itemName = Util.removeFormatting(sib.get(5).getUnformattedText());
            int    volume   = Integer.parseInt(sib.get(3).getUnformattedText().replace(",", ""));

            int idx = Util.findComponentIndex(sib, "for");
            String totalStr = sib.get(idx + 1).getUnformattedText()
                                  .replace(",", "")
                                  .split(" ")[0];
            double unitPrice = Double.parseDouble(totalStr) / volume;

            if (type == MsgType.SELLORDER) {          // add Bazaar tax for sell-offers
                unitPrice /= ((100 - BUConfig.get().bzTax) / 100.0);
            }

            ItemData d = new ItemData(itemName,
                                      unitPrice * volume,
                                      type == MsgType.SELLORDER
                                              ? ItemData.PriceType.INSTABUY
                                              : ItemData.PriceType.INSTASELL,
                                      volume);

            Util.addWatchedItem(d);
            Util.notifyAll(itemName + " added at " + d.getPrice(),
                           Util.notificationTypes.ITEMDATA);
            return;
        }

        /* ───────────────────── order filled ──────────────────── */
        if (type == MsgType.FILLED) {
            int    vol  = Integer.parseInt(raw.substring(raw.indexOf("for") + 4,
                                                         raw.indexOf('x')).replace(",", ""));
            String name = raw.substring(raw.indexOf('x') + 2,
                                        raw.indexOf("was") - 1);

            ItemData d = raw.contains("Sell Offer")
                    ? ItemData.findItem(name, null, vol, ItemData.PriceType.INSTABUY)
                    : ItemData.findItem(name, null, vol, ItemData.PriceType.INSTASELL);

            if (d == null) {
                Util.notifyError("Could not match filled order: " + name, null);
            } else {
                d.markFilled();
                Util.notifyAll(d.getName() + "[" + d.getIndex() + "] was filled",
                               Util.notificationTypes.ITEMDATA);
            }
            return;
        }

        /* ──────────────────── claim message ──────────────────── */
        if (type == MsgType.CLAIMED) {
            handleClaimed(new ArrayList<>(sib));
        }
    }

    /* ───────────────────── claim-helper ─────────────────────── */
    private static void handleClaimed(ArrayList<IChatComponent> sib) {
        Integer volume = null;
        Double  price  = null;
        String  name;
        ItemData d;
        boolean isBuy = sib.get(6).getUnformattedText().contains("worth");

        try {
            if (isBuy) {   // “worth XX coins”
                volume = Integer.parseInt(sib.get(3).getUnformattedText().replace(",", ""));
                name   = sib.get(5).getUnformattedText().trim();

                String pStr = sib.get(7).getUnformattedText()
                                  .split(" coins")[0].replace(",", "");
                price = Double.parseDouble(pStr) / volume;

                d = ItemData.findItem(name, price, volume,
                        ItemData.PriceType.INSTASELL);
            } else {       // insta-sell claim
                name  = sib.get(7).getUnformattedText().trim();
                String pStr = sib.get(9).getUnformattedText().replace(",", "");
                price = Double.parseDouble(pStr);

                d = ItemData.findItem(name, price, null,
                        ItemData.PriceType.INSTABUY);
            }

            if (d == null) {
                Util.notifyAll("Claimed item not found: " + name,
                               Util.notificationTypes.ITEMDATA);
                return;
            }

            if (volume != null && d.getVolume() == volume) {
                Util.notifyAll(d.getGeneralInfo() + " removed (claimed)",
                               Util.notificationTypes.ITEMDATA);
                ItemData.removeFromWatchedItems(d);
            } else if (volume != null) {
                d.setAmountClaimed(d.getAmountClaimed() + volume);
                Util.notifyAll(d.getName() + " claimed " + d.getAmountClaimed() +
                               "/" + d.getVolume(),
                               Util.notificationTypes.ITEMDATA);
            }
        } catch (Exception ex) {
            Util.notifyError("Error parsing claim message: " + sib, ex);
        }
    }
}
