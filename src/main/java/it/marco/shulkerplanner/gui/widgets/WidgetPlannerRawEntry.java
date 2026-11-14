package it.marco.shulkerplanner.gui.widgets;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.item.ItemStack;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.widgets.WidgetListEntryBase;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.util.StringUtils;

import it.marco.shulkerplanner.data.MaterialRow;
import it.marco.shulkerplanner.data.PlannerProject;
import it.marco.shulkerplanner.data.PlannerSession;
import it.marco.shulkerplanner.gui.GuiPlannerRaw;
import it.marco.shulkerplanner.gui.RawRow;
import it.marco.shulkerplanner.gui.ui.PlannerButton;
import it.marco.shulkerplanner.gui.ui.Theme;
import it.marco.shulkerplanner.gui.ui.Ui;
import it.marco.shulkerplanner.recipe.RawMaterialCalculator;
import it.marco.shulkerplanner.recipe.SimpleRecipe;
import it.marco.shulkerplanner.util.Format;

/**
 * A raw material (amount, stacks, what you carry) or a crafting step (the recipe, how many crafts)
 */
public class WidgetPlannerRawEntry extends WidgetListEntryBase<RawRow>
{
    private final WidgetListPlannerRaw list;
    private final boolean isOdd;
    private int buttonsX;

    public WidgetPlannerRawEntry(int x, int y, int width, int height, boolean isOdd, @Nullable RawRow entry, int listIndex, WidgetListPlannerRaw list)
    {
        super(x, y, width, height, entry, listIndex);

        this.list = list;
        this.isOdd = isOdd;
        this.buttonsX = x + width;

        GuiPlannerRaw gui = list.getGui();
        PlannerProject project = gui.getProject();

        if (entry == null || project == null || entry.itemId().isEmpty())
        {
            return;
        }

        String id = entry.itemId();
        PlannerSession session = PlannerSession.INSTANCE;
        RawMaterialCalculator calc = session.getRawCalculator();
        int xRight = x + width - 4;

        if (entry.kind() == RawRow.Kind.RAW)
        {
            // Only items kept by the player can be broken down again
            if (project.getRawBaseItems().contains(id))
            {
                PlannerButton button = new PlannerButton(0, y + 2, -1, StringUtils.translate("shulkerplanner.gui.button.break_down"), PlannerButton.Style.SECONDARY,
                                                   StringUtils.translate("shulkerplanner.gui.button.break_down.hover"));
                button.setPosition(xRight - button.getWidth(), y + 2);
                this.addButton(button, (btn, mb) -> {
                    project.setRawBase(id, false);
                    gui.onRecipeOptionsChanged();
                });
                this.buttonsX = button.getX();
            }
        }
        else if (entry.kind() == RawRow.Kind.STEP)
        {
            PlannerButton keep = new PlannerButton(0, y + 3, -1, StringUtils.translate("shulkerplanner.gui.button.keep_item"), PlannerButton.Style.GHOST,
                                             StringUtils.translate("shulkerplanner.gui.button.keep_item.hover"));
            keep.setPosition(xRight - keep.getWidth(), y + 3);
            this.addButton(keep, (btn, mb) -> {
                project.setRawBase(id, true);
                gui.onRecipeOptionsChanged();
            });
            xRight = keep.getX() - 4;
            this.buttonsX = keep.getX();

            List<SimpleRecipe> recipes = calc.getUsableRecipes(id);

            if (recipes.size() > 1 && entry.step() != null)
            {
                int index = Math.max(0, recipes.indexOf(entry.step().recipe()));
                String label = StringUtils.translate("shulkerplanner.gui.button.recipe", index + 1, recipes.size()) + " ▸";
                PlannerButton cycle = new PlannerButton(0, y + 3, -1, label, PlannerButton.Style.SECONDARY,
                                                  StringUtils.translate("shulkerplanner.gui.button.recipe.hover"));
                cycle.setPosition(xRight - cycle.getWidth(), y + 3);
                this.addButton(cycle, (btn, mb) -> {
                    int next = Math.floorMod(index + (mb == 0 ? 1 : -1), recipes.size());
                    project.setRecipeChoice(id, recipes.get(next).id());
                    gui.onRecipeOptionsChanged();
                });
                this.buttonsX = cycle.getX();
            }
        }
    }

