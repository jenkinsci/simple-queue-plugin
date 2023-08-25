package cz.mendelu.xotradov.test.moves;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

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

    @Test
    public void putAOnTopOfB() {
        try {
            long maxTestTime = 30000;
            helper.fillQueueFor(maxTestTime);
            FreeStyleProject C = helper.createAndSchedule("C",maxTestTime);
            FreeStyleProject D = helper.createAndSchedule("D",maxTestTime);
            FreeStyleProject E = helper.createAndSchedule("E",maxTestTime);
            FreeStyleProject F = helper.createAndSchedule("F",maxTestTime);
            MoveAction moveAction = helper.getMoveAction();
            Queue queue = jenkinsRule.jenkins.getQueue();
            assertEquals(C.getDisplayName(),queue.getItems()[3].task.getDisplayName());
            moveAction.putAOnTopOfB(C.getQueueItem(),D.getQueueItem(),queue);
            queue.maintain();
            assertEquals(C.getDisplayName(),queue.getItems()[2].task.getDisplayName());
            assertEquals(D.getDisplayName(),queue.getItems()[3].task.getDisplayName());
            moveAction.putAOnTopOfB(D.getQueueItem(),E.getQueueItem(),queue);
            queue.maintain();
            assertEquals(F.getDisplayName(),queue.getItems()[0].task.getDisplayName());
            assertEquals(D.getDisplayName(),queue.getItems()[1].task.getDisplayName());
            assertEquals(E.getDisplayName(),queue.getItems()[2].task.getDisplayName());
            assertEquals(C.getDisplayName(),queue.getItems()[3].task.getDisplayName());
            moveAction.putAOnTopOfB(E.getQueueItem(),F.getQueueItem(),queue);
            queue.maintain();
            assertEquals(E.getDisplayName(),queue.getItems()[0].task.getDisplayName());
            assertEquals(F.getDisplayName(),queue.getItems()[1].task.getDisplayName());
            assertEquals(D.getDisplayName(),queue.getItems()[2].task.getDisplayName());
            assertEquals(C.getDisplayName(),queue.getItems()[3].task.getDisplayName());
        }catch (Exception e){
            fail();
        }
    }

}