package it.marco.shulkerplanner.gui.widgets;

import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;

import fi.dy.masa.litematica.gui.Icons;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.LeftRight;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;
import fi.dy.masa.malilib.gui.widgets.WidgetSearchBar;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.util.StringUtils;

import it.marco.shulkerplanner.gui.GuiPlannerBoxes;
import it.marco.shulkerplanner.plan.ItemRequirement;
import it.marco.shulkerplanner.plan.PlannedBox;

public class WidgetListPlannerBoxes extends WidgetListBase<PlannedBox, WidgetPlannerBoxEntry>
{
    private static int lastScrollbarPosition;

    private final GuiPlannerBoxes gui;
    private boolean scrollbarRestored;

    public WidgetListPlannerBoxes(int x, int y, int width, int height, GuiPlannerBoxes gui)
    {
        super(x, y, width, height, null);

        this.gui = gui;
        this.browserEntryHeight = WidgetPlannerBoxEntry.HEIGHT;
        this.widgetSearchBar = new WidgetSearchBar(x + 2, y + 4, width - 16, 14, 0, Icons.FILE_ICON_SEARCH, LeftRight.RIGHT);
        this.widgetSearchBar.setZLevel(1);
        // Space for the search bar above the first entry
        this.browserEntriesOffsetY = 18;
    }

    public GuiPlannerBoxes getGui()
    {
        return this.gui;
    }

    @Override
    protected Collection<PlannedBox> getAllEntries()
    {
        return this.gui.getVisibleBoxes();
    }

    @Override
    protected List<String> getEntryStringsForFilter(PlannedBox entry)
    {
        StringBuilder sb = new StringBuilder();
        sb.append('#').append(entry.getIndex()).append(' ');

        for (ItemRequirement req : entry.getItems().values())
        {
            sb.append(req.proto().getHoverName().getString().toLowerCase()).append(' ').append(req.id()).append(' ');
        }

        return List.of(sb.toString());
    }

    @Override
    protected void refreshBrowserEntries()
    {
        super.refreshBrowserEntries();

        if (this.scrollbarRestored == false && lastScrollbarPosition <= this.scrollBar.getMaxValue())
        {
            this.scrollBar.setValue(lastScrollbarPosition);
            this.scrollbarRestored = true;
            this.reCreateListEntryWidgets();
        }
    }

    @Override
    public void drawContents(GuiContext ctx, int mouseX, int mouseY, float partialTicks)
    {
        super.drawContents(ctx, mouseX, mouseY, partialTicks);
        lastScrollbarPosition = this.scrollBar.getValue();

        if (this.widgetSearchBar != null && this.widgetSearchBar.isSearchOpen() == false)
        {
            String hint = GuiBase.TXT_DARK_GRAY + StringUtils.translate("shulkerplanner.gui.label.type_to_search");
            this.drawString(ctx, hint, this.posX + 4, this.posY + 7, 0xFFFFFFFF);
        }
    }

    @Override
    protected void offsetSelectionOrScrollbar(int amount, boolean changeSelection)
    {
        // Scroll one box at a time, the entries are tall
        super.offsetSelectionOrScrollbar(Integer.signum(amount), changeSelection);
        lastScrollbarPosition = this.scrollBar.getValue();
    }

    @Override
    protected WidgetPlannerBoxEntry createListEntryWidget(int x, int y, int listIndex, boolean isOdd, @Nullable PlannedBox entry)
    {
        return new WidgetPlannerBoxEntry(x, y, this.browserEntryWidth, this.getBrowserEntryHeightFor(entry), isOdd, entry, listIndex, this);
    }
}