    @Override
    public boolean canSelectAt(MouseButtonEvent click)
    {
        return false;
    }

    @Override
    public void render(GuiContext ctx, int mouseX, int mouseY, boolean selected)
    {
        RawRow row = this.entry;

        if (row == null)
        {
            return;
        }

        int x = this.x;
        int y = this.y;

        if (row.kind() == RawRow.Kind.HEADER_RAW || row.kind() == RawRow.Kind.HEADER_STEPS)
        {
            boolean raw = row.kind() == RawRow.Kind.HEADER_RAW;
            String title = StringUtils.translate(raw ? "shulkerplanner.gui.label.raw.header_raw" : "shulkerplanner.gui.label.raw.header_steps");
            String count = raw ? Format.number(row.amount()) : String.valueOf(row.amount());
            Ui.text(ctx, title, x + 4, y + 8, Theme.ACCENT_HOVER);
            Ui.text(ctx, count, x + 8 + Ui.width(title), y + 8, Theme.TEXT_MUTED);
            Ui.rect(ctx, x + 12 + Ui.width(title) + Ui.width(count), y + 12, this.width - 16 - Ui.width(title) - Ui.width(count), 1, Theme.BORDER);
            return;
        }

        boolean hover = this.isMouseOver(mouseX, mouseY);
        Ui.rect(ctx, x, y, this.width, this.height, hover ? Theme.SURFACE_3 : (this.isOdd ? Theme.withAlpha(Theme.SURFACE_2, 0x90) : 0));

        MaterialRow item = new MaterialRow(row.itemId(), 0, 0);
        ItemStack stack = item.getStack();
        int maxStack = Math.max(1, stack.getMaxStackSize());
        int ty = y + (this.height - 8) / 2;
        Ui.item(ctx, stack, x + 6, y + (this.height - 16) / 2);

        if (row.kind() == RawRow.Kind.RAW)
        {
            int nameW = Math.min(170, Math.max(90, this.width / 3));
            Ui.text(ctx, Ui.trim(item.getDisplayName(), nameW - 4), x + 26, ty, Theme.TEXT);
            int cx = x + 26 + nameW;
            String amount = Format.number(row.amount());
            Ui.text(ctx, amount, cx, ty, Theme.TEXT);
            cx += Math.max(50, Ui.width(amount) + 10);

            if (cx + 60 < this.buttonsX)
            {
                Ui.text(ctx, Format.stacks((int) Math.min(Integer.MAX_VALUE, row.amount()), maxStack), cx, ty, Theme.TEXT_DIM);
                cx += 80;
            }

            if (cx + 40 < this.buttonsX)
            {
                Ui.text(ctx, Format.boxes((int) Math.min(Integer.MAX_VALUE, row.amount()), maxStack) + " " + StringUtils.translate("shulkerplanner.gui.label.raw.boxes"),
                        cx, ty, Theme.INFO);
                cx += 70;
            }

            int have = PlannerSession.INSTANCE.getAvailable(row.itemId());

            if (cx + 50 < this.buttonsX)
            {
                Ui.text(ctx, StringUtils.translate("shulkerplanner.gui.label.raw.have", Format.number(have)), cx, ty,
                        have >= row.amount() ? Theme.SUCCESS : (have > 0 ? Theme.WARN : Theme.TEXT_MUTED));
            }

            return;
        }

        RawMaterialCalculator.Step step = row.step();

        if (step == null)
        {
            return;
        }

        // Craft 12 x Stone Bricks  [Stonecutter]  from  (ingredient icons)
        SimpleRecipe recipe = step.recipe();
        String crafts = StringUtils.translate("shulkerplanner.gui.label.raw.crafts", Format.number(step.crafts()));
        int cx = x + 26;
        Ui.text(ctx, crafts, cx, ty, Theme.TEXT_DIM);
        cx += Ui.width(crafts) + 4;
        String name = Ui.trim(item.getDisplayName(), 120);
        Ui.text(ctx, name, cx, ty, Theme.TEXT);
        cx += Ui.width(name) + 6;

        String station = StringUtils.translate("shulkerplanner.recipe_type." + recipe.type().getName());
        int stationColor = switch (recipe.type())
        {
            case SMELTING -> Theme.WARN;
            case STONECUTTING -> Theme.INFO;
            case SMITHING -> Theme.DANGER;
            default -> Theme.SUCCESS;
        };

        if (cx + Ui.chipWidth(station) < this.buttonsX - 10)
        {
            cx += Ui.chip(ctx, station, cx, y + (this.height - 11) / 2, stationColor, Theme.withAlpha(stationColor, 0x30)) + 6;
        }

        RawMaterialCalculator calc = PlannerSession.INSTANCE.getRawCalculator();

        for (SimpleRecipe.Ingredient ing : recipe.ingredients())
        {
            if (cx + 30 > this.buttonsX - 6)
            {
                break;
            }

            String option = calc.bestOption(ing);
            Ui.item(ctx, new MaterialRow(option, 0, 0).getStack(), cx, y + (this.height - 12) / 2, 12, false);
            String c = "×" + Format.number(step.crafts() * ing.count());
            Ui.text(ctx, c, cx + 13, ty, Theme.TEXT_DIM);
            cx += 13 + Ui.width(c) + 6;
        }

        if (step.leftover() > 0 && cx + 40 < this.buttonsX - 6)
        {
            Ui.text(ctx, StringUtils.translate("shulkerplanner.gui.label.raw.leftover", step.leftover()), cx, ty, Theme.TEXT_MUTED);
        }

        super.render(ctx, mouseX, mouseY, selected);
    }

