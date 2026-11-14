package it.marco.shulkerplanner.recipe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * All the known recipes, by result item
 */
public class RecipeIndex
{
    public static final RecipeIndex EMPTY = new RecipeIndex(List.of());

    private final Map<String, List<SimpleRecipe>> byResult = new HashMap<>();
    private final int size;

    public RecipeIndex(Collection<SimpleRecipe> recipes)
    {
        for (SimpleRecipe recipe : recipes)
        {
            this.byResult.computeIfAbsent(recipe.result(), k -> new ArrayList<>()).add(recipe);
        }

        // Stable order: by station (stonecutter first), then by id
        for (List<SimpleRecipe> list : this.byResult.values())
        {
            list.sort(Comparator.comparing((SimpleRecipe r) -> r.type().ordinal()).thenComparing(SimpleRecipe::id));
        }

        this.size = recipes.size();
    }

    public List<SimpleRecipe> getRecipesFor(String itemId)
    {
        return this.byResult.getOrDefault(itemId, List.of());
    }

    public java.util.Set<String> getResults()
    {
        return this.byResult.keySet();
    }

    public int size()
    {
        return this.size;
    }

    public boolean isEmpty()
    {
        return this.size == 0;
    }
}
