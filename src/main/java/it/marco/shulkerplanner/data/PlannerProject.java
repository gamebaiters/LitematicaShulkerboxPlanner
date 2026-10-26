package it.marco.shulkerplanner.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import fi.dy.masa.malilib.util.data.json.JsonUtils;

import it.marco.shulkerplanner.plan.CountMode;
import it.marco.shulkerplanner.plan.ItemRequirement;
import it.marco.shulkerplanner.plan.PackMode;
import it.marco.shulkerplanner.plan.PlannedBox;

/**
 * Everything that gets saved per schematic: the material snapshot, the planner options,
 * the excluded and gathered items, the filled boxes and the reminders.
 */
public class PlannerProject
{
    public static final int COLOR_UNDYED = 0;
    public static final int COLOR_RAINBOW = 17;
    public static final int COLOR_COUNT = 18;

    private final String name;
    private final List<MaterialRow> materials = new ArrayList<>();
    private final Set<String> excluded = new LinkedHashSet<>();
    private final Set<String> gathered = new LinkedHashSet<>();
    private final Map<String, Integer> doneBoxes = new HashMap<>();
    private final List<Reminder> reminders = new ArrayList<>();
    private String sourceTitle = "";
    private String schematicName = "";
    private long materialsUpdated;

    private CountMode countMode = CountMode.MISSING;
    private PackMode packMode = PackMode.SMART;
    private MaterialSort materialSort = MaterialSort.COUNT;
    private boolean roundUpStacks;
    private boolean fillSpareSlots;
    private boolean hideExcluded;
    private int multiplier = 1;
    private int boxColor = 11; // purple, the classic shulker color

    public PlannerProject(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return this.name;
    }

    // ---------------------------------------------------------------- materials

    public List<MaterialRow> getMaterials()
    {
        return this.materials;
    }

    public void setMaterials(List<MaterialRow> materials, String sourceTitle)
    {
        this.materials.clear();
        this.materials.addAll(materials);
        this.sourceTitle = sourceTitle;
        this.materialsUpdated = System.currentTimeMillis();
    }

    /**
     * @return the name of the schematic this project was created for, or the project name if unknown
     */
    public String getSchematicName()
    {
        return this.schematicName.isEmpty() ? this.name : this.schematicName;
    }

    public void setSchematicName(String schematicName)
    {
        this.schematicName = schematicName != null ? schematicName : "";
    }

    public String getSourceTitle()
    {
        return this.sourceTitle;
    }

    public long getMaterialsUpdated()
    {
        return this.materialsUpdated;
    }

    @Nullable
    public MaterialRow getMaterial(String id)
    {
        for (MaterialRow row : this.materials)
        {
            if (row.getId().equals(id))
            {
                return row;
            }
        }

        return null;
    }

    /**
     * @return the amount of this material that should be packed, based on the count mode and the multiplier
     */
    public int getRequiredCount(MaterialRow row)
    {
        long total = (long) row.getTotal() * this.multiplier;
        long missing = this.multiplier > 1 ? total : row.getMissing();

        long count = switch (this.countMode)
        {
            case TOTAL -> total;
            case MISSING -> missing;
            case TO_GATHER -> Math.max(0, missing - row.getAvailable());
        };

        return (int) Math.min(Integer.MAX_VALUE, count);
    }

    public List<ItemRequirement> buildRequirements()
    {
        List<ItemRequirement> list = new ArrayList<>();

        for (MaterialRow row : this.materials)
        {
            if (this.isExcluded(row.getId()) || row.isValid() == false)
            {
                continue;
            }

            int count = this.getRequiredCount(row);

            if (count <= 0)
            {
                continue;
            }

            ItemStack stack = row.getStack();
            Item item = stack.getItem();
            int maxStack = Math.max(1, Math.min(99, stack.getMaxStackSize()));
            boolean boxable = item.canFitInsideContainerItems();

            list.add(new ItemRequirement(row.getId(), stack, count, maxStack, boxable, BuiltInRegistries.ITEM.getId(item)));
        }

        return list;
    }

    // ---------------------------------------------------------------- item states

    public boolean isExcluded(String id)
    {
        return this.excluded.contains(id);
    }

    public void setExcluded(String id, boolean excluded)
    {
        if (excluded)
        {
            this.excluded.add(id);
        }
        else
        {
            this.excluded.remove(id);
        }
    }

    public int getExcludedCount()
    {
        int count = 0;

        for (MaterialRow row : this.materials)
        {
            if (this.excluded.contains(row.getId()))
            {
                ++count;
            }
        }

        return count;
    }

    public void clearExcluded()
    {
        this.excluded.clear();
    }

