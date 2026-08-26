package com.meowcape.grounditemicons;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

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
    @Range(
        min = 1,
        max = 32
    )
    default int iconSize()
    {
        return 16;
    }

    @ConfigItem(
        keyName = "iconPosition",
        name = "Icon position",
        description = "Horizontal distance between the item icon and Ground Items text.",
        position = 2
    )
    @Range(
        min = 0,
        max = 15
    )
    default int iconPosition()
    {
        return 6;
    }
}