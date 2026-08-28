package com.meowcape.grounditemicons;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

import net.runelite.api.Client;
import net.runelite.api.Constants;
import net.runelite.api.ItemComposition;
import net.runelite.api.Perspective;
import net.runelite.api.Scene;
import net.runelite.api.SpritePixels;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.ItemQuantityMode;
import net.runelite.client.config.ConfigManager;
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
    private static final int RUNE_ICON_SIZE_REDUCTION = 2;
    private static final int MAX_SPRITE_CACHE_SIZE = 256;

    private final Client client;
    private final ItemManager itemManager;
    private final GroundItemIconsConfig config;
    private final ConfigManager configManager;
    private final GroundItemIconsState state;

    private final Map<SpriteKey, BufferedImage> spriteCache =
        new HashMap<>();

    @Inject
    private GroundItemIconsOverlay(
        Client client,
        ItemManager itemManager,
        GroundItemIconsConfig config,
        ConfigManager configManager,
        GroundItemIconsState state)
    {
        this.client = client;
        this.itemManager = itemManager;
        this.config = config;
        this.configManager = configManager;
        this.state = state;

        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (state.isHidden())
        {
            return null;
        }

        final net.runelite.api.Player player =
            client.getLocalPlayer();

        if (player == null)
        {
            return null;
        }

        final WorldView worldView =
            client.getTopLevelWorldView();

        if (worldView == null)
        {
            return null;
        }

        final Scene scene =
            worldView.getScene();

        if (scene == null)
        {
            return null;
        }

        final Tile[][][] tiles =
            scene.getTiles();

        if (tiles == null)
        {
            return null;
        }

        final int configuredIconSize =
            getConfiguredIconSize(
                graphics);

        final int iconGap =
            config.iconGap();

        final float opacity =
            Math.max(
                0,
                Math.min(
                    config.iconOpacity(),
                    100))
                / 100.0f;

        final Composite originalComposite =
            graphics.getComposite();

        if (opacity < 1.0f)
        {
            graphics.setComposite(
                AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER,
                    opacity));
        }

        final Map<WorldPoint, Map<Integer, Integer>> offsetMap =
            new HashMap<>();

        final Map<WorldPoint, Integer> locationOffsets =
            new HashMap<>();

        for (GroundItemIconsEntry entry :
            state.getAllItems())
        {
            final TileItem item =
                entry.getItem();

            final int quantity =
                entry.getQuantity();

            final WorldPoint worldPoint =
                entry.getWorldPoint();

            if (!groundItemsWouldDisplay(
                item,
                quantity))
            {
                if (!config.showHiddenItems()
                    || !isGroundItemsHidden(
                        item,
                        quantity))
                {
                    continue;
                }
            }

            final int offset =
                locationOffsets.compute(
                    worldPoint,
                    (k, v) ->
                        v == null
                            ? 0
                            : v + 1);

            offsetMap
                .computeIfAbsent(
                    worldPoint,
                    k -> new HashMap<>())
                .put(
                    entry.getItemId(),
                    offset);
        }

        for (int plane = 0;
             plane < tiles.length;
             plane++)
        {
            final Tile[][] planeTiles =
                tiles[plane];

            if (planeTiles == null)
            {
                continue;
            }

            for (int x = 0;
                 x < planeTiles.length;
                 x++)
            {
                final Tile[] row =
                    planeTiles[x];

                if (row == null)
                {
                    continue;
                }

                for (int y = 0;
                     y < row.length;
                     y++)
                {
                    final Tile tile =
                        row[y];

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
                            .distanceTo(
                                groundPoint)
                            > MAX_DISTANCE)
                    {
                        continue;
                    }

                    final Map<Integer, Integer> itemOffsets =
                        offsetMap.get(
                            worldPoint);

                    if (itemOffsets == null
                        || itemOffsets.isEmpty())
                    {
                        continue;
                    }

                    final Collection<GroundItemIconsEntry> entries =
                        state.getItems(
                            worldPoint);

                    for (GroundItemIconsEntry entry :
                        entries)
                    {
                        final int itemId =
                            entry.getItemId();

                        final TileItem item =
                            entry.getItem();

                        final int quantity =
                            entry.getQuantity();

                        if (!shouldDisplayIcon(
                            item,
                            quantity))
                        {
                            continue;
                        }

                        final Integer offset =
                            itemOffsets.get(
                                itemId);

                        if (offset == null)
                        {
                            continue;
                        }

                        final ItemComposition itemComposition =
                            itemManager.getItemComposition(
                                itemId);

                        if (itemComposition == null)
                        {
                            continue;
                        }

                        final String itemString =
                            buildItemString(
                                item,
                                itemComposition,
                                quantity);

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
                            getItemSprite(
                                itemId,
                                quantity);

                        if (image == null)
                        {
                            continue;
                        }

                        final int iconSize =
                            getIconSize(
                                itemComposition,
                                configuredIconSize);

                        final int textX =
                            textPoint.getX();

                        final int textY =
                            textPoint.getY()
                                - STRING_GAP * offset;

                        final int iconCenterX;

                        if (config.iconSide()
                            == IconSide.RIGHT)
                        {
                            iconCenterX =
                                textX
                                    + graphics.getFontMetrics()
                                        .stringWidth(
                                            itemString)
                                    + iconGap
                                    + configuredIconSize / 2;
                        }
                        else
                        {
                            iconCenterX =
                                textX
                                    - iconGap
                                    - configuredIconSize / 2;
                        }

                        final int textCenterY =
                            textY
                                - (
                                    graphics.getFontMetrics()
                                        .getAscent()
                                    + graphics.getFontMetrics()
                                        .getDescent()
                                ) / 2;

                        final int iconX =
                            iconCenterX
                                - iconSize / 2;

                        final int iconY =
                            textCenterY
                                - iconSize / 2;

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

        graphics.setComposite(
            originalComposite);

        return null;
    }

    private BufferedImage getItemSprite(
        int itemId,
        int quantity)
    {
        final ItemComposition composition =
            itemManager.getItemComposition(
                itemId);

        if (composition == null)
        {
            return null;
        }

        final SpriteKey key =
            new SpriteKey(
                itemId,
                quantity);

        BufferedImage cached =
            spriteCache.get(
                key);

        if (cached != null)
        {
            return cached;
        }

        final SpritePixels sprite =
            client.createItemSprite(
                itemId,
                quantity,
                1,
                SpritePixels.DEFAULT_SHADOW_COLOR,
                ItemQuantityMode.NEVER,
                false,
                Constants.CLIENT_DEFAULT_ZOOM);

        if (sprite == null)
        {
            return null;
        }

        final BufferedImage image =
            new BufferedImage(
                Constants.ITEM_SPRITE_WIDTH,
                Constants.ITEM_SPRITE_HEIGHT,
                BufferedImage.TYPE_INT_ARGB);

        sprite.toBufferedImage(
            image);

        if (spriteCache.size()
            >= MAX_SPRITE_CACHE_SIZE)
        {
            spriteCache.clear();
        }

        spriteCache.put(
            key,
            image);

        return image;
    }

    private int getConfiguredIconSize(
        Graphics2D graphics)
    {
        if (config.scaleWithText())
        {
            return Math.max(
                1,
                graphics.getFontMetrics()
                    .getHeight());
        }

        return Math.max(
            8,
            Math.min(
                config.iconSize(),
                64));
    }

    private int getIconSize(
        ItemComposition composition,
        int configuredIconSize)
    {
        if (isRune(
            composition))
        {
            return Math.max(
                1,
                configuredIconSize
                    - RUNE_ICON_SIZE_REDUCTION);
        }

        return configuredIconSize;
    }

    private boolean isRune(
        ItemComposition composition)
    {
        final String name =
            composition.getName();

        return name != null
            && name.toLowerCase()
                .endsWith(" rune");
    }

    private boolean groundItemsWouldDisplay(
        TileItem item,
        int quantity)
    {
        if (!ownershipMatches(item))
        {
            return false;
        }

        final ItemComposition composition =
            itemManager.getItemComposition(
                item.getId());

        if (composition == null)
        {
            return false;
        }

        final int hiddenOrHighlighted =
            getHiddenOrHighlighted(
                item,
                composition,
                quantity);

        if (hiddenOrHighlighted == HIGHLIGHTED)
        {
            return true;
        }

        if (hiddenOrHighlighted == HIDDEN)
        {
            return false;
        }

        if (groundItemsShowHighlightedOnly())
        {
            return isImplicitlyHighlighted(
                item,
                composition,
                quantity);
        }

        return !isImplicitlyHidden(
            item,
            quantity);
    }

    private boolean shouldDisplayIcon(
        TileItem item,
        int quantity)
    {
        if (!ownershipMatches(item))
        {
            return false;
        }

        final ItemComposition composition =
            itemManager.getItemComposition(
                item.getId());

        if (composition == null)
        {
            return false;
        }

        final int hiddenOrHighlighted =
            getHiddenOrHighlighted(
                item,
                composition,
                quantity);

        if (hiddenOrHighlighted == HIGHLIGHTED)
        {
            return true;
        }

        if (hiddenOrHighlighted == HIDDEN)
        {
            return config.showHiddenItems();
        }

        if (!groundItemsWouldDisplay(
            item,
            quantity))
        {
            return false;
        }

        if (config.showHighlightedOnly()
            && !isImplicitlyHighlighted(
                item,
                composition,
                quantity))
        {
            return false;
        }

        return true;
    }

    private boolean isGroundItemsHidden(
        TileItem item,
        int quantity)
    {
        if (!ownershipMatches(item))
        {
            return false;
        }

        final ItemComposition composition =
            itemManager.getItemComposition(
                item.getId());

        if (composition == null)
        {
            return false;
        }

        final int hiddenOrHighlighted =
            getHiddenOrHighlighted(
                item,
                composition,
                quantity);

        if (hiddenOrHighlighted == HIGHLIGHTED)
        {
            return false;
        }

        if (hiddenOrHighlighted == HIDDEN)
        {
            return true;
        }

        return isImplicitlyHidden(
            item,
            quantity);
    }

    private int getHiddenOrHighlighted(
        TileItem item,
        ItemComposition composition,
        int quantity)
    {
        final String name =
            composition.getName();

        final int highlightedMatch =
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

        if (highlightedMatch == 2)
        {
            return HIGHLIGHTED;
        }

        if (hiddenMatch == 2)
        {
            return HIDDEN;
        }

        if (highlightedMatch == 1)
        {
            return HIGHLIGHTED;
        }

        if (hiddenMatch == 1)
        {
            return HIDDEN;
        }

        return NONE;
    }

    private boolean groundItemsShowHighlightedOnly()
    {
        final Boolean value =
            configManager.getConfiguration(
                GROUND_ITEMS_GROUP,
                "showHighlightedOnly",
                Boolean.class);

        return value != null
            && value;
    }

    private boolean isImplicitlyHighlighted(
        TileItem item,
        ItemComposition composition,
        int quantity)
    {
        final int realItemId =
            composition.getNote() != -1
                ? composition.getLinkedNoteId()
                : item.getId();

        final int gePrice =
            realItemId == ItemID.COINS
                ? quantity
                : itemManager.getItemPrice(
                    realItemId)
                    * quantity;

        final int haPrice =
            composition.getHaPrice()
                * quantity;

        final boolean customColor =
            configManager.getConfiguration(
                GROUND_ITEMS_GROUP,
                ICON_COLOR_PREFIX + item.getId(),
                Color.class) != null;

        final int value =
            getValueByMode(
                gePrice,
                haPrice);

        return customColor
            || isPriceHighlighted(value);
    }

    private boolean isImplicitlyHidden(
        TileItem item,
        int quantity)
    {
        final ItemComposition composition =
            itemManager.getItemComposition(
                item.getId());

        if (composition == null)
        {
            return false;
        }

        final int realItemId =
            composition.getNote() != -1
                ? composition.getLinkedNoteId()
                : item.getId();

        final int gePrice =
            realItemId == ItemID.COINS
                ? quantity
                : itemManager.getItemPrice(
                    realItemId)
                    * quantity;

        final int haPrice =
            composition.getHaPrice()
                * quantity;

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
            gePrice
                < hideUnderValue;

        final boolean underHa =
            haPrice
                < hideUnderValue;

        return canBeHidden
            && underGe
            && underHa;
    }

    private boolean ownershipMatches(
        TileItem item)
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

        if ("DROPS".equalsIgnoreCase(
            mode))
        {
            return ownership
                == TileItem.OWNERSHIP_SELF
                || ownership
                == TileItem.OWNERSHIP_GROUP;
        }

        if ("TAKEABLE".equalsIgnoreCase(
            mode))
        {
            return ownership
                != TileItem.OWNERSHIP_OTHER
                || accountType == 0;
        }

        return true;
    }

    private boolean isPriceHighlighted(
        int value)
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

        if ("GE".equalsIgnoreCase(
            mode))
        {
            return gePrice;
        }

        if ("HA".equalsIgnoreCase(
            mode))
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
                    realItemId)
                    * quantity;

            final int haPrice =
                composition.getHaPrice()
                    * quantity;

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

    private static final int NONE = 0;
    private static final int HIGHLIGHTED = 1;
    private static final int HIDDEN = 2;

    private static final class SpriteKey
    {
        private final int itemId;
        private final int quantity;

        private SpriteKey(
            int itemId,
            int quantity)
        {
            this.itemId = itemId;
            this.quantity = quantity;
        }

        @Override
        public boolean equals(
            Object object)
        {
            if (this == object)
            {
                return true;
            }

            if (!(object instanceof SpriteKey))
            {
                return false;
            }

            final SpriteKey other =
                (SpriteKey) object;

            return itemId == other.itemId
                && quantity == other.quantity;
        }

        @Override
        public int hashCode()
        {
            int result =
                Integer.hashCode(
                    itemId);

            result =
                31 * result
                    + Integer.hashCode(
                        quantity);

            return result;
        }
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
            return quantity == 0
                || (lessThan
                    ? itemCount < quantity
                    : itemCount > quantity);
        }
    }
}