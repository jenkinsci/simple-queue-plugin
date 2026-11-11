package cz.mendelu.xotradov;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import hudson.model.Queue;
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
        Queue.BuildableItem item = Mockito.mock(Queue.BuildableItem.class);
        when(item.getId()).thenReturn(id);
        when(item.getInQueueSince()).thenReturn(100L - id);
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
        Queue.BuildableItem a = Mockito.mock(Queue.BuildableItem.class);
        Queue.BuildableItem b = Mockito.mock(Queue.BuildableItem.class);
        Queue.BuildableItem c = Mockito.mock(Queue.BuildableItem.class);
        Queue.BuildableItem d = Mockito.mock(Queue.BuildableItem.class);
        when(a.getId()).thenReturn(1L);
        when(b.getId()).thenReturn(2L);
        when(c.getId()).thenReturn(3L);
        when(d.getId()).thenReturn(4L);
        when(a.getInQueueSince()).thenReturn(100L);
        when(b.getInQueueSince()).thenReturn(90L);
        when(c.getInQueueSince()).thenReturn(80L);
        when(d.getInQueueSince()).thenReturn(70L);
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
