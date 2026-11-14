package it.marco.shulkerplanner.gui.ui;

import fi.dy.masa.malilib.gui.widgets.WidgetBase;
import fi.dy.masa.malilib.render.GuiContext;

/**
 * A one line text label in the planner style
 */
public class UiLabel extends WidgetBase
{
    private final String text;
    private final int color;

    public UiLabel(int x, int y, String text, int color)
    {
        super(x, y, Ui.width(text), 8);

        this.text = text;
        this.color = color;
    }

    @Override
    public void render(GuiContext ctx, int mouseX, int mouseY, boolean selected)
    {
        Ui.text(ctx, this.text, this.x, this.y, this.color);
    }
}
