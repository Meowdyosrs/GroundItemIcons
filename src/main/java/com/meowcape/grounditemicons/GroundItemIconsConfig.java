package com.meowcape.grounditemicons;

import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Keybind;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup(GroundItemIconsConfig.GROUP)
public interface GroundItemIconsConfig extends Config
{
    String GROUP = "grounditemicons";

    @ConfigItem(
        keyName = "iconSize",
        name = "Item icon size",
        description = "Size of ground item icons in pixels.",
        position = 0
    )
    @Range(
        min = 8,
        max = 64
    )
    default int iconSize()
    {
        return 16;
    }

    @ConfigItem(
        keyName = "iconSide",
        name = "Icon side",
        description = "Choose which side of the Ground Items text the icon is displayed on.",
        position = 1
    )
    default IconSide iconSide()
    {
        return IconSide.LEFT;
    }

    @ConfigItem(
        keyName = "iconGap",
        name = "Icon gap",
        description = "Horizontal distance between the item icon and Ground Items text.",
        position = 2
    )
    @Range(
        min = 0,
        max = 15
    )
    default int iconGap()
    {
        return 0;
    }

    @Alpha
    @ConfigItem(
        keyName = "iconOpacity",
        name = "Icon opacity",
        description = "Opacity of the item icons.",
        position = 3
    )
    @Range(
        min = 0,
        max = 100
    )
    default int iconOpacity()
    {
        return 100;
    }

    @ConfigItem(
        keyName = "scaleWithText",
        name = "Scale icon with text",
        description = "Automatically size the icon to match the current Ground Items text height.",
        position = 4
    )
    default boolean scaleWithText()
    {
        return false;
    }

    @ConfigItem(
        keyName = "showHighlightedOnly",
        name = "Show highlighted items only",
        description = "Only display icons for items on the Ground Items highlighted items list.",
        position = 5
    )
    default boolean showHighlightedOnly()
    {
        return false;
    }

    @ConfigItem(
        keyName = "showHiddenItems",
        name = "Show icons for hidden items",
        description = "Display icons for items hidden by the Ground Items hidden items list.",
        position = 6
    )
    default boolean showHiddenItems()
    {
        return false;
    }

    @ConfigItem(
        keyName = "doubleTapDelay",
        name = "Double-tap delay",
        description = "Delay for the double-tap hotkey to hide item icons. 0 to disable.",
        position = 7
    )
    @Units(Units.MILLISECONDS)
    @Range(
        min = 0,
        max = 1000
    )
    default int doubleTapDelay()
    {
        return 250;
    }

    @ConfigItem(
        keyName = "hotkey",
        name = "Hotkey",
        description = "Configures the hotkey used by the Ground Item Icons plugin. Cannot be the same as Ground Items.",
        position = 8
    )
    default Keybind hotkey()
    {
        return Keybind.ALT;
    }
}