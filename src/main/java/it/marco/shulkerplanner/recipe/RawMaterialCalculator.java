package it.marco.shulkerplanner.recipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * Breaks a list of required items down into the raw materials needed to craft them.
 * <p>
 * Every item that has a recipe is crafted, using the recipe that needs the fewest raw items.
 * Items without a recipe, items marked as raw by the player, and items that can only be made out of
 * themselves (iron ingot from an iron block from iron ingots...) are raw materials.
 * Crafts are counted in whole batches: 6 stairs need 2 crafts of 4, which is 12 planks, and
 * every intermediate item is crafted for all its uses at once.
 */
public class RawMaterialCalculator
{
    private static final double INF = Double.POSITIVE_INFINITY;
    /** Prefer shorter crafting chains when the amount of raw items is the same */
    private static final double STEP_PENALTY = 0.001;
    /** Prefer raw ores (raw iron) over ore blocks, which need silk touch */
    private static final double ORE_PENALTY = 0.01;

    /**
     * Items that have a recipe but are normally gathered, not crafted
     * (cobblestone can be cut from stone, leather crafted from rabbit hide...)
     */
    public static final Set<String> GATHERED_ITEMS = Set.of(
            "minecraft:cobblestone", "minecraft:cobbled_deepslate", "minecraft:leather", "minecraft:white_wool",
            "minecraft:magma_block", "minecraft:dripstone_block", "minecraft:amethyst_block", "minecraft:blackstone");

    private final RecipeIndex index;
    private final Options options;
    @Nullable private Map<String, Double> costs;
    private final Map<String, SimpleRecipe> choice = new HashMap<>();
    private final Set<String> raw = new HashSet<>();

    public RawMaterialCalculator(RecipeIndex index, Options options)
    {
        this.index = index;
        this.options = options;
    }

    /**
     * @param baseItems       items that never get broken down
     * @param recipeChoice    item id to the id of the recipe to use for it
     * @param allowSmelting   use furnace recipes (glass from sand, stone from cobblestone...)
     * @param allowStonecutter use stonecutter recipes
     */
    public record Options(Set<String> baseItems, Map<String, String> recipeChoice, boolean allowSmelting, boolean allowStonecutter)
    {
        public static Options defaults()
        {
            return new Options(Set.of(), Map.of(), true, true);
        }
    }

    /**
     * One crafting step of the result
     *
     * @param item     the crafted item
     * @param recipe   the recipe used
     * @param crafts   how many times the recipe is used
     * @param needed   how many of the item are needed
     */
    public record Step(String item, SimpleRecipe recipe, long crafts, long needed)
    {
        public long produced()
        {
            return this.crafts * this.recipe.resultCount();
        }

        public long leftover()
        {
            return this.produced() - this.needed;
        }
    }

    /**
     * @param raw   the raw materials and their amounts, biggest first
     * @param steps the crafting steps, in the order they have to be done (ingredients first)
     */
    public record Result(Map<String, Long> raw, List<Step> steps)
    {
        @Nullable
        public Step getStep(String item)
        {
            for (Step step : this.steps)
            {
                if (step.item().equals(item))
                {
                    return step;
                }
            }

            return null;
        }
    }

    public boolean isAllowed(SimpleRecipe recipe)
    {
        return switch (recipe.type())
        {
            case SMELTING -> this.options.allowSmelting();
            case STONECUTTING -> this.options.allowStonecutter();
            default -> true;
        };
    }

    /**
     * @return the recipes that can be used for the item, after the station options
     */
    public List<SimpleRecipe> getUsableRecipes(String item)
    {
        List<SimpleRecipe> list = new ArrayList<>();

        for (SimpleRecipe recipe : this.index.getRecipesFor(item))
        {
            if (this.isAllowed(recipe))
            {
                list.add(recipe);
            }
        }

        return list;
    }

    /**
     * @return the recipe that would be used for the item, or null if it is a raw material
     */
    @Nullable
    public SimpleRecipe getChosenRecipe(String item)
    {
        this.ensureCosts();
        return this.choice.get(item);
    }

    public boolean isRaw(String item)
    {
        return this.getChosenRecipe(item) == null;
    }

