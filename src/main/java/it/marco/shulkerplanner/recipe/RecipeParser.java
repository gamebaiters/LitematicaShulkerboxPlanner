package it.marco.shulkerplanner.recipe;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javax.annotation.Nullable;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Turns the recipe json files of a data pack (the vanilla one is inside the game jar) into {@link SimpleRecipe}s.
 * Only the recipes that make an item out of other items are kept: crafting, stonecutting, smelting and smithing.
 * Blasting, smoking and campfire recipes duplicate smelting ones and are skipped, as are the special recipes
 * (dyeing armor, fireworks, map cloning...).
 */
public class RecipeParser
{
    /**
     * @param tagResolver item tag id (without '#') to the item ids in that tag
     */
    @Nullable
    public static SimpleRecipe parse(String recipeId, JsonObject json, Function<String, List<String>> tagResolver)
    {
        try
        {
            String type = normalize(getString(json, "type"));

            return switch (type)
            {
                case "minecraft:crafting_shaped" -> parseShaped(recipeId, json, tagResolver);
                case "minecraft:crafting_shapeless" -> parseShapeless(recipeId, json, tagResolver);
                case "minecraft:crafting_transmute" -> parseTransmute(recipeId, json, tagResolver);
                case "minecraft:stonecutting" -> parseSingle(recipeId, json, tagResolver, SimpleRecipe.RecipeType.STONECUTTING);
                case "minecraft:smelting" -> parseSingle(recipeId, json, tagResolver, SimpleRecipe.RecipeType.SMELTING);
                case "minecraft:smithing_transform" -> parseSmithing(recipeId, json, tagResolver);
                default -> null;
            };
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private static String normalize(@Nullable String id)
    {
        if (id == null)
        {
            return "";
        }

        return id.indexOf(':') < 0 ? "minecraft:" + id : id;
    }

    @Nullable
    private static String getString(JsonObject obj, String key)
    {
        JsonElement el = obj.get(key);
        return el != null && el.isJsonPrimitive() ? el.getAsString() : null;
    }

    /**
     * @return the result item and count, or null
     */
    @Nullable
    private static Result parseResult(JsonObject json)
    {
        JsonElement el = json.get("result");

        if (el == null)
        {
            return null;
        }

        if (el.isJsonPrimitive())
        {
            return new Result(normalize(el.getAsString()), 1);
        }

        if (el.isJsonObject())
        {
            JsonObject obj = el.getAsJsonObject();
            String id = getString(obj, "id");

            if (id == null)
            {
                id = getString(obj, "item");
            }

            if (id == null)
            {
                return null;
            }

            int count = obj.has("count") ? obj.get("count").getAsInt() : 1;

            // Results with extra components (enchanted books, potions...) are not plain items
            if (obj.has("components") && obj.getAsJsonObject("components").size() > 0)
            {
                return null;
            }

            return new Result(normalize(id), Math.max(1, count));
        }

        return null;
    }

    /**
     * Reads an ingredient: "minecraft:stone", "#minecraft:planks", a list of those,
     * or the old object forms {"item": ...} / {"tag": ...}
     */
    @Nullable
    static List<String> parseIngredient(@Nullable JsonElement el, Function<String, List<String>> tagResolver)
    {
        if (el == null || el.isJsonNull())
        {
            return null;
        }

        List<String> options = new ArrayList<>();

        if (el.isJsonPrimitive())
        {
            addOption(el.getAsString(), options, tagResolver);
        }
        else if (el.isJsonArray())
        {
            for (JsonElement e : el.getAsJsonArray())
            {
                List<String> sub = parseIngredient(e, tagResolver);

                if (sub != null)
                {
                    for (String id : sub)
                    {
                        if (options.contains(id) == false)
                        {
                            options.add(id);
                        }
                    }
                }
            }
        }
        else if (el.isJsonObject())
        {
            JsonObject obj = el.getAsJsonObject();

            if (obj.has("item"))
            {
                addOption(obj.get("item").getAsString(), options, tagResolver);
            }
            else if (obj.has("tag"))
            {
                addOption("#" + obj.get("tag").getAsString(), options, tagResolver);
            }
        }

        return options.isEmpty() ? null : options;
    }

    private static void addOption(String str, List<String> options, Function<String, List<String>> tagResolver)
    {
        if (str.startsWith("#"))
        {
            for (String id : tagResolver.apply(normalize(str.substring(1))))
            {
                if (options.contains(id) == false)
                {
                    options.add(id);
                }
            }
        }
        else
        {
            String id = normalize(str);

            if (options.contains(id) == false)
            {
                options.add(id);
            }
        }
    }

    @Nullable
    private static SimpleRecipe parseShaped(String recipeId, JsonObject json, Function<String, List<String>> tagResolver)
    {
        Result result = parseResult(json);

        if (result == null || json.has("key") == false || json.has("pattern") == false)
        {
            return null;
        }

        JsonObject key = json.getAsJsonObject("key");
        Map<Character, Integer> counts = new LinkedHashMap<>();

        for (JsonElement row : json.getAsJsonArray("pattern"))
        {
            for (char c : row.getAsString().toCharArray())
            {
                if (c != ' ')
                {
                    counts.merge(c, 1, Integer::sum);
                }
            }
        }

        List<SimpleRecipe.Ingredient> ingredients = new ArrayList<>();

        for (Map.Entry<Character, Integer> e : counts.entrySet())
        {
            List<String> options = parseIngredient(key.get(String.valueOf(e.getKey())), tagResolver);

            if (options == null)
            {
                return null;
            }

            mergeIngredient(ingredients, options, e.getValue());
        }

        return ingredients.isEmpty() ? null : new SimpleRecipe(recipeId, SimpleRecipe.RecipeType.CRAFTING, result.id, result.count, ingredients);
    }

    @Nullable
    private static SimpleRecipe parseShapeless(String recipeId, JsonObject json, Function<String, List<String>> tagResolver)
    {
        Result result = parseResult(json);

        if (result == null || json.has("ingredients") == false)
        {
            return null;
        }

        JsonArray arr = json.getAsJsonArray("ingredients");
        List<SimpleRecipe.Ingredient> ingredients = new ArrayList<>();

        for (JsonElement el : arr)
        {
            List<String> options = parseIngredient(el, tagResolver);

            if (options == null)
            {
                return null;
            }

            mergeIngredient(ingredients, options, 1);
        }

        return ingredients.isEmpty() ? null : new SimpleRecipe(recipeId, SimpleRecipe.RecipeType.CRAFTING, result.id, result.count, ingredients);
    }

    /**
     * Transmute recipes (dyeing a shulker box, a bundle...): the input item plus the material give the result
     */
    @Nullable
    private static SimpleRecipe parseTransmute(String recipeId, JsonObject json, Function<String, List<String>> tagResolver)
    {
        Result result = parseResult(json);
        List<String> input = parseIngredient(json.get("input"), tagResolver);
        List<String> material = parseIngredient(json.get("material"), tagResolver);

        if (result == null || input == null || material == null)
        {
            return null;
        }

        // The undyed input of a dyed result: prefer the plain item over other colors
        List<String> inputs = new ArrayList<>(input);
        inputs.remove(result.id);

        if (inputs.isEmpty())
        {
            return null;
        }

        List<SimpleRecipe.Ingredient> ingredients = new ArrayList<>();
        mergeIngredient(ingredients, inputs, 1);
        mergeIngredient(ingredients, material, 1);

        return new SimpleRecipe(recipeId, SimpleRecipe.RecipeType.CRAFTING, result.id, result.count, ingredients);
    }

    @Nullable
    private static SimpleRecipe parseSingle(String recipeId, JsonObject json, Function<String, List<String>> tagResolver, SimpleRecipe.RecipeType type)
    {
        Result result = parseResult(json);
        List<String> options = parseIngredient(json.get("ingredient"), tagResolver);

        if (result == null || options == null)
        {
            return null;
        }

        return new SimpleRecipe(recipeId, type, result.id, result.count, List.of(new SimpleRecipe.Ingredient(options, 1)));
    }

    @Nullable
    private static SimpleRecipe parseSmithing(String recipeId, JsonObject json, Function<String, List<String>> tagResolver)
    {
        Result result = parseResult(json);

        if (result == null)
        {
            return null;
        }

        List<SimpleRecipe.Ingredient> ingredients = new ArrayList<>();

        for (String key : new String[] { "template", "base", "addition" })
        {
            List<String> options = parseIngredient(json.get(key), tagResolver);

            if (options != null)
            {
                mergeIngredient(ingredients, options, 1);
            }
        }

        return ingredients.isEmpty() ? null : new SimpleRecipe(recipeId, SimpleRecipe.RecipeType.SMITHING, result.id, result.count, ingredients);
    }

    private static void mergeIngredient(List<SimpleRecipe.Ingredient> list, List<String> options, int count)
    {
        for (int i = 0; i < list.size(); ++i)
        {
            SimpleRecipe.Ingredient ing = list.get(i);

            if (ing.options().equals(options))
            {
                list.set(i, new SimpleRecipe.Ingredient(options, ing.count() + count));
                return;
            }
        }

        list.add(new SimpleRecipe.Ingredient(List.copyOf(options), count));
    }

    private record Result(String id, int count)
    {
    }
}
