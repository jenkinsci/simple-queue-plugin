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
}
