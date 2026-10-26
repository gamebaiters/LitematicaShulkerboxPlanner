package it.marco.shulkerplanner.gui.widgets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import javax.annotation.Nullable;

import net.minecraft.core.registries.BuiltInRegistries;

import fi.dy.masa.litematica.gui.Icons;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.LeftRight;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;
import fi.dy.masa.malilib.gui.widgets.WidgetSearchBar;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.util.StringUtils;

import it.marco.shulkerplanner.data.MaterialRow;
import it.marco.shulkerplanner.data.PlannerProject;
import it.marco.shulkerplanner.gui.GuiPlannerMaterials;
import it.marco.shulkerplanner.util.Format;

public class WidgetListPlannerMaterials extends WidgetListBase<MaterialRow, WidgetPlannerMaterialEntry>
{
    private static int lastScrollbarPosition;

    private final GuiPlannerMaterials gui;
    private final Layout layout = new Layout();
    private boolean scrollbarRestored;

    public WidgetListPlannerMaterials(int x, int y, int width, int height, GuiPlannerMaterials gui)
    {
        super(x, y, width, height, null);

        this.gui = gui;
        this.browserEntryHeight = 22;
        this.shouldSortList = true;
        this.widgetSearchBar = new WidgetSearchBar(x + 2, y + 4, width - 16, 14, 0, Icons.FILE_ICON_SEARCH, LeftRight.RIGHT);
        this.widgetSearchBar.setZLevel(1);
    }

    public GuiPlannerMaterials getGui()
    {
        return this.gui;
    }

    public Layout getLayout()
    {
        return this.layout;
    }

    @Override
    protected Collection<MaterialRow> getAllEntries()
    {
        PlannerProject project = this.gui.getProject();

        if (project == null)
        {
            return List.of();
        }

        List<MaterialRow> list = new ArrayList<>();

        for (MaterialRow row : project.getMaterials())
        {
            if (project.getHideExcluded() == false || project.isExcluded(row.getId()) == false)
            {
                list.add(row);
            }
        }

        return list;
    }

    @Override
    protected Comparator<MaterialRow> getComparator()
    {
        PlannerProject project = this.gui.getProject();

        if (project == null)
        {
            return Comparator.comparing(MaterialRow::getId);
        }

        Comparator<MaterialRow> byName = Comparator.comparing(row -> row.getDisplayName().toLowerCase());
        Comparator<MaterialRow> byCount = Comparator.<MaterialRow>comparingInt(project::getRequiredCount).reversed().thenComparing(byName);
        Comparator<MaterialRow> byCategory = Comparator.<MaterialRow>comparingInt(row -> BuiltInRegistries.ITEM.getId(row.getStack().getItem())).thenComparing(byName);

        return switch (project.getMaterialSort())
        {
            case COUNT -> byCount;
            case NAME -> byName;
            case CATEGORY -> byCategory;
            case STATUS -> Comparator.<MaterialRow>comparingInt(row -> project.isExcluded(row.getId()) ? 2 : (project.isGathered(row.getId()) ? 1 : 0))
                                     .thenComparing(byCount);
        };
    }

    @Override
    protected List<String> getEntryStringsForFilter(MaterialRow entry)
    {
        return List.of(entry.getDisplayName().toLowerCase(), entry.getId().toLowerCase());
    }

    @Override
    protected void refreshBrowserEntries()
    {
        this.layout.update(this, this.gui.getProject());

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
    }

    @Override
    protected void offsetSelectionOrScrollbar(int amount, boolean changeSelection)
    {
        super.offsetSelectionOrScrollbar(amount, changeSelection);
        lastScrollbarPosition = this.scrollBar.getValue();
    }

    @Override
    @Nullable
    protected WidgetPlannerMaterialEntry createHeaderWidget(int x, int y, int listIndexStart, int usableHeight, int usedHeight)
    {
        if ((usedHeight + this.browserEntryHeight) > usableHeight)
        {
            return null;
        }

        return this.createListEntryWidget(x, y, -1, true, null);
    }

    @Override
    protected WidgetPlannerMaterialEntry createListEntryWidget(int x, int y, int listIndex, boolean isOdd, @Nullable MaterialRow entry)
    {
        return new WidgetPlannerMaterialEntry(x, y, this.browserEntryWidth, this.getBrowserEntryHeightFor(entry), isOdd, entry, listIndex, this);
    }

    /**
     * The column positions, shared by all the entries. Columns that don't fit get hidden.
     */
    public static class Layout
    {
        public static final String KEY_HEADER = "shulkerplanner.gui.label.materials.header.";

        public int nameWidth;
        public int colRequired;
        public int colStacks = -1;
        public int colBoxes = -1;
        public int colAvailable = -1;
        public int buttonsWidth;
        public int widthInclude;
        public int widthGathered;
        public int widthNote;

        public void update(WidgetListPlannerMaterials list, @Nullable PlannerProject project)
        {
            this.widthInclude = Math.max(StringUtils.getStringWidth(WidgetPlannerMaterialEntry.getIncludeLabel(true)),
                                         StringUtils.getStringWidth(WidgetPlannerMaterialEntry.getIncludeLabel(false))) + 10;
            this.widthGathered = Math.max(StringUtils.getStringWidth(WidgetPlannerMaterialEntry.getGatheredLabel(true)),
                                          StringUtils.getStringWidth(WidgetPlannerMaterialEntry.getGatheredLabel(false))) + 10;
            this.widthNote = StringUtils.getStringWidth(StringUtils.translate("shulkerplanner.gui.button.add_note_short")) + 10;
            this.buttonsWidth = this.widthInclude + this.widthGathered + this.widthNote + 6;

            int nameW = header("item");
            int reqW = header("required");
            int stacksW = header("stacks");
            int boxesW = header("boxes");
            int availW = header("available");

            if (project != null)
            {
                for (MaterialRow row : project.getMaterials())
                {
                    int count = project.getRequiredCount(row);
                    int maxStack = row.getStack().getMaxStackSize();
                    nameW = Math.max(nameW, StringUtils.getStringWidth(row.getDisplayName()) + 20);
                    reqW = Math.max(reqW, StringUtils.getStringWidth(Format.number(count)));
                    stacksW = Math.max(stacksW, StringUtils.getStringWidth(Format.stacks(count, maxStack)));
                    boxesW = Math.max(boxesW, StringUtils.getStringWidth(Format.boxes(count, maxStack)));
                    availW = Math.max(availW, StringUtils.getStringWidth(Format.number(row.getAvailable())));
                }
            }

            int gap = 12;
            int available = list.browserEntryWidth - this.buttonsWidth - 12;
            this.nameWidth = Math.min(nameW, Math.max(80, available / 3));

            int x = 4 + this.nameWidth + gap;
            this.colRequired = x;
            x += reqW + gap;

            this.colAvailable = -1;
            this.colBoxes = -1;
            this.colStacks = -1;

            if (x + availW <= available)
            {
                this.colAvailable = x;
                x += availW + gap;
            }

            if (x + boxesW <= available)
            {
                this.colBoxes = x;
                x += boxesW + gap;
            }

            if (x + stacksW <= available)
            {
                this.colStacks = x;
            }
        }

        private static int header(String key)
        {
            return StringUtils.getStringWidth(GuiBase.TXT_BOLD + StringUtils.translate(KEY_HEADER + key)) + 2;
        }
    }
}
