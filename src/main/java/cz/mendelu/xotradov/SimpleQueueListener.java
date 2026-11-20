package cz.mendelu.xotradov;

import hudson.Extension;
import hudson.model.Queue;
import hudson.model.queue.QueueListener;

@SuppressWarnings("unused") // justification: is used because of the @Extension below
@Extension
public class SimpleQueueListener extends QueueListener {
    /**
     * {@inheritDoc}
     *
     * <p>For {@link SimpleQueueSorter} logic, this override method
     * actively forgets information about the departed queue item
     * from {@code SimpleQueueComparator#moveDesires} table.</p>
     *
     * @param li    The item which left the queue
     *              (canceled or promoted to a running build)
     *
     * @see SimpleQueueComparator#removeStaleReferences
     * @see SimpleQueueComparator#removeDesireOfKey
     */
    @Override
    public void onLeft(Queue.LeftItem li) {
        long id = li.getId();
        SimpleQueueComparator comparator = SimpleQueueComparator.getInstance();
        comparator.removeDesireOfKey(id);
        comparator.removeStaleReferences(id);
        super.onLeft(li);
    }
}
