package com.meowcape.grounditemicons;

import javax.inject.Singleton;

@Singleton
public class GroundItemIconsState
{
    private boolean hidden;

    public boolean isHidden()
    {
        return hidden;
    }

    public void setHidden(boolean hidden)
    {
        this.hidden = hidden;
    }
}