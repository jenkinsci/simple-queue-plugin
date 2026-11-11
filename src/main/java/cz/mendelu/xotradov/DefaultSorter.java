package cz.mendelu.xotradov;

import hudson.model.Queue;
import hudson.model.queue.AbstractQueueSorterImpl;
import java.io.Serializable;
import java.util.List;

/**
 * Uses Jenkins' default buildable-item priority, based on buildableStartMilliseconds.
 */
public class DefaultSorter extends AbstractQueueSorterImpl implements Serializable {
    @Override
    public void sortBuildableItems(List<Queue.BuildableItem> list) {
        list.sort(this); // sort is ascending order
    }
}
