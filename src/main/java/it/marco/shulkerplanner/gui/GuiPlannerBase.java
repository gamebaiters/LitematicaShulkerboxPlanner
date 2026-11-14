package it.marco.shulkerplanner.gui;

import java.util.List;
import javax.annotation.Nullable;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import fi.dy.masa.litematica.gui.GuiMaterialList;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.GuiTextFieldGeneric;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.interfaces.ITextFieldListener;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;
import fi.dy.masa.malilib.gui.widgets.WidgetListEntryBase;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.util.GuiUtils;
import fi.dy.masa.malilib.util.StringUtils;

import it.marco.shulkerplanner.config.Configs;
import it.marco.shulkerplanner.data.PlannerProject;
import it.marco.shulkerplanner.data.PlannerSession;
import it.marco.shulkerplanner.gui.ui.PlannerButton;
import it.marco.shulkerplanner.gui.ui.Theme;
import it.marco.shulkerplanner.gui.ui.Ui;
import it.marco.shulkerplanner.gui.ui.UiLabel;
import it.marco.shulkerplanner.plan.ShulkerPlan;

/**
 * The frame shared by every planner page:
 * <pre>
 * +---------------------------------------------------------------+
 * | [icon] SHULKER PLANNER / Schematic name   progress   HUD  [x]  |  header: where am I, how far am I
 * +-----------+---------------------------------------------------+
 * | Boxes     | toolbar (main options, more options on demand)    |
 * | Materials | banner (plan state, warnings)                     |
 * | Raw       | list                                              |
 * | Reminders |                                                   |
 * | Settings  | summary                                           |
 * +-----------+---------------------------------------------------+
 * </pre>
 */
