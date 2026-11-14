package it.marco.shulkerplanner.gui.widgets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;

import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.gui.widgets.WidgetListEntryBase;
import fi.dy.masa.malilib.render.GuiContext;

import it.marco.shulkerplanner.config.Configs;
import it.marco.shulkerplanner.gui.SettingRow;
import it.marco.shulkerplanner.gui.ui.FlatButton;
import it.marco.shulkerplanner.gui.ui.Theme;
import it.marco.shulkerplanner.gui.ui.Ui;

/**
 * One setting row: section title or boolean toggle with hover comment
 */
public class WidgetPlannerSettingEntry extends WidgetListEntryBase<SettingRow>
{
    private final WidgetListPlannerSettings list;
    private final boolean isOdd;
    private int toggleX;

    public WidgetPlannerSettingEntry(int x, int y, int width, int height, boolean isOdd,
                                     @Nullable SettingRow entry, int listIndex, WidgetListPlannerSettings list)
    {
        super(x, y, width, height, entry, listIndex);

        this.list = list;
        this.isOdd = isOdd;
        this.toggleX = x + width;

        if (entry == null || entry.config() == null)
        {
            return;
        }

        ConfigBoolean config = entry.config();
        FlatButton toggle = FlatButton.toggle("", config.getBooleanValue(), config.getComment());
        toggle.setPosition(x + width - toggle.getWidth() - 6, y + 3);
        this.addButton(toggle, (btn, mb) -> {
            config.setBooleanValue(! config.getBooleanValue());
            Configs.saveToFile();
            this.list.getGui().initGui();
        });
        this.toggleX = toggle.getX();
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
        Ui.text(ctx, Ui.trim(entry.title(), maxTextW), x + 8, y + 8, Theme.TEXT);

        super.render(ctx, mouseX, mouseY, selected);
    }

    @Override
    public void postRenderHovered(GuiContext ctx, int mouseX, int mouseY, boolean selected)
    {
        super.postRenderHovered(ctx, mouseX, mouseY, selected);

        SettingRow entry = this.entry;

        if (entry == null || entry.config() == null || Ui.inside(mouseX, mouseY, this.x, this.y, this.toggleX - this.x, this.height) == false)
        {
            return;
        }

        String comment = entry.config().getComment();

        if (comment != null && comment.isEmpty() == false)
        {
            List<String> lines = new ArrayList<>();
            lines.add(entry.title());
            lines.addAll(Arrays.asList(comment.split("\n")));
            Ui.tooltip(ctx, mouseX, mouseY, lines);
        }
    }
}
