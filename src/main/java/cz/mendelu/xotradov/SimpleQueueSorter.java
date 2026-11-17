package cz.mendelu.xotradov;

import hudson.model.Queue;
import hudson.model.queue.QueueSorter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import jenkins.model.Jenkins;
import jenkins.model.queue.QueueItem;

/**
 * The new sorter of Simple Queue plugin, based on sysadmin desires
 * expressed via UI buttons (or equivalent API calls) for blocked
 * and buildable queue items. This plugin deals with specifically
 * {@link Queue.BlockedItem} and {@link Queue.BuildableItem} classes
 * which both descend from {@link Queue.NotWaitingItem} and follow
 * similar default ordering semantics based on initial scheduling time
 * (per {@link Queue.NotWaitingItem#buildableStartMilliseconds}).
 * Healthy Queue items do not normally spend significant time in
 * other life cycle phases, so adding support for their (different)
 * sorting approaches makes little sense.<br/>
 *
 * As a base-line default case, when no explicit desires have been made,
 * it preserves the order of default Jenkins sorter (or a queue sorting
 * plugin that would have been considered previously per registration
 * order) by remembering the {@link #originalQueueSorter} reference.
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

    @Override
    public void sortBlockedItems(List<Queue.BlockedItem> list) {
        if (this.originalQueueSorter != null) {
            this.originalQueueSorter.sortBlockedItems(list);
        }
        Collections.sort(list, simpleQueueComparator);
    }

    /** Extension of this class over {@link QueueSorter}: a way to sort any
     *  {@link Queue.NotWaitingItem} (e.g. covering both {@link Queue.BlockedItem}
     *  and {@link Queue.BuildableItem}) in the same space of priority desires.
     */
    public void sortNotWaitingItems(List<Queue.NotWaitingItem> list) {
        if (this.originalQueueSorter != null) {
            List<Queue.BlockedItem> listBlocked = new ArrayList<>();
            List<Queue.BuildableItem> listBuildable = new ArrayList<>();
            for (QueueItem i : list) {
                if (i == null)
                    continue;
                if (i instanceof Queue.BlockedItem)
                    listBlocked.add((Queue.BlockedItem)i);
                if (i instanceof Queue.BuildableItem) {
                    listBuildable.add((Queue.BuildableItem)i);
                }
            }

            this.originalQueueSorter.sortBlockedItems(listBlocked);
            this.originalQueueSorter.sortBuildableItems(listBuildable);
        }

        // Follow up by applying our relative desire ordering:
        Collections.sort(list, simpleQueueComparator);
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
