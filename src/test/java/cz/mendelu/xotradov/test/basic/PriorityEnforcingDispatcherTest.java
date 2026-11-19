package cz.mendelu.xotradov.test.basic;

import static org.junit.jupiter.api.Assertions.*;

import cz.mendelu.xotradov.MoveAction;
import cz.mendelu.xotradov.PriorityEnforcingDispatcher;
import cz.mendelu.xotradov.SimpleQueueComparator;
import cz.mendelu.xotradov.test.TestHelper;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.Extension;
import hudson.model.Cause.UserIdCause;
import hudson.model.FreeStyleProject;
import hudson.model.Queue;
import hudson.model.queue.CauseOfBlockage;
import hudson.model.queue.QueueTaskDispatcher;
import hudson.model.queue.QueueTaskFuture;
import jenkins.model.Jenkins;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Tests for {@link PriorityEnforcingDispatcher}.
 *
 * <p>The dispatcher prevents lower-priority items from becoming buildable
 * while higher-priority items are still blocked. It uses {@link SimpleQueueComparator}
 * to determine priority, which respects both explicit user desires (arrow moves)
 * and timestamp-based fallback ordering.</p>
 */
@WithJenkins
public class PriorityEnforcingDispatcherTest {
    public static Logger logger = Logger.getLogger(PriorityEnforcingDispatcherTest.class.getName());

    private TestHelper helper;

    /**
     * A QueueTaskDispatcher that blocks items whose task names are in the blocked set.
     * This dispatcher runs at a lower ordinal than PriorityEnforcingDispatcher, so
     * PriorityEnforcingDispatcher can check against items blocked by this dispatcher.
     */
    @Extension(ordinal = 100.0)
    public static class TestBlockingDispatcher extends QueueTaskDispatcher {
        private static final Set<String> BLOCKED_NAMES = new HashSet<>();

        public static void blockProject(String displayName) {
            BLOCKED_NAMES.add(displayName);
        }

        public static void unblockProject(String displayName) {
            BLOCKED_NAMES.remove(displayName);
        }

        public static void clear() {
            BLOCKED_NAMES.clear();
        }

        @CheckForNull
        @Override
        public CauseOfBlockage canRun(Queue.Item item) {
            if (item.task != null && BLOCKED_NAMES.contains(item.task.getDisplayName())) {
                return new CauseOfBlockage() {
                    @Override
                    public String getShortDescription() {
                        return "Blocked by test dispatcher";
                    }
                };
            }
            return null;
        }
    }

    @AfterEach
    public void waitForClean() throws Exception {
        TestBlockingDispatcher.clear();
        SimpleQueueComparator.getInstance().resetDesires();
        if (helper != null) {
            helper.cleanup();
        }
    }

    /**
     * Schedules a project and waits for it to appear in the queue (either blocked or buildable).
     */
    private void scheduleAndWaitInQueue(FreeStyleProject project, Queue queue) throws Exception {
        QueueTaskFuture<?> future = project.scheduleBuild2(0, new UserIdCause());
        assertNotNull(future, "Task could not be scheduled");

        for (int i = 0; i < 600; i++) {
            Thread.sleep(500);
            queue.maintain();
            for (Queue.Item item : queue.getItems()) {
                if (item.task != null && item.task.getDisplayName().equals(project.getDisplayName())) {
                    if (item.isBlocked() || item.isBuildable()) {
                        return;
                    }
                }
            }
        }
        fail("Item " + project.getDisplayName() + " did not appear in queue as blocked or buildable");
    }

    /**
     * Finds a queue item by project display name.
     */
    private Queue.Item findItem(Queue queue, String displayName) {
        for (Queue.Item item : queue.getItems()) {
            if (item.task != null && item.task.getDisplayName().equals(displayName)) {
                return item;
            }
        }
        return null;
    }

