package it.marco.shulkerplanner.gui;

import javax.annotation.Nullable;

import fi.dy.masa.malilib.config.options.ConfigBoolean;

/**
 * One row of the settings tab: a section header or a boolean config option
 */
public record SettingRow(Kind kind, String title, @Nullable ConfigBoolean config)
{
    public enum Kind
    {
        HEADER,
        OPTION
    }

    public static SettingRow header(String title)
    {
        return new SettingRow(Kind.HEADER, title, null);
    }

    public static SettingRow option(ConfigBoolean config)
    {
        return new SettingRow(Kind.OPTION, config.getPrettyName(), config);
    }
}