    public Result calculate(Map<String, Long> demand)
    {
        // Pick the recipe for every reachable item, then order the items so that every item comes
        // before its ingredients (consumers first), and push the demand down that order.
        List<String> order = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> onStack = new HashSet<>();
        Set<String> forcedRaw = new HashSet<>();

        for (String item : demand.keySet())
        {
            this.visitOrder(item, visited, onStack, order, forcedRaw);
        }

        Map<String, Long> need = new HashMap<>(demand);
        Map<String, Long> raw = new LinkedHashMap<>();
        List<Step> steps = new ArrayList<>();

        // order is a post order (ingredients first): walk it backwards
        for (int i = order.size() - 1; i >= 0; --i)
        {
            String item = order.get(i);
            long amount = need.getOrDefault(item, 0L);

            if (amount <= 0)
            {
                continue;
            }

            SimpleRecipe recipe = forcedRaw.contains(item) ? null : this.getChosenRecipe(item);

            if (recipe == null)
            {
                raw.merge(item, amount, Long::sum);
                continue;
            }

            long crafts = Math.ceilDiv(amount, recipe.resultCount());
            steps.add(new Step(item, recipe, crafts, amount));

            for (SimpleRecipe.Ingredient ing : recipe.ingredients())
            {
                String option = this.bestOption(ing);
                need.merge(option, crafts * ing.count(), Long::sum);
            }
        }

        // Steps in crafting order: ingredients first
        List<Step> ordered = new ArrayList<>(steps.size());

        for (String item : order)
        {
            for (Step step : steps)
            {
                if (step.item().equals(item))
                {
                    ordered.add(step);
                }
            }
        }

        Map<String, Long> sorted = new LinkedHashMap<>();
        raw.entrySet().stream()
           .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
           .forEach(e -> sorted.put(e.getKey(), e.getValue()));

        return new Result(sorted, ordered);
    }

    private void visitOrder(String item, Set<String> visited, Set<String> onStack, List<String> order, Set<String> forcedRaw)
    {
        if (visited.contains(item))
        {
            return;
        }

        visited.add(item);
        onStack.add(item);

        SimpleRecipe recipe = this.getChosenRecipe(item);

        if (recipe != null)
        {
            for (SimpleRecipe.Ingredient ing : recipe.ingredients())
            {
                String option = this.bestOption(ing);

                if (onStack.contains(option))
                {
                    // Should not happen with the cycle checks of the cost evaluation, but never loop
                    forcedRaw.add(item);
                    continue;
                }

                this.visitOrder(option, visited, onStack, order, forcedRaw);
            }
        }

        onStack.remove(item);
        order.add(item);
    }

    /**
     * @return the cheapest option of an ingredient (the first one on ties, tags list the "main" item first)
     */
    public String bestOption(SimpleRecipe.Ingredient ing)
    {
        this.ensureCosts();
        String best = ing.options().getFirst();
        double bestCost = INF;

        for (String option : ing.options())
        {
            double cost = this.cost(option);

            if (cost < bestCost)
            {
                bestCost = cost;
                best = option;
            }
        }

        return best;
    }

    private double rawCost(String item)
    {
        return item.endsWith("_ore") ? 1 + ORE_PENALTY : 1;
    }

    private double cost(String item)
    {
        Double cost = this.costs != null ? this.costs.get(item) : null;

        if (cost != null)
        {
            return cost;
        }

        // Items never seen as a result of a recipe: raw
        return this.rawCost(item);
    }

    private boolean isBase(String item)
    {
        return this.options.baseItems().contains(item) || GATHERED_ITEMS.contains(item);
    }

