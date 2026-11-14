package it.marco.shulkerplanner.gui.widgets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;

import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigInteger;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.widgets.WidgetListEntryBase;
import fi.dy.masa.malilib.render.GuiContext;

import it.marco.shulkerplanner.config.Configs;
import it.marco.shulkerplanner.gui.SettingRow;
import it.marco.shulkerplanner.gui.ui.PlannerButton;
import it.marco.shulkerplanner.gui.ui.Theme;
import it.marco.shulkerplanner.gui.ui.Ui;

/**
 * One setting row: section title or boolean toggle with hover comment
 */
public class WidgetPlannerSettingEntry extends WidgetListEntryBase<SettingRow>
{
    private final WidgetListPlannerSettings list;
    private final boolean isOdd;
    private static final int VALUE_WIDTH = 34;
    private int toggleX;
    private int valueX;

    public WidgetPlannerSettingEntry(int x, int y, int width, int height, boolean isOdd,
                                     @Nullable SettingRow entry, int listIndex, WidgetListPlannerSettings list)
    {
        super(x, y, width, height, entry, listIndex);

        this.list = list;
        this.isOdd = isOdd;
        this.toggleX = x + width;

        if (entry != null && entry.number() != null)
        {
            // Number: [-] value [+], in steps of 10 (or 1 with Shift)
            ConfigInteger number = entry.number();
            int by = y + (height - PlannerButton.HEIGHT) / 2;
            PlannerButton plus = new PlannerButton(x + width - 24, by, 18, "+", PlannerButton.Style.SECONDARY, number.getComment());
            this.addButton(plus, (btn, mb) -> this.changeNumber(number, 1));
            this.valueX = plus.getX() - 4 - VALUE_WIDTH;
            PlannerButton minus = new PlannerButton(this.valueX - 4 - 18, by, 18, "-", PlannerButton.Style.SECONDARY, number.getComment());
            this.addButton(minus, (btn, mb) -> this.changeNumber(number, -1));
            this.toggleX = minus.getX();
            return;
        }

        if (entry == null || entry.config() == null)
        {
            return;
        }

        ConfigBoolean config = entry.config();
        PlannerButton toggle = PlannerButton.toggle("", config.getBooleanValue(), config.getComment());
        toggle.setPosition(x + width - toggle.getWidth() - 6, y + (height - PlannerButton.HEIGHT) / 2);
        this.addButton(toggle, (btn, mb) -> {
            config.setBooleanValue(! config.getBooleanValue());
            Configs.saveToFile();
            this.list.getGui().initGui();
        });
        this.toggleX = toggle.getX();
    }

    private void changeNumber(ConfigInteger number, int direction)
    {
        int step = GuiBase.isShiftDown() ? 1 : 10;
        int value = number.getIntegerValue() + direction * step;
        number.setIntegerValue(Math.max(number.getMinIntegerValue(), Math.min(number.getMaxIntegerValue(), value)));
        Configs.saveToFile();
        this.list.getGui().initGui();
    }

    @Override
    public void render(GuiContext ctx, int mouseX, int mouseY, boolean selected)
    {
        SettingRow entry = this.entry;

        if (entry == null)
        {
            return;
        }

        int x = this.x;
        int y = this.y;
        int w = this.width;
        int h = this.height;

        if (entry.kind() == SettingRow.Kind.HEADER)
        {
            Ui.rect(ctx, x, y + 2, 2, h - 4, Theme.ACCENT);
            Ui.textShadow(ctx, entry.title(), x + 8, y + 6, Theme.ACCENT_HOVER);
            Ui.hline(ctx, x, y + h - 1, w, Theme.BORDER);
            return;
        }

        boolean hovered = Ui.inside(mouseX, mouseY, x, y, w, h);
        int bg = hovered ? Theme.SURFACE_HOVER : (this.isOdd ? Theme.SURFACE_2 : Theme.SURFACE);
        Ui.panel(ctx, x, y + 1, w, h - 2, bg);

        int maxTextW = Math.max(40, this.toggleX - x - 16);
        int ty = y + (h - 8) / 2;
        Ui.text(ctx, Ui.trim(entry.title(), maxTextW), x + 8, ty, Theme.TEXT);

        if (entry.number() != null)
        {
            String value = entry.number().getIntegerValue() + (entry.number() == Configs.Generic.ASSISTANT_OPACITY ? "%" : "");
            Ui.panel(ctx, this.valueX, y + (h - 16) / 2, VALUE_WIDTH, 16, Theme.SLOT_BG, Theme.BORDER);
            Ui.textCentered(ctx, value, this.valueX + VALUE_WIDTH / 2, ty, Theme.TEXT);
        }

        super.render(ctx, mouseX, mouseY, selected);
    }

    @Override
    public void postRenderHovered(GuiContext ctx, int mouseX, int mouseY, boolean selected)
    {
        super.postRenderHovered(ctx, mouseX, mouseY, selected);

        SettingRow entry = this.entry;

        if (entry == null || entry.kind() == SettingRow.Kind.HEADER || Ui.inside(mouseX, mouseY, this.x, this.y, this.toggleX - this.x, this.height) == false)
        {
            return;
        }

        String comment = entry.comment();

        if (comment != null && comment.isEmpty() == false)
        {
            List<String> lines = new ArrayList<>();
            lines.add(entry.title());
            lines.addAll(Arrays.asList(comment.split("\n")));
            Ui.tooltip(ctx, mouseX, mouseY, lines);
        }
    }
}
