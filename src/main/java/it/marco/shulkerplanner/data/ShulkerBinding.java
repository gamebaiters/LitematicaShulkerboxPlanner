package it.marco.shulkerplanner.data;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nullable;
import com.google.gson.JsonObject;

import net.minecraft.core.BlockPos;

import fi.dy.masa.malilib.util.data.json.JsonUtils;

import it.marco.shulkerplanner.plan.PlannedBox;

/**
 * A real shulker box the player linked by hand to one box (phase) of the plan.
 * The link stays when the box is broken, carried around and placed again: while it's in the
 * inventory it's recognised by its color, name and contents, and when it's placed or opened again
 * the new position is taken over.
 */
public class ShulkerBinding
{
    private final int boxId;
    private String itemId;
    @Nullable private String customName;
    @Nullable private String dimension;
    @Nullable private BlockPos pos;
    private Map<String, Integer> contents = new LinkedHashMap<>();
    private long lastSeen;

    public ShulkerBinding(int boxId, String itemId, @Nullable String customName)
    {
        this.boxId = boxId;
        this.itemId = itemId;
        this.customName = customName;
        this.lastSeen = System.currentTimeMillis();
    }

    public int getBoxId()
    {
        return this.boxId;
    }

    /**
     * @return the shulker box item id, which is also its color (minecraft:purple_shulker_box)
     */
    public String getItemId()
    {
        return this.itemId;
    }

    public void setItemId(String itemId)
    {
        this.itemId = itemId;
    }

    @Nullable
    public String getCustomName()
    {
        return this.customName;
    }

    public void setCustomName(@Nullable String customName)
    {
        this.customName = customName;
    }

    /**
     * @return true if the box is placed somewhere (its position is known)
     */
    public boolean isPlaced()
    {
        return this.pos != null;
    }

    @Nullable
    public BlockPos getPos()
    {
        return this.pos;
    }

    @Nullable
    public String getDimension()
    {
        return this.dimension;
    }

    public boolean isAt(String dimension, BlockPos pos)
    {
        return this.pos != null && this.pos.equals(pos) && dimension.equals(this.dimension);
    }

    public void setPlaced(String dimension, BlockPos pos)
    {
        this.dimension = dimension;
        this.pos = pos.immutable();
        this.lastSeen = System.currentTimeMillis();
    }

    /**
     * The box was broken (picked up): from now on it's looked for in the inventory
     */
    public void setCarried()
    {
        this.pos = null;
        this.lastSeen = System.currentTimeMillis();
    }

    /**
     * @return the contents seen the last time (screen open, or the item in the inventory)
     */
    public Map<String, Integer> getContents()
    {
        return this.contents;
    }

    public String getContentsSignature()
    {
        return PlannedBox.createSignature(this.contents);
    }

    public void setContents(Map<String, Integer> contents)
    {
        this.contents = new LinkedHashMap<>(contents);
        this.lastSeen = System.currentTimeMillis();
    }

    public long getLastSeen()
    {
        return this.lastSeen;
    }

    /**
     * @return true if the given shulker box item (id, name and contents) is this box while carried
     */
    public boolean matchesItem(String itemId, @Nullable String customName, Map<String, Integer> contents)
    {
        if (this.itemId.equals(itemId) == false)
        {
            return false;
        }

        if (this.customName != null && this.customName.equals(customName) == false)
        {
            return false;
        }

        return PlannedBox.createSignature(contents).equals(this.getContentsSignature());
    }

    public JsonObject toJson()
    {
        JsonObject obj = new JsonObject();
        obj.addProperty("box", this.boxId);
        obj.addProperty("item", this.itemId);

        if (this.customName != null)
        {
            obj.addProperty("name", this.customName);
        }

        if (this.pos != null && this.dimension != null)
        {
            obj.addProperty("dim", this.dimension);
            obj.addProperty("x", this.pos.getX());
            obj.addProperty("y", this.pos.getY());
            obj.addProperty("z", this.pos.getZ());
        }

        JsonObject contents = new JsonObject();
        this.contents.forEach(contents::addProperty);
        obj.add("contents", contents);
        obj.addProperty("seen", this.lastSeen);

        return obj;
    }

    @Nullable
    public static ShulkerBinding fromJson(JsonObject obj)
    {
        if (JsonUtils.hasInteger(obj, "box") == false || JsonUtils.hasString(obj, "item") == false)
        {
            return null;
        }

        ShulkerBinding b = new ShulkerBinding(JsonUtils.getInteger(obj, "box"), JsonUtils.getString(obj, "item"),
                                              JsonUtils.hasString(obj, "name") ? JsonUtils.getString(obj, "name") : null);

        if (JsonUtils.hasString(obj, "dim") && JsonUtils.hasInteger(obj, "x"))
        {
            b.dimension = JsonUtils.getString(obj, "dim");
            b.pos = new BlockPos(JsonUtils.getInteger(obj, "x"), JsonUtils.getInteger(obj, "y"), JsonUtils.getInteger(obj, "z"));
        }

        if (JsonUtils.hasObject(obj, "contents"))
        {
            for (var e : obj.getAsJsonObject("contents").entrySet())
            {
                try
                {
                    b.contents.put(e.getKey(), e.getValue().getAsInt());
                }
                catch (Exception ignore) {}
            }
        }

        b.lastSeen = JsonUtils.getLongOrDefault(obj, "seen", 0L);
        return b;
    }
}
