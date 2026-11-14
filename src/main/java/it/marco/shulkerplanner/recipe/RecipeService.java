package it.marco.shulkerplanner.recipe;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import it.marco.shulkerplanner.ShulkerPlanner;

/**
 * Loads the recipes for the raw materials calculation.
 * In singleplayer the recipes of the world are used (data packs and mods included);
 * on a server the client doesn't get the recipes, so the vanilla ones from the game jar are used.
 */
public class RecipeService
{
    private static RecipeIndex index;
    private static boolean fromWorld;
    @Nullable private static Object loadedFor;

    /**
     * @return the recipes, loaded the first time (and again after a world change)
     */
    public static synchronized RecipeIndex getIndex()
    {
        Minecraft mc = Minecraft.getInstance();
        MinecraftServer server = mc.getSingleplayerServer();
        Object key = server != null ? server : (Object) "vanilla";

        if (index == null || loadedFor != key)
        {
            long start = System.nanoTime();
            index = load(server);
            loadedFor = key;
            ShulkerPlanner.LOGGER.info("Shulker Planner: {} recipes loaded ({}) in {} ms", index.size(), fromWorld ? "world" : "vanilla",
                                       (System.nanoTime() - start) / 1_000_000);
        }

        return index;
    }

    public static boolean isFromWorld()
    {
        return fromWorld;
    }

    public static synchronized void invalidate()
    {
        index = null;
        loadedFor = null;
    }

    private static RecipeIndex load(@Nullable MinecraftServer server)
    {
        ResourceManager manager = null;
        fromWorld = false;

        try
        {
            if (server != null)
            {
                manager = server.getResourceManager();
                fromWorld = true;
            }
        }
        catch (Exception ignore) {}

        if (manager == null)
        {
            manager = ServerPacksSource.createVanillaPackSource().asResourceManager();
        }

        final ResourceManager rm = manager;
        TagResolver tags = new TagResolver(tag -> readTagFile(rm, tag), RecipeService::liveTag);
        List<SimpleRecipe> recipes = new ArrayList<>();

        try
        {
            Map<Identifier, Resource> files = rm.listResources("recipe", id -> id.getPath().endsWith(".json"));

            for (Map.Entry<Identifier, Resource> e : files.entrySet())
            {
                JsonObject json = read(e.getValue());

                if (json == null)
                {
                    continue;
                }

                Identifier file = e.getKey();
                String path = file.getPath();
                String recipeId = file.getNamespace() + ":" + path.substring("recipe/".length(), path.length() - ".json".length());
                SimpleRecipe recipe = RecipeParser.parse(recipeId, json, tags);

                if (recipe != null)
                {
                    recipes.add(recipe);
                }
            }
        }
        catch (Exception e)
        {
            ShulkerPlanner.LOGGER.warn("Shulker Planner: failed to load the recipes", e);
        }

        return new RecipeIndex(recipes);
    }

    @Nullable
    private static JsonObject read(Resource resource)
    {
        try (Reader reader = resource.openAsReader())
        {
            JsonElement el = JsonParser.parseReader(reader);
            return el.isJsonObject() ? el.getAsJsonObject() : null;
        }
        catch (Exception e)
        {
            return null;
        }
    }

    @Nullable
    private static JsonObject readTagFile(ResourceManager manager, String tag)
    {
        Identifier id = Identifier.tryParse(tag);

        if (id == null)
        {
            return null;
        }

        Identifier file = Identifier.fromNamespaceAndPath(id.getNamespace(), "tags/item/" + id.getPath() + ".json");
        return manager.getResource(file).map(RecipeService::read).orElse(null);
    }

    /**
     * @return the items of a tag as the game knows it (synced by the server), empty if unknown
     */
    private static List<String> liveTag(String tag)
    {
        List<String> list = new ArrayList<>();

        try
        {
            Identifier id = Identifier.tryParse(tag);

            if (id == null)
            {
                return list;
            }

            TagKey<Item> key = TagKey.create(Registries.ITEM, id);
            HolderSet.Named<Item> set = BuiltInRegistries.ITEM.get(key).orElse(null);

            if (set != null)
            {
                for (Holder<Item> holder : set)
                {
                    holder.unwrapKey().ifPresent(k -> list.add(k.identifier().toString()));
                }
            }
        }
        catch (Exception ignore) {}

        return list;
    }
}
