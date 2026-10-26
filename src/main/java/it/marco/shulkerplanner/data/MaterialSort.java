package it.marco.shulkerplanner.data;

import fi.dy.masa.malilib.util.StringUtils;

public enum MaterialSort
{
    COUNT,
    NAME,
    CATEGORY,
    STATUS;

    public String getDisplayName()
    {
        return StringUtils.translate("shulkerplanner.material_sort." + this.name().toLowerCase());
    }

    public MaterialSort cycle(boolean forward)
    {
        MaterialSort[] values = values();
        int id = (this.ordinal() + (forward ? 1 : values.length - 1)) % values.length;
        return values[id];
    }

    public static MaterialSort fromName(String name)
    {
        for (MaterialSort sort : values())
        {
            if (sort.name().equalsIgnoreCase(name))
            {
                return sort;
            }
        }

        return COUNT;
    }
}
