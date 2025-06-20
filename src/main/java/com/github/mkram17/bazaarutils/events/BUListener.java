package com.github.mkram17.bazaarutils.events;

import com.github.mkram17.bazaarutils.BazaarUtils;
import com.github.mkram17.bazaarutils.data.BazaarData;
import com.github.mkram17.bazaarutils.misc.JoinMessages;
import com.github.mkram17.bazaarutils.utils.ItemUpdater;
import com.github.mkram17.bazaarutils.utils.Util;

import java.util.ArrayList;
import java.util.List;

public interface BUListener {
    List<BUListener> transientEvents = new ArrayList<>();
    //must add to getTransientEvents()
    void subscribe();

    static void addTransientEvents(){
        transientEvents.add(new ChestLoadedEvent());
        transientEvents.add(new ChatHandler());
        transientEvents.add(new JoinMessages());
        transientEvents.add(BazaarUtils.gui);
        transientEvents.add(new ItemUpdater());
        transientEvents.add(new BazaarData());
        transientEvents.add(new Util());
    }

    //dont use lombok here, it fucks shit up
    static List<BUListener> getTransientEvents(){
        return transientEvents;
    }
}
