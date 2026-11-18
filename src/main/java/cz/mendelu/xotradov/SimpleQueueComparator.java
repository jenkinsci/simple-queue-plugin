package cz.mendelu.xotradov;

import com.google.common.annotations.VisibleForTesting;
import hudson.model.Queue;
import java.util.*;
import java.util.logging.Logger;

/**
 * The comparator backs up the SimpleQueueSorter.
 * It stores all current desires - acts of will concerning order of build items.
 * @see #moveDesires
 * @see #compare
 * @author Jaroslav Otradovec
 */
public class SimpleQueueComparator implements Comparator<Queue.Item> {
    private static Logger logger = Logger.getLogger(SimpleQueueComparator.class.getName());

    /** <ul>Record of relative priority desires.
     *  <li>Keys are IDs of the "more important" queue items.</li>
     *  <li>Values are lists of IDs of the "less important" queue items
     *  (which should be prioritized to build after the "key").</li>
     *  </ul>
     * @see #compare
     */
    private Hashtable<Long, List<Long>> moveDesires = new Hashtable<>();

    private static class SimpleQueueComparatorHolder {
        static final SimpleQueueComparator INSTANCE = new SimpleQueueComparator();
    }

    public static SimpleQueueComparator getInstance() {
        return SimpleQueueComparatorHolder.INSTANCE;
    }

    Map<Long, Set<Long>> snapshotDesires() {
        Map<Long, Set<Long>> snapshot = new HashMap<>();
        // ArrayList snapshots via toArray(), avoiding a live fail-fast iterator
        // when a concurrent manual move changes the desire list. This is a
        // best-effort copy, not an atomic snapshot across a compound move;
        // atomicity requires writers and the snapshot to share one lock.
        moveDesires.forEach((id, after) -> snapshot.put(id, new HashSet<>(new ArrayList<>(after))));
        return snapshot;
    }

    public boolean hasDesiresFor(long key) {
        return moveDesires.containsKey(key);
    }

    /**
     * {@inheritDoc}
     * @return -1 when first queue item is recorded in {@link #moveDesires}
     *         as more important, 1 when second is more important, default 0
     */
    @Override
    public int compare(Queue.Item item1, Queue.Item item2) {
        if (isFirstItemOverSecond(item1.getId(), item2.getId())) {
            return -1;
        } else if (isFirstItemOverSecond(item2.getId(), item1.getId())) {
            return 1;
        } else {
            return 0;
        }
    }

    /** Desire-aware part of the {@link compare} logic.
     * @param longA Id of desired more important {@link Queue.Item}
     * @param longB Id of less important {@link Queue.Item}
     * @return {@code true} if we have a recorded desire list for {@code longA}
     *         AND that list contains {@code longB}; {@code false} otherwise
     */
    private boolean isFirstItemOverSecond(long longA, long longB) {
        if (moveDesires.get(longA) != null) {
            return moveDesires.get(longA).contains(longB);
        } else {
            return false;
        }
    }

    /**
     * Add desire of order relationship between two items.
     * If an opposite relation is present, it is deleted.
     * @param longA Id of desired more important {@link Queue.Item}
     * @param longB Id of less important {@link Queue.Item}
     */
    public void addDesire(long longA, long longB) {
        List<Long> bList;
        if (!moveDesires.containsKey(longA)) {
            bList = new ArrayList<>();
            moveDesires.put(longA, bList);
        } else {
            bList = moveDesires.get(longA);
        }
        if (bList != null) {
            if (bList.isEmpty()) {
                bList.add(longB);
            } else {
                if (!bList.contains(longB)) {
                    bList.add(longB);
                }
            }
            // Cleaning of previous order
            List<Long> aList = moveDesires.get(longB);
            if (aList != null && aList.contains(longA)) {
                aList.remove(longA);
                if (aList.isEmpty()) {
                    moveDesires.remove(longB);
                }
            }
        }
    }

    @VisibleForTesting
    public void removeDesireOfKey(long id) {
        moveDesires.remove(id);
    }

    @VisibleForTesting
    public void resetDesires() {
        moveDesires.clear();
    }

    /**
     * Sorts the NotWaitingItem entries (BlockedItem and BuildableItem) within the given list
     * in place, using the comparator's desires. Items that are not NotWaitingItem (e.g.
     * WaitingItem) are left in their original position, with the sorted NotWaitingItems
     * placed between the before/after groups.
     * <p>
     * The sort order matches the widget display: lower priority at the top (start of list),
     * higher priority at the bottom (end of list). The comparator returns -1 for higher
     * priority, so we reverse the arguments to achieve the display order.
     *
     * @param items the mutable list of queue items to sort in place
     */
    public void sortNotWaitingItems(List<Queue.Item> items) {
        if (items.size() <= 1) {
            return;
        }

        // Collect NotWaitingItems (BlockedItem and BuildableItem) for sorting
        List<Queue.NotWaitingItem> notWaitingItems = new ArrayList<>();
        for (Queue.Item item : items) {
            if (item instanceof Queue.NotWaitingItem) {
                notWaitingItems.add((Queue.NotWaitingItem) item);
            }
        }

        if (notWaitingItems.isEmpty()) {
            return;
        }

        // Sort NotWaitingItems using the comparator (reversed for display order)
        notWaitingItems.sort((a, b) -> compare(b, a));

        // Separate items into three groups:
        // 1. Items before the first NotWaitingItem (waiting items, etc.)
        // 2. NotWaitingItems (sorted)
        // 3. Items after the last NotWaitingItem
        List<Queue.Item> before = new ArrayList<>();
        List<Queue.Item> after = new ArrayList<>();

        int firstNotWaitingIndex = -1;
        int lastNotWaitingIndex = -1;
        for (int i = 0; i < items.size(); i++) {
            Queue.Item item = items.get(i);
            if (item instanceof Queue.NotWaitingItem) {
                if (firstNotWaitingIndex == -1) {
                    firstNotWaitingIndex = i;
                }
                lastNotWaitingIndex = i;
            }
        }

        for (int i = 0; i < items.size(); i++) {
            Queue.Item item = items.get(i);
            if (item instanceof Queue.NotWaitingItem) {
                continue;
            }
            if (firstNotWaitingIndex != -1 && i < firstNotWaitingIndex) {
                before.add(item);
            } else if (lastNotWaitingIndex != -1 && i > lastNotWaitingIndex) {
                after.add(item);
            } else {
                before.add(item);
            }
        }

        // Rebuild the items list: before + sorted NotWaitingItems + after
        items.clear();
        items.addAll(before);
        items.addAll(notWaitingItems);
        items.addAll(after);
    }
}
