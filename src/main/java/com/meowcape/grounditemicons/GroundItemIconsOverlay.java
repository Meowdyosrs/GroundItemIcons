package com.meowcape.grounditemicons;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.Perspective;
import net.runelite.api.WorldView;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.util.QuantityFormatter;
import net.runelite.client.util.Text;
import net.runelite.client.util.WildcardMatcher;

public class GroundItemIconsOverlay extends Overlay
{
    private static final String GROUND_ITEMS_GROUP = "grounditems";
    private static final String ICON_COLOR_PREFIX = "highlight_";
    private static final int MAX_DISTANCE = 2500;
    private static final int OFFSET_Z = 20;
    private static final int STRING_GAP = 15;
    private static final int ICON_GAP = 2;

    private final Client client;
    private final ItemManager itemManager;
    private final GroundItemIconsConfig config;
    private final net.runelite.client.config.ConfigManager configManager;

    @Inject
    private GroundItemIconsOverlay(
        Client client,
        ItemManager itemManager,
        GroundItemIconsConfig config,
        net.runelite.client.config.ConfigManager configManager)
    {
        this.client = client;
        this.itemManager = itemManager;
        this.config = config;
        this.configManager = configManager;

        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!config.showItemIcons())
        {
            return null;
        }

        if (!groundItemsOverlayIsVisible())
        {
            return null;
        }

        final net.runelite.api.Player player = client.getLocalPlayer();

        if (player == null)
        {
            return null;
        }

        final WorldView worldView = client.getTopLevelWorldView();

        if (worldView == null)
        {
            return null;
        }

        final Scene scene = worldView.getScene();

        if (scene == null)
        {
            return null;
        }

        final Tile[][][] tiles = scene.getTiles();

        if (tiles == null)
        {
            return null;
        }

        final int iconSize = Math.max(1, Math.min(config.iconSize(), 64));
        final Map<WorldPoint, Integer> offsetMap = new HashMap<>();

        for (int plane = 0; plane < tiles.length; plane++)
        {
            final Tile[][] planeTiles = tiles[plane];

            if (planeTiles == null)
            {
                continue;
            }

            for (int x = 0; x < planeTiles.length; x++)
            {
                final Tile[] row = planeTiles[x];

                if (row == null)
                {
                    continue;
                }

                for (int y = 0; y < row.length; y++)
                {
                    final Tile tile = row[y];

                    if (tile == null || tile.getGroundItems() == null)
                    {
                        continue;
                    }

                    final WorldPoint worldPoint = tile.getWorldLocation();

                    if (worldPoint == null)
                    {
                        continue;
                    }

                    final LocalPoint groundPoint = LocalPoint.fromWorld(worldView, worldPoint);

                    if (groundPoint == null || player.getLocalLocation().distanceTo(groundPoint) > MAX_DISTANCE)
                    {
                        continue;
                    }

                    for (TileItem item : tile.getGroundItems())
                    {
                        if (!shouldDisplayItem(item))
                        {
                            continue;
                        }

                        final ItemComposition itemComposition = itemManager.getItemComposition(item.getId());

                        if (itemComposition == null)
                        {
                            continue;
                        }

                        final String itemString = buildItemString(item, itemComposition);
                        final int offset = offsetMap.compute(
                            worldPoint,
                            (k, v) -> v == null ? 0 : v + 1);

                        final net.runelite.api.Point textPoint = Perspective.getCanvasTextLocation(
                            client,
                            graphics,
                            groundPoint,
                            itemString,
                            tile.getItemLayer() != null ? tile.getItemLayer().getHeight() + OFFSET_Z : OFFSET_Z);

                        if (textPoint == null)
                        {
                            continue;
                        }

                        final BufferedImage image = itemManager.getImage(item.getId());

                        if (image == null)
                        {
                            continue;
                        }

                        final int textX = textPoint.getX();
                        final int textY = textPoint.getY() - STRING_GAP * offset;

                        final int iconX = textX - iconSize - ICON_GAP;
                        final int iconY = textY - iconSize + 2;

                        graphics.drawImage(
                            image,
                            iconX,
                            iconY,
                            iconSize,
                            iconSize,
                            null);
                    }
                }
            }
        }

