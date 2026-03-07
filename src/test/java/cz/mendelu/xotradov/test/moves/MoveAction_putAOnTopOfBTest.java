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

public class MoveAction_putAOnTopOfBTest {

    @Rule
    public final JenkinsRule jenkinsRule = new JenkinsRule();
    public final TestHelper helper = new TestHelper(jenkinsRule);

    @After
    public void waitForClean() throws Exception {
        jenkinsRule.jenkins.getQueue().clear();
        jenkinsRule.waitUntilNoActivity();
    }

    @Test
    public void putAOnTopOfB() {
        try {
            long maxTestTime = 30000;
            helper.fillQueueFor(maxTestTime);
            FreeStyleProject C = helper.createAndSchedule("C", maxTestTime);
            FreeStyleProject D = helper.createAndSchedule("D", maxTestTime);
            FreeStyleProject E = helper.createAndSchedule("E", maxTestTime);
            FreeStyleProject F = helper.createAndSchedule("F", maxTestTime);
            MoveAction moveAction = helper.getMoveAction();
            Queue queue = jenkinsRule.jenkins.getQueue();
            assertEquals(C.getDisplayName(), queue.getItems()[3].task.getDisplayName());
            moveAction.putAOnTopOfB(C.getQueueItem(), D.getQueueItem(), queue);
            queue.maintain();
            assertEquals(C.getDisplayName(), queue.getItems()[2].task.getDisplayName());
            assertEquals(D.getDisplayName(), queue.getItems()[3].task.getDisplayName());
            moveAction.putAOnTopOfB(D.getQueueItem(), E.getQueueItem(), queue);
            queue.maintain();
            assertEquals(F.getDisplayName(), queue.getItems()[0].task.getDisplayName());
            assertEquals(D.getDisplayName(), queue.getItems()[1].task.getDisplayName());
            assertEquals(E.getDisplayName(), queue.getItems()[2].task.getDisplayName());
            assertEquals(C.getDisplayName(), queue.getItems()[3].task.getDisplayName());
            moveAction.putAOnTopOfB(E.getQueueItem(), F.getQueueItem(), queue);
            queue.maintain();
            assertEquals(E.getDisplayName(), queue.getItems()[0].task.getDisplayName());
            assertEquals(F.getDisplayName(), queue.getItems()[1].task.getDisplayName());
            assertEquals(D.getDisplayName(), queue.getItems()[2].task.getDisplayName());
            assertEquals(C.getDisplayName(), queue.getItems()[3].task.getDisplayName());

            // Tests of no change (already there):

            moveAction.putAOnTopOfB(E.getQueueItem(), F.getQueueItem(), queue);
            queue.maintain();
            assertEquals(E.getDisplayName(), queue.getItems()[0].task.getDisplayName());
            assertEquals(F.getDisplayName(), queue.getItems()[1].task.getDisplayName());
            assertEquals(D.getDisplayName(), queue.getItems()[2].task.getDisplayName());
            assertEquals(C.getDisplayName(), queue.getItems()[3].task.getDisplayName());

            moveAction.putAOnTopOfB(D.getQueueItem(), C.getQueueItem(), queue);
            queue.maintain();
            assertEquals(E.getDisplayName(), queue.getItems()[0].task.getDisplayName());
            assertEquals(F.getDisplayName(), queue.getItems()[1].task.getDisplayName());
            assertEquals(D.getDisplayName(), queue.getItems()[2].task.getDisplayName());
            assertEquals(C.getDisplayName(), queue.getItems()[3].task.getDisplayName());

            // Tests with array moves:

            // EFDC => move E+F over D => EFDC (same)
            moveAction.putAOnTopOfB(new Queue.Item[]{E.getQueueItem(), F.getQueueItem()}, D.getQueueItem(), queue);
            queue.maintain();
            assertEquals(E.getDisplayName(), queue.getItems()[0].task.getDisplayName());
            assertEquals(F.getDisplayName(), queue.getItems()[1].task.getDisplayName());
            assertEquals(D.getDisplayName(), queue.getItems()[2].task.getDisplayName());
            assertEquals(C.getDisplayName(), queue.getItems()[3].task.getDisplayName());

            // EFDC => move F+D over E => FDEC
            moveAction.putAOnTopOfB(new Queue.Item[]{F.getQueueItem(), D.getQueueItem()}, E.getQueueItem(), queue);
            queue.maintain();
            assertEquals(F.getDisplayName(), queue.getItems()[0].task.getDisplayName());
            assertEquals(D.getDisplayName(), queue.getItems()[1].task.getDisplayName());
            assertEquals(E.getDisplayName(), queue.getItems()[2].task.getDisplayName());
            assertEquals(C.getDisplayName(), queue.getItems()[3].task.getDisplayName());

            // FDEC => move D+C (with hole) over F => DCFE
            // messed-up order in array argument (C+D) does not impact original-queue item order (D+C)
            moveAction.putAOnTopOfB(new Queue.Item[]{C.getQueueItem(), D.getQueueItem()}, F.getQueueItem(), queue);
            queue.maintain();
            assertEquals(D.getDisplayName(), queue.getItems()[0].task.getDisplayName());
            assertEquals(C.getDisplayName(), queue.getItems()[1].task.getDisplayName());
            assertEquals(F.getDisplayName(), queue.getItems()[2].task.getDisplayName());
            assertEquals(E.getDisplayName(), queue.getItems()[3].task.getDisplayName());

            // DCFE => move D+E (with hole) over F (between them) => DECF
            moveAction.putAOnTopOfB(new Queue.Item[]{E.getQueueItem(), D.getQueueItem()}, F.getQueueItem(), queue);
            queue.maintain();
            assertEquals(D.getDisplayName(), queue.getItems()[0].task.getDisplayName());
            assertEquals(E.getDisplayName(), queue.getItems()[1].task.getDisplayName());
            assertEquals(C.getDisplayName(), queue.getItems()[2].task.getDisplayName());
            assertEquals(F.getDisplayName(), queue.getItems()[3].task.getDisplayName());
        } catch (Exception e) {
            fail();
        }
    }

}
