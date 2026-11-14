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
import it.marco.shulkerplanner.gui.ui.FlatButton;
import it.marco.shulkerplanner.gui.ui.Theme;
import it.marco.shulkerplanner.gui.ui.Ui;
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

        if (this.banner != null)
        {
            this.bannerY = y;
            this.bannerHeight = 18;
            y += this.bannerHeight + 4;

            if (this.banner.action() != null && this.banner.onAction() != null)
            {
                Runnable action = this.banner.onAction();
                FlatButton button = new FlatButton(0, 0, -1, this.banner.action(), FlatButton.Style.PRIMARY);
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

        FlatButton close = new FlatButton(0, y, 18, "✕", FlatButton.Style.GHOST, StringUtils.translate("shulkerplanner.gui.button.close"));
        close.setPosition(xRight - close.getWidth(), y);
        this.addButton(close, (btn, mb) -> this.closeGui(true));
        xRight = close.getX() - 4;

        boolean hud = Configs.Generic.HUD_ENABLED.getBooleanValue();
        FlatButton hudButton = FlatButton.toggle(StringUtils.translate("shulkerplanner.gui.button.hud_short"), hud,
                                                 StringUtils.translate("shulkerplanner.gui.button.hud.hover"));
        hudButton.setPosition(xRight - hudButton.getWidth(), y);
        this.addButton(hudButton, (btn, mb) -> {
            Configs.Generic.HUD_ENABLED.setBooleanValue(! Configs.Generic.HUD_ENABLED.getBooleanValue());
            this.initGui();
        });
        xRight = hudButton.getX() - 4;

        if (this.isCompact() == false)
        {
            FlatButton editHud = new FlatButton(0, y, -1, StringUtils.translate("shulkerplanner.gui.button.edit_hud"), FlatButton.Style.SECONDARY,
                                                StringUtils.translate("shulkerplanner.gui.button.edit_hud.hover"));
            editHud.setPosition(xRight - editHud.getWidth(), y);
            this.addButton(editHud, (btn, mb) -> GuiBase.openGui(new GuiHudEditor(this)));
        }
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

            FlatButton button = new FlatButton(x, y, w, label, FlatButton.Style.TAB, tab.getDisplayName(), tab.getDescription());
            button.setIcon(tab.getIcon());
            button.setWidth(w);
            button.setActive(tab == this.tab);
            this.addButton(button, (btn, mouseButton) -> {
                if (tab != this.tab)
                {
                    GuiBase.openGui(PlannerGuis.create(tab, this.getParent()));
                }
            });
            y += FlatButton.HEIGHT + 4;
        }

        if (compact)
        {
            return;
        }

        // Source of the materials, at the bottom of the side bar
        int by = this.getScreenHeight() - BOTTOM_HEIGHT - 2 * (FlatButton.HEIGHT + 4) + 4;
        MaterialListBase materialList = this.session.getMaterialList();

        if (materialList != null && (this.getParent() instanceof GuiMaterialList) == false)
        {
            FlatButton button = new FlatButton(x, by, w, StringUtils.translate("shulkerplanner.gui.button.open_material_list"), FlatButton.Style.GHOST);
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
            FlatButton button = new FlatButton(x, by + FlatButton.HEIGHT + 4, w, StringUtils.translate("shulkerplanner.gui.button.refresh_materials"),
                                               FlatButton.Style.GHOST, StringUtils.translate("shulkerplanner.gui.button.refresh_materials.hover"));
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
        int width = this.getStringWidth(text) + 4;
        this.advanceFlow(width);
        this.addLabel(this.flowX, this.flowY + 5, width, 10, Theme.TEXT_DIM, text);
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
    protected FlatButton createValueButton(String labelKey, String value, @Nullable String hover)
    {
        String label = StringUtils.translate(labelKey, value) + " ▾";
        return hover != null ? new FlatButton(0, 0, -1, label, FlatButton.Style.SECONDARY, hover, StringUtils.translate("shulkerplanner.gui.hover.cycle"))
                             : new FlatButton(0, 0, -1, label, FlatButton.Style.SECONDARY, StringUtils.translate("shulkerplanner.gui.hover.cycle"));
    }

    protected FlatButton createToggle(String labelKey, boolean value, @Nullable String hover)
    {
        String label = StringUtils.translate(labelKey);
        return hover != null ? FlatButton.toggle(label, value, hover) : FlatButton.toggle(label, value);
    }

    protected FlatButton createAction(String labelKey, @Nullable String hover)
    {
        String label = StringUtils.translate(labelKey);
        return hover != null ? new FlatButton(0, 0, -1, label, FlatButton.Style.SECONDARY, hover) : new FlatButton(0, 0, -1, label, FlatButton.Style.SECONDARY);
    }

    @Override
    protected void drawScreenBackground(GuiContext ctx, int mouseX, int mouseY)
    {
        int w = this.getScreenWidth();
        int h = this.getScreenHeight();

        Ui.rect(ctx, 0, 0, w, h, Theme.SCRIM);
        Ui.rect(ctx, 0, 0, w, HEADER_HEIGHT, Theme.SURFACE);
        Ui.hline(ctx, 0, HEADER_HEIGHT, w, Theme.BORDER);
        Ui.rect(ctx, 0, HEADER_HEIGHT + 1, this.sidebarWidth, h - HEADER_HEIGHT - 1, Theme.withAlpha(Theme.SURFACE, 0xC0));
        Ui.vline(ctx, this.sidebarWidth, HEADER_HEIGHT + 1, h - HEADER_HEIGHT - 1, Theme.BORDER);
    }

    @Override
    protected void drawTitle(GuiContext ctx, int mouseX, int mouseY, float partialTicks)
    {
        PlannerProject project = this.getProject();
        int x = MARGIN;
        ItemStack icon = new ItemStack(project != null ? project.getShulkerItem(1) : Items.SHULKER_BOX);
        Ui.item(ctx, icon, x, 9);
        x += 22;

        Ui.text(ctx, StringUtils.translate("shulkerplanner.gui.brand"), x, 7, Theme.ACCENT_HOVER);
        String name = project != null ? project.getSchematicName() : "-";
        int maxName = Math.max(60, this.getScreenWidth() / 3);
        Ui.textShadow(ctx, Ui.trim(name, maxName), x, 19, Theme.TEXT);

        // Overall progress, the most important status of the whole screen
        if (project != null && this.isCompact() == false)
        {
            ShulkerPlan plan = this.session.getPlan();
            int ready = this.session.getReadyBoxCount();
            int count = plan.getBoxCount();
            int px = x + Math.min(Ui.width(name), maxName) + 20;
            int pw = 120;

            if (px + pw < this.getScreenWidth() - 200 && count > 0)
            {
                String label = StringUtils.translate("shulkerplanner.gui.header.progress", ready, count);
                Ui.text(ctx, label, px, 8, ready >= count ? Theme.SUCCESS : Theme.TEXT_DIM);
                Ui.progress(ctx, px, 20, pw, 6, (double) ready / count, ready >= count ? Theme.SUCCESS : Theme.ACCENT, Theme.SURFACE_3);

                int cx = px + pw + 10;
                if (this.session.isPlanLocked())
                {
                    Ui.lockChip(ctx, StringUtils.translate("shulkerplanner.gui.chip.locked"), cx, 17, Theme.ACCENT_HOVER, Theme.ACCENT_BG);
                }
                else
                {
                    Ui.chip(ctx, StringUtils.translate("shulkerplanner.gui.chip.draft"), cx, 17, Theme.TEXT_DIM, Theme.SURFACE_3);
                }

                if (project.getPackRawMaterials())
                {
                    int cw = Ui.chipWidth(StringUtils.translate(this.session.isPlanLocked() ? "shulkerplanner.gui.chip.locked" : "shulkerplanner.gui.chip.draft")) + (this.session.isPlanLocked() ? 10 : 0);
                    Ui.chip(ctx, StringUtils.translate("shulkerplanner.gui.chip.raw"), cx + cw + 4, 17, Theme.WARN, Theme.WARN_BG);
                }
            }
        }

    }

    @Override
    public void drawContents(GuiContext ctx, int mouseX, int mouseY, float partialTicks)
    {
        if (this.banner != null && this.bannerY >= 0)
        {
            int x = this.contentX;
            int w = this.getContentWidth();
            Ui.panel(ctx, x, this.bannerY, w, this.bannerHeight, this.banner.background());
            Ui.rect(ctx, x, this.bannerY + 2, 2, this.bannerHeight - 4, this.banner.color());
            int actionWidth = this.banner.action() != null ? FlatButton.autoWidth(this.banner.action(), false, null) + 8 : 0;
            Ui.text(ctx, Ui.trim(this.banner.text(), w - 14 - actionWidth), x + 8, this.bannerY + 5, this.banner.color());
        }

        super.drawContents(ctx, mouseX, mouseY, partialTicks);

        List<String> lines = this.getSummaryLines();
        int y = this.getScreenHeight() - BOTTOM_HEIGHT + 4;
        Ui.hline(ctx, this.contentX, y - 4, this.getContentWidth(), Theme.BORDER);

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
