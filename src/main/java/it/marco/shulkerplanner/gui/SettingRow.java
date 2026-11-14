package it.marco.shulkerplanner.gui;

import javax.annotation.Nullable;

import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigInteger;

/**
 * One row of the settings tab: a section header, a boolean config option or a number config option
 */
public record SettingRow(Kind kind, String title, @Nullable ConfigBoolean config, @Nullable ConfigInteger number)
{
    public enum Kind
    {
        HEADER,
        OPTION,
        NUMBER
    }

    public static SettingRow header(String title)
    {
        return new SettingRow(Kind.HEADER, title, null, null);
    }

    public static SettingRow option(ConfigBoolean config)
    {
        return new SettingRow(Kind.OPTION, config.getPrettyName(), config, null);
    }

    public static SettingRow number(ConfigInteger config)
    {
        return new SettingRow(Kind.NUMBER, config.getPrettyName(), null, config);
    }

    @Nullable
    public String comment()
    {
        return this.config != null ? this.config.getComment() : (this.number != null ? this.number.getComment() : null);
    }
}
