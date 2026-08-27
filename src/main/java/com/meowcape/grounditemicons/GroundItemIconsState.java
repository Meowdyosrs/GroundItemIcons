package com.meowcape.grounditemicons;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.inject.Singleton;

import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;

@Singleton
public class GroundItemIconsState
{
    private final Table<WorldPoint, Integer, GroundItemIconsEntry> groundItems =
        HashBasedTable.create();

    private boolean hidden;

    public boolean isHidden()
    {
        return hidden;
    }

    public void setHidden(boolean hidden)
    {
        this.hidden = hidden;
    }

    void clear()
    {
        groundItems.clear();
    }

    void addItem(
        Tile tile,
        TileItem item)
    {
        final WorldPoint worldPoint =
            tile.getWorldLocation();

        if (worldPoint == null)
        {
            return;
        }

        final int itemId =
            item.getId();

        final GroundItemIconsEntry existing =
            groundItems.get(
                worldPoint,
                itemId);

        if (existing != null)
        {
            existing.addQuantity(
                item.getQuantity());

            return;
        }

        final WorldView worldView =
            tile.getItemLayer() != null
                ? tile.getItemLayer().getWorldView()
                : null;

        groundItems.put(
            worldPoint,
            itemId,
            new GroundItemIconsEntry(
                worldPoint,
                item,
                worldView));
    }

    void removeItem(
        Tile tile,
        TileItem item)
    {
        final WorldPoint worldPoint =
            tile.getWorldLocation();

        if (worldPoint == null)
        {
            return;
        }

        final int itemId =
            item.getId();

        final GroundItemIconsEntry existing =
            groundItems.get(
                worldPoint,
                itemId);

        if (existing == null)
        {
            return;
        }

        if (existing.getQuantity()
            <= item.getQuantity())
        {
            groundItems.remove(
                worldPoint,
                itemId);

            return;
        }

        existing.removeQuantity(
            item.getQuantity());
    }

    void updateQuantity(
        Tile tile,
        TileItem item,
        int oldQuantity,
        int newQuantity)
    {
        final WorldPoint worldPoint =
            tile.getWorldLocation();

        if (worldPoint == null)
        {
            return;
        }

        final GroundItemIconsEntry existing =
            groundItems.get(
                worldPoint,
                item.getId());

        if (existing == null)
        {
            return;
        }

        existing.addQuantity(
            newQuantity - oldQuantity);

        if (existing.getQuantity() <= 0)
        {
            groundItems.remove(
                worldPoint,
                item.getId());
        }
    }

    void removeWorldView(
        WorldView worldView)
    {
        groundItems.values().removeIf(
            entry -> entry.getWorldView() == worldView);
    }

    Collection<GroundItemIconsEntry> getItems(
        WorldPoint worldPoint)
    {
        return groundItems.row(worldPoint).values();
    }

    List<GroundItemIconsEntry> getAllItems()
    {
        return new ArrayList<>(
            groundItems.values());
    }

    void populateFromScene(
        WorldView worldView)
    {
        if (worldView == null
            || worldView.getScene() == null
            || worldView.getScene().getTiles() == null)
        {
            return;
        }

        final Tile[][][] tiles =
            worldView.getScene().getTiles();

        for (Tile[][] plane : tiles)
        {
            if (plane == null)
            {
                continue;
            }

            for (Tile[] row : plane)
            {
                if (row == null)
                {
                    continue;
                }

                for (Tile tile : row)
                {
                    if (tile == null
                        || tile.getGroundItems() == null)
                    {
                        continue;
                    }

                    for (TileItem item : tile.getGroundItems())
                    {
                        addItem(
                            tile,
                            item);
                    }
                }
            }
        }
    }
}