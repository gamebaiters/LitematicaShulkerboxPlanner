package it.marco.shulkerplanner.gui.widgets;

import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;

import fi.dy.masa.litematica.gui.Icons;
import fi.dy.masa.malilib.gui.LeftRight;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;
import fi.dy.masa.malilib.gui.widgets.WidgetSearchBar;

import it.marco.shulkerplanner.gui.ui.Ui;
import it.marco.shulkerplanner.gui.GuiPlannerSettings;
import it.marco.shulkerplanner.gui.SettingRow;

public class WidgetListPlannerSettings extends WidgetListBase<SettingRow, WidgetPlannerSettingEntry>
{
    private final GuiPlannerSettings gui;

    public WidgetListPlannerSettings(int x, int y, int width, int height, GuiPlannerSettings gui)
    {
        super(x, y, width, height, null);
        Ui.styleScrollbar(this.getScrollbar());

        this.gui = gui;
        this.browserEntryHeight = 24;
        this.widgetSearchBar = new WidgetSearchBar(x + 2, y + 2, width - 16, 14, 0, Icons.FILE_ICON_SEARCH, LeftRight.RIGHT);
        this.widgetSearchBar.setZLevel(1);
        this.browserEntriesOffsetY = 16;
    }

    public GuiPlannerSettings getGui()
    {
        return this.gui;
    }

    @Override
    protected Collection<SettingRow> getAllEntries()
    {
        return this.gui.getRows();
    }

    @Override
    protected boolean filterMatchesEmptyEntry(SettingRow entry)
    {
        return entry.kind() == SettingRow.Kind.HEADER;
    }

    @Override
    protected List<String> getEntryStringsForFilter(SettingRow entry)
    {
        if (entry.config() == null)
        {
            return List.of("");
        }

        String comment = entry.config().getComment();
        return List.of(entry.title().toLowerCase(), entry.config().getName().toLowerCase(), comment != null ? comment.toLowerCase() : "");
    }

    @Override
    protected int getBrowserEntryHeightFor(@Nullable SettingRow entry)
    {
        return entry != null && entry.kind() == SettingRow.Kind.HEADER ? 20 : 24;
    }

    @Override
    protected WidgetPlannerSettingEntry createListEntryWidget(int x, int y, int listIndex, boolean isOdd, @Nullable SettingRow entry)
    {
        return new WidgetPlannerSettingEntry(x, y, this.browserEntryWidth, this.getBrowserEntryHeightFor(entry), isOdd, entry, listIndex, this);
    }
}
