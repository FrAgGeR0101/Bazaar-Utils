package com.github.mkram17.bazaarutils.features.restrictsell;

/**
 * Lightweight POJO representing one line inside the Hypixel insta-sell
 * confirmation (“ 128x Cobblestone ”, etc.).
 *
 * No Lombok – explicit fields and accessors for 1.8.9 compatibility.
 */
public final class SellItem {

    /* ─── data ─── */
    private int    volume;
    private String name;

    /* ─── ctor ─── */
    public SellItem(int volume, String name) {
        this.volume = volume;
        this.name   = name;
    }

    /* ─── getters / setters ─── */
    public int    getVolume()         { return volume; }
    public void   setVolume(int v)    { this.volume = v; }

    public String getName()           { return name;   }
    public void   setName(String n)   { this.name = n; }
}
