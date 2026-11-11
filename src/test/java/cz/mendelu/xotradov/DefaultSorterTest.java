package cz.mendelu.xotradov;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import hudson.model.Queue;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class DefaultSorterTest {

    @Test
    public void sortBuildableItems() throws ReflectiveOperationException {
        DefaultSorter sorter = new DefaultSorter();
        List<Queue.BuildableItem> list = new ArrayList<>();
        Queue.BuildableItem a = Mockito.mock(Queue.BuildableItem.class);
        Queue.BuildableItem b = Mockito.mock(Queue.BuildableItem.class);
        Queue.BuildableItem c = Mockito.mock(Queue.BuildableItem.class);
        when(a.getId()).thenReturn(1L);
        when(b.getId()).thenReturn(2L);
        when(c.getId()).thenReturn(3L);
        Field started = Queue.NotWaitingItem.class.getField("buildableStartMilliseconds");
        started.setAccessible(true);
        started.setLong(a, 100L);
        started.setLong(b, 90L);
        started.setLong(c, 80L);
        list.add(a);
        list.add(b);
        list.add(c);
        sorter.sortBuildableItems(list);
        assertEquals(c.getId(), list.get(0).getId());
        assertEquals(b.getId(), list.get(1).getId());
        assertEquals(a.getId(), list.get(2).getId());
    }
}
