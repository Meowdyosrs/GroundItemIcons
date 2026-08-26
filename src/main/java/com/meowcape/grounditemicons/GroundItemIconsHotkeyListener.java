package com.meowcape.grounditemicons;

import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.time.Duration;
import java.time.Instant;
import javax.inject.Inject;

class GroundItemIconsHotkeyListener implements KeyEventDispatcher
{
    private static final int DOUBLE_TAP_DELAY = 250;

    private final GroundItemIconsConfig config;
    private final GroundItemIconsState state;

    private Instant lastPress;
    private boolean hotKeyPressed;

    @Inject
    private GroundItemIconsHotkeyListener(
        GroundItemIconsConfig config,
        GroundItemIconsState state)
    {
        this.config = config;
        this.state = state;
    }

    public void register()
    {
        KeyboardFocusManager
            .getCurrentKeyboardFocusManager()
            .addKeyEventDispatcher(this);
    }

    public void unregister()
    {
        KeyboardFocusManager
            .getCurrentKeyboardFocusManager()
            .removeKeyEventDispatcher(this);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e)
    {
        if (e.getID() == KeyEvent.KEY_PRESSED)
        {
            if (!config.hotkey().matches(e))
            {
                return false;
            }

            if (hotKeyPressed)
            {
                return false;
            }

            if (state.isHidden())
            {
                state.setHidden(false);
                hotKeyPressed = true;
                lastPress = null;
                return false;
            }

            if (lastPress != null
                && Duration.between(lastPress, Instant.now())
                    .compareTo(
                        Duration.ofMillis(DOUBLE_TAP_DELAY)) < 0)
            {
                state.setHidden(true);
                hotKeyPressed = true;
                lastPress = null;
                return false;
            }

            hotKeyPressed = true;
            lastPress = Instant.now();

            return false;
        }

        if (e.getID() == KeyEvent.KEY_RELEASED)
        {
            if (config.hotkey().matches(e))
            {
                hotKeyPressed = false;
            }
        }

        return false;
    }
}