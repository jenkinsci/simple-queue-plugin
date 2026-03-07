package cz.mendelu.xotradov.test.moves;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import cz.mendelu.xotradov.MoveAction;
import cz.mendelu.xotradov.test.TestHelper;
import hudson.model.FreeStyleProject;
import hudson.model.Queue;

import java.util.ArrayList;

public class MoveAction_moveToTopTest {

    @Rule
    public final JenkinsRule jenkinsRule = new JenkinsRule();
    public final TestHelper helper = new TestHelper(jenkinsRule);

    @After
    public void waitForClean() throws Exception {
        jenkinsRule.jenkins.getQueue().clear();
        jenkinsRule.waitUntilNoActivity();
    }

    @Test
    public void moveToTop() {
        try {
            long maxTestTime = 20000;
            helper.fillQueueFor(maxTestTime);
            FreeStyleProject C = helper.createAndSchedule("C", maxTestTime);
            FreeStyleProject D = helper.createAndSchedule("D", maxTestTime);
            FreeStyleProject E = helper.createAndSchedule("E", maxTestTime);
            MoveAction moveAction = helper.getMoveAction();
            Queue queue = jenkinsRule.jenkins.getQueue();
            assertEquals(C.getDisplayName(), queue.getItems()[2].task.getDisplayName());
            moveAction.moveToTop(C.getQueueItem(), queue);
            queue.maintain();
            assertEquals(C.getDisplayName(), queue.getItems()[0].task.getDisplayName());
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void moveToTopMany() {
        try {
            long maxTestTime = 10000;
            helper.fillQueueFor(maxTestTime);
            FreeStyleProject C = helper.createAndSchedule("C", maxTestTime);
            FreeStyleProject A = helper.createAndSchedule("A", maxTestTime);
            FreeStyleProject D = helper.createAndSchedule("D", maxTestTime);
            FreeStyleProject B = helper.createAndSchedule("B", maxTestTime);
            FreeStyleProject E = helper.createAndSchedule("E", maxTestTime);
            MoveAction moveAction = helper.getMoveAction();
            Queue queue = jenkinsRule.jenkins.getQueue();

            // Debugging aid:
            ArrayList<String> namesBefore = new ArrayList<>();
            for (Queue.Item item: queue.getItems())
                namesBefore.add(item.getDisplayName());

            // Inverse of job addition order - first added (C) has highest number,
            // so will be built first, and last added (E) has lowest number so will
            // be built last... unless something changes...
            assertEquals(E.getDisplayName(), queue.getItems()[0].task.getDisplayName());
            assertEquals(B.getDisplayName(), queue.getItems()[1].task.getDisplayName());
            assertEquals(D.getDisplayName(), queue.getItems()[2].task.getDisplayName());
            assertEquals(A.getDisplayName(), queue.getItems()[3].task.getDisplayName());
            assertEquals(C.getDisplayName(), queue.getItems()[4].task.getDisplayName());

            // Items should be moved in order they were in original queue,
            // regardless of order in the first argument, and group on top
            // (least importance, lowest numbers in queueItems() array)
            moveAction.moveToTop(new Queue.Item[] {B.getQueueItem(), A.getQueueItem()}, queue);
            queue.maintain();

            // Debugging aid:
            ArrayList<String> namesAfter = new ArrayList<>();
            for (Queue.Item item: queue.getItems())
                namesAfter.add(item.getDisplayName());

            assertEquals(B.getDisplayName(), queue.getItems()[0].task.getDisplayName());
            assertEquals(A.getDisplayName(), queue.getItems()[1].task.getDisplayName());
            assertEquals(E.getDisplayName(), queue.getItems()[2].task.getDisplayName());
            assertEquals(D.getDisplayName(), queue.getItems()[3].task.getDisplayName());
            assertEquals(C.getDisplayName(), queue.getItems()[4].task.getDisplayName());

            // Move top+neighbor to top (0) - no-op
            moveAction.moveToTop(new Queue.Item[] {B.getQueueItem(), A.getQueueItem()}, queue);
            queue.maintain();

            assertEquals(B.getDisplayName(), queue.getItems()[0].task.getDisplayName());
            assertEquals(A.getDisplayName(), queue.getItems()[1].task.getDisplayName());
            assertEquals(E.getDisplayName(), queue.getItems()[2].task.getDisplayName());
            assertEquals(D.getDisplayName(), queue.getItems()[3].task.getDisplayName());
            assertEquals(C.getDisplayName(), queue.getItems()[4].task.getDisplayName());

            // Move someone+top to top (0)
            moveAction.moveToTop(new Queue.Item[] {B.getQueueItem(), D.getQueueItem()}, queue);
            queue.maintain();

            assertEquals(B.getDisplayName(), queue.getItems()[0].task.getDisplayName());
            assertEquals(D.getDisplayName(), queue.getItems()[1].task.getDisplayName());
            assertEquals(A.getDisplayName(), queue.getItems()[2].task.getDisplayName());
            assertEquals(E.getDisplayName(), queue.getItems()[3].task.getDisplayName());
            assertEquals(C.getDisplayName(), queue.getItems()[4].task.getDisplayName());

            // Move bottom+top to top (0)
            moveAction.moveToTop(new Queue.Item[] {B.getQueueItem(), C.getQueueItem()}, queue);
            queue.maintain();

            assertEquals(B.getDisplayName(), queue.getItems()[0].task.getDisplayName());
            assertEquals(C.getDisplayName(), queue.getItems()[1].task.getDisplayName());
            assertEquals(D.getDisplayName(), queue.getItems()[2].task.getDisplayName());
            assertEquals(A.getDisplayName(), queue.getItems()[3].task.getDisplayName());
            assertEquals(E.getDisplayName(), queue.getItems()[4].task.getDisplayName());

        } catch (Exception e) {
            fail();
        }
    }
}
