package cz.mendelu.xotradov.test.moves;

import cz.mendelu.xotradov.MoveAction;
import cz.mendelu.xotradov.MoveType;
import cz.mendelu.xotradov.test.TestHelper;
import hudson.model.Action;
import hudson.model.FreeStyleProject;

import hudson.model.Queue;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;
import org.mockito.Mockito;

import java.util.List;

import static cz.mendelu.xotradov.MoveAction.ITEM_ID_PARAM_NAME;
import static cz.mendelu.xotradov.MoveAction.MOVE_TYPE_PARAM_NAME;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

public class MoveAction_doMoveTest {

    @Rule
    public final JenkinsRule jenkinsRule = new JenkinsRule();
    public final TestHelper helper = new TestHelper(jenkinsRule);


    @After
    public void waitForClean() throws Exception {
        jenkinsRule.jenkins.getQueue().clear();
        jenkinsRule.waitUntilNoActivity();
    }

    @Test
    public void doMoveByQueueItemId() throws Exception {
        try {
            long maxTestTime = 10000;
            helper.fillQueueFor(maxTestTime);
            FreeStyleProject C = helper.createAndSchedule("C", maxTestTime);
            FreeStyleProject D = helper.createAndSchedule("D", maxTestTime);
            Queue queue = jenkinsRule.jenkins.getQueue();

            assertEquals(D.getDisplayName(), queue.getItems()[0].task.getDisplayName());
            assertEquals(C.getDisplayName(), queue.getItems()[1].task.getDisplayName());

            List<Action> list = jenkinsRule.jenkins.getActions();
            MoveAction moveAction = helper.getMoveAction();
            StaplerRequest2 request = Mockito.mock(StaplerRequest2.class);
            StaplerResponse2 response = Mockito.mock(StaplerResponse2.class);
            when(request.getParameter(MOVE_TYPE_PARAM_NAME)).thenReturn(MoveType.UP.toString());
            when(request.getParameter(ITEM_ID_PARAM_NAME)).thenReturn(
                    String.valueOf(queue.getItems()[1].getId()));
            moveAction.doMove(request, response);

            // We asked to move C up (lower its priority, closer to [0]), so it
            // becomes just above whoever was at just one point lower priority (D):
            assertEquals(C.getDisplayName(), queue.getItems()[0].task.getDisplayName());
            assertEquals(D.getDisplayName(), queue.getItems()[1].task.getDisplayName());
        } catch (Exception e) {
            fail();
        }
    }

}
