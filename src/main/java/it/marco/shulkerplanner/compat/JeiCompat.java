package it.marco.shulkerplanner.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.item.ItemStack;

/**
 * Optional bridge to JEI so the mod runs fine even when JEI is not installed.
 */
public final class JeiCompat
{
    private static final boolean LOADED = FabricLoader.getInstance().isModLoaded("jei");

    private JeiCompat()
    {
    }

    public static boolean isAvailable()
    {
        return LOADED && JeiPluginImpl.hasRuntime();
    }

    public static boolean showRecipes(ItemStack stack)
    {
        if (LOADED == false || stack.isEmpty())
        {
            return false;
        }

        return JeiPluginImpl.showRecipes(stack);
    }

    public static boolean showUses(ItemStack stack)
    {
        if (LOADED == false || stack.isEmpty())
        {
            return false;
        }

        return JeiPluginImpl.showUses(stack);
    }
}
