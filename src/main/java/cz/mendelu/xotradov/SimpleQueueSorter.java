package cz.mendelu.xotradov;

import hudson.model.Queue;
import hudson.model.queue.QueueSorter;
import jenkins.model.Jenkins;

import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * The new sorter of Simple Queue plugin. Preserves the order of default jenkins sorter, when no desires have been made.
 */
@SuppressWarnings("unused")
public class SimpleQueueSorter extends QueueSorter {
    private static Logger logger = Logger.getLogger(SimpleQueueSorter.class.getName());
    private final QueueSorter originalQueueSorter;
    private final SimpleQueueComparator simpleQueueComparator;

    public SimpleQueueSorter(QueueSorter originalQueueSorter) {
        this.originalQueueSorter = originalQueueSorter;
        this.simpleQueueComparator = SimpleQueueComparator.getInstance();
    }

    @Override
    public void sortBuildableItems(List<Queue.BuildableItem> list) {
        if(this.originalQueueSorter != null) {
            this.originalQueueSorter.sortBuildableItems(list);
        }
        Collections.sort(list, simpleQueueComparator);
    }
    public SimpleQueueComparator getSimpleQueueComparator(){
        return simpleQueueComparator;
    }

    public void reset() {
        simpleQueueComparator.resetDesires();
        sortBuildableItems(Jenkins.get().getQueue().getBuildableItems());
        Queue.getInstance().maintain();
    }
}
