package com.github.mkram17.bazaarutils.misc;

import com.github.mkram17.bazaarutils.utils.Util;
import com.google.gson.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Lightweight Gson (de-)serializer for {@link ItemStack} that works on
 * vanilla 1.8.9 — no DataFixer / Codec dependencies required.
 * <p>
 * The stack is written to an {@link NBTTagCompound} via
 * {@link ItemStack#writeToNBT(NBTTagCompound)}; the compound’s
 * <em>string</em> representation becomes a single JSON string.  
 * The reverse direction reconstructs the tag with
 * {@link JsonToNBT#getTagFromJson(String)} and feeds it into
 * {@link ItemStack#loadItemStackFromNBT(NBTTagCompound)}.
 */
public final class ItemStackCodecGsonAdapter
        implements JsonSerializer<ItemStack>, JsonDeserializer<ItemStack> {

    /* ────────────────────────── encode ────────────────────────── */

    @Override
    public JsonElement serialize(ItemStack stack,
                                 java.lang.reflect.Type type,
                                 JsonSerializationContext ctx) {

        if (stack == null || stack.stackSize == 0)            // empty / null
            return JsonNull.INSTANCE;

        try {
            NBTTagCompound tag = new NBTTagCompound();
            stack.writeToNBT(tag);
            return new JsonPrimitive(tag.toString());         // raw NBT JSON
        } catch (Exception ex) {
            Util.notifyError("ItemStack→JSON serialisation failed", ex);
            return JsonNull.INSTANCE;
        }
    }

    /* ────────────────────────── decode ────────────────────────── */

    @Override
    public ItemStack deserialize(JsonElement json,
                                 java.lang.reflect.Type type,
                                 JsonDeserializationContext ctx)
            throws JsonParseException {

        if (json == null || json.isJsonNull())
            return null;

        if (!json.isJsonPrimitive() || !json.getAsJsonPrimitive().isString())
            throw new JsonParseException("Expected JSON string for ItemStack");

        try {
            String nbtString = json.getAsString();
            NBTTagCompound tag = JsonToNBT.func_150315_a(nbtString); // getTagFromJson
            return ItemStack.loadItemStackFromNBT(tag);
        } catch (Exception ex) {
            Util.notifyError("JSON→ItemStack deserialisation failed", ex);
            return null;
        }
    }
}
