package it.marco.shulkerplanner.hud;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import fi.dy.masa.malilib.util.data.json.JsonUtils;

import it.marco.shulkerplanner.config.HudCountFormat;

/**
 * Everything about how one HUD looks: title, colors, visible elements, size and position.
 * Every project (schematic) has its own style.
 */
public class HudStyle
{
    public static final float MIN_SCALE = 0.5f;
    public static final float MAX_SCALE = 2.5f;

    private String title = "";
    private HudTheme theme = HudTheme.PURPLE;
    private final Map<HudColor, Integer> colors = new EnumMap<>(HudColor.class);
    private EnumSet<HudElement> elements = HudPreset.FULL.getElements();
    private float scale = 1.0f;
    private boolean compact;
    private int padding = 4;
    private boolean textShadow = true;
    private boolean accentBar = true;
    private boolean border;
    private int x = 4;
    private int y = 40;
    private int maxBoxItems = 6;
    private int maxReminders = 6;
    private HudCountFormat countFormat = HudCountFormat.STACKS;

    public HudStyle()
    {
        this.applyTheme(HudTheme.PURPLE);
    }

    public HudStyle copy()
    {
        HudStyle s = new HudStyle();
        s.fromJson(this.toJson());
        return s;
    }

    // ---------------------------------------------------------------- title

    /**
     * @return the custom title, empty to use the schematic name
     */
    public String getTitle()
    {
        return this.title;
    }

    public void setTitle(String title)
    {
        this.title = title == null ? "" : title.length() > 64 ? title.substring(0, 64) : title;
    }

    public String resolveTitle(String schematicName)
    {
        return this.title.isBlank() ? schematicName : this.title;
    }

    // ---------------------------------------------------------------- colors

    public HudTheme getTheme()
    {
        return this.theme;
    }

    public void applyTheme(HudTheme theme)
    {
        this.theme = theme;

        for (HudColor role : HudColor.values())
        {
            this.colors.put(role, theme.getColor(role));
        }
    }

    public int getColor(HudColor role)
    {
        return this.colors.getOrDefault(role, this.theme.getColor(role));
    }

    /**
     * Changing a color by hand turns the theme into "custom"
     */
    public void setColor(HudColor role, int argb)
    {
        if (role.hasAlpha() == false)
        {
            argb |= 0xFF000000;
        }

        this.colors.put(role, argb);
        this.theme = HudTheme.CUSTOM;
    }

    public int getBackgroundOpacity()
    {
        return (this.getColor(HudColor.BACKGROUND) >>> 24) * 100 / 255;
    }

    public void setBackgroundOpacity(int percent)
    {
        int alpha = Math.max(0, Math.min(255, Math.round(percent * 255 / 100f)));
        int rgb = this.getColor(HudColor.BACKGROUND) & 0x00FFFFFF;
        this.colors.put(HudColor.BACKGROUND, (alpha << 24) | rgb);
    }

    // ---------------------------------------------------------------- elements

    public boolean isShown(HudElement element)
    {
        return this.elements.contains(element);
    }

    public void setShown(HudElement element, boolean shown)
    {
        if (shown)
        {
            this.elements.add(element);
        }
        else
        {
            this.elements.remove(element);
        }
    }

    public EnumSet<HudElement> getElements()
    {
        return EnumSet.copyOf(this.elements);
    }

    public void applyPreset(HudPreset preset)
    {
        this.elements = preset.getElements();
    }

    // ---------------------------------------------------------------- layout

    public float getScale()
    {
        return this.scale;
    }

