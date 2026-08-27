package com.meowcape.grounditemicons;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Keybind;
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
        min = -10,
        max = 15
    )
    default int iconPosition()
    {
        return 6;
    }

    @ConfigItem(
        keyName = "hotkey",
        name = "Hotkey",
        description = "Hotkey used to hide and show ground item icons. This should NOT be the same hotkey as the one used in Ground Items.",
        position = 3
    )
    default Keybind hotkey()
    {
        return Keybind.ALT;
    }
}