    public boolean isGathered(String id)
    {
        return this.gathered.contains(id);
    }

    public void setGathered(String id, boolean gathered)
    {
        if (gathered)
        {
            this.gathered.add(id);
        }
        else
        {
            this.gathered.remove(id);
        }
    }

    // ---------------------------------------------------------------- boxes

    /**
     * Identical boxes (same contents) share one counter, so marking one of them done
     * marks "one more of those" as done, regardless of which one it was.
     */
    public boolean isBoxDone(PlannedBox box)
    {
        return box.getDuplicateOrdinal() < this.doneBoxes.getOrDefault(box.getSignature(), 0);
    }

    public void setBoxDone(PlannedBox box, boolean done)
    {
        int count = this.doneBoxes.getOrDefault(box.getSignature(), 0);
        count = Math.max(0, count + (done ? 1 : -1));

        if (count > 0)
        {
            this.doneBoxes.put(box.getSignature(), count);
        }
        else
        {
            this.doneBoxes.remove(box.getSignature());
        }
    }

    public int getDoneCount(String signature)
    {
        return this.doneBoxes.getOrDefault(signature, 0);
    }

    public void setDoneCount(String signature, int count)
    {
        if (count > 0)
        {
            this.doneBoxes.put(signature, count);
        }
        else
        {
            this.doneBoxes.remove(signature);
        }
    }

    public void clearDoneBoxes()
    {
        this.doneBoxes.clear();
    }

    // ---------------------------------------------------------------- reminders

    public List<Reminder> getReminders()
    {
        return this.reminders;
    }

    public void addReminder(Reminder reminder)
    {
        this.reminders.add(reminder);
    }

    public boolean hasReminderFor(String itemId)
    {
        for (Reminder reminder : this.reminders)
        {
            if (itemId.equals(reminder.getItemId()))
            {
                return true;
            }
        }

        return false;
    }

    public boolean hasOpenReminder(String text)
    {
        for (Reminder reminder : this.reminders)
        {
            if (reminder.isDone() == false && reminder.getText().equalsIgnoreCase(text.trim()))
            {
                return true;
            }
        }

        return false;
    }

    public int getOpenReminderCount()
    {
        int count = 0;

        for (Reminder reminder : this.reminders)
        {
            if (reminder.isDone() == false)
            {
                ++count;
            }
        }

        return count;
    }

    // ---------------------------------------------------------------- options

    public CountMode getCountMode()
    {
        return this.countMode;
    }

    public void setCountMode(CountMode countMode)
    {
        this.countMode = countMode;
    }

    public PackMode getPackMode()
    {
        return this.packMode;
    }

    public void setPackMode(PackMode packMode)
    {
        this.packMode = packMode;
    }

    public MaterialSort getMaterialSort()
    {
        return this.materialSort;
    }

    public void setMaterialSort(MaterialSort materialSort)
    {
        this.materialSort = materialSort;
    }

    public boolean getRoundUpStacks()
    {
        return this.roundUpStacks;
    }

    public void setRoundUpStacks(boolean roundUpStacks)
    {
        this.roundUpStacks = roundUpStacks;
    }

    public boolean getFillSpareSlots()
    {
        return this.fillSpareSlots;
    }

    public void setFillSpareSlots(boolean fillSpareSlots)
    {
        this.fillSpareSlots = fillSpareSlots;
    }

    public boolean getHideExcluded()
    {
        return this.hideExcluded;
    }

    public void setHideExcluded(boolean hideExcluded)
    {
        this.hideExcluded = hideExcluded;
    }

    public int getMultiplier()
    {
        return this.multiplier;
    }

    public void setMultiplier(int multiplier)
    {
        this.multiplier = Math.max(1, Math.min(10000, multiplier));
    }

    public int getBoxColor()
    {
        return this.boxColor;
    }

    public void setBoxColor(int boxColor)
    {
        this.boxColor = Math.floorMod(boxColor, COLOR_COUNT);
    }

    /**
     * @return the shulker box item to use for the given (1-based) box number
     */
    public Item getShulkerItem(int boxIndex)
    {
        int color = this.boxColor;

        if (color == COLOR_RAINBOW)
        {
            color = 1 + Math.floorMod(boxIndex - 1, 16);
        }

        if (color <= COLOR_UNDYED || color > 16)
        {
            return Items.SHULKER_BOX;
        }

        return Items.DYED_SHULKER_BOX.pick(DyeColor.byId(color - 1));
    }

    // ---------------------------------------------------------------- json

