package com.meowcape.grounditemicons;

import java.util.List;

import net.runelite.api.Model;
import net.runelite.api.Node;
import net.runelite.api.TileItem;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GroundItemIconsOverlayTest
{
	@Test
	public void orderToMatchReordersGroupsToMatchReferenceOrder()
	{
		// Reproduces the reported bug: icons drew in our own tile-scan order, which doesn't match
		// the order GroundItemsPlugin stacks its text lines in (its own internal collected-items
		// table, populated over time from spawn events) - so on a tile with several distinct items,
		// an icon could end up next to the wrong item's text row. Here groups start in a different
		// order than the "reference" (GroundItemsPlugin's row) order, and must come out matching it.
		List<GroundItemIconsOverlay.GroupedGroundItem> groups = List.of(
			group(995, 100),   // coins
			group(526, 1),     // bones
			group(1931, 1));   // logs
		List<Integer> referenceOrder = List.of(526, 1931, 995);

		List<GroundItemIconsOverlay.GroupedGroundItem> ordered =
			GroundItemIconsOverlay.orderToMatch(groups, referenceOrder);

		assertEquals(
			List.of(526, 1931, 995),
			ordered.stream().map(g -> g.getRepresentative().getId()).collect(java.util.stream.Collectors.toList()));
	}

	@Test
	public void orderToMatchAppendsUnmatchedGroupsAtTheEndPreservingOrder()
	{
		// If GroundItemsPlugin isn't loaded, or hasn't recorded a given tile yet, some (or all) of
		// our groups won't appear in the reference order - they must still render, just falling back
		// to the end in their original relative order, rather than being dropped.
		List<GroundItemIconsOverlay.GroupedGroundItem> groups = List.of(
			group(995, 100),
			group(526, 1),
			group(1931, 1));
		List<Integer> referenceOrder = List.of(1931); // only "logs" is known

		List<GroundItemIconsOverlay.GroupedGroundItem> ordered =
			GroundItemIconsOverlay.orderToMatch(groups, referenceOrder);

		assertEquals(
			List.of(1931, 995, 526),
			ordered.stream().map(g -> g.getRepresentative().getId()).collect(java.util.stream.Collectors.toList()));
	}

	@Test
	public void orderToMatchWithEmptyReferenceOrderKeepsOriginalOrder()
	{
		List<GroundItemIconsOverlay.GroupedGroundItem> groups = List.of(
			group(995, 100),
			group(526, 1));

		List<GroundItemIconsOverlay.GroupedGroundItem> ordered =
			GroundItemIconsOverlay.orderToMatch(groups, List.of());

		assertEquals(
			List.of(995, 526),
			ordered.stream().map(g -> g.getRepresentative().getId()).collect(java.util.stream.Collectors.toList()));
	}

	private static GroundItemIconsOverlay.GroupedGroundItem group(int itemId, int quantity)
	{
		return new GroundItemIconsOverlay.GroupedGroundItem(new FakeTileItem(itemId, quantity), quantity);
	}

	@Test
	public void groupsMultipleTileItemsWithSameIdIntoOneEntry()
	{
		// Reproduces the reported bug: dropping a stack of 9 unstackable items (e.g. burnt food)
		// spawns 9 separate TileItem entries at the same tile, each with quantity 1 - the game's
		// own client still renders this as a single pile labelled "(9)". Before this fix, the
		// overlay drew one icon per raw TileItem, producing 9 overlapping icons instead of 1.
		List<TileItem> items = List.of(
			new FakeTileItem(7946, 1),
			new FakeTileItem(7946, 1),
			new FakeTileItem(7946, 1),
			new FakeTileItem(7946, 1),
			new FakeTileItem(7946, 1),
			new FakeTileItem(7946, 1),
			new FakeTileItem(7946, 1),
			new FakeTileItem(7946, 1),
			new FakeTileItem(7946, 1));

		List<GroundItemIconsOverlay.GroupedGroundItem> grouped =
			GroundItemIconsOverlay.groupByItemId(items);

		assertEquals("expected exactly one icon for the whole pile", 1, grouped.size());
		assertEquals(7946, grouped.get(0).getRepresentative().getId());
		assertEquals(9, grouped.get(0).getTotalQuantity());
	}

	@Test
	public void sumsQuantitiesWhenIndividualEntriesAreAlreadyStacked()
	{
		// A single TileItem can itself carry a quantity > 1 (stackable items, e.g. coins) - if
		// multiple such stacks of the same item exist on one tile, their quantities must still add
		// up rather than only counting entries.
		List<TileItem> items = List.of(
			new FakeTileItem(995, 1000),
			new FakeTileItem(995, 500));

		List<GroundItemIconsOverlay.GroupedGroundItem> grouped =
			GroundItemIconsOverlay.groupByItemId(items);

		assertEquals(1, grouped.size());
		assertEquals(1500, grouped.get(0).getTotalQuantity());
	}

	@Test
	public void keepsDifferentItemsAsSeparateGroups()
	{
		// Different item IDs on the same tile (e.g. bones next to burnt food) must still produce
		// one icon each - grouping must not merge across different items.
		List<TileItem> items = List.of(
			new FakeTileItem(526, 1),
			new FakeTileItem(7946, 9),
			new FakeTileItem(995, 42));

		List<GroundItemIconsOverlay.GroupedGroundItem> grouped =
			GroundItemIconsOverlay.groupByItemId(items);

		assertEquals(3, grouped.size());
		assertTrue(grouped.stream().anyMatch(g -> g.getRepresentative().getId() == 526 && g.getTotalQuantity() == 1));
		assertTrue(grouped.stream().anyMatch(g -> g.getRepresentative().getId() == 7946 && g.getTotalQuantity() == 9));
		assertTrue(grouped.stream().anyMatch(g -> g.getRepresentative().getId() == 995 && g.getTotalQuantity() == 42));
	}

	@Test
	public void returnsEmptyListForNoItems()
	{
		List<GroundItemIconsOverlay.GroupedGroundItem> grouped =
			GroundItemIconsOverlay.groupByItemId(List.of());

		assertTrue(grouped.isEmpty());
	}

	@Test
	public void keepsFirstEntryAsRepresentativeForLookups()
	{
		// The representative is only ever used for id-based lookups (icon image, price), which are
		// identical across every entry sharing the id - but pin down that it's specifically the
		// first entry encountered, not e.g. the last, so behavior stays predictable.
		FakeTileItem first = new FakeTileItem(7946, 1);
		FakeTileItem second = new FakeTileItem(7946, 1);

		List<GroundItemIconsOverlay.GroupedGroundItem> grouped =
			GroundItemIconsOverlay.groupByItemId(List.of(first, second));

		assertEquals(1, grouped.size());
		assertTrue(grouped.get(0).getRepresentative() == first);
	}

	/**
	 * Minimal {@link TileItem} test double - only {@link #getId()} and {@link #getQuantity()} are
	 * exercised by {@link GroundItemIconsOverlay#groupByItemId}; everything else throws, so a test
	 * relying on unintended behavior fails loudly instead of silently returning a meaningless value.
	 */
	private static final class FakeTileItem implements TileItem
	{
		private final int id;
		private final int quantity;

		private FakeTileItem(int id, int quantity)
		{
			this.id = id;
			this.quantity = quantity;
		}

		@Override
		public int getId()
		{
			return id;
		}

		@Override
		public int getQuantity()
		{
			return quantity;
		}

		@Override
		public int getVisibleTime()
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public int getDespawnTime()
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public int getOwnership()
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean isPrivate()
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public Model getModel()
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public int getModelHeight()
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public void setModelHeight(int modelHeight)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public int getAnimationHeightOffset()
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public int getRenderMode()
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public Node getNext()
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public Node getPrevious()
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public long getHash()
		{
			throw new UnsupportedOperationException();
		}
	}
}
