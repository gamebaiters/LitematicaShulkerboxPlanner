package it.marco.shulkerplanner.compat;

import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IClickableIngredientFactory;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.runtime.IClickableIngredient;
import mezz.jei.api.runtime.IJeiRuntime;

import it.marco.shulkerplanner.Reference;
import it.marco.shulkerplanner.render.PackingAssistant;

@JeiPlugin
public class JeiPluginImpl implements IModPlugin
{
    private static final Identifier UID = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "jei_plugin");
    @Nullable private static IJeiRuntime runtime;

    @Override
    public Identifier getPluginUid()
    {
        return UID;
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime)
    {
        runtime = jeiRuntime;
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration)
    {
        registration.addGenericGuiContainerHandler(AbstractContainerScreen.class, new IGuiContainerHandler<AbstractContainerScreen<?>>()
        {
            @Override
            public List<Rect2i> getGuiExtraAreas(AbstractContainerScreen<?> containerScreen)
            {
                int[] b = PackingAssistant.getPanelBounds();

                if (b[2] > 0 && b[3] > 0)
                {
                    return List.of(new Rect2i(b[0], b[1], b[2], b[3]));
                }

                return List.of();
            }

            @Override
            public Optional<? extends IClickableIngredient<?>> getClickableIngredientUnderMouse(
                    IClickableIngredientFactory builder,
                    AbstractContainerScreen<?> containerScreen,
                    double mouseX,
                    double mouseY)
            {
                PackingAssistant.HoveredItem hovered = PackingAssistant.getHoveredItem(mouseX, mouseY);

                if (hovered != null && hovered.stack().isEmpty() == false)
                {
                    return builder.createBuilder(hovered.stack())
                            .buildWithArea(hovered.x(), hovered.y(), hovered.w(), hovered.h());
                }

                return Optional.empty();
            }
        });
    }

    static boolean hasRuntime()
    {
        return runtime != null;
    }

    static boolean showRecipes(ItemStack stack)
    {
        return show(stack, RecipeIngredientRole.OUTPUT);
    }

    static boolean showUses(ItemStack stack)
    {
        return show(stack, RecipeIngredientRole.INPUT);
    }

    private static boolean show(ItemStack stack, RecipeIngredientRole role)
    {
        IJeiRuntime rt = runtime;

        if (rt == null || stack.isEmpty())
        {
            return false;
        }

        IFocus<ItemStack> focus = rt.getJeiHelpers().getFocusFactory().createFocus(role, VanillaTypes.ITEM_STACK, stack);
        rt.getRecipesGui().show(focus);
        return true;
    }
}
