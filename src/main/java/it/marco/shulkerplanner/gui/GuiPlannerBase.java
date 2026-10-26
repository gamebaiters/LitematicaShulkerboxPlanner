package it.marco.shulkerplanner.gui;

import java.util.List;
import javax.annotation.Nullable;

import net.minecraft.client.gui.screens.Screen;

import fi.dy.masa.litematica.gui.GuiMaterialList;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.GuiTextFieldGeneric;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.interfaces.ITextFieldListener;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;
import fi.dy.masa.malilib.gui.widgets.WidgetListEntryBase;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.GuiUtils;
import fi.dy.masa.malilib.util.StringUtils;

import it.marco.shulkerplanner.config.Configs;
import it.marco.shulkerplanner.data.PlannerProject;
import it.marco.shulkerplanner.data.PlannerSession;

/**
 * Common layout of the three planner tabs:
 * title, tab buttons, a flowing row (or rows) of options, the list, and a summary row at the bottom.
 */
public abstract class GuiPlannerBase<TYPE, WIDGET extends WidgetListEntryBase<TYPE>, WIDGETLIST extends WidgetListBase<TYPE, WIDGET>>
        extends GuiListBase<TYPE, WIDGET, WIDGETLIST>
{
    protected static final int MARGIN = 10;
    protected static final int ROW_HEIGHT = 22;
    protected static final int BOTTOM_HEIGHT = 28;

    protected final PlannerSession session = PlannerSession.INSTANCE;
    protected final PlannerTab tab;
    private int flowX;
    private int flowY;

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

    @Override
    protected int getBrowserWidth()
    {
        return this.getScreenWidth() - MARGIN * 2;
    }

    @Override
    protected int getBrowserHeight()
    {
        return Math.max(40, this.getScreenHeight() - this.getListY() - BOTTOM_HEIGHT - 4);
    }

    @Override
    public void initGui()
    {
        PlannerProject project = this.getProject();
        this.title = StringUtils.translate("shulkerplanner.gui.title", project != null ? project.getName() : "-");

        super.initGui();

        this.session.setUpdateListener(this::onSessionUpdated);

        this.createTabButtons();

        this.startFlow(MARGIN, 46);
        this.createOptionButtons();

        int listY = this.flowY + ROW_HEIGHT + 2;

        if (listY != this.getListY())
        {
            this.setListPosition(MARGIN, listY);
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

    private void createTabButtons()
    {
        int x = MARGIN;
        int y = 22;

        for (PlannerTab tab : PlannerTab.values())
        {
            String label = tab.getDisplayName();

            if (tab == PlannerTab.REMINDERS && this.getProject() != null)
            {
                int open = this.getProject().getOpenReminderCount();

                if (open > 0)
                {
                    label += " (" + open + ")";
                }
            }

            ButtonGeneric button = new ButtonGeneric(x, y, -1, 20, label);
            button.setEnabled(tab != this.tab);
            this.addButton(button, (btn, mouseButton) -> GuiBase.openGui(PlannerGuis.create(tab, this.getParent())));
            x += button.getWidth() + 2;
        }

        // Right aligned: back to the material list / close
        int xRight = this.getScreenWidth() - MARGIN;
        ButtonGeneric close = new ButtonGeneric(xRight, y, -1, true, "shulkerplanner.gui.button.close");
        this.addButton(close, (btn, mouseButton) -> this.closeGui(true));
        xRight = close.getX() - 2;

        boolean hud = Configs.Generic.HUD_ENABLED.getBooleanValue();
        String hudLabel = StringUtils.translate("shulkerplanner.gui.button.hud",
                                                StringUtils.translate(hud ? "malilib.gui.label_colored.on" : "malilib.gui.label_colored.off"));
        ButtonGeneric hudButton = new ButtonGeneric(xRight, y, -1, 20, hudLabel, StringUtils.translate("shulkerplanner.gui.button.hud.hover"));
        hudButton.setPosition(xRight - hudButton.getWidth(), y);
        this.addButton(hudButton, (btn, mouseButton) -> {
            Configs.Generic.HUD_ENABLED.setBooleanValue(! Configs.Generic.HUD_ENABLED.getBooleanValue());
            this.initGui();
        });
        xRight = hudButton.getX() - 2;

        ButtonGeneric moveHud = new ButtonGeneric(xRight, y, -1, 20, StringUtils.translate("shulkerplanner.gui.button.edit_hud"),
                                                  StringUtils.translate("shulkerplanner.gui.button.edit_hud.hover"));
        moveHud.setPosition(xRight - moveHud.getWidth(), y);
        this.addButton(moveHud, (btn, mouseButton) -> GuiBase.openGui(new GuiHudEditor(this)));
        xRight = moveHud.getX() - 2;

        MaterialListBase materialList = this.session.getMaterialList();

        if (materialList != null && (this.getParent() instanceof GuiMaterialList) == false)
        {
            ButtonGeneric button = new ButtonGeneric(xRight, y, -1, true, "shulkerplanner.gui.button.open_material_list");
            this.addButton(button, (btn, mouseButton) -> {
                GuiMaterialList gui = new GuiMaterialList(materialList);
                gui.setParent(this);
                GuiBase.openGui(gui);
            });
            xRight = button.getX() - 2;
        }

        if (materialList != null)
        {
            ButtonGeneric button = new ButtonGeneric(xRight, y, -1, true, "shulkerplanner.gui.button.refresh_materials");
            button.setHoverStrings(StringUtils.translate("shulkerplanner.gui.button.refresh_materials.hover"));
            this.addButton(button, (btn, mouseButton) -> {
                if (this.session.requestMaterialListRefresh())
                {
                    this.addMessage(fi.dy.masa.malilib.gui.Message.MessageType.INFO, "shulkerplanner.message.refreshing_materials");
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
    }

    protected int getFlowY()
    {
        return this.flowY;
    }

    private void advanceFlow(int width)
    {
        if (this.flowX > MARGIN && this.flowX + width > this.getScreenWidth() - MARGIN)
        {
            this.flowX = MARGIN;
            this.flowY += ROW_HEIGHT;
        }
    }

    protected <T extends ButtonBase> T addFlowButton(T button, IButtonActionListener listener)
    {
        this.advanceFlow(button.getWidth());
        button.setPosition(this.flowX, this.flowY);
        this.flowX += button.getWidth() + 2;
        return this.addButton(button, listener);
    }

    protected void addFlowLabel(String text)
    {
        int width = this.getStringWidth(text) + 4;
        this.advanceFlow(width);
        this.addLabel(this.flowX, this.flowY + 6, width, 10, 0xFFFFFFFF, text);
        this.flowX += width;
    }

    /**
     * Creates a text field at the next flow position. The field must be created with its final position,
     * because vanilla's EditBox caches the text position in the constructor.
     */
    protected <T extends GuiTextFieldGeneric> T addFlowTextField(TextFieldFactory<T> factory, int width, ITextFieldListener<T> listener)
    {
        this.advanceFlow(width);
        T textField = factory.create(this.flowX, this.flowY + 2, width, 16);
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
    protected ButtonGeneric createValueButton(String labelKey, String value, @Nullable String hover)
    {
        String label = StringUtils.translate(labelKey, value);
        ButtonGeneric button = new ButtonGeneric(0, 0, -1, 20, label);

        if (hover != null)
        {
            button.setHoverStrings(hover);
        }

        return button;
    }

    protected ButtonGeneric createOnOffButton(String labelKey, boolean value, @Nullable String hover)
    {
        String status = StringUtils.translate(value ? "malilib.gui.label_colored.on" : "malilib.gui.label_colored.off");
        return this.createValueButton(labelKey, status, hover);
    }

    @Override
    protected void drawTitle(GuiContext ctx, int mouseX, int mouseY, float partialTicks)
    {
        this.drawStringWithShadow(ctx, this.getTitleString(), MARGIN, 8, 0xFFFFFFFF);

        PlannerProject project = this.getProject();

        if (project != null && project.getSourceTitle().isEmpty() == false)
        {
            String src = GuiBase.TXT_GRAY + project.getSourceTitle();
            int w = this.getStringWidth(src);
            this.drawString(ctx, src, this.getScreenWidth() - MARGIN - w, 8, 0xFFFFFFFF);
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
            y += 6;
        }

        for (String line : lines)
        {
            this.drawString(ctx, line, MARGIN + 2, y, 0xFFFFFFFF);
            y += 11;
        }

        if (this.session.isWaitingForMaterialList())
        {
            String str = GuiBase.TXT_GOLD + StringUtils.translate("shulkerplanner.gui.label.waiting_for_material_list");
            int w = this.getStringWidth(str);
            int x = (this.getScreenWidth() - w) / 2;
            int yc = this.getListY() + 30;
            RenderUtils.drawOutlinedBox(ctx, x - 6, yc - 6, w + 12, 20, 0xE0000000, 0xFFFFAA00);
            this.drawString(ctx, str, x, yc, 0xFFFFFFFF);
        }
    }
}
