package it.marco.shulkerplanner.gui.widgets;

import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;

import fi.dy.masa.litematica.gui.Icons;
import fi.dy.masa.malilib.gui.LeftRight;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;
import fi.dy.masa.malilib.gui.widgets.WidgetSearchBar;

import it.marco.shulkerplanner.data.MaterialRow;
import it.marco.shulkerplanner.gui.GuiPlannerRaw;
import it.marco.shulkerplanner.gui.RawRow;

public class WidgetListPlannerRaw extends WidgetListBase<RawRow, WidgetPlannerRawEntry>
{
    private final GuiPlannerRaw gui;

    public WidgetListPlannerRaw(int x, int y, int width, int height, GuiPlannerRaw gui)
    {
        super(x, y, width, height, null);

        this.gui = gui;
        this.browserEntryHeight = 22;
        this.widgetSearchBar = new WidgetSearchBar(x + 2, y + 2, width - 16, 14, 0, Icons.FILE_ICON_SEARCH, LeftRight.RIGHT);
        this.widgetSearchBar.setZLevel(1);
        this.browserEntriesOffsetY = 16;
    }

    public GuiPlannerRaw getGui()
    {
        return this.gui;
    }

    @Override
    protected Collection<RawRow> getAllEntries()
    {
        return this.gui.getRows();
    }

    @Override
    protected boolean filterMatchesEmptyEntry(RawRow entry)
    {
        return entry.kind() == RawRow.Kind.HEADER_RAW || entry.kind() == RawRow.Kind.HEADER_STEPS;
    }

    @Override
    protected List<String> getEntryStringsForFilter(RawRow entry)
    {
        if (entry.itemId().isEmpty())
        {
            return List.of("");
        }

        return List.of(new MaterialRow(entry.itemId(), 0, 0).getDisplayName().toLowerCase(), entry.itemId());
    }

    @Override
    protected int getBrowserEntryHeightFor(@Nullable RawRow entry)
    {
        if (entry != null && (entry.kind() == RawRow.Kind.HEADER_RAW || entry.kind() == RawRow.Kind.HEADER_STEPS))
        {
            return 20;
        }

        return entry != null && entry.kind() == RawRow.Kind.STEP ? 24 : 22;
    }

    @Override
    protected WidgetPlannerRawEntry createListEntryWidget(int x, int y, int listIndex, boolean isOdd, @Nullable RawRow entry)
    {
        return new WidgetPlannerRawEntry(x, y, this.browserEntryWidth, this.getBrowserEntryHeightFor(entry), isOdd, entry, listIndex, this);
    }
}
