package cz.mendelu.xotradov;

import hudson.model.Queue;
import hudson.model.queue.AbstractQueueSorterImpl;
import java.io.Serializable;
import java.util.List;

/**
 * This imitates default jenkins sorter.
 * Has comparing from QueueSorter extension point which compares Items based on getInQueueSince().
 */
public class DefaultSorter extends AbstractQueueSorterImpl implements Serializable {
    @Override
    public void sortBuildableItems(List<Queue.BuildableItem> list) {
        list.sort(this); // sort is ascending order
    }
}
