package cz.mendelu.xotradov.test.moves;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import cz.mendelu.xotradov.MoveAction;
import cz.mendelu.xotradov.test.TestHelper;
import hudson.model.FreeStyleProject;
import hudson.model.Queue;
import java.util.ArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
public class MoveAction_moveDownTest {

    private TestHelper helper;

    @AfterEach
    public void waitForClean() throws Exception {
        helper.cleanup();
    }

    @Test
    public void moveDown(JenkinsRule jenkinsRule) throws Exception {
        helper = new TestHelper(jenkinsRule);
        try {
            long maxTestTime = 10000;
            helper.fillQueueFor(maxTestTime);
            FreeStyleProject C = helper.createAndSchedule("C", maxTestTime);
            FreeStyleProject D = helper.createAndSchedule("D", maxTestTime);
            MoveAction moveAction = helper.getMoveAction();
            assertNotNull(moveAction);
            assertNotNull(C.getQueueItem());
            Queue queue = jenkinsRule.jenkins.getQueue();
            assertEquals(C.getDisplayName(), queue.getItems()[1].task.getDisplayName());
            moveAction.moveDown(D.getQueueItem(), queue);
            queue.maintain();
            assertEquals(C.getDisplayName(), queue.getItems()[0].task.getDisplayName());
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void moveDownMany(JenkinsRule jenkinsRule) throws Exception {
        helper = new TestHelper(jenkinsRule);
        try {
            long maxTestTime = 10000;
            helper.fillQueueFor(maxTestTime);
            FreeStyleProject C = helper.createAndSchedule("C", maxTestTime);
            FreeStyleProject G = helper.createAndSchedule("G", maxTestTime);
            FreeStyleProject D = helper.createAndSchedule("D", maxTestTime);
            FreeStyleProject A = helper.createAndSchedule("A", maxTestTime);
            FreeStyleProject E = helper.createAndSchedule("E", maxTestTime);
            FreeStyleProject F = helper.createAndSchedule("F", maxTestTime);
            FreeStyleProject B = helper.createAndSchedule("B", maxTestTime);
            MoveAction moveAction = helper.getMoveAction();
            assertNotNull(moveAction);
            assertNotNull(C.getQueueItem());
            Queue queue = jenkinsRule.jenkins.getQueue();

            // Debugging aid:
            ArrayList<String> namesBefore = new ArrayList<>();
            for (Queue.Item item : queue.getItems()) namesBefore.add(item.getDisplayName());

            // least important [0](B newest queued) => most important [6](C oldest queued)
            assertEquals(B.getDisplayName(), queue.getItems()[0].task.getDisplayName());
            assertEquals(F.getDisplayName(), queue.getItems()[1].task.getDisplayName());
            assertEquals(E.getDisplayName(), queue.getItems()[2].task.getDisplayName());
            assertEquals(A.getDisplayName(), queue.getItems()[3].task.getDisplayName());
            assertEquals(D.getDisplayName(), queue.getItems()[4].task.getDisplayName());
            assertEquals(G.getDisplayName(), queue.getItems()[5].task.getDisplayName());
            assertEquals(C.getDisplayName(), queue.getItems()[6].task.getDisplayName());

            moveAction.moveDown(new Queue.Item[] {D.getQueueItem(), E.getQueueItem()}, queue);
            queue.maintain();

            // Debugging aid:
            ArrayList<String> namesAfter = new ArrayList<>();
            for (Queue.Item item : queue.getItems()) namesAfter.add(item.getDisplayName());

            // Moved D+E to be under (higher prio) whoever was just above them (G)
            assertEquals(B.getDisplayName(), queue.getItems()[0].task.getDisplayName());
            assertEquals(F.getDisplayName(), queue.getItems()[1].task.getDisplayName());
            assertEquals(A.getDisplayName(), queue.getItems()[2].task.getDisplayName());
            assertEquals(G.getDisplayName(), queue.getItems()[3].task.getDisplayName());
            assertEquals(E.getDisplayName(), queue.getItems()[4].task.getDisplayName());
            assertEquals(D.getDisplayName(), queue.getItems()[5].task.getDisplayName());
            assertEquals(C.getDisplayName(), queue.getItems()[6].task.getDisplayName());
        } catch (Exception e) {
            fail();
        }
    }
}
