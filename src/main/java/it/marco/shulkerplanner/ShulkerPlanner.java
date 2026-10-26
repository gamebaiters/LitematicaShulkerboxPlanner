package it.marco.shulkerplanner;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.fabricmc.api.ClientModInitializer;

import fi.dy.masa.malilib.event.InitializationHandler;

public class ShulkerPlanner implements ClientModInitializer
{
    public static final Logger LOGGER = LogManager.getLogger(Reference.MOD_ID);

    @Override
    public void onInitializeClient()
    {
        InitializationHandler.getInstance().registerInitializationHandler(new InitHandler());
    }
}
