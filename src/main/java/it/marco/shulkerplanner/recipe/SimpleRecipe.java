package it.marco.shulkerplanner.recipe;

import java.util.List;

/**
 * A recipe reduced to what the raw materials calculation needs: what goes in, what comes out.
 *
 * @param id          the recipe id (minecraft:stone_bricks_from_stone_stonecutting)
 * @param type        the kind of station
 * @param result      the result item id
 * @param resultCount how many result items one craft gives
 * @param ingredients the ingredients of one craft
 */
public record SimpleRecipe(String id, RecipeType type, String result, int resultCount, List<Ingredient> ingredients)
{
    /**
     * One ingredient slot group: any of the options, count times
     */
    public record Ingredient(List<String> options, int count)
    {
    }

    public enum RecipeType
    {
        // Ties between recipes go to the first type: the stonecutter never wastes a partial batch
        STONECUTTING    ("stonecutting",    "minecraft:stonecutter"),
        CRAFTING        ("crafting",        "minecraft:crafting_table"),
        SMELTING        ("smelting",        "minecraft:furnace"),
        SMITHING        ("smithing",        "minecraft:smithing_table");

        private final String name;
        private final String stationItem;

        RecipeType(String name, String stationItem)
        {
            this.name = name;
            this.stationItem = stationItem;
        }

        public String getName()
        {
            return this.name;
        }

        public String getStationItem()
        {
            return this.stationItem;
        }
    }
}