        return null;
    }

    private boolean groundItemsOverlayIsVisible()
    {
        final String mode = getString("itemHighlightMode", "BOTH");
        return !"NONE".equalsIgnoreCase(mode) && !"MENU".equalsIgnoreCase(mode);
    }

    private boolean shouldDisplayItem(TileItem item)
    {
        if (!ownershipMatches(item))
        {
            return false;
        }

        final ItemComposition composition = itemManager.getItemComposition(item.getId());

        if (composition == null)
        {
            return false;
        }

        final String name = composition.getName();
        final int quantity = item.getQuantity();

        final int match = listMatch(getString("highlightedItems", ""), name, quantity);

        final int hiddenMatch = listMatch(getString(
            "hiddenItems",
            "Vial, Ashes, Coins, Bones, Bucket, Jug, Seaweed"), name, quantity);

        final boolean highlightedByList = match != 0;
        final boolean hiddenByList = hiddenMatch != 0;

        if (highlightedByList)
        {
            return true;
        }

        if (hiddenByList)
        {
            return false;
        }

        final int realItemId = composition.getNote() != -1
            ? composition.getLinkedNoteId()
            : item.getId();

        final int gePrice = realItemId == ItemID.COINS
            ? 1
            : itemManager.getItemPrice(realItemId);

        final int haPrice = composition.getHaPrice();

        final boolean customColor = configManager.getConfiguration(
            GROUND_ITEMS_GROUP,
            ICON_COLOR_PREFIX + item.getId(),
            Color.class) != null;

        final int value = getValueByMode(gePrice, haPrice);

        final boolean implicitlyHighlighted = customColor || isPriceHighlighted(value);

        if (getBoolean("showHighlightedOnly", false) && !implicitlyHighlighted)
        {
            return false;
        }

        final boolean dontHideUntradeables = getBoolean("dontHideUntradeables", true);
        final boolean canBeHidden =
            gePrice > 0
                || composition.isTradeable()
                || !dontHideUntradeables;

        final int hideUnderValue = getInt("hideUnderValue", 0);

        final boolean underGe = gePrice < hideUnderValue;
        final boolean underHa = haPrice < hideUnderValue;

        final boolean implicitlyHidden =
            canBeHidden && underGe && underHa;

        return !implicitlyHidden;
    }

    private boolean ownershipMatches(TileItem item)
    {
        final String mode = getString("ownershipFilterMode", "ALL");
        final int ownership = item.getOwnership();
        final int accountType = client.getVarbitValue(VarbitID.IRONMAN);

        if ("DROPS".equalsIgnoreCase(mode))
        {
            return ownership == TileItem.OWNERSHIP_SELF
                || ownership == TileItem.OWNERSHIP_GROUP;
        }

        if ("TAKEABLE".equalsIgnoreCase(mode))
        {
            return ownership != TileItem.OWNERSHIP_OTHER || accountType == 0;
        }

        return true;
    }

    private boolean isPriceHighlighted(int value)
    {
        return isAboveConfiguredThreshold(value, "lowValuePrice", 20000)
            || isAboveConfiguredThreshold(value, "mediumValuePrice", 100000)
            || isAboveConfiguredThreshold(value, "highValuePrice", 1000000)
            || isAboveConfiguredThreshold(value, "insaneValuePrice", 10000000);
    }

    private boolean isAboveConfiguredThreshold(int value, String key, int defaultValue)
    {
        final int threshold = getInt(key, defaultValue);
        return threshold > 0 && value > threshold;
    }

    private int getValueByMode(int gePrice, int haPrice)
    {
        final String mode = getString("highlightValueCalculation", "HIGHEST");

        if ("GE".equalsIgnoreCase(mode))
        {
            return gePrice;
        }

        if ("HA".equalsIgnoreCase(mode))
        {
            return haPrice;
        }

        return Math.max(gePrice, haPrice);
    }

    private String buildItemString(TileItem item, ItemComposition composition)
    {
        final StringBuilder builder = new StringBuilder(composition.getName());

        if (item.getQuantity() > 1)
        {
            builder.append(" (")
                .append(QuantityFormatter.quantityToStackSize(item.getQuantity()))
                .append(')');
        }

        if (item.getId() != ItemID.COINS)
        {
            final int realItemId = composition.getNote() != -1
                ? composition.getLinkedNoteId()
                : item.getId();

            final int gePrice = itemManager.getItemPrice(realItemId);
            final int haPrice = composition.getHaPrice();
            final String displayMode = getString("priceDisplayMode", "BOTH");

            if ("BOTH".equalsIgnoreCase(displayMode))
            {
                if (gePrice > 0)
                {
                    builder.append(" (GE: ")
                        .append(QuantityFormatter.quantityToStackSize(gePrice))
                        .append(" gp)");
                }

                if (haPrice > 0)
                {
                    builder.append(" (HA: ")
                        .append(QuantityFormatter.quantityToStackSize(haPrice))
                        .append(" gp)");
                }
            }
            else if (!"OFF".equalsIgnoreCase(displayMode))
            {
                final int price = "GE".equalsIgnoreCase(displayMode) ? gePrice : haPrice;

                if (price > 0)
                {
                    builder.append(" (")
                        .append(QuantityFormatter.quantityToStackSize(price))
                        .append(" gp)");
                }
            }
        }

        return builder.toString();
    }

    private int listMatch(String csv, String itemName, int quantity)
    {
        final List<String> entries = Text.fromCSV(csv);

        for (String entry : entries)
        {
            final ItemRule rule = ItemRule.parse(entry);

            if (rule != null && !rule.wildcard
                && rule.name.equalsIgnoreCase(itemName)
                && rule.quantityMatches(quantity))
            {
                return 2;
            }
        }

        for (String entry : entries)
        {
            final ItemRule rule = ItemRule.parse(entry);

            if (rule != null && rule.wildcard
                && WildcardMatcher.matches(rule.name, itemName)
                && rule.quantityMatches(quantity))
            {
                return 1;
            }
        }

        return 0;
    }

    private String getString(String key, String defaultValue)
    {
        final String value = configManager.getConfiguration(GROUND_ITEMS_GROUP, key, String.class);
        return value == null ? defaultValue : value;
    }

    private boolean getBoolean(String key, boolean defaultValue)
    {
        final Boolean value = configManager.getConfiguration(GROUND_ITEMS_GROUP, key, Boolean.class);
        return value == null ? defaultValue : value;
    }

    private int getInt(String key, int defaultValue)
    {
        final Integer value = configManager.getConfiguration(GROUND_ITEMS_GROUP, key, Integer.class);
        return value == null ? defaultValue : value;
    }

    private static final class ItemRule
    {
        private final String name;
        private final int quantity;
        private final boolean lessThan;
        private final boolean wildcard;

        private ItemRule(String name, int quantity, boolean lessThan, boolean wildcard)
        {
            this.name = name;
            this.quantity = quantity;
            this.lessThan = lessThan;
            this.wildcard = wildcard;
        }

        private static ItemRule parse(String entry)
        {
            if (entry == null || entry.trim().isEmpty())
            {
                return null;
            }

            String value = entry.trim();
            int quantity = 0;
            boolean lessThan = false;
            boolean wildcard = value.contains("*");

            for (int i = value.length() - 1; i >= 0; i--)
            {
                char c = value.charAt(i);

                if ((c >= '0' && c <= '9') || Character.isWhitespace(c))
                {
                    continue;
                }

                if (c == '<' || c == '>')
                {
                    if (i + 1 < value.length())
                    {
                        try
                        {
                            quantity = Integer.parseInt(value.substring(i + 1).trim());
                        }
                        catch (NumberFormatException e)
                        {
                            quantity = 0;
                            lessThan = false;
                        }

                        lessThan = c == '<';
                        value = value.substring(0, i);
                    }
                }

                break;
            }

            return new ItemRule(value.trim(), quantity, lessThan, wildcard);
        }

        private boolean quantityMatches(int itemCount)
        {
            return lessThan ? itemCount < quantity : itemCount > quantity;
        }
    }
}
