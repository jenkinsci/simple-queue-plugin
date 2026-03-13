package cz.mendelu.xotradov.test.moves;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import cz.mendelu.xotradov.MoveAction;
import cz.mendelu.xotradov.test.TestHelper;
import hudson.model.FreeStyleProject;
import hudson.model.Queue;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class MoveAction_moveToBottomTest {

    @Rule
    public final JenkinsRule jenkinsRule = new JenkinsRule();

    public final TestHelper helper = new TestHelper(jenkinsRule);

    @After
    public void waitForClean() throws Exception {
        jenkinsRule.jenkins.getQueue().clear();
        jenkinsRule.waitUntilNoActivity();
    }

    @Test
    public void moveToBottom() {
        try {
            long maxTestTime = 20000;
            helper.fillQueueFor(maxTestTime);
            FreeStyleProject C = helper.createAndSchedule("C", maxTestTime);
            FreeStyleProject D = helper.createAndSchedule("D", maxTestTime);
            FreeStyleProject E = helper.createAndSchedule("E", maxTestTime);
            MoveAction moveAction = helper.getMoveAction();
            Queue queue = jenkinsRule.jenkins.getQueue();
            assertEquals(C.getDisplayName(), queue.getItems()[2].task.getDisplayName());
            assertEquals(D.getDisplayName(), queue.getItems()[1].task.getDisplayName());
            assertEquals(E.getDisplayName(), queue.getItems()[0].task.getDisplayName());

            // Send bottom to bottom (no-op, no error)
            moveAction.moveToBottom(C.getQueueItem(), queue);
            queue.maintain();
            assertEquals(C.getDisplayName(), queue.getItems()[2].task.getDisplayName());
            assertEquals(D.getDisplayName(), queue.getItems()[1].task.getDisplayName());
            assertEquals(E.getDisplayName(), queue.getItems()[0].task.getDisplayName());

            // Send someone else to bottom
            moveAction.moveToBottom(D.getQueueItem(), queue);
            queue.maintain();
            assertEquals(D.getDisplayName(), queue.getItems()[2].task.getDisplayName());
            assertEquals(C.getDisplayName(), queue.getItems()[1].task.getDisplayName());
            assertEquals(E.getDisplayName(), queue.getItems()[0].task.getDisplayName());

            // Send someone else to bottom
            moveAction.moveToBottom(E.getQueueItem(), queue);
            queue.maintain();
            assertEquals(E.getDisplayName(), queue.getItems()[2].task.getDisplayName());
            assertEquals(D.getDisplayName(), queue.getItems()[1].task.getDisplayName());
            assertEquals(C.getDisplayName(), queue.getItems()[0].task.getDisplayName());
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void moveToBottomMany() {
        try {
            long maxTestTime = 20000;
            helper.fillQueueFor(maxTestTime);
            FreeStyleProject C = helper.createAndSchedule("C", maxTestTime);
            FreeStyleProject D = helper.createAndSchedule("D", maxTestTime);
            FreeStyleProject E = helper.createAndSchedule("E", maxTestTime);
            FreeStyleProject F = helper.createAndSchedule("F", maxTestTime);
            FreeStyleProject G = helper.createAndSchedule("G", maxTestTime);
            FreeStyleProject H = helper.createAndSchedule("H", maxTestTime);
            MoveAction moveAction = helper.getMoveAction();
            Queue queue = jenkinsRule.jenkins.getQueue();

            // C is bottom (earliest scheduled, highest priority)
            assertEquals(C.getDisplayName(), queue.getItems()[5].task.getDisplayName());
            assertEquals(D.getDisplayName(), queue.getItems()[4].task.getDisplayName());
            assertEquals(E.getDisplayName(), queue.getItems()[3].task.getDisplayName());
            assertEquals(F.getDisplayName(), queue.getItems()[2].task.getDisplayName());
            assertEquals(G.getDisplayName(), queue.getItems()[1].task.getDisplayName());
            assertEquals(H.getDisplayName(), queue.getItems()[0].task.getDisplayName());
            // H is top (lowest priority)

            // Assign new bottom dwellers
            moveAction.moveToBottom(new Queue.Item[] {G.getQueueItem(), E.getQueueItem()}, queue);
            queue.maintain();
            assertEquals(E.getDisplayName(), queue.getItems()[5].task.getDisplayName());
            assertEquals(G.getDisplayName(), queue.getItems()[4].task.getDisplayName());
            assertEquals(C.getDisplayName(), queue.getItems()[3].task.getDisplayName());
            assertEquals(D.getDisplayName(), queue.getItems()[2].task.getDisplayName());
            assertEquals(F.getDisplayName(), queue.getItems()[1].task.getDisplayName());
            assertEquals(H.getDisplayName(), queue.getItems()[0].task.getDisplayName());

            // Resend another set, where E is already at the bottom
            moveAction.moveToBottom(new Queue.Item[] {D.getQueueItem(), E.getQueueItem()}, queue);
            queue.maintain();
            assertEquals(E.getDisplayName(), queue.getItems()[5].task.getDisplayName());
            assertEquals(D.getDisplayName(), queue.getItems()[4].task.getDisplayName());
            assertEquals(G.getDisplayName(), queue.getItems()[3].task.getDisplayName());
            assertEquals(C.getDisplayName(), queue.getItems()[2].task.getDisplayName());
            assertEquals(F.getDisplayName(), queue.getItems()[1].task.getDisplayName());
            assertEquals(H.getDisplayName(), queue.getItems()[0].task.getDisplayName());

            // Move current top+bottom to the bottom
            moveAction.moveToBottom(new Queue.Item[] {H.getQueueItem(), E.getQueueItem()}, queue);
            queue.maintain();
            assertEquals(E.getDisplayName(), queue.getItems()[5].task.getDisplayName());
            assertEquals(H.getDisplayName(), queue.getItems()[4].task.getDisplayName());
            assertEquals(D.getDisplayName(), queue.getItems()[3].task.getDisplayName());
            assertEquals(G.getDisplayName(), queue.getItems()[2].task.getDisplayName());
            assertEquals(C.getDisplayName(), queue.getItems()[1].task.getDisplayName());
            assertEquals(F.getDisplayName(), queue.getItems()[0].task.getDisplayName());
        } catch (Exception e) {
            fail();
        }
    }
}