    public JsonObject toJson()
    {
        JsonObject obj = new JsonObject();

        obj.addProperty("name", this.name);
        obj.addProperty("source_title", this.sourceTitle);
        obj.addProperty("schematic_name", this.schematicName);
        obj.addProperty("materials_updated", this.materialsUpdated);

        JsonObject options = new JsonObject();
        options.addProperty("count_mode", this.countMode.getName());
        options.addProperty("pack_mode", this.packMode.getName());
        options.addProperty("material_sort", this.materialSort.name());
        options.addProperty("round_up_stacks", this.roundUpStacks);
        options.addProperty("fill_spare_slots", this.fillSpareSlots);
        options.addProperty("hide_excluded", this.hideExcluded);
        options.addProperty("multiplier", this.multiplier);
        options.addProperty("box_color", this.boxColor);
        obj.add("options", options);

        JsonArray arr = new JsonArray();
        this.materials.forEach(row -> arr.add(row.toJson()));
        obj.add("materials", arr);

        obj.add("excluded", toArray(this.excluded));
        obj.add("gathered", toArray(this.gathered));

        JsonObject boxes = new JsonObject();
        this.doneBoxes.forEach(boxes::addProperty);
        obj.add("done_boxes", boxes);

        JsonArray rem = new JsonArray();
        this.reminders.forEach(r -> rem.add(r.toJson()));
        obj.add("reminders", rem);

        return obj;
    }

    public static PlannerProject fromJson(JsonObject obj, String fallbackName)
    {
        PlannerProject project = new PlannerProject(JsonUtils.getStringOrDefault(obj, "name", fallbackName));

        project.sourceTitle = JsonUtils.getStringOrDefault(obj, "source_title", "");
        project.schematicName = JsonUtils.getStringOrDefault(obj, "schematic_name", "");
        project.materialsUpdated = JsonUtils.getLongOrDefault(obj, "materials_updated", 0L);

        if (JsonUtils.hasObject(obj, "options"))
        {
            JsonObject options = obj.getAsJsonObject("options");
            project.countMode = CountMode.fromName(JsonUtils.getStringOrDefault(options, "count_mode", ""));
            project.packMode = PackMode.fromName(JsonUtils.getStringOrDefault(options, "pack_mode", ""));
            project.materialSort = MaterialSort.fromName(JsonUtils.getStringOrDefault(options, "material_sort", ""));
            project.roundUpStacks = JsonUtils.getBooleanOrDefault(options, "round_up_stacks", false);
            project.fillSpareSlots = JsonUtils.getBooleanOrDefault(options, "fill_spare_slots", false);
            project.hideExcluded = JsonUtils.getBooleanOrDefault(options, "hide_excluded", false);
            project.setMultiplier(JsonUtils.getIntegerOrDefault(options, "multiplier", 1));
            project.setBoxColor(JsonUtils.getIntegerOrDefault(options, "box_color", 11));
        }

        if (JsonUtils.hasArray(obj, "materials"))
        {
            for (JsonElement el : obj.getAsJsonArray("materials"))
            {
                if (el.isJsonObject())
                {
                    MaterialRow row = MaterialRow.fromJson(el.getAsJsonObject());

                    if (row != null)
                    {
                        project.materials.add(row);
                    }
                }
            }
        }

        readStrings(obj, "excluded", project.excluded);
        readStrings(obj, "gathered", project.gathered);

        if (JsonUtils.hasObject(obj, "done_boxes"))
        {
            for (Map.Entry<String, JsonElement> entry : obj.getAsJsonObject("done_boxes").entrySet())
            {
                try
                {
                    int count = entry.getValue().getAsInt();

                    if (count > 0)
                    {
                        project.doneBoxes.put(entry.getKey(), count);
                    }
                }
                catch (Exception ignore) {}
            }
        }

        if (JsonUtils.hasArray(obj, "reminders"))
        {
            for (JsonElement el : obj.getAsJsonArray("reminders"))
            {
                if (el.isJsonObject())
                {
                    Reminder reminder = Reminder.fromJson(el.getAsJsonObject());

                    if (reminder != null)
                    {
                        project.reminders.add(reminder);
                    }
                }
            }
        }

        return project;
    }

    private static JsonArray toArray(Set<String> set)
    {
        JsonArray arr = new JsonArray();
        set.forEach(s -> arr.add(new JsonPrimitive(s)));
        return arr;
    }

    private static void readStrings(JsonObject obj, String key, Set<String> out)
    {
        if (JsonUtils.hasArray(obj, key))
        {
            for (JsonElement el : obj.getAsJsonArray(key))
            {
                if (el.isJsonPrimitive())
                {
                    out.add(el.getAsString());
                }
            }
        }
    }
}
