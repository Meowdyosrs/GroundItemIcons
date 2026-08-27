package com.meowcape.grounditemicons;

import java.time.Duration;
import java.time.Instant;
import javax.inject.Inject;
import net.runelite.client.util.HotkeyListener;

class GroundItemIconsHotkeyListener extends HotkeyListener
{
    private static final int DOUBLE_TAP_DELAY = 250;

    private final GroundItemIconsState state;

    private Instant lastPress;

    @Inject
    private GroundItemIconsHotkeyListener(
        GroundItemIconsConfig config,
        GroundItemIconsState state)
    {
        super(config::hotkey);
        this.state = state;
    }

    @Override
    public void hotkeyPressed()
    {
        if (state.isHidden())
        {
            state.setHidden(false);
            lastPress = null;
        }
        else if (lastPress != null
            && Duration.between(
                lastPress,
                Instant.now())
                .compareTo(
                    Duration.ofMillis(DOUBLE_TAP_DELAY)) < 0)
        {
            state.setHidden(true);
            lastPress = null;
        }
        else
        {
            lastPress = Instant.now();
        }
    }

    @Override
    public void hotkeyReleased()
    {
    }
}