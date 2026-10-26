package it.marco.shulkerplanner.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import fi.dy.masa.malilib.config.ConfigUtils;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IConfigHandler;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigInteger;
import fi.dy.masa.malilib.config.options.ConfigOptionList;
import fi.dy.masa.malilib.config.options.ConfigString;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.data.json.JsonUtils;

import it.marco.shulkerplanner.Reference;
import it.marco.shulkerplanner.ShulkerPlanner;

public class Configs implements IConfigHandler
{
    private static final String CONFIG_FILE_NAME = Reference.MOD_ID + ".json";
    private static final String GENERIC_KEY = Reference.MOD_ID + ".config.generic";

    public static class Generic
    {
        public static final ConfigBoolean HUD_ENABLED               = new ConfigBoolean("hudEnabled",               true).apply(GENERIC_KEY);
        public static final ConfigInteger HUD_X                     = new ConfigInteger("hudOffsetX",               4, 0, 4000).apply(GENERIC_KEY);
        public static final ConfigInteger HUD_Y                     = new ConfigInteger("hudOffsetY",               40, 0, 4000).apply(GENERIC_KEY);
        public static final ConfigInteger HUD_MAX_LINES             = new ConfigInteger("hudMaxReminders",          6, 0, 30).apply(GENERIC_KEY);
        public static final ConfigBoolean HUD_SHOW_PROGRESS         = new ConfigBoolean("hudShowProgress",          true).apply(GENERIC_KEY);
        public static final ConfigBoolean HUD_SHOW_CURRENT_BOX      = new ConfigBoolean("hudShowCurrentBox",        true).apply(GENERIC_KEY);
        public static final ConfigInteger HUD_MAX_BOX_LINES         = new ConfigInteger("hudMaxBoxItems",           6, 1, 27).apply(GENERIC_KEY);
        public static final ConfigOptionList HUD_BOX_VIEW           = new ConfigOptionList("hudBoxView",            HudBoxView.BOTH).apply(GENERIC_KEY);
        public static final ConfigOptionList HUD_COUNT_FORMAT       = new ConfigOptionList("hudCountFormat",        HudCountFormat.STACKS).apply(GENERIC_KEY);
        public static final ConfigBoolean HUD_FOLLOW_PLACEMENT      = new ConfigBoolean("hudFollowSelectedPlacement", true).apply(GENERIC_KEY);
        public static final ConfigBoolean PACKING_ASSISTANT         = new ConfigBoolean("packingAssistant",         true).apply(GENERIC_KEY);
        public static final ConfigBoolean PACKING_HIGHLIGHT_SLOTS   = new ConfigBoolean("packingHighlightSlots",    true).apply(GENERIC_KEY);
        public static final ConfigBoolean AUTO_MARK_BOXES           = new ConfigBoolean("autoMarkFilledBoxes",      true).apply(GENERIC_KEY);
        public static final ConfigBoolean ADD_BUTTON_MATERIAL_LIST  = new ConfigBoolean("addButtonToMaterialList",  true).apply(GENERIC_KEY);
        public static final ConfigBoolean ADD_BUTTON_MAIN_MENU      = new ConfigBoolean("addButtonToMainMenu",      true).apply(GENERIC_KEY);
        public static final ConfigString  LAST_PROJECT              = new ConfigString("lastProject",               "").apply(GENERIC_KEY);

        public static HudBoxView getHudBoxView()
        {
            return (HudBoxView) HUD_BOX_VIEW.getOptionListValue();
        }

        public static HudCountFormat getHudCountFormat()
        {
            return (HudCountFormat) HUD_COUNT_FORMAT.getOptionListValue();
        }

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                HUD_ENABLED,
                HUD_X,
                HUD_Y,
                HUD_MAX_LINES,
                HUD_SHOW_PROGRESS,
                HUD_SHOW_CURRENT_BOX,
                HUD_MAX_BOX_LINES,
                HUD_BOX_VIEW,
                HUD_COUNT_FORMAT,
                HUD_FOLLOW_PLACEMENT,
                PACKING_ASSISTANT,
                PACKING_HIGHLIGHT_SLOTS,
                AUTO_MARK_BOXES,
                ADD_BUTTON_MATERIAL_LIST,
                ADD_BUTTON_MAIN_MENU
        );

        // Internal values, saved but not shown in the config GUI
        public static final List<IConfigBase> INTERNAL = ImmutableList.of(LAST_PROJECT);
    }

    public static void loadFromFile()
    {
        Path configFile = FileUtils.getConfigDirectory().resolve(CONFIG_FILE_NAME);

        if (Files.exists(configFile) && Files.isReadable(configFile))
        {
            JsonElement element = JsonUtils.parseJsonFile(configFile);

            if (element != null && element.isJsonObject())
            {
                JsonObject root = element.getAsJsonObject();

                ConfigUtils.readConfigBase(root, "Generic", Generic.OPTIONS);
                ConfigUtils.readConfigBase(root, "Internal", Generic.INTERNAL);
                ConfigUtils.readConfigBase(root, "Hotkeys", Hotkeys.HOTKEY_LIST);
            }
            else
            {
                ShulkerPlanner.LOGGER.error("Failed to load config file '{}'", configFile.toAbsolutePath());
            }
        }
    }

    public static void saveToFile()
    {
        Path dir = FileUtils.getConfigDirectory();

        if (Files.exists(dir) == false)
        {
            FileUtils.createDirectoriesIfMissing(dir);
        }

        JsonObject root = new JsonObject();

        ConfigUtils.writeConfigBase(root, "Generic", Generic.OPTIONS);
        ConfigUtils.writeConfigBase(root, "Internal", Generic.INTERNAL);
        ConfigUtils.writeConfigBase(root, "Hotkeys", Hotkeys.HOTKEY_LIST);

        JsonUtils.writeJsonToFile(root, dir.resolve(CONFIG_FILE_NAME));
    }

    @Override
    public void load()
    {
        loadFromFile();
    }

    @Override
    public void save()
    {
        saveToFile();
    }
}
