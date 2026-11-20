package cz.mendelu.xotradov;

import com.google.common.annotations.VisibleForTesting;
import hudson.model.Queue;
import java.util.*;
import java.util.logging.Logger;

/**
 * The comparator backs up the SimpleQueueSorter.
 * It stores all current desires - acts of will concerning order of build items.
 * @author Jaroslav Otradovec
 */
public class SimpleQueueComparator implements Comparator<Queue.Item> {
    private static Logger logger = Logger.getLogger(SimpleQueueComparator.class.getName());
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
     * @return -1 when first is more important, 1 when second is more important, default 0
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

    private boolean isFirstItemOverSecond(long id0, long id1) {
        if (moveDesires.get(id0) != null) {
            return moveDesires.get(id0).contains(id1);
        } else {
            return false;
        }
    }

    /**
     * Add desire of order relationship between two items. If is present vice versa relation, it is deleted.
     * @param longA Id of desired more important item
     * @param longB Id of less important Queue.Item
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

    /**
     * Forget that queue item {@code id} expressed any {@link #moveDesires}
     * to have higher priority than other items.
     *
     * @param id
     *
     * @see #removeStaleReferences
     * @see SimpleQueueListener#onLeft
     */
    @VisibleForTesting
    public void removeDesireOfKey(long id) {
        moveDesires.remove(id);
    }

    /**
     * Removes all references to the given item ID from the value lists of
     * every desire entry (when some other items desire higher priority
     * than this item {@code id}).<br/>
     *
     * This cleans up stale references left behind when a queue item departs
     * (promoted to a build or canceled).  Entries whose value list becomes
     * empty after such removal are also removed to keep the map compact.
     *
     * @param id
     *
     * @see #removeDesireOfKey
     * @see SimpleQueueListener#onLeft
     */
    @VisibleForTesting
    public void removeStaleReferences(long id) {
        // Collect keys for removal first to avoid ConcurrentModificationException
        // when removing empty entries from the Hashtable during iteration.
        List<Long> emptyKeys = new ArrayList<>();
        moveDesires.forEach((key, list) -> {
            list.remove(Long.valueOf(id));
            if (list.isEmpty()) {
                emptyKeys.add(key);
            }
        });
        for (Long key : emptyKeys) {
            moveDesires.remove(key);
        }
    }

    /**
     * Clear the {@link #moveDesires} table, effectively falling back
     * to system default ordering of items by timestamps (almost -- the
     * blocked and buildable items are intermixed here, and separated
     * in system default code), or whatever other Queue sorting plugins
     * say.
     */
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
