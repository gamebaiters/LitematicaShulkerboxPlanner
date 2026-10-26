package it.marco.shulkerplanner;

import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.event.RenderEventHandler;
import fi.dy.masa.malilib.event.TickHandler;
import fi.dy.masa.malilib.event.WorldLoadHandler;
import fi.dy.masa.malilib.interfaces.IInitializationHandler;
import fi.dy.masa.malilib.registry.Registry;
import fi.dy.masa.malilib.util.data.ModInfo;

import it.marco.shulkerplanner.config.Configs;
import it.marco.shulkerplanner.data.PlannerSession;
import it.marco.shulkerplanner.event.InputHandler;
import it.marco.shulkerplanner.event.KeyCallbacks;
import it.marco.shulkerplanner.event.WorldLoadListener;
import it.marco.shulkerplanner.gui.GuiPlannerConfigs;
import it.marco.shulkerplanner.render.HudRenderer;

public class InitHandler implements IInitializationHandler
{
    @Override
    public void registerModHandlers()
    {
        ConfigManager.getInstance().registerConfigHandler(Reference.MOD_ID, new Configs());
        Registry.CONFIG_SCREEN.registerConfigScreenFactory(new ModInfo(Reference.MOD_ID, Reference.MOD_NAME, GuiPlannerConfigs::new));

        InputEventHandler.getKeybindManager().registerKeybindProvider(InputHandler.getInstance());
        RenderEventHandler.getInstance().registerInGameGuiRenderer(HudRenderer.INSTANCE);

        TickHandler.getInstance().registerClientTickHandler(PlannerSession.INSTANCE);

        WorldLoadListener listener = new WorldLoadListener();
        WorldLoadHandler.getInstance().registerWorldLoadPreHandler(listener);
        WorldLoadHandler.getInstance().registerWorldLoadPostHandler(listener);

        KeyCallbacks.init();
    }
}
