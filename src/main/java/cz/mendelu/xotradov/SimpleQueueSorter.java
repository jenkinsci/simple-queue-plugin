package cz.mendelu.xotradov;

import hudson.model.Queue;
import hudson.model.queue.QueueSorter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import jenkins.model.Jenkins;

/**
 * The new sorter of Simple Queue plugin. Preserves the order of default jenkins sorter, when no desires have been made.
 */
@SuppressWarnings("unused")
public class SimpleQueueSorter extends QueueSorter {
    private static Logger logger = Logger.getLogger(SimpleQueueSorter.class.getName());
    private final QueueSorter originalQueueSorter;
    private final SimpleQueueComparator simpleQueueComparator;
    private final transient List<Queue.BuildableItem> listPrevious = new ArrayList<>();

    public SimpleQueueSorter(QueueSorter originalQueueSorter) {
        this.originalQueueSorter = originalQueueSorter;
        this.simpleQueueComparator = SimpleQueueComparator.getInstance();
    }

    @Override
    public void sortBuildableItems(List<Queue.BuildableItem> list) {
        synchronized (simpleQueueComparator) {
            // Avoid unnecessary sorting if nothing changed since last round:
            // same items in same order
            if (!(simpleQueueComparator.hasChangedDesires()) && list.equals(listPrevious)) return;

            if (this.originalQueueSorter != null) {
                // Note: if DefaultSorter (usually is), we pre-sort by timestamps
                // and then follow up below with desires for relative priorities
                // of specific items.
                this.originalQueueSorter.sortBuildableItems(list);
            }

            // Note: the sort() method does not compare everyone to everyone,
            // it passes the list comparing nearby couples and only steps back
            // a bit if some two (neighboring!) entries were swapped. Example:
            //   * list start: [C]3 [A]4 [D]5 [B]6 [E]7
            //   * comparator desires, one: 7 (more important) => [6, 4]
            //     so E should move to the middle of the list, A and B to the right?..
            //   * comparisons as traced in debugger (steps into the compare()
            //     method and its two args):
            //     * Round 1, starting with C A D B E:
            //       * A, C => 0
            //       * D, A => 0
            //       * B, D => 0
            //       * E, B => -1 (E more important)
            //     * List swapped (E vs B)
            //     * Round 2, proceeding with C A D E B
            //       * E, D => 0
            //       * E, B => -1
            //     * No more swapping (correct order of E vs B already),
            //     * List remains C A D E B
            //   * Note that [E]7 was never compared to A[4], they were too far away
            //     so the expected desire for C D E A B (nor C D E B A technically)
            //     was not fulfilled UNTIL desires for A  vs. D and A vs. E got defined,
            //     and the move*() methods for arrays got more complex that initially.
            Collections.sort(list, simpleQueueComparator);
            simpleQueueComparator.resetChangedDesires();

            listPrevious.clear();
            listPrevious.addAll(list);
        }
    }

    public SimpleQueueComparator getSimpleQueueComparator() {
        return simpleQueueComparator;
    }

    void reset() {
        // NOTE: This logic is not "synchronized(simpleQueueComparator)"
        // to avoid deadlocks with methods it calls which sync on that.
        listPrevious.clear();
        simpleQueueComparator.resetDesires();
        sortBuildableItems(Jenkins.get().getQueue().getBuildableItems());
        Queue.getInstance().maintain();
    }
}