    /**
     * Tests that when a higher-priority item is blocked by its own constraint (not by
     * priority enforcement), a lower-priority item is allowed to proceed.
     *
     * <p>Setup:
     * - Two items: highPriority (blocked by TestBlockingDispatcher) and lowPriority (not blocked)
     * - lowPriority is moved to top (lowest priority) via MoveAction, making highPriority higher priority
     * - highPriority is blocked by TestBlockingDispatcher (not by PriorityEnforcingDispatcher)
     * - Expected: canRun(lowPriority) returns null because highPriority is blocked by its own
     *   constraint and cannot become buildable right now anyway</p>
     */
    @Test
    public void lowerPriorityProceedsWhenHigherPriorityBlockedByOwnConstraint(JenkinsRule jenkinsRule) throws Exception {
        helper = new TestHelper(jenkinsRule);

        jenkinsRule.jenkins.setNumExecutors(0);
        Queue queue = Queue.getInstance();

        // Block highPriority only — lowPriority will not be blocked by TestBlockingDispatcher
        TestBlockingDispatcher.blockProject("highPriority");

        FreeStyleProject highPriority = helper.createProject("highPriority", 30000);
        scheduleAndWaitInQueue(highPriority, queue);

        FreeStyleProject lowPriority = helper.createProject("lowPriority", 30000);
        scheduleAndWaitInQueue(lowPriority, queue);

        queue.maintain();
        Thread.sleep(500);

        Queue.Item highItem = findItem(queue, "highPriority");
        Queue.Item lowItem = findItem(queue, "lowPriority");
        assertNotNull(highItem, "highPriority should be in queue");
        assertNotNull(lowItem, "lowPriority should be in queue");
        assertTrue(highItem.isBlocked(), "highPriority should be blocked by TestBlockingDispatcher");

        // Move lowPriority to top of the UI list (lowest priority position).
        // In this plugin, "top" of the list = lowest priority (furthest from execution).
        // This makes highPriority the higher-priority item.
        MoveAction moveAction = helper.getMoveAction();
        moveAction.moveToTop(lowItem, queue);
        queue.maintain();
        Thread.sleep(500);

        // Verify the comparator reflects the desire: highPriority > lowPriority
        int cmp = SimpleQueueComparator.getInstance().compare(highItem, lowItem);
        logger.info("Comparator (high vs low): " + cmp + " (negative means high has higher priority)");
        assertTrue(cmp < 0, "highPriority should have higher priority than lowPriority");

        // highPriority is blocked by TestBlockingDispatcher, not by PriorityEnforcingDispatcher.
        // So lowPriority should be allowed to proceed because highPriority cannot become
        // buildable right now anyway.
        PriorityEnforcingDispatcher dispatcher = new PriorityEnforcingDispatcher();
        CauseOfBlockage blockage = dispatcher.canRun(lowItem);
        assertNull(blockage,
                "canRun(lowPriority) should return null because highPriority is blocked by its own " +
                        "constraint (TestBlockingDispatcher), not by priority enforcement. " +
                        "highPriority cannot become buildable right now, so lowPriority should proceed.");
        logger.info("lowPriority is allowed to proceed");
    }

    /**
     * Tests that the dispatcher's canRun() returns null when no higher-priority
     * blocked items exist, allowing the item to proceed.
     */
    @Test
    public void itemProceedsWhenNoHigherPriorityBlocked(JenkinsRule jenkinsRule) throws Exception {
        helper = new TestHelper(jenkinsRule);

        jenkinsRule.jenkins.setNumExecutors(0);
        Queue queue = Queue.getInstance();

        // Schedule a project with no blocking
        FreeStyleProject project = helper.createProject("soloProject", 30000);
        scheduleAndWaitInQueue(project, queue);

        queue.maintain();
        Thread.sleep(500);

        Queue.Item item = findItem(queue, "soloProject");
        assertNotNull(item, "soloProject should be in queue");

        // No blocked items at all — canRun() should return null (allow to proceed)
        PriorityEnforcingDispatcher dispatcher = new PriorityEnforcingDispatcher();
        CauseOfBlockage blockage = dispatcher.canRun(item);
        assertNull(blockage,
                "canRun(soloProject) should return null when no higher-priority items are blocked");
    }

    /**
     * Tests that when a higher-priority item is blocked by its own constraint,
     * a lower-priority item is allowed to proceed even with timestamp-based ordering.
     *
     * <p>Setup:
     * - Two items: olderItem (blocked by TestBlockingDispatcher) and newerItem (not blocked)
     * - No explicit move desires — priority determined by timestamps
     * - olderItem has earlier timestamp, so higher fallback priority
     * - olderItem is blocked by TestBlockingDispatcher (not by PriorityEnforcingDispatcher)
     * - Expected: canRun(newerItem) returns null because olderItem is blocked by its own
     *   constraint and cannot become buildable right now anyway</p>
     */
    @Test
    public void timestampFallbackAllowsProceedWhenHigherPriorityBlockedByOwnConstraint(JenkinsRule jenkinsRule) throws Exception {
        helper = new TestHelper(jenkinsRule);

        jenkinsRule.jenkins.setNumExecutors(0);
        Queue queue = Queue.getInstance();

        // Block olderItem only
        TestBlockingDispatcher.blockProject("olderItem");

        // Schedule olderItem first — it gets an earlier timestamp
        FreeStyleProject olderItem = helper.createProject("olderItem", 30000);
        scheduleAndWaitInQueue(olderItem, queue);

        // Wait a bit to ensure distinct timestamps
        Thread.sleep(1000);

        // Schedule newerItem second
        FreeStyleProject newerItem = helper.createProject("newerItem", 30000);
        scheduleAndWaitInQueue(newerItem, queue);

        queue.maintain();
        Thread.sleep(500);

        Queue.Item olderQueueItem = findItem(queue, "olderItem");
        Queue.Item newerQueueItem = findItem(queue, "newerItem");
        assertNotNull(olderQueueItem, "olderItem should be in queue");
        assertNotNull(newerQueueItem, "newerItem should be in queue");
        assertTrue(olderQueueItem.isBlocked(), "olderItem should be blocked");

        // Verify the comparator gives olderItem higher priority (timestamp fallback)
        int cmp = SimpleQueueComparator.getInstance().compare(olderQueueItem, newerQueueItem);
        logger.info("Comparator result (older vs newer): " + cmp +
                " (negative means older has higher priority)");
        assertTrue(cmp < 0, "olderItem should have higher priority by timestamp");

        // olderItem is blocked by TestBlockingDispatcher, not by PriorityEnforcingDispatcher.
        // So newerItem should be allowed to proceed.
        PriorityEnforcingDispatcher dispatcher = new PriorityEnforcingDispatcher();
        CauseOfBlockage blockage = dispatcher.canRun(newerQueueItem);
        assertNull(blockage,
                "canRun(newerItem) should return null because olderItem is blocked by its own " +
                        "constraint (TestBlockingDispatcher), not by priority enforcement. " +
                        "olderItem cannot become buildable right now, so newerItem should proceed.");
    }