public abstract class GuiPlannerBase<TYPE, WIDGET extends WidgetListEntryBase<TYPE>, WIDGETLIST extends WidgetListBase<TYPE, WIDGET>>
        extends GuiListBase<TYPE, WIDGET, WIDGETLIST>
{
    protected static final int MARGIN = 8;
    protected static final int HEADER_HEIGHT = 34;
    protected static final int ROW_HEIGHT = 22;
    protected static final int BOTTOM_HEIGHT = 26;

    protected final PlannerSession session = PlannerSession.INSTANCE;
    protected final PlannerTab tab;
    private int flowX;
    private int flowY;
    private int flowLeft;
    private int contentX;
    private int sidebarWidth;
    private int bannerY = -1;
    /** The left edge of the buttons on the right of the header: the title and the progress must end before it */
    private int headerButtonsX;
    private int bannerHeight;
    @Nullable private Banner banner;

    /**
     * A one line message above the list, with an optional action
     */
    protected record Banner(String text, int color, int background, @Nullable String action, @Nullable Runnable onAction)
    {
    }

    protected GuiPlannerBase(PlannerTab tab)
    {
        super(MARGIN, 70);

        this.tab = tab;
        this.useTitleHierarchy = false;
    }

    @Nullable
    public PlannerProject getProject()
    {
        return this.session.getProject();
    }

    protected boolean isCompact()
    {
        return this.getScreenWidth() < 560;
    }

    protected int getContentX()
    {
        return this.contentX;
    }

    protected int getContentWidth()
    {
        return this.getScreenWidth() - this.contentX - MARGIN;
    }

    @Override
    protected int getBrowserWidth()
    {
        return this.getContentWidth();
    }

    @Override
    protected int getBrowserHeight()
    {
        return Math.max(40, this.getScreenHeight() - this.getListY() - BOTTOM_HEIGHT - 2);
    }

    /**
     * @return the banner shown above the list, or null
     */
    @Nullable
    protected Banner createBanner()
    {
        return null;
    }

    @Override
    public void initGui()
    {
        PlannerProject project = this.getProject();
        this.title = StringUtils.translate("shulkerplanner.gui.title", project != null ? project.getName() : "-");
        this.sidebarWidth = this.isCompact() ? 28 : 104;
        this.contentX = this.sidebarWidth + MARGIN;

        super.initGui();

        this.session.setUpdateListener(this::onSessionUpdated);

        this.createHeaderButtons();
        this.createSidebar();

        this.startFlow(this.contentX, HEADER_HEIGHT + 6);
        this.createOptionButtons();

        int y = this.flowY + ROW_HEIGHT;
        this.banner = this.createBanner();

        // On a short screen (GUI scale 4 on 1080p) the list needs the room more than a banner that only informs
        if (this.banner != null && this.banner.action() == null && this.banner.color() != Theme.WARN && this.getScreenHeight() < 320)
        {
            this.banner = null;
        }

        if (this.banner != null)
        {
            this.bannerY = y;
            this.bannerHeight = 18;
            y += this.bannerHeight + 4;

            if (this.banner.action() != null && this.banner.onAction() != null)
            {
                Runnable action = this.banner.onAction();
                PlannerButton button = new PlannerButton(0, 0, -1, this.banner.action(), PlannerButton.Style.PRIMARY);
                button.setPosition(this.contentX + this.getContentWidth() - button.getWidth() - 1, this.bannerY);
                this.addButton(button, (btn, mb) -> action.run());
            }
        }
        else
        {
            this.bannerY = -1;
        }

        if (y != this.getListY() || this.getListX() != this.contentX)
        {
            this.setListPosition(this.contentX, y);
            this.reCreateListWidget();

            WIDGETLIST list = this.getListWidget();

            if (list != null)
            {
                list.setSize(this.getBrowserWidth(), this.getBrowserHeight());
                list.initGui();
            }
        }

        this.createBottomButtons(this.getScreenHeight() - BOTTOM_HEIGHT + 4);
    }

    @Override
    public void removed()
    {
        super.removed();
        this.session.setUpdateListener(null);
        this.session.save();
    }

    private void onSessionUpdated()
    {
        // Only re-create the list entries, so that a focused text field keeps its focus and contents
        if (GuiUtils.getCurrentScreen() == this && this.getListWidget() != null)
        {
            this.getListWidget().refreshEntries();
        }
    }

    /**
     * Call after the project was changed from within the GUI
     */
    public void onProjectChanged()
    {
        this.session.onProjectChanged();
        this.initGui();
    }

    /**
     * Call after a packing option was changed: a locked plan gets updated, keeping the started boxes
     */
    public void onPackingOptionsChanged()
    {
        this.session.onPackingOptionsChanged();
        this.initGui();
    }

    private void createHeaderButtons()
    {
        int y = 8;
        int xRight = this.getScreenWidth() - MARGIN;

        PlannerButton close = new PlannerButton(0, y, 18, "✕", PlannerButton.Style.GHOST, StringUtils.translate("shulkerplanner.gui.button.close"));
        close.setPosition(xRight - close.getWidth(), y);
        this.addButton(close, (btn, mb) -> this.closeGui(true));
        xRight = close.getX() - 4;

        boolean hud = Configs.Generic.HUD_ENABLED.getBooleanValue();
        PlannerButton hudButton = PlannerButton.toggle(StringUtils.translate("shulkerplanner.gui.button.hud_short"), hud,
                                                 StringUtils.translate("shulkerplanner.gui.button.hud.hover"));
        hudButton.setPosition(xRight - hudButton.getWidth(), y);
        this.addButton(hudButton, (btn, mb) -> {
            Configs.Generic.HUD_ENABLED.setBooleanValue(! Configs.Generic.HUD_ENABLED.getBooleanValue());
            this.initGui();
        });
        xRight = hudButton.getX() - 4;

        if (this.isCompact() == false)
        {
            PlannerButton editHud = new PlannerButton(0, y, -1, StringUtils.translate("shulkerplanner.gui.button.edit_hud"), PlannerButton.Style.SECONDARY,
                                                StringUtils.translate("shulkerplanner.gui.button.edit_hud.hover"));
            editHud.setPosition(xRight - editHud.getWidth(), y);
            this.addButton(editHud, (btn, mb) -> GuiBase.openGui(new GuiHudEditor(this)));
            xRight = editHud.getX() - 4;
        }

        this.headerButtonsX = xRight + 4;
    }

    private void createSidebar()
    {
        boolean compact = this.isCompact();
        int x = 4;
        int y = HEADER_HEIGHT + 6;
        int w = this.sidebarWidth - 8;

        for (PlannerTab tab : PlannerTab.values())
        {
            String label = compact ? "" : tab.getDisplayName();

            if (compact == false && tab == PlannerTab.REMINDERS && this.getProject() != null)
            {
                int open = this.getProject().getOpenReminderCount();

                if (open > 0)
                {
                    label += "  " + open;
                }
            }

            PlannerButton button = new PlannerButton(x, y, w, label, PlannerButton.Style.TAB, tab.getDisplayName(), tab.getDescription());
            button.setIcon(tab.getIcon());
            button.setWidth(w);
            button.setActive(tab == this.tab);
            this.addButton(button, (btn, mouseButton) -> {
                if (tab != this.tab)
                {
                    GuiBase.openGui(PlannerGuis.create(tab, this.getParent()));
                }
            });
            y += PlannerButton.HEIGHT + 4;
        }

        if (compact)
        {
            return;
        }

        // Source of the materials, at the bottom of the side bar
        int by = this.getScreenHeight() - BOTTOM_HEIGHT - 2 * (PlannerButton.HEIGHT + 4) + 4;
        MaterialListBase materialList = this.session.getMaterialList();

        if (materialList != null && (this.getParent() instanceof GuiMaterialList) == false)
        {
            PlannerButton button = new PlannerButton(x, by, w, StringUtils.translate("shulkerplanner.gui.button.open_material_list"), PlannerButton.Style.GHOST);
            button.setIcon(new ItemStack(Items.PAPER));
            button.setWidth(w);
            this.addButton(button, (btn, mouseButton) -> {
                GuiMaterialList gui = new GuiMaterialList(materialList);
                gui.setParent(this);
                GuiBase.openGui(gui);
            });
        }

        if (materialList != null)
        {
            PlannerButton button = new PlannerButton(x, by + PlannerButton.HEIGHT + 4, w, StringUtils.translate("shulkerplanner.gui.button.refresh_materials"),
                                               PlannerButton.Style.GHOST, StringUtils.translate("shulkerplanner.gui.button.refresh_materials.hover"));
            button.setIcon(new ItemStack(Items.COMPASS));
            button.setWidth(w);
            this.addButton(button, (btn, mouseButton) -> {
                if (this.session.requestMaterialListRefresh())
                {
                    this.addMessage(MessageType.INFO, "shulkerplanner.message.refreshing_materials");
                }
                this.initGui();
            });
        }
    }

    /**
     * Creates the tab specific option buttons, using {@link #addFlowButton}
     */
    protected abstract void createOptionButtons();

    /**
     * Creates the tab specific buttons on the bottom row, right aligned. Keep these few, the summary is on the left.
     * @param y the y position of the bottom row
     */
    protected void createBottomButtons(int y)
    {
    }

    /**
     * @return the summary lines drawn at the bottom left
     */
    protected abstract List<String> getSummaryLines();

    protected void startFlow(int x, int y)
    {
        this.flowX = x;
        this.flowY = y;
        this.flowLeft = x;
    }

    protected int getFlowY()
    {
        return this.flowY;
    }

    /**
     * Starts a new row of the toolbar
     */
    protected void newFlowRow()
    {
        if (this.flowX > this.flowLeft)
        {
            this.flowX = this.flowLeft;
            this.flowY += ROW_HEIGHT;
        }
    }

    private void advanceFlow(int width)
    {
        if (this.flowX > this.flowLeft && this.flowX + width > this.getScreenWidth() - MARGIN)
        {
            this.flowX = this.flowLeft;
            this.flowY += ROW_HEIGHT;
        }
    }

    protected <T extends ButtonBase> T addFlowButton(T button, IButtonActionListener listener)
    {
        this.advanceFlow(button.getWidth());
        button.setPosition(this.flowX, this.flowY);
        this.flowX += button.getWidth() + 4;
        return this.addButton(button, listener);
    }

    /**
     * A small gap between groups of buttons
     */
    protected void addFlowGap()
    {
        this.flowX += 6;
    }

    protected void addFlowLabel(String text)
    {
        int width = Ui.width(text) + 4;
        this.advanceFlow(width);
        this.addWidget(new UiLabel(this.flowX, this.flowY + 5, text, Theme.TEXT_DIM));
        this.flowX += width;
    }

    /**
     * Creates a text field at the next flow position. The field must be created with its final position,
     * because vanilla's EditBox caches the text position in the constructor.
     */
    protected <T extends GuiTextFieldGeneric> T addFlowTextField(TextFieldFactory<T> factory, int width, ITextFieldListener<T> listener)
    {
        this.advanceFlow(width);
        T textField = factory.create(this.flowX, this.flowY + 1, width, 16);
        this.flowX += width + 4;
        this.addTextField(textField, listener);
        return textField;
    }

    public interface TextFieldFactory<T extends GuiTextFieldGeneric>
    {
        T create(int x, int y, int width, int height);
    }

    /**
     * Creates a button showing "Label: Value", cycling through values on left/right click
     */
    protected PlannerButton createValueButton(String labelKey, String value, @Nullable String hover)
    {
        String label = StringUtils.translate(labelKey, value) + " ▾";
        return hover != null ? new PlannerButton(0, 0, -1, label, PlannerButton.Style.SECONDARY, hover, StringUtils.translate("shulkerplanner.gui.hover.cycle"))
                             : new PlannerButton(0, 0, -1, label, PlannerButton.Style.SECONDARY, StringUtils.translate("shulkerplanner.gui.hover.cycle"));
    }

    protected PlannerButton createToggle(String labelKey, boolean value, @Nullable String hover)
    {
        String label = StringUtils.translate(labelKey);
        return hover != null ? PlannerButton.toggle(label, value, hover) : PlannerButton.toggle(label, value);
    }

    protected PlannerButton createAction(String labelKey, @Nullable String hover)
    {
        String label = StringUtils.translate(labelKey);
        return hover != null ? new PlannerButton(0, 0, -1, label, PlannerButton.Style.SECONDARY, hover) : new PlannerButton(0, 0, -1, label, PlannerButton.Style.SECONDARY);
    }

    @Override
    protected void drawScreenBackground(GuiContext ctx, int mouseX, int mouseY)
    {
        int w = this.getScreenWidth();
        int h = this.getScreenHeight();

        // The header, the side bar and the summary bar are opaque, so the hotbar doesn't show through them
        Ui.rect(ctx, 0, 0, w, h, Theme.SCRIM);
        Ui.gradient(ctx, 0, 0, w, HEADER_HEIGHT, Theme.withAlpha(Theme.SURFACE_2, 0xFF), Theme.withAlpha(Theme.SURFACE, 0xFF));
        Ui.hline(ctx, 0, HEADER_HEIGHT, w, Theme.BORDER);
        Ui.rect(ctx, 0, HEADER_HEIGHT + 1, this.sidebarWidth, h - HEADER_HEIGHT - 1, Theme.withAlpha(Theme.SURFACE, 0xFF));
        Ui.vline(ctx, this.sidebarWidth, HEADER_HEIGHT + 1, h - HEADER_HEIGHT - 1, Theme.BORDER);
        int bottom = h - BOTTOM_HEIGHT;
        Ui.rect(ctx, this.sidebarWidth + 1, bottom, w - this.sidebarWidth - 1, BOTTOM_HEIGHT, Theme.withAlpha(Theme.SURFACE, 0xFF));
        Ui.hline(ctx, this.sidebarWidth + 1, bottom, w - this.sidebarWidth - 1, Theme.BORDER);

        // The banner is drawn here, below its action button
        if (this.banner != null && this.bannerY >= 0)
        {
            int x = this.contentX;
            int bw = this.getContentWidth();
            Ui.panel(ctx, x, this.bannerY, bw, this.bannerHeight, this.banner.background());
            Ui.rect(ctx, x, this.bannerY + 2, 2, this.bannerHeight - 4, this.banner.color());
            int actionWidth = this.banner.action() != null ? PlannerButton.autoWidth(this.banner.action(), false, null) + 8 : 0;
            Ui.text(ctx, Ui.trim(this.banner.text(), bw - 14 - actionWidth), x + 8, this.bannerY + 5, this.banner.color());
        }
    }

    @Override
    protected void drawTitle(GuiContext ctx, int mouseX, int mouseY, float partialTicks)
    {
        // Left: icon, brand and schematic name. Then, if there is room before the buttons on the right,
        // the overall progress (the most important status of the whole screen) and the plan state.
        PlannerProject project = this.getProject();
        int x = MARGIN;
        int limit = this.headerButtonsX - 8;
        ItemStack icon = new ItemStack(project != null ? project.getShulkerItem(1) : Items.SHULKER_BOX);
        Ui.item(ctx, icon, x, 9);
        x += 22;

        String brand = StringUtils.translate("shulkerplanner.gui.brand");
        String name = project != null ? project.getSchematicName() : "-";
        int maxName = Math.max(40, Math.min(Math.max(Ui.width(brand), this.getScreenWidth() / 3), limit - x));
        name = Ui.trim(name, maxName);
        Ui.text(ctx, Ui.trim(brand, limit - x), x, 7, Theme.ACCENT_HOVER);
        Ui.textShadow(ctx, name, x, 19, Theme.TEXT);
        int leftEnd = x + Math.max(Math.min(Ui.width(brand), limit - x), Ui.width(name));

        if (project == null)
        {
            return;
        }

        ShulkerPlan plan = this.session.getPlan();
        int count = plan.getBoxCount();

        if (count <= 0)
        {
            return;
        }

        int ready = this.session.getReadyBoxCount();
        boolean locked = this.session.isPlanLocked();
        String label = StringUtils.translate("shulkerplanner.gui.header.progress", ready, count);
        String state = StringUtils.translate(locked ? "shulkerplanner.gui.chip.locked" : "shulkerplanner.gui.chip.draft");
        String raw = project.getPackRawMaterials() ? StringUtils.translate("shulkerplanner.gui.chip.raw") : null;
        int stateWidth = locked ? Ui.width(state) + 18 : Ui.chipWidth(state);
        int chipsWidth = 6 + stateWidth + (raw != null ? 4 + Ui.chipWidth(raw) : 0);
        int px = leftEnd + 16;
        int room = limit - px;
        boolean chips = room >= Ui.width(label) + chipsWidth;

        if (room < Math.max(60, Ui.width(label)))
        {
            return;
        }

        int pw = Math.min(120, room);
        int done = ready >= count ? Theme.SUCCESS : Theme.TEXT_DIM;
        Ui.text(ctx, label, px, 8, done);
        Ui.progress(ctx, px, 20, pw, 6, (double) ready / count, ready >= count ? Theme.SUCCESS : Theme.ACCENT, Theme.SURFACE_3);

        if (chips)
        {
            int cx = px + Ui.width(label) + 6;

            if (locked)
            {
                cx += Ui.lockChip(ctx, state, cx, 6, Theme.ACCENT_HOVER, Theme.ACCENT_BG) + 4;
            }
            else
            {
                cx += Ui.chip(ctx, state, cx, 6, Theme.TEXT_DIM, Theme.SURFACE_3) + 4;
            }

            if (raw != null)
            {
                Ui.chip(ctx, raw, cx, 6, Theme.WARN, Theme.WARN_BG);
            }
        }
    }

    @Override
    public void drawContents(GuiContext ctx, int mouseX, int mouseY, float partialTicks)
    {
        super.drawContents(ctx, mouseX, mouseY, partialTicks);

        List<String> lines = this.getSummaryLines();
        int y = this.getScreenHeight() - BOTTOM_HEIGHT + 4;

        if (lines.size() == 1)
        {
            y += 5;
        }

        for (String line : lines)
        {
            Ui.text(ctx, Ui.trim(line, this.getContentWidth()), this.contentX + 2, y, Theme.TEXT);
            y += 10;
        }

        if (this.session.isWaitingForMaterialList())
        {
            String str = StringUtils.translate("shulkerplanner.gui.label.waiting_for_material_list");
            int w = Ui.width(str);
            int x = this.contentX + (this.getContentWidth() - w) / 2;
            int yc = this.getListY() + 40;
            Ui.panel(ctx, x - 8, yc - 6, w + 16, 20, Theme.SURFACE_2, Theme.WARN);
            Ui.text(ctx, str, x, yc, Theme.WARN);
        }
    }
}
