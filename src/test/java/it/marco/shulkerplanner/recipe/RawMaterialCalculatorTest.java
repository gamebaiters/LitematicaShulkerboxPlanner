package it.marco.shulkerplanner.recipe;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Raw materials of real vanilla recipes, read from the game jar on the test classpath
 */
class RawMaterialCalculatorTest
{
    private static RecipeIndex index;

    @BeforeAll
    static void loadVanilla() throws Exception
    {
        URL url = RawMaterialCalculatorTest.class.getClassLoader().getResource("data/minecraft/recipe/stone_bricks.json");
        assertNotNull(url, "vanilla recipes on the classpath");
        URI uri = url.toURI();
        FileSystem fs = null;

        if ("jar".equals(uri.getScheme()))
        {
            try { fs = FileSystems.newFileSystem(uri, Map.of()); }
            catch (java.nio.file.FileSystemAlreadyExistsException e) { fs = FileSystems.getFileSystem(uri); }
        }

        Path recipeDir = Path.of(uri).getParent();
        Path tagDir = recipeDir.getParent().resolve("tags").resolve("item");
        TagResolver tags = new TagResolver(tag -> readJson(tagDir.resolve(tag.substring(tag.indexOf(':') + 1) + ".json")), null);
        List<SimpleRecipe> recipes = new ArrayList<>();

        try (Stream<Path> files = Files.list(recipeDir))
        {
            for (Path file : files.toList())
            {
                String name = file.getFileName().toString();

                if (name.endsWith(".json"))
                {
                    SimpleRecipe r = RecipeParser.parse("minecraft:" + name.replace(".json", ""), readJson(file), tags);

                    if (r != null)
                    {
                        recipes.add(r);
                    }
                }
            }
        }

        index = new RecipeIndex(recipes);
        System.out.println("Vanilla recipes parsed: " + index.size());
        assertTrue(index.size() > 1200);
    }

