package it.marco.shulkerplanner.data;

import javax.annotation.Nullable;
import com.google.gson.JsonObject;

import fi.dy.masa.malilib.util.data.json.JsonUtils;

/**
 * A free text "to do" note of a project, optionally linked to an item.
 */
public class Reminder
{
    private String text;
    private boolean done;
    @Nullable private final String itemId;
    private final long created;

    public Reminder(String text, @Nullable String itemId)
    {
        this(text, itemId, false, System.currentTimeMillis());
    }

    private Reminder(String text, @Nullable String itemId, boolean done, long created)
    {
        this.text = text;
        this.itemId = itemId;
        this.done = done;
        this.created = created;
    }

    public String getText()
    {
        return this.text;
    }

    public void setText(String text)
    {
        this.text = text;
    }

    public boolean isDone()
    {
        return this.done;
    }

    public void setDone(boolean done)
    {
        this.done = done;
    }

    @Nullable
    public String getItemId()
    {
        return this.itemId;
    }

    public long getCreated()
    {
        return this.created;
    }

    public JsonObject toJson()
    {
        JsonObject obj = new JsonObject();
        obj.addProperty("text", this.text);
        obj.addProperty("done", this.done);
        obj.addProperty("created", this.created);

        if (this.itemId != null)
        {
            obj.addProperty("item", this.itemId);
        }

        return obj;
    }

    @Nullable
    public static Reminder fromJson(JsonObject obj)
    {
        if (JsonUtils.hasString(obj, "text") == false)
        {
            return null;
        }

        String item = JsonUtils.hasString(obj, "item") ? JsonUtils.getString(obj, "item") : null;
        long created = JsonUtils.hasLong(obj, "created") ? JsonUtils.getLong(obj, "created") : System.currentTimeMillis();

        return new Reminder(JsonUtils.getString(obj, "text"), item, JsonUtils.getBooleanOrDefault(obj, "done", false), created);
    }
}
