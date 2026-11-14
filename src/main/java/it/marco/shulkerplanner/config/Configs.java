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
    private static final String WARNINGS_KEY = Reference.MOD_ID + ".config.warnings";

    public static class Generic
    {
        public static final ConfigBoolean HUD_ENABLED               = new ConfigBoolean("hudEnabled",               true).apply(GENERIC_KEY);
        public static final ConfigBoolean HUD_FOLLOW_PLACEMENT      = new ConfigBoolean("hudFollowSelectedPlacement", true).apply(GENERIC_KEY);
        public static final ConfigBoolean HUD_FOLLOW_OPENED_BOX     = new ConfigBoolean("hudFollowOpenedBox",       true).apply(GENERIC_KEY);
        public static final ConfigBoolean PACKING_ASSISTANT         = new ConfigBoolean("packingAssistant",         true).apply(GENERIC_KEY);
        public static final ConfigBoolean AUTO_LINK_OPENED_BOXES    = new ConfigBoolean("autoLinkOpenedBoxes",      false).apply(GENERIC_KEY);
        public static final ConfigBoolean AUTO_MARK_BOXES           = new ConfigBoolean("autoMarkFilledBoxes",      true).apply(GENERIC_KEY);
        public static final ConfigBoolean AUTO_DETECT_INVENTORY_BOXES = new ConfigBoolean("autoDetectInventoryBoxes", true).apply(GENERIC_KEY);
        public static final ConfigBoolean RESPECT_LITEMATICA_IGNORED = new ConfigBoolean("respectLitematicaIgnored", true).apply(GENERIC_KEY);
        public static final ConfigBoolean ADD_BUTTON_MATERIAL_LIST  = new ConfigBoolean("addButtonToMaterialList",  true).apply(GENERIC_KEY);
        public static final ConfigBoolean ADD_BUTTON_MAIN_MENU      = new ConfigBoolean("addButtonToMainMenu",      true).apply(GENERIC_KEY);
        public static final ConfigInteger ASSISTANT_OPACITY         = new ConfigInteger("packingAssistantOpacity",  60, 10, 100).apply(GENERIC_KEY);
        public static final ConfigString  LAST_PROJECT              = new ConfigString("lastProject",               "").apply(GENERIC_KEY);

        // Position and size of the packing assistant panel, anchored to the container screen:
        // side 1 = right of it, 2 = left of it, 0 = not moved yet. Width/height 0 = automatic.
        public static final ConfigInteger ASSISTANT_SIDE            = new ConfigInteger("assistantSide",            0, 0, 2);
        public static final ConfigInteger ASSISTANT_GAP             = new ConfigInteger("assistantGap",             4, -8000, 8000);
        public static final ConfigInteger ASSISTANT_OFFSET_Y        = new ConfigInteger("assistantOffsetY",         0, -8000, 8000);
        public static final ConfigInteger ASSISTANT_WIDTH           = new ConfigInteger("assistantWidth",           0, 0, 8000);
        public static final ConfigInteger ASSISTANT_HEIGHT          = new ConfigInteger("assistantHeight",          0, 0, 8000);

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                HUD_ENABLED,
                HUD_FOLLOW_PLACEMENT,
                HUD_FOLLOW_OPENED_BOX,
                PACKING_ASSISTANT,
                ASSISTANT_OPACITY,
                AUTO_LINK_OPENED_BOXES,
                AUTO_MARK_BOXES,
                AUTO_DETECT_INVENTORY_BOXES,
                RESPECT_LITEMATICA_IGNORED,
                ADD_BUTTON_MATERIAL_LIST,
                ADD_BUTTON_MAIN_MENU
        );

        // Internal values, saved but not shown in the config GUI
        public static final List<IConfigBase> INTERNAL = ImmutableList.of(LAST_PROJECT, ASSISTANT_SIDE, ASSISTANT_GAP, ASSISTANT_OFFSET_Y,
                                                                          ASSISTANT_WIDTH, ASSISTANT_HEIGHT);
    }

    /**
     * Every warning and highlight, each one can be turned off
     */
    public static class Warnings
    {
        public static final ConfigBoolean PANEL_WRONG_ITEMS         = new ConfigBoolean("warnWrongItemsInBox",      true).apply(WARNINGS_KEY);
        public static final ConfigBoolean HIGHLIGHT_WRONG_SLOTS     = new ConfigBoolean("highlightWrongSlots",      true).apply(WARNINGS_KEY);
        public static final ConfigBoolean HIGHLIGHT_OVERFILLED      = new ConfigBoolean("highlightOverfilledSlots", true).apply(WARNINGS_KEY);
        public static final ConfigBoolean HIGHLIGHT_ITEMS_TO_MOVE   = new ConfigBoolean("highlightItemsToMove",     true).apply(WARNINGS_KEY);
        public static final ConfigBoolean HIGHLIGHT_UNLINKED        = new ConfigBoolean("highlightUnlinkedPreview", false).apply(WARNINGS_KEY);
        public static final ConfigBoolean HUD_WRONG_ITEMS           = new ConfigBoolean("hudWrongItems",            true).apply(WARNINGS_KEY);
        public static final ConfigBoolean HINT_UNLINKED_BOX         = new ConfigBoolean("hintUnlinkedBox",          true).apply(WARNINGS_KEY);
        public static final ConfigBoolean NOTIFY_BOX_COMPLETE       = new ConfigBoolean("notifyBoxComplete",        true).apply(WARNINGS_KEY);
        public static final ConfigBoolean WARN_PLAN_OUTDATED        = new ConfigBoolean("warnPlanOutdated",         true).apply(WARNINGS_KEY);

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                PANEL_WRONG_ITEMS,
                HIGHLIGHT_WRONG_SLOTS,
                HIGHLIGHT_OVERFILLED,
                HIGHLIGHT_ITEMS_TO_MOVE,
                HIGHLIGHT_UNLINKED,
                HUD_WRONG_ITEMS,
                HINT_UNLINKED_BOX,
                NOTIFY_BOX_COMPLETE,
                WARN_PLAN_OUTDATED
        );

        /** Turns every warning and highlight on or off at once */
        public static void setAll(boolean enabled)
        {
            for (IConfigBase config : OPTIONS)
            {
                ((ConfigBoolean) config).setBooleanValue(enabled);
            }
        }
    }

    /**
     * The old global HUD options. They are only read, to create the first default HUD style from them.
     */
    public static class Legacy
    {
        public static final ConfigInteger HUD_X                     = new ConfigInteger("hudOffsetX",               4, 0, 4000);
        public static final ConfigInteger HUD_Y                     = new ConfigInteger("hudOffsetY",               40, 0, 4000);
        public static final ConfigInteger HUD_MAX_LINES             = new ConfigInteger("hudMaxReminders",          6, 0, 30);
        public static final ConfigBoolean HUD_SHOW_PROGRESS         = new ConfigBoolean("hudShowProgress",          true);
        public static final ConfigBoolean HUD_SHOW_CURRENT_BOX      = new ConfigBoolean("hudShowCurrentBox",        true);
        public static final ConfigInteger HUD_MAX_BOX_LINES         = new ConfigInteger("hudMaxBoxItems",           6, 1, 27);
        public static final ConfigString  HUD_BOX_VIEW              = new ConfigString("hudBoxView",                "both");
        public static final ConfigOptionList HUD_COUNT_FORMAT       = new ConfigOptionList("hudCountFormat",        HudCountFormat.STACKS);

        public static final List<IConfigBase> OPTIONS = ImmutableList.of(HUD_X, HUD_Y, HUD_MAX_LINES, HUD_SHOW_PROGRESS,
                                                                          HUD_SHOW_CURRENT_BOX, HUD_MAX_BOX_LINES, HUD_BOX_VIEW, HUD_COUNT_FORMAT);
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
                ConfigUtils.readConfigBase(root, "Warnings", Warnings.OPTIONS);
                ConfigUtils.readConfigBase(root, "Generic", Legacy.OPTIONS);
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
        ConfigUtils.writeConfigBase(root, "Warnings", Warnings.OPTIONS);
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