    @Override
    public void postRenderHovered(GuiContext ctx, int mouseX, int mouseY, boolean selected)
    {
        super.postRenderHovered(ctx, mouseX, mouseY, selected);

        RawRow row = this.entry;

        if (row == null || row.itemId().isEmpty() || mouseX >= this.buttonsX)
        {
            return;
        }

        List<String> lines = new ArrayList<>();
        MaterialRow item = new MaterialRow(row.itemId(), 0, 0);
        lines.add(GuiBase.TXT_BOLD + item.getDisplayName() + GuiBase.TXT_RST + GuiBase.TXT_DARK_GRAY + "  " + row.itemId());

        if (row.step() != null)
        {
            RawMaterialCalculator.Step step = row.step();
            RawMaterialCalculator calc = PlannerSession.INSTANCE.getRawCalculator();
            lines.add(StringUtils.translate("shulkerplanner.gui.hover.raw.step", Format.number(step.needed()), Format.number(step.crafts()),
                                            step.recipe().resultCount(), Format.number(step.produced())));
            lines.add(GuiBase.TXT_DARK_GRAY + step.recipe().id());

            for (SimpleRecipe.Ingredient ing : step.recipe().ingredients())
            {
                String option = calc.bestOption(ing);
                String alt = ing.options().size() > 1 ? GuiBase.TXT_DARK_GRAY + " (" + StringUtils.translate("shulkerplanner.gui.hover.raw.any_of", ing.options().size()) + ")" : "";
                lines.add(GuiBase.TXT_GRAY + " " + Format.number(step.crafts() * ing.count()) + " " + new MaterialRow(option, 0, 0).getDisplayName() + alt);
            }
        }
        else
        {
            lines.add(StringUtils.translate("shulkerplanner.gui.hover.raw.raw", Format.number(row.amount())));
        }

        Ui.tooltip(ctx, mouseX, mouseY, lines);
    }
}
