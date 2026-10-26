package it.marco.shulkerplanner.data;

import javax.annotation.Nullable;
import com.google.gson.JsonObject;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import fi.dy.masa.malilib.util.data.json.JsonUtils;

/**
 * A snapshot of one Litematica material list entry.
 * The snapshot is saved with the project, so that the planner keeps working
 * after a restart even when the material list hasn't been re-created yet.
 */
public class MaterialRow
{
    private final String id;
    private final int total;
    private final int missing;
    private int available;
    @Nullable private ItemStack stack;

    public MaterialRow(String id, int total, int missing)
    {
        this.id = id;
        this.total = total;
        this.missing = missing;
    }

    public String getId()
    {
        return this.id;
    }

    public int getTotal()
    {
        return this.total;
    }

    public int getMissing()
    {
        return this.missing;
    }

    public int getAvailable()
    {
        return this.available;
    }

    public void setAvailable(int available)
    {
        this.available = available;
    }

    /**
     * @return a stack of size 1 of this item
     */
    public ItemStack getStack()
    {
        if (this.stack == null)
        {
            Identifier rl = Identifier.tryParse(this.id);
            Item item = rl != null ? BuiltInRegistries.ITEM.getValue(rl) : null;

            // Unknown items (removed mods etc.) get shown as a barrier
            this.stack = new ItemStack(item != null && item != Items.AIR ? item : Items.BARRIER);
        }

        return this.stack;
    }

    public boolean isValid()
    {
        return this.getStack().getItem() != Items.BARRIER || this.id.equals("minecraft:barrier");
    }

    public String getDisplayName()
    {
        return this.isValid() ? this.getStack().getHoverName().getString() : this.id;
    }

    public JsonObject toJson()
    {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", this.id);
        obj.addProperty("total", this.total);
        obj.addProperty("missing", this.missing);
        return obj;
    }

    @Nullable
    public static MaterialRow fromJson(JsonObject obj)
    {
        if (JsonUtils.hasString(obj, "id") == false)
        {
            return null;
        }

        return new MaterialRow(JsonUtils.getString(obj, "id"),
                               JsonUtils.getIntegerOrDefault(obj, "total", 0),
                               JsonUtils.getIntegerOrDefault(obj, "missing", 0));
    }
}