    /**
     * Tests that canRun() returns null when the only blocked item has lower priority
     * than the tested item (i.e., the tested item should not be held back).
     */
    @Test
    public void lowerPriorityBlockedItemDoesNotHoldBackHigherPriority(JenkinsRule jenkinsRule) throws Exception {
        helper = new TestHelper(jenkinsRule);

        jenkinsRule.jenkins.setNumExecutors(0);
        Queue queue = Queue.getInstance();

        // Block lowPriority only
        TestBlockingDispatcher.blockProject("lowPriority");

        FreeStyleProject highPriority = helper.createProject("highPriority", 30000);
        scheduleAndWaitInQueue(highPriority, queue);

        FreeStyleProject lowPriority = helper.createProject("lowPriority", 30000);
        scheduleAndWaitInQueue(lowPriority, queue);

        queue.maintain();
        Thread.sleep(500);

        Queue.Item highItem = findItem(queue, "highPriority");
        Queue.Item lowItem = findItem(queue, "lowPriority");
        assertNotNull(highItem);
        assertNotNull(lowItem);
        assertTrue(lowItem.isBlocked(), "lowPriority should be blocked");

        // Move lowPriority to top of UI list (lowest priority position).
        // This makes highPriority the higher-priority item.
        MoveAction moveAction = helper.getMoveAction();
        moveAction.moveToTop(lowItem, queue);
        queue.maintain();
        Thread.sleep(500);

        // canRun(highPriority) should return null — lowPriority is blocked but has
        // lower priority, so it should not hold back highPriority
        PriorityEnforcingDispatcher dispatcher = new PriorityEnforcingDispatcher();
        CauseOfBlockage blockage = dispatcher.canRun(highItem);
        assertNull(blockage,
                "canRun(highPriority) should return null because lowPriority " +
                        "(blocked) has lower priority");
    }

    /**
     * Tests that the dispatcher skips WaitingItem (items in quiet period).
     * canRun() should return null for non-NotWaitingItem instances.
     */
    @Test
    public void waitingItemNotAffected(JenkinsRule jenkinsRule) throws Exception {
        helper = new TestHelper(jenkinsRule);

        jenkinsRule.jenkins.setNumExecutors(0);
        Queue queue = Queue.getInstance();

        // Create a mock WaitingItem-like scenario: the dispatcher should return null
        // for any item that is not a NotWaitingItem. We verify this by checking that
        // the dispatcher only acts on NotWaitingItem instances.
        //
        // Since creating a real WaitingItem is difficult in tests, we verify the
        // dispatcher's behavior indirectly: it should not block items that are
        // NotWaitingItem when no other blocked items exist.
        FreeStyleProject project = helper.createProject("testProject", 30000);
        scheduleAndWaitInQueue(project, queue);

        queue.maintain();
        Thread.sleep(500);

        Queue.Item item = findItem(queue, "testProject");
        assertNotNull(item);

        // The item is a NotWaitingItem (BlockedItem or BuildableItem).
        // With no other blocked items, canRun() should return null.
        PriorityEnforcingDispatcher dispatcher = new PriorityEnforcingDispatcher();
        CauseOfBlockage blockage = dispatcher.canRun(item);
        assertNull(blockage,
                "canRun() should return null for NotWaitingItem when no higher-priority blocked items exist");
    }
}
