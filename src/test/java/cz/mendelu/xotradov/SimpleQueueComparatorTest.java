package cz.mendelu.xotradov;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import hudson.model.Queue;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class SimpleQueueComparatorTest {

    SimpleQueueComparator comparator = SimpleQueueComparator.getInstance();

    @Test
    public void getInstance() {
        assertNotNull(SimpleQueueComparator.getInstance());
    }

    @Test
    public void compareBuildableItems() {
        Queue.BuildableItem a = Mockito.mock(Queue.BuildableItem.class);
        when(a.getId()).thenReturn(1L);
        Queue.BuildableItem b = Mockito.mock(Queue.BuildableItem.class);
        when(b.getId()).thenReturn(2L);
        assertEquals(0, comparator.compare(a, b)); // no desires
        comparator.addDesire(2, 1);
        assertEquals(1, comparator.compare(a, b)); // second is more important
        comparator.addDesire(1, 2);
        assertEquals(-1, comparator.compare(a, b)); // first is more important
    }

    @Test
    public void compareBlockedItems() {
        Queue.BlockedItem a = Mockito.mock(Queue.BlockedItem.class);
        when(a.getId()).thenReturn(10L);
        Queue.BlockedItem b = Mockito.mock(Queue.BlockedItem.class);
        when(b.getId()).thenReturn(20L);
        assertEquals(0, comparator.compare(a, b)); // no desires
        comparator.addDesire(20, 10);
        assertEquals(1, comparator.compare(a, b)); // second is more important
        comparator.addDesire(10, 20);
        assertEquals(-1, comparator.compare(a, b)); // first is more important
    }

    @Test
    public void compareMixedTypes() {
        Queue.BuildableItem buildable = Mockito.mock(Queue.BuildableItem.class);
        when(buildable.getId()).thenReturn(30L);
        Queue.BlockedItem blocked = Mockito.mock(Queue.BlockedItem.class);
        when(blocked.getId()).thenReturn(40L);
        assertEquals(0, comparator.compare(buildable, blocked)); // no desires
        comparator.addDesire(40, 30);
        assertEquals(1, comparator.compare(buildable, blocked)); // blocked is more important
        comparator.addDesire(30, 40);
        assertEquals(-1, comparator.compare(buildable, blocked)); // buildable is more important
    }

    @Test
    public void addDesire() {
        assertFalse(comparator.hasDesiresFor(1));
        comparator.addDesire(1, 2);
        assertTrue(comparator.hasDesiresFor(1));
        comparator.resetDesires();
    }

    @Test
    public void removeDesireOfKey() {
        comparator.addDesire(1, 2);
        comparator.removeDesireOfKey(1);
        assertFalse(comparator.hasDesiresFor(1));
    }

    @Test
    public void resetDesires() {
        comparator.addDesire(1, 5);
        comparator.addDesire(2, 6);
        comparator.resetDesires();
        assertFalse(comparator.hasDesiresFor(1));
        assertFalse(comparator.hasDesiresFor(2));
    }

    @Test
    public void removeStaleReferences_removesFromValueLists() {
        comparator.resetDesires();
        // Set up: item 10 should precede items 20 and 30
        //          item 40 should precede item 20
        // So desires are: 10 -> [20, 30], 40 -> [20]
        comparator.addDesire(10, 20);
        comparator.addDesire(10, 30);
        comparator.addDesire(40, 20);
        assertTrue(comparator.hasDesiresFor(10));
        assertTrue(comparator.hasDesiresFor(40));

        // Simulate item 20 leaving the queue
        comparator.removeStaleReferences(20);

        // Item 10's desire list should no longer contain 20, but still contain 30
        assertTrue(comparator.hasDesiresFor(10));
        assertEquals(0, comparator.compare(Mockito.mock(Queue.BuildableItem.class), Mockito.mock(Queue.BuildableItem.class)));
        // Verify via snapshot: 10 should only reference 30
        var snapshot = comparator.snapshotDesires();
        assertTrue(snapshot.containsKey(10L));
        assertEquals(1, snapshot.get(10L).size());
        assertTrue(snapshot.get(10L).contains(30L));
        assertFalse(snapshot.get(10L).contains(20L));

        // Item 40's desire list was [20], now empty -> key removed entirely
        assertFalse(comparator.hasDesiresFor(40));
        comparator.resetDesires();
    }

    @Test
    public void removeStaleReferences_removesStaleKeyReferences() {
        comparator.resetDesires();
        // Simulate: item 5 should precede items 1, 2, 3
        comparator.addDesire(5, 1);
        comparator.addDesire(5, 2);
        comparator.addDesire(5, 3);

        // Items 1 and 3 leave the queue
        comparator.removeStaleReferences(1);
        comparator.removeStaleReferences(3);

        // Only reference to item 2 should remain
        var snapshot = comparator.snapshotDesires();
        assertTrue(snapshot.containsKey(5L));
        assertEquals(1, snapshot.get(5L).size());
        assertTrue(snapshot.get(5L).contains(2L));
        comparator.resetDesires();
    }
}
