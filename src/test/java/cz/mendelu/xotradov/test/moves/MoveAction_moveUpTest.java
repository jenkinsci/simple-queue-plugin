package cz.mendelu.xotradov.test.moves;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import cz.mendelu.xotradov.MoveAction;
import cz.mendelu.xotradov.test.TestHelper;
import hudson.model.FreeStyleProject;
import hudson.model.Queue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
public class MoveAction_moveUpTest {

    private TestHelper helper;

    @AfterEach
    public void waitForClean() throws Exception {
        helper.cleanup();
    }

    @Test
    public void moveUp(JenkinsRule jenkinsRule) throws Exception {
        helper = new TestHelper(jenkinsRule);
        try {
            long maxTestTime = 10000;
            helper.fillQueueFor(maxTestTime);
            FreeStyleProject C = helper.createAndSchedule("C", maxTestTime);
            FreeStyleProject D = helper.createAndSchedule("D", maxTestTime);
            MoveAction moveAction = helper.getMoveAction();
            assertNotNull(C.getQueueItem());
            Queue queue = jenkinsRule.jenkins.getQueue();
            assertEquals(C.getDisplayName(), queue.getItems()[1].task.getDisplayName());
            moveAction.moveUp(C.getQueueItem(), queue);
            queue.maintain();
            assertEquals(C.getDisplayName(), queue.getItems()[0].task.getDisplayName());
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void moveUpMany(JenkinsRule jenkinsRule) throws Exception {
        helper = new TestHelper(jenkinsRule);
        try {
            long maxTestTime = 10000;
            helper.fillQueueFor(maxTestTime);
            FreeStyleProject C = helper.createAndSchedule("C", maxTestTime);
            FreeStyleProject D = helper.createAndSchedule("D", maxTestTime);
            FreeStyleProject E = helper.createAndSchedule("E", maxTestTime);
            FreeStyleProject F = helper.createAndSchedule("F", maxTestTime);
            FreeStyleProject G = helper.createAndSchedule("G", maxTestTime);
            FreeStyleProject H = helper.createAndSchedule("H", maxTestTime);
            MoveAction moveAction = helper.getMoveAction();
            assertNotNull(F.getQueueItem());
            Queue queue = jenkinsRule.jenkins.getQueue();

            // array of one element bubbles up one point
            assertEquals(F.getDisplayName(), queue.getItems()[2].task.getDisplayName());
            moveAction.moveUp(new Queue.Item[] {F.getQueueItem()}, queue);
            queue.maintain();
            assertEquals(F.getDisplayName(), queue.getItems()[1].task.getDisplayName());

            // array of one element bubbles up one more point
            moveAction.moveUp(new Queue.Item[] {F.getQueueItem()}, queue);
            queue.maintain();
            assertEquals(F.getDisplayName(), queue.getItems()[0].task.getDisplayName());

            // same again, it is already on top - should be no change, no error
            moveAction.moveUp(new Queue.Item[] {F.getQueueItem()}, queue);
            queue.maintain();
            assertEquals(F.getDisplayName(), queue.getItems()[0].task.getDisplayName());

            // array of two neighboring elements
            moveAction.moveUp(new Queue.Item[] {E.getQueueItem(), D.getQueueItem()}, queue);
            queue.maintain();
            assertEquals(C.getDisplayName(), queue.getItems()[5].task.getDisplayName());
            assertEquals(G.getDisplayName(), queue.getItems()[4].task.getDisplayName());
            assertEquals(D.getDisplayName(), queue.getItems()[3].task.getDisplayName());
            assertEquals(E.getDisplayName(), queue.getItems()[2].task.getDisplayName());
            assertEquals(H.getDisplayName(), queue.getItems()[1].task.getDisplayName());
            assertEquals(F.getDisplayName(), queue.getItems()[0].task.getDisplayName());

            // array of two distant elements, both will be over the top-most "other" element
            // (H now over E) and move closer to tip (F): FHEDGC => FEGHDC
            moveAction.moveUp(new Queue.Item[] {E.getQueueItem(), G.getQueueItem()}, queue);
            queue.maintain();
            assertEquals(C.getDisplayName(), queue.getItems()[5].task.getDisplayName());
            assertEquals(D.getDisplayName(), queue.getItems()[4].task.getDisplayName());
            assertEquals(H.getDisplayName(), queue.getItems()[3].task.getDisplayName());
            assertEquals(G.getDisplayName(), queue.getItems()[2].task.getDisplayName());
            assertEquals(E.getDisplayName(), queue.getItems()[1].task.getDisplayName());
            assertEquals(F.getDisplayName(), queue.getItems()[0].task.getDisplayName());

            // array of same now-neighboring elements moving one up (so to top)
            moveAction.moveUp(new Queue.Item[] {E.getQueueItem(), G.getQueueItem()}, queue);
            queue.maintain();
            assertEquals(C.getDisplayName(), queue.getItems()[5].task.getDisplayName());
            assertEquals(D.getDisplayName(), queue.getItems()[4].task.getDisplayName());
            assertEquals(H.getDisplayName(), queue.getItems()[3].task.getDisplayName());
            assertEquals(F.getDisplayName(), queue.getItems()[2].task.getDisplayName());
            assertEquals(G.getDisplayName(), queue.getItems()[1].task.getDisplayName());
            assertEquals(E.getDisplayName(), queue.getItems()[0].task.getDisplayName());

            // same array including more distant elements moving one up (so to top)
            moveAction.moveUp(new Queue.Item[] {E.getQueueItem(), G.getQueueItem(), D.getQueueItem()}, queue);
            queue.maintain();
            assertEquals(C.getDisplayName(), queue.getItems()[5].task.getDisplayName());
            assertEquals(H.getDisplayName(), queue.getItems()[4].task.getDisplayName());
            assertEquals(F.getDisplayName(), queue.getItems()[3].task.getDisplayName());
            assertEquals(D.getDisplayName(), queue.getItems()[2].task.getDisplayName());
            assertEquals(G.getDisplayName(), queue.getItems()[1].task.getDisplayName());
            assertEquals(E.getDisplayName(), queue.getItems()[0].task.getDisplayName());

            // EVERYONE (so no-op)
            moveAction.moveUp(
                    new Queue.Item[] {
                        C.getQueueItem(),
                        D.getQueueItem(),
                        E.getQueueItem(),
                        F.getQueueItem(),
                        G.getQueueItem(),
                        H.getQueueItem()
                    },
                    queue);
            queue.maintain();
            assertEquals(C.getDisplayName(), queue.getItems()[5].task.getDisplayName());
            assertEquals(H.getDisplayName(), queue.getItems()[4].task.getDisplayName());
            assertEquals(F.getDisplayName(), queue.getItems()[3].task.getDisplayName());
            assertEquals(D.getDisplayName(), queue.getItems()[2].task.getDisplayName());
            assertEquals(G.getDisplayName(), queue.getItems()[1].task.getDisplayName());
            assertEquals(E.getDisplayName(), queue.getItems()[0].task.getDisplayName());

        } catch (Exception e) {
            fail();
        }
    }
}
