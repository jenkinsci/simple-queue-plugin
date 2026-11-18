package cz.mendelu.xotradov.test.basic;

import static org.junit.jupiter.api.Assertions.*;

import cz.mendelu.xotradov.MoveAction;
import cz.mendelu.xotradov.SimpleQueueWidget;
import cz.mendelu.xotradov.test.TestHelper;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.Extension;
import hudson.model.Cause.UserIdCause;
import hudson.model.FreeStyleProject;
import hudson.model.Queue;
import hudson.model.View;
import hudson.model.queue.QueueTaskDispatcher;
import hudson.model.queue.CauseOfBlockage;
import hudson.model.queue.QueueTaskFuture;
import jenkins.model.queue.QueueItem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Tests for mixed blocked and buildable queue items.
 * 
 * The key issue: Jenkins Queue's getItems() always returns blocked items before
 * buildable items because it concatenates blockedProjects before buildables.
 * The widget must sort the combined list so that user's priority desires are
 * reflected in the displayed order.
 */
@WithJenkins
public class BasicTest_blockedAndBuildableMixedTest {
    public static Logger logger = Logger.getLogger(BasicTest_blockedAndBuildableMixedTest.class.getName());

    private TestHelper helper;

    /**
     * A QueueTaskDispatcher that blocks items whose task names are in the blocked set.
     */
    @Extension
    public static class BlockingDispatcher extends QueueTaskDispatcher {
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
        BlockingDispatcher.clear();
        if (helper != null) {
            helper.cleanup();
        }
    }

