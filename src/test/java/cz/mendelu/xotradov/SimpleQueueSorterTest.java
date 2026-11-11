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
        Queue.BuildableItem item = Mockito.mock(Queue.BuildableItem.class);
        when(item.getId()).thenReturn(id);
        try {
            // Jenkins reads this final field directly, so a method stub has no effect.
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
