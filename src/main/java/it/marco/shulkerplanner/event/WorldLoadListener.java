package it.marco.shulkerplanner.event;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

import fi.dy.masa.malilib.interfaces.IWorldLoadListener;

import it.marco.shulkerplanner.data.PlannerSession;

public class WorldLoadListener implements IWorldLoadListener
{
    @Override
    public void onWorldLoadPre(@Nullable ClientLevel worldBefore, @Nullable ClientLevel worldAfter, Minecraft mc)
    {
        if (worldBefore != null)
        {
            PlannerSession.INSTANCE.save();
        }
    }

    @Override
    public void onWorldLoadPost(@Nullable ClientLevel worldBefore, @Nullable ClientLevel worldAfter, Minecraft mc)
    {
        // Only a real world change, not a dimension change
        if (worldBefore == null || worldAfter == null)
        {
            PlannerSession.INSTANCE.onWorldChanged(worldAfter != null);
        }
    }
}
