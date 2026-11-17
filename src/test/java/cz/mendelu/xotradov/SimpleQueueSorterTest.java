package cz.mendelu.xotradov;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import hudson.model.Queue;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class SimpleQueueSorterTest {

    SimpleQueueSorter sorter = new SimpleQueueSorter(new DefaultSorter());

    @BeforeEach
    @AfterEach
    void resetDesires() {
        sorter.getSimpleQueueComparator().resetDesires();
    }

    private Queue.BuildableItem item(long id) {
        return item(id, id * 10);
    }

    private Queue.BuildableItem item(long id, long buildableStart) {
        return item(Queue.BuildableItem.class, id, buildableStart);
    }

    private Queue.BlockedItem blocked(long id) {
        return item(Queue.BlockedItem.class, id, id * 10);
    }

    private <T extends Queue.NotWaitingItem> T item(Class<T> kind, long id, long buildableStart) {
        T item = Mockito.mock(kind);
        when(item.getId()).thenReturn(id);
        when(item.getInQueueSince()).thenReturn(buildableStart);
        try {
            // Jenkins' buildable sorter reads this final field directly.
            Field started = Queue.NotWaitingItem.class.getField("buildableStartMilliseconds");
            started.setAccessible(true);
            started.setLong(item, buildableStart);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
        return item;
    }

    @Test
    void preservesNonAdjacentManualPrecedenceThroughSortsAndArrivals() {
        Queue.BuildableItem a = item(1);
        Queue.BuildableItem b = item(2);
        Queue.BuildableItem c = item(3);
        Queue.BuildableItem d = item(4);
        Queue.BuildableItem e = item(5);
        List<Queue.BuildableItem> list = new ArrayList<>(List.of(a, b, c, d, e));
        sorter.getSimpleQueueComparator().addDesire(e.getId(), b.getId());
        sorter.getSimpleQueueComparator().addDesire(e.getId(), d.getId());

        sorter.sortBuildableItems(list);
        assertEquals(List.of(a, c, e, b, d), list);
        sorter.sortBuildableItems(list);
        assertEquals(List.of(a, c, e, b, d), list);

        Queue.BuildableItem arrival = item(6);
        list.add(arrival);
        sorter.sortBuildableItems(list);
        assertEquals(List.of(a, c, e, b, d, arrival), list);
    }

    @Test
    void followsTransitiveDesires() {
        Queue.BuildableItem a = item(1);
        Queue.BuildableItem b = item(2);
        Queue.BuildableItem c = item(3);
        List<Queue.BuildableItem> list = new ArrayList<>(List.of(a, b, c));
        sorter.getSimpleQueueComparator().addDesire(c.getId(), a.getId());
        sorter.getSimpleQueueComparator().addDesire(b.getId(), c.getId());
        sorter.sortBuildableItems(list);
        assertEquals(List.of(b, c, a), list);
    }

    @Test
    void ignoresDesiresForItemsOutsideTheBuildableList() {
        Queue.BuildableItem a = item(1);
        Queue.BuildableItem b = item(2);
        List<Queue.BuildableItem> list = new ArrayList<>(List.of(a, b));
        sorter.getSimpleQueueComparator().addDesire(99, a.getId());
        sorter.getSimpleQueueComparator().addDesire(b.getId(), 99);
        sorter.getSimpleQueueComparator().addDesire(b.getId(), b.getId());
        sorter.sortBuildableItems(list);
        assertEquals(List.of(a, b), list);
    }

    @Test
    void breaksCyclesInAutomaticOrderWithoutLosingItems() {
        Queue.BuildableItem a = item(1);
        Queue.BuildableItem b = item(2);
        Queue.BuildableItem c = item(3);
        List<Queue.BuildableItem> list = new ArrayList<>(List.of(a, b, c));
        sorter.getSimpleQueueComparator().addDesire(a.getId(), b.getId());
        sorter.getSimpleQueueComparator().addDesire(b.getId(), c.getId());
        sorter.getSimpleQueueComparator().addDesire(c.getId(), a.getId());
        sorter.sortBuildableItems(list);
        assertEquals(List.of(a, b, c), list);
    }

    @Test
    void keepsDownstreamItemsBehindTheirCyclicPredecessor() {
        Queue.BuildableItem a = item(1);
        Queue.BuildableItem b = item(2);
        Queue.BuildableItem c = item(3);
        Queue.BuildableItem d = item(4, 0);
        List<Queue.BuildableItem> list = new ArrayList<>(List.of(d, a, b, c));
        sorter.getSimpleQueueComparator().addDesire(a.getId(), b.getId());
        sorter.getSimpleQueueComparator().addDesire(b.getId(), c.getId());
        sorter.getSimpleQueueComparator().addDesire(c.getId(), a.getId());
        sorter.getSimpleQueueComparator().addDesire(c.getId(), d.getId());

        sorter.sortBuildableItems(list);

        assertEquals(List.of(a, b, c, d), list);
    }

    @Test
    void choosesEarliestUnblockedItemInAutomaticOrder() {
        Queue.BuildableItem a = item(1);
        Queue.BuildableItem b = item(2);
        Queue.BuildableItem c = item(3);
        List<Queue.BuildableItem> list = new ArrayList<>(List.of(c, b, a));
        sorter.getSimpleQueueComparator().addDesire(c.getId(), a.getId());

        sorter.sortBuildableItems(list);

        // The unconstrained item b keeps its automatic priority over c.
        assertEquals(List.of(b, c, a), list);
    }

    @Test
    void restoresAutomaticOrderWhenThereAreNoDesires() {
        Queue.BuildableItem a = item(1);
        Queue.BuildableItem b = item(2);
        Queue.BuildableItem c = item(3);
        List<Queue.BuildableItem> list = new ArrayList<>(List.of(c, a, b));

        sorter.sortBuildableItems(list);

        assertEquals(List.of(a, b, c), list);
    }

    @Test
    void preservesPreviousSortersPriorityAmongUnblockedItems() {
        Queue.BuildableItem a = item(1);
        Queue.BuildableItem b = item(2);
        Queue.BuildableItem c = item(3);
        Queue.BuildableItem d = item(4);
        Queue.BuildableItem e = item(5);
        List<Queue.BuildableItem> list = new ArrayList<>(List.of(a, b, c, d, e));
        SimpleQueueSorter composed = new SimpleQueueSorter(new hudson.model.queue.QueueSorter() {
            @Override
            public void sortBuildableItems(List<Queue.BuildableItem> items) {
                items.sort((first, second) -> Long.compare(second.getId(), first.getId()));
            }
        });

        composed.sortBuildableItems(list);
        assertEquals(List.of(e, d, c, b, a), list);

        composed.getSimpleQueueComparator().addDesire(a.getId(), c.getId());
        composed.getSimpleQueueComparator().addDesire(a.getId(), e.getId());
        composed.sortBuildableItems(list);

        assertEquals(List.of(d, b, a, e, c), list);
    }

    @Test
    void leavesInputIntactIfOriginalSorterFails() {
        List<Queue.BuildableItem> list = new ArrayList<>(List.of(item(1), item(2)));
        List<Queue.BuildableItem> original = new ArrayList<>(list);
        SimpleQueueSorter failing = new SimpleQueueSorter(new hudson.model.queue.QueueSorter() {
            @Override
            public void sortBuildableItems(List<Queue.BuildableItem> items) {
                items.clear();
                throw new IllegalStateException("sort failed");
            }
        });
        assertThrows(IllegalStateException.class, () -> failing.sortBuildableItems(list));
        assertEquals(original, list);
    }

    @Test
    void preservesBlockedPrecedenceThroughSortsAndArrivals() {
        Queue.BlockedItem a = blocked(1);
        Queue.BlockedItem b = blocked(2);
        Queue.BlockedItem c = blocked(3);
        Queue.BlockedItem d = blocked(4);
        Queue.BlockedItem e = blocked(5);
        List<Queue.BlockedItem> list = new ArrayList<>(List.of(a, b, c, d, e));
        sorter.getSimpleQueueComparator().addDesire(e.getId(), b.getId());
        sorter.getSimpleQueueComparator().addDesire(e.getId(), d.getId());

        sorter.sortBlockedItems(list);
        assertEquals(List.of(a, c, e, b, d), list);
        sorter.sortBlockedItems(list);
        assertEquals(List.of(a, c, e, b, d), list);

        Queue.BlockedItem arrival = blocked(6);
        list.add(arrival);
        sorter.sortBlockedItems(list);
        assertEquals(List.of(a, c, e, b, d, arrival), list);
    }

    @Test
    void keepsBlockedItemsBehindTheirCyclicPredecessor() {
        Queue.BlockedItem a = blocked(1);
        Queue.BlockedItem b = blocked(2);
        Queue.BlockedItem c = blocked(3);
        Queue.BlockedItem d = item(Queue.BlockedItem.class, 4, 0);
        List<Queue.BlockedItem> list = new ArrayList<>(List.of(d, a, b, c));
        sorter.getSimpleQueueComparator().addDesire(a.getId(), b.getId());
        sorter.getSimpleQueueComparator().addDesire(b.getId(), c.getId());
        sorter.getSimpleQueueComparator().addDesire(c.getId(), a.getId());
        sorter.getSimpleQueueComparator().addDesire(c.getId(), d.getId());

        sorter.sortBlockedItems(list);

        assertEquals(List.of(a, b, c, d), list);
    }

    @Test
    void preservesMixedPrecedenceThroughSortsAndArrivals() {
        Queue.NotWaitingItem a = blocked(1);
        Queue.NotWaitingItem b = item(2);
        Queue.NotWaitingItem c = blocked(3);
        Queue.NotWaitingItem d = item(4);
        Queue.NotWaitingItem e = blocked(5);
        List<Queue.NotWaitingItem> list = new ArrayList<>(List.of(a, b, c, d, e));
        sorter.getSimpleQueueComparator().addDesire(e.getId(), b.getId());
        sorter.getSimpleQueueComparator().addDesire(e.getId(), d.getId());

        sorter.sortNotWaitingItems(list);
        assertEquals(List.of(a, c, e, b, d), list);
        sorter.sortNotWaitingItems(list);
        assertEquals(List.of(a, c, e, b, d), list);

        Queue.NotWaitingItem arrival = item(6);
        list.add(arrival);
        sorter.sortNotWaitingItems(list);
        assertEquals(List.of(a, c, e, b, d, arrival), list);
    }

    @Test
    void followsMixedTransitiveDesiresAndIgnoresStaleAndSelfEdges() {
        Queue.NotWaitingItem a = blocked(1);
        Queue.NotWaitingItem b = item(2);
        Queue.NotWaitingItem c = blocked(3);
        List<Queue.NotWaitingItem> list = new ArrayList<>(List.of(a, b, c));
        sorter.getSimpleQueueComparator().addDesire(c.getId(), a.getId());
        sorter.getSimpleQueueComparator().addDesire(b.getId(), c.getId());
        sorter.getSimpleQueueComparator().addDesire(99, b.getId());
        sorter.getSimpleQueueComparator().addDesire(a.getId(), 99);
        sorter.getSimpleQueueComparator().addDesire(c.getId(), c.getId());

        sorter.sortNotWaitingItems(list);

        assertEquals(List.of(b, c, a), list);
    }

    @Test
    void keepsMixedItemsBehindTheirCyclicPredecessor() {
        Queue.NotWaitingItem a = blocked(1);
        Queue.NotWaitingItem b = item(2);
        Queue.NotWaitingItem c = blocked(3);
        Queue.NotWaitingItem d = item(4, 0);
        List<Queue.NotWaitingItem> list = new ArrayList<>(List.of(d, a, b, c));
        SimpleQueueSorter mixed = new SimpleQueueSorter(null);
        mixed.getSimpleQueueComparator().addDesire(a.getId(), b.getId());
        mixed.getSimpleQueueComparator().addDesire(b.getId(), c.getId());
        mixed.getSimpleQueueComparator().addDesire(c.getId(), a.getId());
        mixed.getSimpleQueueComparator().addDesire(c.getId(), d.getId());

        mixed.sortNotWaitingItems(list);

        assertEquals(List.of(a, b, c, d), list);
    }

    @Test
    void composesMixedSortersWithinOriginalKindSlotsBeforeApplyingDesires() {
        Queue.NotWaitingItem a = item(1);
        Queue.NotWaitingItem b = blocked(2);
        Queue.NotWaitingItem c = item(3);
        Queue.NotWaitingItem d = blocked(4);
        Queue.NotWaitingItem e = item(5);
        List<Queue.NotWaitingItem> list = new ArrayList<>(List.of(a, b, c, d, e));
        SimpleQueueSorter mixed = new SimpleQueueSorter(new hudson.model.queue.QueueSorter() {
            @Override
            public void sortBuildableItems(List<Queue.BuildableItem> items) {
                items.sort((first, second) -> Long.compare(second.getId(), first.getId()));
            }

            @Override
            public void sortBlockedItems(List<Queue.BlockedItem> items) {
                items.sort((first, second) -> Long.compare(second.getId(), first.getId()));
            }
        });

        mixed.sortNotWaitingItems(list);
        assertEquals(List.of(e, d, c, b, a), list);

        mixed.getSimpleQueueComparator().addDesire(a.getId(), c.getId());
        mixed.getSimpleQueueComparator().addDesire(a.getId(), e.getId());
        mixed.sortNotWaitingItems(list);

        assertEquals(List.of(d, b, a, e, c), list);
    }

    @Test
    void leavesBlockedInputIntactIfOriginalSorterFails() {
        List<Queue.BlockedItem> list = new ArrayList<>(List.of(blocked(1), blocked(2)));
        List<Queue.BlockedItem> original = new ArrayList<>(list);
        SimpleQueueSorter failing = new SimpleQueueSorter(new hudson.model.queue.QueueSorter() {
            @Override
            public void sortBuildableItems(List<Queue.BuildableItem> items) {}

            @Override
            public void sortBlockedItems(List<Queue.BlockedItem> items) {
                items.clear();
                throw new IllegalStateException("sort failed");
            }
        });

        assertThrows(IllegalStateException.class, () -> failing.sortBlockedItems(list));
        assertEquals(original, list);
    }

    @Test
    void leavesMixedInputIntactIfOriginalSorterFails() {
        List<Queue.NotWaitingItem> list = new ArrayList<>(List.of(item(1), blocked(2), item(3)));
        List<Queue.NotWaitingItem> original = new ArrayList<>(list);
        SimpleQueueSorter failing = new SimpleQueueSorter(new hudson.model.queue.QueueSorter() {
            @Override
            public void sortBuildableItems(List<Queue.BuildableItem> items) {
                items.clear();
                throw new IllegalStateException("sort failed");
            }

            @Override
            public void sortBlockedItems(List<Queue.BlockedItem> items) {
                items.clear();
            }
        });

        assertThrows(IllegalStateException.class, () -> failing.sortNotWaitingItems(list));
        assertEquals(original, list);
    }

    @Test
    public void sortBuildableItems() {
        List<Queue.BuildableItem> list = new ArrayList<>();
        Queue.BuildableItem a = item(1);
        Queue.BuildableItem b = item(2);
        Queue.BuildableItem c = item(3);
        Queue.BuildableItem d = item(4);
        list.add(a);
        list.add(b);
        list.add(c);
        list.add(d);
        sorter.sortBuildableItems(list);
        assertEquals(a.getId(), list.get(0).getId());
        assertEquals(b.getId(), list.get(1).getId());
        assertEquals(c.getId(), list.get(2).getId());
        assertEquals(d.getId(), list.get(3).getId());
        sorter.getSimpleQueueComparator().addDesire(b.getId(), a.getId());
        sorter.sortBuildableItems(list);
        assertEquals(b.getId(), list.get(0).getId());
        assertEquals(a.getId(), list.get(1).getId());
        assertEquals(c.getId(), list.get(2).getId());
        assertEquals(d.getId(), list.get(3).getId());
    }
}
