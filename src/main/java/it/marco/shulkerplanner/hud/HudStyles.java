package it.marco.shulkerplanner.hud;

import java.nio.file.Files;
import java.nio.file.Path;
import com.google.gson.JsonElement;

import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.data.json.JsonUtils;

import it.marco.shulkerplanner.config.Configs;
import it.marco.shulkerplanner.config.HudCountFormat;
import it.marco.shulkerplanner.data.ProjectStorage;

/**
 * The default HUD style, used for projects that don't have their own style yet.
 */
public class HudStyles
{
    private static HudStyle defaultStyle;

    private static Path getFile()
    {
        return ProjectStorage.getBaseDirectory().resolve("hud_default.json");
    }

    public static HudStyle getDefault()
    {
        if (defaultStyle == null)
        {
            defaultStyle = load();
        }

        return defaultStyle;
    }

    /**
     * @return a new style for a project, copied from the default style
     */
    public static HudStyle createForProject()
    {
        return getDefault().copy();
    }

    public static void setDefault(HudStyle style)
    {
        defaultStyle = style.copy();
        save();
    }

    private static HudStyle load()
    {
        Path file = getFile();

        if (Files.isRegularFile(file))
        {
            JsonElement el = JsonUtils.parseJsonFile(file);

            if (el != null && el.isJsonObject())
            {
                HudStyle style = new HudStyle();
                style.fromJson(el.getAsJsonObject());
                return style;
            }
        }

        // First start with per-HUD styles: carry over the settings of the old global HUD options
        HudStyle style = new HudStyle();
        style.setPosition(Configs.Legacy.HUD_X.getIntegerValue(), Configs.Legacy.HUD_Y.getIntegerValue());
        style.setMaxReminders(Configs.Legacy.HUD_MAX_LINES.getIntegerValue());
        style.setMaxBoxItems(Configs.Legacy.HUD_MAX_BOX_LINES.getIntegerValue());
        style.setCountFormat((HudCountFormat) Configs.Legacy.HUD_COUNT_FORMAT.getOptionListValue());
        style.setShown(HudElement.PROGRESS, Configs.Legacy.HUD_SHOW_PROGRESS.getBooleanValue());

        String view = Configs.Legacy.HUD_BOX_VIEW.getStringValue();

        if ("compact".equals(view))
        {
            style.setShown(HudElement.BOX_GRID, false);
            style.setShown(HudElement.GRID_LEGEND, false);
        }
        else if ("shulker".equals(view))
        {
            style.setShown(HudElement.BOX_ITEMS, false);
        }

        if (Configs.Legacy.HUD_SHOW_CURRENT_BOX.getBooleanValue() == false)
        {
            for (HudElement e : new HudElement[] { HudElement.BOX_HEADER, HudElement.BOX_TOTALS, HudElement.BOX_GRID,
                                                   HudElement.GRID_LEGEND, HudElement.BOX_ITEMS, HudElement.WRONG_ITEMS, HudElement.BOX_STATUS })
            {
                style.setShown(e, false);
            }
        }

        defaultStyle = style;
        save();
        return style;
    }

    private static void save()
    {
        Path dir = ProjectStorage.getBaseDirectory();
        FileUtils.createDirectoriesIfMissing(dir);
        JsonUtils.writeJsonToFile(defaultStyle.toJson(), getFile());
    }
}