    /**
     * Tests that when we have both blocked and buildable items, the widget
     * displays them in the correct priority order based on user desires.
     * 
     * Setup:
     * - Occupy executors with long-running builds
     * - Schedule a buildable item (no label constraint)
     * - Schedule a blocked item (with unknown label constraint)
     * - Move the blocked item down (below the buildable item)
     * - Verify the widget shows the buildable item before the blocked item
     */
    /**
     * Schedules a project and waits for it to appear in the queue (either blocked or buildable).
     */
    private void scheduleAndWaitInQueue(FreeStyleProject project, Queue queue) throws Exception {
        QueueTaskFuture<?> future = project.scheduleBuild2(0, new UserIdCause());
        assertNotNull(future, "Task could not be scheduled");
        
        // Wait for the item to appear in the queue (either blocked or buildable)
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

    @Test
    public void blockedItemCanBeMovedBelowBuildable(JenkinsRule jenkinsRule) throws Exception {
        helper = new TestHelper(jenkinsRule);
        
        // Set executors to 0 so all items stay in the queue
        jenkinsRule.jenkins.setNumExecutors(0);
        
        Queue queue = Queue.getInstance();
        
        // Mark the blocked project before scheduling
        BlockingDispatcher.blockProject("blockedJob");
        
        // Schedule a buildable item (no label constraint) - will be buildable but can't run
        FreeStyleProject buildableProject = helper.createProject("buildableJob", 30000);
        scheduleAndWaitInQueue(buildableProject, queue);
        
        // Schedule a blocked item (blocked by our dispatcher)
        FreeStyleProject blockedProject = helper.createProject("blockedJob", 30000);
        scheduleAndWaitInQueue(blockedProject, queue);
        
        // Ensure maintenance has run to finalize blocked state
        queue.maintain();
        Thread.sleep(500);
        
        // Verify we have both types of items
        Queue.Item[] items = queue.getItems();
        logger.info("Queue items: " + java.util.Arrays.toString(items));
        for (Queue.Item item : items) {
            logger.info("Item: " + item.task.getDisplayName() + 
                ", blocked=" + item.isBlocked() + 
                ", buildable=" + item.isBuildable());
        }
        
        // Find our items
        Queue.Item buildableItem = null;
        Queue.Item blockedItem = null;
        for (Queue.Item item : items) {
            if (item.task.getDisplayName().equals("buildableJob")) {
                buildableItem = item;
            }
            if (item.task.getDisplayName().equals("blockedJob")) {
                blockedItem = item;
            }
        }
        
        assertNotNull(buildableItem, "Buildable item should be in queue");
        assertNotNull(blockedItem, "Blocked item should be in queue");
        assertTrue(buildableItem.isBuildable(), "buildableJob should be buildable");
        assertTrue(blockedItem.isBlocked(), "blockedJob should be blocked");
        
        // Before move: blocked items always appear before buildable items in queue.getItems()
        // because that's how Jenkins Queue.getItems() works
        int blockedIndexBefore = -1;
        int buildableIndexBefore = -1;
        for (int i = 0; i < items.length; i++) {
            if (items[i].getId() == blockedItem.getId()) blockedIndexBefore = i;
            if (items[i].getId() == buildableItem.getId()) buildableIndexBefore = i;
        }
        logger.info("Before move: blocked at " + blockedIndexBefore + ", buildable at " + buildableIndexBefore);
        
        // Move the blocked item down (below the buildable item)
        MoveAction moveAction = helper.getMoveAction();
        moveAction.moveDown(blockedItem, queue);
        queue.maintain();
        Thread.sleep(500);
        
        // Force widget refresh by getting a new widget instance
        // The WidgetFactory is called when the view's widgets are refreshed
        View primaryView = jenkinsRule.jenkins.getPrimaryView();
        
        // To get a fresh widget, we need to trigger the WidgetFactory again
        // In a real scenario, the AJAX refresh does this. In tests, we simulate it
        // by directly calling the factory
        SimpleQueueWidget.ViewFactoryImpl factory = new SimpleQueueWidget.ViewFactoryImpl();
        java.util.Collection<SimpleQueueWidget> widgets = factory.createFor(primaryView);
        SimpleQueueWidget widget = widgets.iterator().next();
        
        List<QueueItem> widgetItems = widget.getQueueItems();
        
        logger.info("Widget items after move:");
        for (QueueItem item : widgetItems) {
            if (item instanceof Queue.Item) {
                Queue.Item qi = (Queue.Item) item;
                logger.info("  " + qi.task.getDisplayName() + 
                    ", blocked=" + qi.isBlocked() + 
                    ", buildable=" + qi.isBuildable());
            }
        }
        
        // Find indices in widget
        int blockedIndexInWidget = -1;
        int buildableIndexInWidget = -1;
        for (int i = 0; i < widgetItems.size(); i++) {
            QueueItem qi = widgetItems.get(i);
            if (qi.getId() == blockedItem.getId()) blockedIndexInWidget = i;
            if (qi.getId() == buildableItem.getId()) buildableIndexInWidget = i;
        }
        
        logger.info("Widget: blocked at " + blockedIndexInWidget + ", buildable at " + buildableIndexInWidget);
        
        // The key assertion: after moving blocked down, it should appear AFTER buildable in the widget
        // This is the fix: the widget should sort the combined list
        assertTrue(blockedIndexInWidget > buildableIndexInWidget,
            "After moveDown, blocked item should appear after buildable item in widget. " +
            "Blocked index: " + blockedIndexInWidget + ", Buildable index: " + buildableIndexInWidget);
    }

    /**
     * Tests that when a blocked item is moved to top, it appears at the top
     * of the widget even above other blocked items.
     */
    @Test
    public void blockedItemMovedToTop(JenkinsRule jenkinsRule) throws Exception {
        helper = new TestHelper(jenkinsRule);
        
        // Set executors to 0 so all items stay in the queue
        jenkinsRule.jenkins.setNumExecutors(0);
        
        Queue queue = Queue.getInstance();
        
        // Mark both projects as blocked
        BlockingDispatcher.blockProject("blockedA");
        BlockingDispatcher.blockProject("blockedB");
        
        // Schedule two blocked items
        FreeStyleProject blockedA = helper.createProject("blockedA", 30000);
        scheduleAndWaitInQueue(blockedA, queue);
        
        FreeStyleProject blockedB = helper.createProject("blockedB", 30000);
        scheduleAndWaitInQueue(blockedB, queue);
        
        queue.maintain();
        Thread.sleep(500);
        
        Queue.Item[] items = queue.getItems();
        Queue.Item blockedAItem = null;
        Queue.Item blockedBItem = null;
        for (Queue.Item item : items) {
            if (item.task.getDisplayName().equals("blockedA")) blockedAItem = item;
            if (item.task.getDisplayName().equals("blockedB")) blockedBItem = item;
        }
        
        assertNotNull(blockedAItem, "blockedA should be in queue");
        assertNotNull(blockedBItem, "blockedB should be in queue");
        
        // Move blockedA to top
        MoveAction moveAction = helper.getMoveAction();
        moveAction.moveToTop(blockedAItem, queue);
        queue.maintain();
        Thread.sleep(500);
        
        // Check widget order - force fresh widget creation
        View primaryView = jenkinsRule.jenkins.getPrimaryView();
        SimpleQueueWidget.ViewFactoryImpl factory = new SimpleQueueWidget.ViewFactoryImpl();
        SimpleQueueWidget widget = factory.createFor(primaryView).iterator().next();
        List<QueueItem> widgetItems = widget.getQueueItems();
        
        // Find the first blocked item in the widget (excluding the fillQueue items)
        int blockedAIndex = -1;
        int blockedBIndex = -1;
        for (int i = 0; i < widgetItems.size(); i++) {
            QueueItem qi = widgetItems.get(i);
            if (qi.getId() == blockedAItem.getId()) blockedAIndex = i;
            if (qi.getId() == blockedBItem.getId()) blockedBIndex = i;
        }
        
        // blockedA should be before blockedB (higher priority = earlier in list)
        assertTrue(blockedAIndex < blockedBIndex,
            "blockedA should be before blockedB after moveToTop. " +
            "A index: " + blockedAIndex + ", B index: " + blockedBIndex);
    }

    /**
     * Tests that when a blocked item becomes unblocked (label is removed),
     * its priority position is preserved.
     */
    @Test
    public void blockedItemUnblockedPreservesPriority(JenkinsRule jenkinsRule) throws Exception {
        helper = new TestHelper(jenkinsRule);
        
        // Set executors to 0 so all items stay in the queue
        jenkinsRule.jenkins.setNumExecutors(0);
        
        Queue queue = Queue.getInstance();
        
        // Mark the project as blocked initially
        BlockingDispatcher.blockProject("willBeBuildable");
        
        // Schedule a buildable item first
        FreeStyleProject buildableFirst = helper.createProject("buildableFirst", 30000);
        scheduleAndWaitInQueue(buildableFirst, queue);
        
        // Schedule a blocked item
        FreeStyleProject blockedProject = helper.createProject("willBeBuildable", 30000);
        scheduleAndWaitInQueue(blockedProject, queue);
        
        // Schedule another buildable item
        FreeStyleProject buildableSecond = helper.createProject("buildableSecond", 30000);
        scheduleAndWaitInQueue(buildableSecond, queue);
        
        queue.maintain();
        Thread.sleep(500);
        
        Queue.Item[] items = queue.getItems();
        Queue.Item blockedItem = null;
        for (Queue.Item item : items) {
            if (item.task.getDisplayName().equals("willBeBuildable")) {
                blockedItem = item;
            }
        }
        
        assertNotNull(blockedItem, "Blocked item should be in queue");
        assertTrue(blockedItem.isBlocked(), "Item should be blocked initially");
        
        // Move the blocked item to top (highest priority)
        MoveAction moveAction = helper.getMoveAction();
        moveAction.moveToTop(blockedItem, queue);
        queue.maintain();
        Thread.sleep(500);
        
        // Now unblock the item
        BlockingDispatcher.unblockProject("willBeBuildable");
        queue.maintain();
        
        // Wait for the item to transition from blocked to buildable
        for (int i = 0; i < 30; i++) {
            Thread.sleep(500);
            queue.maintain();
            Queue.Item item = null;
            for (Queue.Item qi : queue.getItems()) {
                if (qi.task.getDisplayName().equals("willBeBuildable")) {
                    item = qi;
                    break;
                }
            }
            if (item != null && item.isBuildable()) {
                break;
            }
        }
        
        // Check the widget order - force fresh widget creation
        View primaryView = jenkinsRule.jenkins.getPrimaryView();
        SimpleQueueWidget.ViewFactoryImpl factory = new SimpleQueueWidget.ViewFactoryImpl();
        SimpleQueueWidget widget = factory.createFor(primaryView).iterator().next();
        List<QueueItem> widgetItems = widget.getQueueItems();
        
        logger.info("Widget items after unblock:");
        for (QueueItem item : widgetItems) {
            if (item instanceof Queue.Item) {
                Queue.Item qi = (Queue.Item) item;
                logger.info("  " + qi.task.getDisplayName() + 
                    ", blocked=" + qi.isBlocked() + 
                    ", buildable=" + qi.isBuildable());
            }
        }
        
        // Find indices
        int willBeBuildableIndex = -1;
        int buildableFirstIndex = -1;
        int buildableSecondIndex = -1;
        for (int i = 0; i < widgetItems.size(); i++) {
            QueueItem qi = widgetItems.get(i);
            if (qi instanceof Queue.Item) {
                Queue.Item qiItem = (Queue.Item) qi;
                if (qiItem.task.getDisplayName().equals("willBeBuildable")) willBeBuildableIndex = i;
                if (qiItem.task.getDisplayName().equals("buildableFirst")) buildableFirstIndex = i;
                if (qiItem.task.getDisplayName().equals("buildableSecond")) buildableSecondIndex = i;
            }
        }
        
        // The item that was moved to top should still be near the top after unblocking
        // It should be before the other buildable items
        if (willBeBuildableIndex >= 0 && buildableFirstIndex >= 0) {
            assertTrue(willBeBuildableIndex < buildableFirstIndex,
                "willBeBuildable should be before buildableFirst after unblocking. " +
                "willBeBuildable index: " + willBeBuildableIndex + 
                ", buildableFirst index: " + buildableFirstIndex);
        }
    }
}
