package com.meowcape.grounditemicons;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

import com.google.common.collect.Table;
import lombok.Value;

import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.Perspective;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.grounditems.GroundItemsPlugin;
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

    private final Client client;
    private final ItemManager itemManager;
    private final GroundItemIconsConfig config;
    private final net.runelite.client.config.ConfigManager configManager;
    private final PluginManager pluginManager;
    private final Method isHideAllMethod;

    @Inject
    private GroundItemIconsOverlay(
        Client client,
        ItemManager itemManager,
        GroundItemIconsConfig config,
        net.runelite.client.config.ConfigManager configManager,
        PluginManager pluginManager)
    {
        this.client = client;
        this.itemManager = itemManager;
        this.config = config;
        this.configManager = configManager;
        this.pluginManager = pluginManager;

        try
        {
            isHideAllMethod =
                GroundItemsPlugin.class.getDeclaredMethod("isHideAll");
            isHideAllMethod.setAccessible(true);
        }
        catch (ReflectiveOperationException e)
        {
            throw new IllegalStateException(
                "Unable to access Ground Items hide state",
                e);
        }

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

        if (isGroundItemsHidden())
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

        final int iconSize =
            Math.max(1, Math.min(config.iconSize(), 64));

        final int iconGap =
            Math.max(0, Math.min(config.iconPosition(), 15));

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

                    if (tile == null
                        || tile.getGroundItems() == null)
                    {
                        continue;
                    }

                    final WorldPoint worldPoint =
                        tile.getWorldLocation();

                    if (worldPoint == null)
                    {
                        continue;
                    }

                    final LocalPoint groundPoint =
                        LocalPoint.fromWorld(
                            worldView,
                            worldPoint);

                    if (groundPoint == null
                        || player.getLocalLocation()
                            .distanceTo(groundPoint) > MAX_DISTANCE)
                    {
                        continue;
                    }

                    final List<TileItem> displayableItems = new ArrayList<>();

                    for (TileItem item : tile.getGroundItems())
                    {
                        if (shouldDisplayItem(item))
                        {
                            displayableItems.add(item);
                        }
                    }

                    final List<GroupedGroundItem> groups =
                        orderToMatch(
                            groupByItemId(displayableItems),
                            orderedItemIdsAt(worldPoint));

                    for (GroupedGroundItem grouped : groups)
                    {
                        final TileItem item = grouped.getRepresentative();
                        final int totalQuantity = grouped.getTotalQuantity();

                        final ItemComposition itemComposition =
                            itemManager.getItemComposition(
                                item.getId());

                        if (itemComposition == null)
                        {
                            continue;
                        }

                        final String itemString =
                            buildItemString(
                                item,
                                itemComposition,
                                totalQuantity);

                        final int offset =
                            offsetMap.compute(
                                worldPoint,
                                (k, v) ->
                                    v == null ? 0 : v + 1);

                        final net.runelite.api.Point textPoint =
                            Perspective.getCanvasTextLocation(
                                client,
                                graphics,
                                groundPoint,
                                itemString,
                                tile.getItemLayer() != null
                                    ? tile.getItemLayer().getHeight()
                                        + OFFSET_Z
                                    : OFFSET_Z);

                        if (textPoint == null)
                        {
                            continue;
                        }

                        final BufferedImage image =
                            itemManager.getImage(item.getId());

                        if (image == null)
                        {
                            continue;
                        }

                        final int textX =
                            textPoint.getX();

                        final int textY =
                            textPoint.getY()
                                - STRING_GAP * offset;

                        final int iconX =
                            textX
                                - iconSize
                                - iconGap;

                        final int iconY =
                            textY
                                - iconSize
                                + 2;

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

    /**
     * Groups {@code items} by item ID, summing quantities into a single entry per ID.
     * <p>The game can spawn multiple separate {@link TileItem} entries for what renders as a
     * single ground item pile (e.g. dropping a stack of unstackable food) - RuneLite's own core
     * {@code GroundItemsPlugin} sums quantities the same way for this exact reason. Without this
     * grouping, each entry got its own icon, stacking N duplicate icons on a pile of N items
     * instead of drawing one icon with the combined quantity.
     * <p>The representative kept for each group is simply the first entry encountered - only
     * {@link TileItem#getId()} is used afterwards (icon image, price lookups), which is identical
     * across every entry in the group by construction.
     */
    static List<GroupedGroundItem> groupByItemId(Collection<TileItem> items)
    {
        final Map<Integer, TileItem> firstItemById = new LinkedHashMap<>();
        final Map<Integer, Integer> quantityById = new LinkedHashMap<>();

        for (TileItem item : items)
        {
            firstItemById.putIfAbsent(item.getId(), item);
            quantityById.merge(item.getId(), item.getQuantity(), Integer::sum);
        }

        final List<GroupedGroundItem> grouped = new ArrayList<>();

        for (Map.Entry<Integer, TileItem> entry : firstItemById.entrySet())
        {
            grouped.add(
                new GroupedGroundItem(
                    entry.getValue(),
                    quantityById.get(entry.getKey())));
        }

        return grouped;
    }

    @Value
    static class GroupedGroundItem
    {
        TileItem representative;
        int totalQuantity;
    }

    private boolean isGroundItemsHidden()
    {
        final GroundItemsPlugin groundItemsPlugin = findGroundItemsPlugin();

        if (groundItemsPlugin == null)
        {
            return false;
        }

        try
        {
            return (Boolean) isHideAllMethod.invoke(groundItemsPlugin);
        }
        catch (ReflectiveOperationException e)
        {
            return false;
        }
    }

    private GroundItemsPlugin findGroundItemsPlugin()
    {
        for (Plugin plugin : pluginManager.getPlugins())
        {
            if (plugin instanceof GroundItemsPlugin)
            {
                return (GroundItemsPlugin) plugin;
            }
        }

        return null;
    }

    /**
     * @return the item IDs present at {@code worldPoint} in the exact order the core {@code
     *         GroundItemsPlugin} assigns its own text-stacking offsets, or an empty list if that
     *         plugin isn't loaded or has nothing recorded there yet.
     * <p>{@code GroundItemsPlugin} keeps its own {@code Table<WorldPoint, Integer, GroundItem>} of
     * everything it has seen (accumulated from spawn events over time), and stacks its text lines
     * in the iteration order of that table's per-tile row - which has no relationship to the order
     * {@link Tile#getGroundItems()} happens to return in the current frame. Icons drawn using our
     * own scan order therefore don't line up with the text row they belong to whenever a tile holds
     * more than one distinct item.
     * <p>{@code GroundItemsPlugin#getCollectedGroundItems()} is public and returns that same table,
     * but its value type ({@code GroundItem}) is package-private, so it can't be named here. Only
     * the row keys (item IDs, {@link Integer} - a public type) are read, via a raw {@code Table}
     * reference to sidestep that - no reflection needed, since the method and the key type are both
     * accessible even though the value type isn't.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<Integer> orderedItemIdsAt(WorldPoint worldPoint)
    {
        final GroundItemsPlugin groundItemsPlugin = findGroundItemsPlugin();

        if (groundItemsPlugin == null)
        {
            return List.of();
        }

        final Table table = groundItemsPlugin.getCollectedGroundItems();
        final Map row = table.row(worldPoint);

        return new ArrayList<Integer>(row.keySet());
    }

    /**
     * @return {@code groups} reordered so that groups whose representative item ID appears in
     *         {@code referenceOrder} come first, in that same relative order - any group whose item
     *         isn't in {@code referenceOrder} (e.g. {@code GroundItemsPlugin} isn't loaded, or
     *         hasn't recorded that tile yet) keeps its original relative position at the end, so
     *         icons still render instead of silently disappearing.
     */
    static List<GroupedGroundItem> orderToMatch(
        List<GroupedGroundItem> groups,
        List<Integer> referenceOrder)
    {
        final Map<Integer, Integer> priorityByItemId = new HashMap<>();

        for (int i = 0; i < referenceOrder.size(); i++)
        {
            priorityByItemId.putIfAbsent(referenceOrder.get(i), i);
        }

        final List<GroupedGroundItem> sorted = new ArrayList<>(groups);
        sorted.sort(
            Comparator.comparingInt(
                g -> priorityByItemId.getOrDefault(
                    g.getRepresentative().getId(),
                    Integer.MAX_VALUE)));

        return sorted;
    }

    private boolean shouldDisplayItem(TileItem item)
    {
        if (!ownershipMatches(item))
        {
            return false;
        }

        final ItemComposition composition =
            itemManager.getItemComposition(item.getId());

        if (composition == null)
        {
            return false;
        }

        final String name =
            composition.getName();

        final int quantity =
            item.getQuantity();

        final int match =
            listMatch(
                getString(
                    "highlightedItems",
                    ""),
                name,
                quantity);

        final int hiddenMatch =
            listMatch(
                getString(
                    "hiddenItems",
                    "Vial, Ashes, Coins, Bones, Bucket, Jug, Seaweed"),
                name,
                quantity);

        final boolean highlightedByList =
            match != 0;

        final boolean hiddenByList =
            hiddenMatch != 0;

        if (highlightedByList)
        {
            return true;
        }

        if (hiddenByList)
        {
            return false;
        }

        final int realItemId =
            composition.getNote() != -1
                ? composition.getLinkedNoteId()
                : item.getId();

        final int gePrice =
            realItemId == ItemID.COINS
                ? 1
                : itemManager.getItemPrice(
                    realItemId);

        final int haPrice =
            composition.getHaPrice();

        final boolean customColor =
            configManager.getConfiguration(
                GROUND_ITEMS_GROUP,
                ICON_COLOR_PREFIX + item.getId(),
                Color.class) != null;

        final int value =
            getValueByMode(
                gePrice,
                haPrice);

        final boolean implicitlyHighlighted =
            customColor
                || isPriceHighlighted(value);

        if (getBoolean(
                "showHighlightedOnly",
                false)
            && !implicitlyHighlighted)
        {
            return false;
        }

        final boolean dontHideUntradeables =
            getBoolean(
                "dontHideUntradeables",
                true);

        final boolean canBeHidden =
            gePrice > 0
                || composition.isTradeable()
                || !dontHideUntradeables;

        final int hideUnderValue =
            getInt(
                "hideUnderValue",
                0);

        final boolean underGe =
            gePrice < hideUnderValue;

        final boolean underHa =
            haPrice < hideUnderValue;

        final boolean implicitlyHidden =
            canBeHidden
                && underGe
                && underHa;

        return !implicitlyHidden;
    }

    private boolean ownershipMatches(TileItem item)
    {
        final String mode =
            getString(
                "ownershipFilterMode",
                "ALL");

        final int ownership =
            item.getOwnership();

        final int accountType =
            client.getVarbitValue(
                VarbitID.IRONMAN);

        if ("DROPS".equalsIgnoreCase(mode))
        {
            return ownership == TileItem.OWNERSHIP_SELF
                || ownership == TileItem.OWNERSHIP_GROUP;
        }

        if ("TAKEABLE".equalsIgnoreCase(mode))
        {
            return ownership != TileItem.OWNERSHIP_OTHER
                || accountType == 0;
        }

        return true;
    }

    private boolean isPriceHighlighted(int value)
    {
        return isAboveConfiguredThreshold(
                value,
                "lowValuePrice",
                20000)
            || isAboveConfiguredThreshold(
                value,
                "mediumValuePrice",
                100000)
            || isAboveConfiguredThreshold(
                value,
                "highValuePrice",
                1000000)
            || isAboveConfiguredThreshold(
                value,
                "insaneValuePrice",
                10000000);
    }

    private boolean isAboveConfiguredThreshold(
        int value,
        String key,
        int defaultValue)
    {
        final int threshold =
            getInt(
                key,
                defaultValue);

        return threshold > 0
            && value > threshold;
    }

    private int getValueByMode(
        int gePrice,
        int haPrice)
    {
        final String mode =
            getString(
                "highlightValueCalculation",
                "HIGHEST");

        if ("GE".equalsIgnoreCase(mode))
        {
            return gePrice;
        }

        if ("HA".equalsIgnoreCase(mode))
        {
            return haPrice;
        }

        return Math.max(
            gePrice,
            haPrice);
    }

    private String buildItemString(
        TileItem item,
        ItemComposition composition,
        int quantity)
    {
        final StringBuilder builder =
            new StringBuilder(
                composition.getName());

        if (quantity > 1)
        {
            builder.append(" (")
                .append(
                    QuantityFormatter.quantityToStackSize(
                        quantity))
                .append(')');
        }

        if (item.getId() != ItemID.COINS)
        {
            final int realItemId =
                composition.getNote() != -1
                    ? composition.getLinkedNoteId()
                    : item.getId();

            final int gePrice =
                itemManager.getItemPrice(
                    realItemId);

            final int haPrice =
                composition.getHaPrice();

            final String displayMode =
                getString(
                    "priceDisplayMode",
                    "BOTH");

            if ("BOTH".equalsIgnoreCase(
                    displayMode))
            {
                if (gePrice > 0)
                {
                    builder.append(" (GE: ")
                        .append(
                            QuantityFormatter.quantityToStackSize(
                                gePrice))
                        .append(" gp)");
                }

                if (haPrice > 0)
                {
                    builder.append(" (HA: ")
                        .append(
                            QuantityFormatter.quantityToStackSize(
                                haPrice))
                        .append(" gp)");
                }
            }
            else if (!"OFF".equalsIgnoreCase(
                displayMode))
            {
                final int price =
                    "GE".equalsIgnoreCase(
                        displayMode)
                        ? gePrice
                        : haPrice;

                if (price > 0)
                {
                    builder.append(" (")
                        .append(
                            QuantityFormatter.quantityToStackSize(
                                price))
                        .append(" gp)");
                }
            }
        }

        return builder.toString();
    }

    private int listMatch(
        String csv,
        String itemName,
        int quantity)
    {
        final List<String> entries =
            Text.fromCSV(csv);

        for (String entry : entries)
        {
            final ItemRule rule =
                ItemRule.parse(entry);

            if (rule != null
                && !rule.wildcard
                && rule.name.equalsIgnoreCase(
                    itemName)
                && rule.quantityMatches(
                    quantity))
            {
                return 2;
            }
        }

        for (String entry : entries)
        {
            final ItemRule rule =
                ItemRule.parse(entry);

            if (rule != null
                && rule.wildcard
                && WildcardMatcher.matches(
                    rule.name,
                    itemName)
                && rule.quantityMatches(
                    quantity))
            {
                return 1;
            }
        }

        return 0;
    }

    private String getString(
        String key,
        String defaultValue)
    {
        final String value =
            configManager.getConfiguration(
                GROUND_ITEMS_GROUP,
                key,
                String.class);

        return value == null
            ? defaultValue
            : value;
    }

    private boolean getBoolean(
        String key,
        boolean defaultValue)
    {
        final Boolean value =
            configManager.getConfiguration(
                GROUND_ITEMS_GROUP,
                key,
                Boolean.class);

        return value == null
            ? defaultValue
            : value;
    }

    private int getInt(
        String key,
        int defaultValue)
    {
        final Integer value =
            configManager.getConfiguration(
                GROUND_ITEMS_GROUP,
                key,
                Integer.class);

        return value == null
            ? defaultValue
            : value;
    }

    private static final class ItemRule
    {
        private final String name;
        private final int quantity;
        private final boolean lessThan;
        private final boolean wildcard;

        private ItemRule(
            String name,
            int quantity,
            boolean lessThan,
            boolean wildcard)
        {
            this.name = name;
            this.quantity = quantity;
            this.lessThan = lessThan;
            this.wildcard = wildcard;
        }

        private static ItemRule parse(
            String entry)
        {
            if (entry == null
                || entry.trim().isEmpty())
            {
                return null;
            }

            String value =
                entry.trim();

            int quantity = 0;
            boolean lessThan = false;
            boolean wildcard =
                value.contains("*");

            for (int i =
                value.length() - 1;
                i >= 0;
                i--)
            {
                char c =
                    value.charAt(i);

                if ((c >= '0'
                    && c <= '9')
                    || Character.isWhitespace(c))
                {
                    continue;
                }

                if (c == '<'
                    || c == '>')
                {
                    if (i + 1 <
                        value.length())
                    {
                        try
                        {
                            quantity =
                                Integer.parseInt(
                                    value.substring(
                                        i + 1)
                                        .trim());
                        }
                        catch (NumberFormatException e)
                        {
                            quantity = 0;
                            lessThan = false;
                        }

                        lessThan =
                            c == '<';

                        value =
                            value.substring(
                                0,
                                i);
                    }
                }

                break;
            }

            return new ItemRule(
                value.trim(),
                quantity,
                lessThan,
                wildcard);
        }

        private boolean quantityMatches(
            int itemCount)
        {
            return lessThan
                ? itemCount < quantity
                : itemCount > quantity;
        }
    }
}