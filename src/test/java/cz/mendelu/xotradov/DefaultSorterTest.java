package cz.mendelu.xotradov;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import hudson.model.Queue;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class DefaultSorterTest {

    @Test
    public void sortBuildableItems() {
        DefaultSorter sorter = new DefaultSorter();
        List<Queue.BuildableItem> list = new ArrayList<>();
        Queue.BuildableItem a = Mockito.mock(Queue.BuildableItem.class);
        Queue.BuildableItem b = Mockito.mock(Queue.BuildableItem.class);
        Queue.BuildableItem c = Mockito.mock(Queue.BuildableItem.class);
        when(a.getId()).thenReturn(1L);
        when(b.getId()).thenReturn(2L);
        when(c.getId()).thenReturn(3L);
        when(a.getInQueueSince()).thenReturn(100L);
        when(b.getInQueueSince()).thenReturn(90L);
        when(c.getInQueueSince()).thenReturn(80L);
        list.add(a);
        list.add(b);
        list.add(c);
        sorter.sortBuildableItems(list);
        assertEquals(a.getId(), list.get(0).getId());
        assertEquals(b.getId(), list.get(1).getId());
        assertEquals(c.getId(), list.get(2).getId());
    }
}