    private static JsonObject readJson(Path file)
    {
        if (Files.exists(file) == false)
        {
            return null;
        }

        try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8))
        {
            return JsonParser.parseReader(r).getAsJsonObject();
        }
        catch (IOException e)
        {
            return null;
        }
    }

    private static RawMaterialCalculator.Result calc(Map<String, Long> demand)
    {
        return new RawMaterialCalculator(index, RawMaterialCalculator.Options.defaults()).calculate(demand);
    }

    private static Map<String, Long> of(Object... kv)
    {
        Map<String, Long> map = new LinkedHashMap<>();

        for (int i = 0; i < kv.length; i += 2)
        {
            map.put("minecraft:" + kv[i], ((Number) kv[i + 1]).longValue());
        }

        return map;
    }

    @Test
    void stoneBricksComeFromCobblestone()
    {
        RawMaterialCalculator.Result r = calc(of("stone_bricks", 64));
        System.out.println(r);
        assertEquals(Map.of("minecraft:cobblestone", 64L), r.raw());
    }

    @Test
    void stairsUseStonecutterOrWholeBatches()
    {
        // Stonecutter: 1 stone brick stairs per stone -> 6 cobblestone for 6 stairs
        RawMaterialCalculator.Result r = calc(of("stone_brick_stairs", 6));
        assertEquals(Map.of("minecraft:cobblestone", 6L), r.raw());

        // Without the stonecutter: 2 crafts of 6 bricks -> 12 bricks -> 12 stone
        RawMaterialCalculator.Result noCutter = new RawMaterialCalculator(index,
                new RawMaterialCalculator.Options(Set.of(), Map.of(), true, false)).calculate(of("stone_brick_stairs", 6));
        System.out.println(noCutter);
        assertEquals(Map.of("minecraft:cobblestone", 12L), noCutter.raw());
    }

    @Test
    void planksAndSticksFromLogs()
    {
        // 8 sticks = 2 crafts (4 each) = 4 planks = 1 log; 20 oak planks = 5 logs
        RawMaterialCalculator.Result r = calc(of("oak_planks", 20, "torch", 16));
        System.out.println(r);
        assertEquals(5L + 1L, r.raw().get("minecraft:oak_log"));
        assertEquals(4L, r.raw().get("minecraft:coal"));
    }

    @Test
    void ingotsAndBlocksDoNotLoop()
    {
        RawMaterialCalculator.Result r = calc(of("iron_block", 2, "iron_bars", 32, "lantern", 4));
        System.out.println(r);
        // 18 ingots for the blocks, 12 for the bars (2 crafts x 6), 4 lanterns = 32 nuggets = 4 ingots
        assertEquals(18L + 12L + 4L, r.raw().get("minecraft:raw_iron"));
        assertNull(r.raw().get("minecraft:iron_block"));
        assertNull(r.raw().get("minecraft:iron_ingot"));
        assertTrue(r.raw().containsKey("minecraft:oak_log"), "torches need sticks: " + r.raw());
    }

    @Test
    void naturalItemsAreRaw()
    {
        RawMaterialCalculator calc = new RawMaterialCalculator(index, RawMaterialCalculator.Options.defaults());
        for (String id : List.of("diamond", "redstone", "coal", "raw_iron", "wheat", "bone", "oak_log", "sand", "cobblestone", "slime_ball"))
        {
            assertTrue(calc.isRaw("minecraft:" + id), id + " should be raw, recipe: " + calc.getChosenRecipe("minecraft:" + id));
        }
    }

    @Test
    void glassAndDyes()
    {
        RawMaterialCalculator.Result r = calc(of("glass_pane", 32, "red_stained_glass", 8, "white_concrete_powder", 8));
        System.out.println(r);
        assertTrue(r.raw().containsKey("minecraft:sand"));
        assertFalse(r.raw().containsKey("minecraft:glass"));
    }

    @Test
    void listRawItemsWithRecipes()
    {
        RawMaterialCalculator calc = new RawMaterialCalculator(index, RawMaterialCalculator.Options.defaults());
        List<String> list = new ArrayList<>();
        for (String id : index.getResults())
        {
            if (calc.isRaw(id))
            {
                list.add(id);
            }
        }
        java.util.Collections.sort(list);
        System.out.println("RAW WITH RECIPES (" + list.size() + "): " + list);
    }

    @Test
    void baseItemsAreKept()
    {
        RawMaterialCalculator.Result r = new RawMaterialCalculator(index,
                new RawMaterialCalculator.Options(Set.of("minecraft:stone"), Map.of(), true, true)).calculate(of("stone_bricks", 10));
        assertEquals(Map.of("minecraft:stone", 10L), r.raw());
        assertEquals(1, r.steps().size());
    }

    @Test
    void wholeCastleIsConsistent()
    {
        Map<String, Long> demand = of("stone_bricks", 7400, "cobblestone", 3300, "oak_planks", 2200, "stone_brick_stairs", 1100,
                                      "spruce_log", 900, "glass_pane", 640, "oak_stairs", 610, "stone_brick_slab", 410,
                                      "cobblestone_wall", 380, "red_carpet", 230, "iron_bars", 160, "lantern", 120,
                                      "spruce_trapdoor", 96, "ladder", 90, "torch", 90, "bookshelf", 64, "hay_block", 40, "oak_door", 14);
        RawMaterialCalculator.Result r = calc(demand);
        System.out.println("Castle raw: " + r.raw());
        r.steps().forEach(s -> System.out.println("  step " + s));
        assertEquals(3300L + 380L + 7400L + 1100L + 205L, r.raw().get("minecraft:cobblestone") - extraCobble(r));

        for (String id : r.raw().keySet())
        {
            assertTrue(index.getRecipesFor(id).isEmpty() || new RawMaterialCalculator(index, RawMaterialCalculator.Options.defaults()).isRaw(id), id);
        }
    }

    /** Cobblestone used by other recipes than the bricks/walls (furnaces etc), none expected in this list */
    private static long extraCobble(RawMaterialCalculator.Result r)
    {
        return 0;
    }
}
