package com.meowcape.grounditemicons;

import com.google.inject.Provides;
import javax.inject.Inject;

import net.runelite.api.events.ItemDespawned;
import net.runelite.api.events.ItemQuantityChanged;
import net.runelite.api.events.ItemSpawned;
import net.runelite.api.events.WorldViewUnloaded;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.GameState;

import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
    name = "Ground Item Icons",
    description = "Displays item icons next to Ground Items names.",
    tags = {"ground", "items", "icons", "loot"},
    enabledByDefault = true
)
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

    @Inject
    private KeyManager keyManager;

    @Inject
    private net.runelite.api.Client client;

    @Provides
    GroundItemIconsConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(
            GroundItemIconsConfig.class);
    }

    @Override
    protected void startUp()
    {
        state.setHidden(false);
        state.clear();

        keyManager.registerKeyListener(
            hotkeyListener);

        overlayManager.add(
            overlay);

        populateCurrentWorldView();
    }

    @Override
    protected void shutDown()
    {
        keyManager.unregisterKeyListener(
            hotkeyListener);

        overlayManager.remove(
            overlay);

        state.clear();
    }

    @Subscribe
    public void onItemSpawned(
        ItemSpawned event)
    {
        state.addItem(
            event.getTile(),
            event.getItem());
    }

    @Subscribe
    public void onItemDespawned(
        ItemDespawned event)
    {
        state.removeItem(
            event.getTile(),
            event.getItem());
    }

    @Subscribe
    public void onItemQuantityChanged(
        ItemQuantityChanged event)
    {
        state.updateQuantity(
            event.getTile(),
            event.getItem(),
            event.getOldQuantity(),
            event.getNewQuantity());
    }

    @Subscribe
    public void onWorldViewUnloaded(
        WorldViewUnloaded event)
    {
        state.removeWorldView(
            event.getWorldView());
    }

    @Subscribe
    public void onGameStateChanged(
        GameStateChanged event)
    {
        if (event.getGameState() == GameState.LOGGED_IN)
        {
            populateCurrentWorldView();
        }
    }

    private void populateCurrentWorldView()
    {
        state.clear();

        final net.runelite.api.WorldView worldView =
            client.getTopLevelWorldView();

        if (worldView != null)
        {
            state.populateFromScene(
                worldView);
        }
    }
}