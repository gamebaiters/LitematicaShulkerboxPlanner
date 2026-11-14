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

import it.marco.shulkerplanner.gui.ui.Ui;
import it.marco.shulkerplanner.data.MaterialRow;
import java.util.Map;
import it.marco.shulkerplanner.data.PlannerProject;
import it.marco.shulkerplanner.data.PlannerSession;
import it.marco.shulkerplanner.gui.ui.PlannerButton;
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
        Ui.styleScrollbar(this.getScrollbar());

        this.gui = gui;
        this.browserEntryHeight = 22;
        this.layout.update(this, gui.getProject());
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
        Comparator<MaterialRow> byCount = Comparator.<MaterialRow>comparingLong(row -> this.layout.needed(row.getId())).reversed()
                .thenComparing(Comparator.<MaterialRow>comparingInt(project::getRequiredCount).reversed()).thenComparing(byName);
        Comparator<MaterialRow> byCategory = Comparator.<MaterialRow>comparingInt(row -> BuiltInRegistries.ITEM.getId(row.getStack().getItem())).thenComparing(byName);

        return switch (project.getMaterialSort())
        {
            case COUNT -> byCount;
            case NAME -> byName;
            case CATEGORY -> byCategory;
            case STATUS -> Comparator.<MaterialRow>comparingInt(row -> project.isExcluded(row.getId()) ? 3 :
                                                     (project.isGathered(row.getId()) || this.layout.stillToGet(row.getId()) <= 0 ? 2 : 0))
                                     .thenComparing(Comparator.<MaterialRow>comparingLong(row -> this.layout.stillToGet(row.getId())).reversed())
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
     * The column positions, shared by all the entries, and the progress numbers of every material.
     * Columns that don't fit get hidden, the most important ones last.
     */
    public static class Layout
    {
        public static final String KEY_HEADER = "shulkerplanner.gui.label.materials.header.";

        public int nameWidth;
        public int colNeeded;
        public int colPacked = -1;
        public int colCarrying = -1;
        public int colToGet = -1;
        public int colBar = -1;
        public int barWidth;
        public int colStacks = -1;
        public int buttonsWidth;
        public int widthInclude;
        public int widthGathered;
        public int widthNote;
        private Map<String, Long> needed = Map.of();
        private Map<String, Long> packed = Map.of();
        private Map<String, Long> carrying = Map.of();

        public long needed(String id)
        {
            return this.needed.getOrDefault(id, 0L);
        }

        public long packed(String id)
        {
            return this.packed.getOrDefault(id, 0L);
        }

        public long carrying(String id)
        {
            return this.carrying.getOrDefault(id, 0L);
        }

        public long stillToGet(String id)
        {
            return Math.max(0, this.needed(id) - this.packed(id) - this.carrying(id));
        }

        public void update(WidgetListPlannerMaterials list, @Nullable PlannerProject project)
        {
            PlannerSession session = PlannerSession.INSTANCE;
            this.needed = session.getListedCounts();
            this.packed = session.getPackedCounts();
            this.carrying = session.getCarryingCounts();

            this.widthInclude = Math.max(PlannerButton.autoWidth(StringUtils.translate("shulkerplanner.gui.button.include"), true, null),
                                         PlannerButton.autoWidth(StringUtils.translate("shulkerplanner.gui.button.include"), true, null));
            this.widthGathered = PlannerButton.autoWidth(StringUtils.translate("shulkerplanner.gui.button.gathered_short"), true, null);
            this.widthNote = PlannerButton.autoWidth("+", false, null) + 2;
            this.buttonsWidth = this.widthInclude + this.widthGathered + this.widthNote + 8;

            int nameW = header("item");
            int neededW = header("needed");
            int packedW = header("packed");
            int carryW = header("carrying");
            int toGetW = header("to_get");
            int stacksW = header("stacks");

            if (project != null)
            {
                for (MaterialRow row : project.getMaterials())
                {
                    String id = row.getId();
                    int maxStack = row.getStack().getMaxStackSize();
                    nameW = Math.max(nameW, StringUtils.getStringWidth(row.getDisplayName()) + 22);
                    neededW = Math.max(neededW, StringUtils.getStringWidth(Format.number(this.needed(id))));
                    packedW = Math.max(packedW, StringUtils.getStringWidth(Format.number(this.packed(id))));
                    carryW = Math.max(carryW, StringUtils.getStringWidth(Format.number(this.carrying(id))));
                    toGetW = Math.max(toGetW, StringUtils.getStringWidth("✔ " + Format.number(this.stillToGet(id))));
                    stacksW = Math.max(stacksW, StringUtils.getStringWidth(Format.stacks((int) Math.min(Integer.MAX_VALUE, this.stillToGet(id)), maxStack)));
                }
            }

            int gap = 10;
            int available = list.browserEntryWidth - this.buttonsWidth - 12;
            this.nameWidth = Math.min(nameW, Math.max(90, available / 3));

            int x = 6 + this.nameWidth + gap;
            this.colNeeded = x;
            x += neededW + gap;

            this.colPacked = -1;
            this.colCarrying = -1;
            this.colToGet = -1;
            this.colBar = -1;
            this.colStacks = -1;

            // Most useful first: what's left to get, the progress bar, then the details
            int remaining = available - x;
            boolean toGet = remaining >= toGetW;
            remaining -= toGet ? toGetW + gap : 0;
            boolean bar = remaining >= 44;
            remaining -= bar ? 50 + gap : 0;
            boolean packedCol = remaining >= packedW + carryW + gap;
            remaining -= packedCol ? packedW + carryW + gap * 2 : 0;
            boolean stacks = remaining >= stacksW;

            if (packedCol)
            {
                this.colPacked = x;
                x += packedW + gap;
                this.colCarrying = x;
                x += carryW + gap;
            }

            if (toGet)
            {
                this.colToGet = x;
                x += toGetW + gap;
            }

            if (bar)
            {
                this.colBar = x;
                this.barWidth = 50;
                x += 50 + gap;
            }

            if (stacks)
            {
                this.colStacks = x;
            }
        }

        private static int header(String key)
        {
            return StringUtils.getStringWidth(StringUtils.translate(KEY_HEADER + key)) + 2;
        }
    }
}
