package com.github.mkram17.bazaarutils.misc;

import com.github.mkram17.bazaarutils.BazaarUtils;
import com.github.mkram17.bazaarutils.config.BUConfig;
import com.github.mkram17.bazaarutils.events.BUListener;
import com.github.mkram17.bazaarutils.utils.Util;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class JoinMessages implements BUListener {
    private static final Text welcomeMessage = Text.literal("[Bazaar utils] ")
            .formatted(Formatting.WHITE)
            .append(Text.literal("Thanks for installing! Use /buconfig to configure the mod.")
                    .formatted(Formatting.GREEN));
    private static final Text discordMessage = Text.literal("[Bazaar utils] ")
            .formatted(Formatting.WHITE)
            .append(Text.literal("For more help or to report a bug, join the ")
                    .formatted(Formatting.GREEN)
                    .append(Util.DISCORDLINK)
                    .append(Text.literal("!")
                            .formatted(Formatting.GREEN)));
    private static final Text updateMessage = Text.literal("[Bazaar Utils] ")
            .formatted(Formatting.WHITE)
            .append(Text.literal(BazaarUtils.getUpdateNotes())
                    .formatted(Formatting.DARK_GREEN));


    @Override
    public void subscribe(){
        registerWelcomeMessageSender();
    }

    //TODO gotta be a better way to do the null checks
    private static void registerWelcomeMessageSender() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
                var isFirstLoad = BUConfig.get().firstLoad;
                if (isFirstLoad) {
                    Util.tickExecuteLater(40, () -> {
                        if (client.player != null)
                            client.player.sendMessage(welcomeMessage, false);
                        Util.tickExecuteLater(60, () -> {
                            if (client.player != null)
                                Util.notifyAll(Util.HELPMESSAGE);

                            Util.tickExecuteLater(40, () -> {
                                if (client.player != null)
                                    client.player.sendMessage(discordMessage, false);
                            });

                        });
                    });
                    BUConfig.get().firstLoad = false;
                    BUConfig.HANDLER.save();
                }

                if(BazaarUtils.updatedMajorVersion && !isFirstLoad){
                    Util.tickExecuteLater(40, () -> client.player.sendMessage(updateMessage, false));
                    Util.tickExecuteLater(41, () -> client.player.sendMessage(Util.CHANGELOG, false));
                    BazaarUtils.updatedMajorVersion = false;
                }
        });
    }

}
