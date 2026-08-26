package com.meowcape.grounditemicons;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class GroundItemIconsPluginTest
{
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(GroundItemIconsPlugin.class);
        RuneLite.main(args);
    }
}