    /**
     * Ore blocks only make sense as an ingredient with silk touch, the ore drops the item itself
     */
    private static boolean isOreOnly(SimpleRecipe recipe)
    {
        for (SimpleRecipe.Ingredient ing : recipe.ingredients())
        {
            for (String option : ing.options())
            {
                if (option.endsWith("_ore") == false)
                {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * @return the recipes the item can be made with: allowed stations, the player's choice, no ore smelting
     */
    private List<SimpleRecipe> candidates(String item)
    {
        List<SimpleRecipe> list = new ArrayList<>();
        String forced = this.options.recipeChoice().get(item);

        for (SimpleRecipe recipe : this.getUsableRecipes(item))
        {
            if (forced != null && recipe.id().equals(forced))
            {
                return List.of(recipe);
            }

            if (isOreOnly(recipe) == false)
            {
                list.add(recipe);
            }
        }

        return list;
    }

    /**
     * Cost of every item in raw items, as a fixed point: raw items cost 1, a crafted item costs
     * the cheapest of its recipes. Items that can only be made out of each other (diamond and diamond block)
     * have no finite cost after the first pass: the ones that come out of an "unpacking" recipe
     * (1 block gives 9 diamonds) become raw, then the rest if still needed.
     */
    private void ensureCosts()
    {
        if (this.costs != null)
        {
            return;
        }

        Map<String, Double> costs = new HashMap<>();
        Set<String> crafted = new HashSet<>();

        // Every item reachable from the recipes
        Set<String> items = new HashSet<>();

        for (String result : this.index.getResults())
        {
            items.add(result);

            for (SimpleRecipe recipe : this.index.getRecipesFor(result))
            {
                for (SimpleRecipe.Ingredient ing : recipe.ingredients())
                {
                    items.addAll(ing.options());
                }
            }
        }

        for (String item : items)
        {
            if (this.isBase(item) || this.candidates(item).isEmpty())
            {
                costs.put(item, this.rawCost(item));
                this.raw.add(item);
            }
            else
            {
                costs.put(item, INF);
                crafted.add(item);
            }
        }

        this.costs = costs;
        this.relax(crafted);

        // Cycles without a way in: one item at a time becomes raw, unpacked items first
        while (true)
        {
            String pick = null;
            int pickRank = Integer.MAX_VALUE;

            for (String item : crafted)
            {
                if (this.raw.contains(item) || costs.get(item) != INF)
                {
                    continue;
                }

                int rank = this.stuckRank(item);

                if (rank < pickRank || (rank == pickRank && item.compareTo(pick) < 0))
                {
                    pick = item;
                    pickRank = rank;
                }
            }

            if (pick == null)
            {
                break;
            }

            costs.put(pick, this.rawCost(pick));
            this.raw.add(pick);
            this.relax(crafted);
        }

        // The chosen recipe of every crafted item
        for (String item : crafted)
        {
            if (this.raw.contains(item))
            {
                continue;
            }

            SimpleRecipe best = null;
            double bestCost = INF;

            for (SimpleRecipe recipe : this.candidates(item))
            {
                double cost = this.recipeCost(recipe, costs);

                if (cost < bestCost - 1e-12)
                {
                    bestCost = cost;
                    best = recipe;
                }
            }

            if (best != null)
            {
                this.choice.put(item, best);
            }
        }
    }

    /**
     * Which item of a closed cycle becomes raw first: the one that comes out of unpacking a block
     * (diamond from a diamond block), then one that is needed to make itself (smithing templates)
     */
    private int stuckRank(String item)
    {
        int rank = 3;

        for (SimpleRecipe recipe : this.candidates(item))
        {
            if (recipe.resultCount() >= 2 && recipe.ingredients().size() == 1 && recipe.ingredients().getFirst().count() == 1)
            {
                // Unpacking: the packed item is made out of this one (diamond block <-> diamond)
                for (String packed : recipe.ingredients().getFirst().options())
                {
                    for (SimpleRecipe back : this.candidates(packed))
                    {
                        for (SimpleRecipe.Ingredient ing : back.ingredients())
                        {
                            if (ing.options().contains(item))
                            {
                                return 1;
                            }
                        }
                    }
                }
            }

            for (SimpleRecipe.Ingredient ing : recipe.ingredients())
            {
                if (ing.options().contains(item))
                {
                    rank = 2;
                }
            }
        }

        return rank;
    }

    private double recipeCost(SimpleRecipe recipe, Map<String, Double> costs)
    {
        double cost = 0;

        for (SimpleRecipe.Ingredient ing : recipe.ingredients())
        {
            double best = INF;

            for (String option : ing.options())
            {
                best = Math.min(best, costs.getOrDefault(option, this.rawCost(option)));
            }

            cost += best * ing.count();
        }

        return cost / recipe.resultCount() + STEP_PENALTY + (recipe.type() == SimpleRecipe.RecipeType.SMELTING ? STEP_PENALTY / 2 : 0);
    }

    private void relax(Set<String> crafted)
    {
        Map<String, Double> costs = this.costs;

        for (int iteration = 0; iteration < 200; ++iteration)
        {
            boolean changed = false;

            for (String item : crafted)
            {
                if (this.raw.contains(item))
                {
                    continue;
                }

                double current = costs.get(item);

                for (SimpleRecipe recipe : this.candidates(item))
                {
                    double cost = this.recipeCost(recipe, costs);

                    if (cost < current - 1e-12)
                    {
                        current = cost;
                        changed = true;
                    }
                }

                costs.put(item, current);
            }

            if (changed == false)
            {
                break;
            }
        }
    }
}
