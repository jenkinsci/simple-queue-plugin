package cz.mendelu.xotradov;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import hudson.model.Queue;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.mockito.Mockito;

public class SimpleQueueSorterTest {

    SimpleQueueSorter sorter = new SimpleQueueSorter(new DefaultSorter());

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
