package it.marco.shulkerplanner.recipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.Nullable;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Resolves item tags from the tag json files of a data pack (data/ns/tags/item/path.json), including nested tags.
 * Used when the game doesn't know a tag (not connected to a world yet).
 */
public class TagResolver implements Function<String, List<String>>
{
    private final Function<String, JsonObject> tagFileLoader;
    @Nullable private final Function<String, List<String>> liveTags;
    private final Map<String, List<String>> cache = new HashMap<>();

    /**
     * @param tagFileLoader tag id (minecraft:planks) to the json of its file, or null
     * @param liveTags      tag id to the items the game knows for it (empty if unknown), or null
     */
    public TagResolver(Function<String, JsonObject> tagFileLoader, @Nullable Function<String, List<String>> liveTags)
    {
        this.tagFileLoader = tagFileLoader;
        this.liveTags = liveTags;
    }

    @Override
    public List<String> apply(String tag)
    {
        return this.resolve(tag, new HashSet<>());
    }

    private List<String> resolve(String tag, Set<String> visiting)
    {
        List<String> cached = this.cache.get(tag);

        if (cached != null)
        {
            return cached;
        }

        if (this.liveTags != null)
        {
            List<String> live = this.liveTags.apply(tag);

            if (live != null && live.isEmpty() == false)
            {
                this.cache.put(tag, live);
                return live;
            }
        }

        if (visiting.add(tag) == false)
        {
            return List.of();
        }

        List<String> list = new ArrayList<>();
        JsonObject json = this.tagFileLoader.apply(tag);

        if (json != null && json.has("values"))
        {
            for (JsonElement el : json.getAsJsonArray("values"))
            {
                String value = null;

                if (el.isJsonPrimitive())
                {
                    value = el.getAsString();
                }
                else if (el.isJsonObject() && el.getAsJsonObject().has("id"))
                {
                    value = el.getAsJsonObject().get("id").getAsString();
                }

                if (value == null)
                {
                    continue;
                }

                if (value.startsWith("#"))
                {
                    String sub = value.substring(1);
                    sub = sub.indexOf(':') < 0 ? "minecraft:" + sub : sub;

                    for (String id : this.resolve(sub, visiting))
                    {
                        if (list.contains(id) == false)
                        {
                            list.add(id);
                        }
                    }
                }
                else
                {
                    String id = value.indexOf(':') < 0 ? "minecraft:" + value : value;

                    if (list.contains(id) == false)
                    {
                        list.add(id);
                    }
                }
            }
        }

        visiting.remove(tag);
        List<String> result = List.copyOf(list);
        this.cache.put(tag, result);
        return result;
    }
}
