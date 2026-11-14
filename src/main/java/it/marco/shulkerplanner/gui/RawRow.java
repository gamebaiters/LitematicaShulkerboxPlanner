package it.marco.shulkerplanner.gui;

import javax.annotation.Nullable;

import it.marco.shulkerplanner.recipe.RawMaterialCalculator;

/**
 * One row of the raw materials page: a section title, a raw material, or a crafting step
 */
public record RawRow(Kind kind, String itemId, long amount, @Nullable RawMaterialCalculator.Step step)
{
    public enum Kind
    {
        HEADER_RAW,
        RAW,
        HEADER_STEPS,
        STEP
    }

    public static RawRow header(Kind kind, long amount)
    {
        return new RawRow(kind, "", amount, null);
    }
}
