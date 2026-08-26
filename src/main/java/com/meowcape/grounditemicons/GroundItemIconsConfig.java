package com.meowcape.grounditemicons;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(GroundItemIconsConfig.GROUP)
public interface GroundItemIconsConfig extends Config
{
    String GROUP = "grounditemicons";

    @ConfigItem(
        keyName = "showItemIcons",
        name = "Show item icons",
        description = "Display item icons next to Ground Items names.",
        position = 0
    )
    default boolean showItemIcons()
    {
        return true;
    }

    @ConfigItem(
        keyName = "iconSize",
        name = "Item icon size",
        description = "Size of ground item icons in pixels.",
        position = 1
    )
    default int iconSize()
    {
        return 16;
    }
}
