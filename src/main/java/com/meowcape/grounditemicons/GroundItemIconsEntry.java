package com.meowcape.grounditemicons;

import net.runelite.api.TileItem;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;

class GroundItemIconsEntry
{
    private final WorldPoint worldPoint;
    private final int itemId;
    private final TileItem item;
    private final WorldView worldView;

    private int quantity;

    GroundItemIconsEntry(
        WorldPoint worldPoint,
        TileItem item,
        WorldView worldView)
    {
        this.worldPoint = worldPoint;
        this.itemId = item.getId();
        this.item = item;
        this.worldView = worldView;
        this.quantity = item.getQuantity();
    }

    WorldPoint getWorldPoint()
    {
        return worldPoint;
    }

    int getItemId()
    {
        return itemId;
    }

    TileItem getItem()
    {
        return item;
    }

    WorldView getWorldView()
    {
        return worldView;
    }

    int getQuantity()
    {
        return quantity;
    }

    void addQuantity(int amount)
    {
        quantity += amount;
    }

    void removeQuantity(int amount)
    {
        quantity -= amount;
    }
}