package cz.mendelu.xotradov;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.Extension;
import hudson.model.Queue;
import hudson.model.queue.CauseOfBlockage;
import hudson.model.queue.QueueTaskDispatcher;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.Jenkins;

/**
 * A {@link QueueTaskDispatcher} that enforces the Simple Queue priority ordering
 * when items transition from blocked to buildable.
 *
 * <p>The problem it solves: Jenkins Queue's {@code maintain()} method processes
 * blocked items one by one. When a blocked item's constraint clears (causeOfBlockage
 * becomes null), it is immediately added to the buildables list — regardless of
 * its priority relative to other still-blocked items. This means a lower-priority
 * item can start building before a higher-priority item even becomes unblocked.</p>
 *
 * <p>This dispatcher intercepts the transition by implementing {@link #canRun(Queue.Item)}.
 * When an item is about to become buildable, it checks whether there are higher-priority
 * items still blocked. If so, it returns a non-null {@link CauseOfBlockage} to hold
 * the item back until those higher-priority items are also unblocked.</p>
 *
 * <p>The priority check uses {@link SimpleQueueComparator}, which respects both
 * explicit user desires (arrow moves) and timestamp-based fallback ordering.</p>
 */
@Extension(ordinal = Double.MAX_VALUE)
public class PriorityEnforcingDispatcher extends QueueTaskDispatcher {
    private static final Logger logger = Logger.getLogger(PriorityEnforcingDispatcher.class.getName());

    @Override
    public @CheckForNull CauseOfBlockage canRun(Queue.Item item) {
        // Only enforce priority for NotWaitingItem (BlockedItem and BuildableItem).
        // WaitingItem is in the quiet period phase and should not be affected.
        if (!(item instanceof Queue.NotWaitingItem)) {
            return null;
        }

        Jenkins jenkins = Jenkins.getInstanceOrNull();
        if (jenkins == null) {
            return null;
        }

        Queue queue = jenkins.getQueue();
        SimpleQueueComparator comparator = SimpleQueueComparator.getInstance();

        // Check against all blocked items: if any blocked item has higher priority
        // than this item, hold this item back.
        // Queue.getBlockedItems() is protected, so we use getItems() and filter.
        try {
            for (Queue.Item qi : queue.getItems()) {
                if (!qi.isBlocked()) {
                    continue;
                }
                // Skip self (in case the item is still technically blocked while being checked)
                if (qi.getId() == item.getId()) {
                    continue;
                }

                // comparator.compare(a, b) returns -1 if a has higher priority than b.
                // If the blocked item has higher priority (compare returns -1),
                // we should block the current item.
                int cmp = comparator.compare(qi, item);
                if (cmp < 0) {
                    // The blocked item has higher priority — hold this item back.
                    logger.log(
                            Level.FINE,
                            "Holding back item {0} (id={1}) because higher-priority blocked item {2} (id={3}) is still waiting",
                            new Object[]{item.task.getFullDisplayName(), item.getId(),
                                    qi.task.getFullDisplayName(), qi.getId()}
                    );
                    return new CauseOfBlockage() {
                        @Override
                        public String getShortDescription() {
                            return "Waiting for higher-priority items to become available";
                        }
                    };
                }
            }
        } catch (Exception e) {
            // If getItems() fails (e.g., access control), skip the check.
            logger.log(Level.FINE, "Failed to check blocked items for priority enforcement", e);
        }

        return null;
    }
}