    public void setScale(float scale)
    {
        this.scale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, Math.round(scale * 20) / 20f));
    }

    public boolean isCompact()
    {
        return this.compact;
    }

    public void setCompact(boolean compact)
    {
        this.compact = compact;
    }

    public int getPadding()
    {
        return this.padding;
    }

    public void setPadding(int padding)
    {
        this.padding = Math.max(0, Math.min(12, padding));
    }

    public boolean hasTextShadow()
    {
        return this.textShadow;
    }

    public void setTextShadow(boolean textShadow)
    {
        this.textShadow = textShadow;
    }

    public boolean hasAccentBar()
    {
        return this.accentBar;
    }

    public void setAccentBar(boolean accentBar)
    {
        this.accentBar = accentBar;
    }

    public boolean hasBorder()
    {
        return this.border;
    }

    public void setBorder(boolean border)
    {
        this.border = border;
    }

    public int getX()
    {
        return this.x;
    }

    public int getY()
    {
        return this.y;
    }

    public void setPosition(int x, int y)
    {
        this.x = Math.max(0, x);
        this.y = Math.max(0, y);
    }

    public int getMaxBoxItems()
    {
        return this.maxBoxItems;
    }

    public void setMaxBoxItems(int max)
    {
        this.maxBoxItems = Math.max(1, Math.min(27, max));
    }

    public int getMaxReminders()
    {
        return this.maxReminders;
    }

    public void setMaxReminders(int max)
    {
        this.maxReminders = Math.max(1, Math.min(30, max));
    }

    public HudCountFormat getCountFormat()
    {
        return this.countFormat;
    }

    public void setCountFormat(HudCountFormat countFormat)
    {
        this.countFormat = countFormat;
    }

    // ---------------------------------------------------------------- json

    public JsonObject toJson()
    {
        JsonObject obj = new JsonObject();
        obj.addProperty("title", this.title);
        obj.addProperty("theme", this.theme.getName());

        JsonObject colors = new JsonObject();
        for (HudColor role : HudColor.values())
        {
            colors.addProperty(role.getName(), String.format("#%08X", this.getColor(role)));
        }
        obj.add("colors", colors);

        JsonArray elements = new JsonArray();
        this.elements.forEach(e -> elements.add(e.getName()));
        obj.add("elements", elements);

        obj.addProperty("scale", this.scale);
        obj.addProperty("compact", this.compact);
        obj.addProperty("padding", this.padding);
        obj.addProperty("text_shadow", this.textShadow);
        obj.addProperty("accent_bar", this.accentBar);
        obj.addProperty("border", this.border);
        obj.addProperty("x", this.x);
        obj.addProperty("y", this.y);
        obj.addProperty("max_box_items", this.maxBoxItems);
        obj.addProperty("max_reminders", this.maxReminders);
        obj.addProperty("count_format", this.countFormat.getStringValue());
        return obj;
    }

    public void fromJson(JsonObject obj)
    {
        this.setTitle(JsonUtils.getStringOrDefault(obj, "title", ""));
        this.applyTheme(HudTheme.fromName(JsonUtils.getStringOrDefault(obj, "theme", "purple")));

        if (JsonUtils.hasObject(obj, "colors"))
        {
            JsonObject colors = obj.getAsJsonObject("colors");

            for (HudColor role : HudColor.values())
            {
                if (JsonUtils.hasString(colors, role.getName()))
                {
                    Integer argb = parseColor(colors.get(role.getName()).getAsString());

                    if (argb != null)
                    {
                        this.colors.put(role, argb);
                    }
                }
            }
        }

        if (JsonUtils.hasArray(obj, "elements"))
        {
            this.elements = EnumSet.noneOf(HudElement.class);

            for (JsonElement el : obj.getAsJsonArray("elements"))
            {
                HudElement element = HudElement.fromName(el.getAsString());

                if (element != null)
                {
                    this.elements.add(element);
                }
            }
        }

        this.setScale((float) JsonUtils.getDoubleOrDefault(obj, "scale", 1.0));
        this.compact = JsonUtils.getBooleanOrDefault(obj, "compact", false);
        this.setPadding(JsonUtils.getIntegerOrDefault(obj, "padding", 4));
        this.textShadow = JsonUtils.getBooleanOrDefault(obj, "text_shadow", true);
        this.accentBar = JsonUtils.getBooleanOrDefault(obj, "accent_bar", true);
        this.border = JsonUtils.getBooleanOrDefault(obj, "border", false);
        this.setPosition(JsonUtils.getIntegerOrDefault(obj, "x", 4), JsonUtils.getIntegerOrDefault(obj, "y", 40));
        this.setMaxBoxItems(JsonUtils.getIntegerOrDefault(obj, "max_box_items", 6));
        this.setMaxReminders(JsonUtils.getIntegerOrDefault(obj, "max_reminders", 6));
        this.countFormat = HudCountFormat.STACKS.fromString(JsonUtils.getStringOrDefault(obj, "count_format", "stacks"));
    }

    /**
     * Parses "#RRGGBB", "#AARRGGBB", "RRGGBB" or "AARRGGBB"
     */
    public static Integer parseColor(String str)
    {
        if (str == null)
        {
            return null;
        }

        String s = str.trim();

        if (s.startsWith("#"))
        {
            s = s.substring(1);
        }

        if (s.length() != 6 && s.length() != 8)
        {
            return null;
        }

        try
        {
            long value = Long.parseLong(s, 16);
            return s.length() == 6 ? (int) (0xFF000000L | value) : (int) value;
        }
        catch (NumberFormatException e)
        {
            return null;
        }
    }
}
