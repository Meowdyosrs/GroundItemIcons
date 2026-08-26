package com.meowcape.grounditemicons;

import com.google.inject.Provides;
import javax.inject.Inject;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.grounditems.GroundItemsPlugin;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
    name = "Ground Item Icons",
    description = "Displays item icons next to Ground Items text.",
    tags = {"ground", "items", "icons", "loot"},
    enabledByDefault = true
)
@PluginDependency(GroundItemsPlugin.class)
public class GroundItemIconsPlugin extends Plugin
{
    @Inject
    private OverlayManager overlayManager;

    @Inject
    private GroundItemIconsOverlay overlay;

    @Inject
    private GroundItemIconsHotkeyListener hotkeyListener;

    @Inject
    private GroundItemIconsState state;

    @Provides
    GroundItemIconsConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(GroundItemIconsConfig.class);
    }

    @Override
    protected void startUp()
    {
        state.setHidden(false);
        hotkeyListener.register();
        overlayManager.add(overlay);
    }

    @Override
    protected void shutDown()
    {
        hotkeyListener.unregister();
        overlayManager.remove(overlay);
    }
}