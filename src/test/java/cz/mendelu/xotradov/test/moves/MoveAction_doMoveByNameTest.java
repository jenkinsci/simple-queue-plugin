package cz.mendelu.xotradov.test.moves;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import static cz.mendelu.xotradov.MoveAction.ITEM_ID_PARAM_NAME;
import static cz.mendelu.xotradov.MoveAction.MOVE_TYPE_PARAM_NAME;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.mockito.Mockito;

import java.util.List;

import cz.mendelu.xotradov.MoveAction;
import cz.mendelu.xotradov.MoveType;
import cz.mendelu.xotradov.test.TestHelper;
import hudson.model.Action;
import hudson.model.FreeStyleProject;

public class MoveAction_doMoveByNameTest {

    @Rule
    public final JenkinsRule jenkinsRule = new JenkinsRule();
    public final TestHelper helper = new TestHelper(jenkinsRule);



    @Test
    public void doMoveByName() throws Exception {
        try {
            long maxTestTime = 10000;
            helper.fillQueueFor(maxTestTime);
            FreeStyleProject C = helper.createAndSchedule("C", maxTestTime);
            FreeStyleProject D = helper.createAndSchedule("D", maxTestTime);
            assertEquals(C.getDisplayName(), jenkinsRule.jenkins.getQueue().getItems()[1].task.getDisplayName());
            List<Action> list = jenkinsRule.jenkins.getActions();
            MoveAction moveAction = helper.getMoveAction();
            StaplerRequest request = Mockito.mock(StaplerRequest.class);
            StaplerResponse response = Mockito.mock(StaplerResponse.class);
            when(request.getParameter(MOVE_TYPE_PARAM_NAME)).thenReturn(MoveType.UP.toString());
            when(request.getParameter(ITEM_ID_PARAM_NAME)).thenReturn(
                    String.valueOf(jenkinsRule.jenkins.getQueue().getItems()[1].task.getDisplayName()));
            moveAction.doMove(request, response);
            assertEquals(C.getDisplayName(), jenkinsRule.jenkins.getQueue().getItems()[0].task.getDisplayName());
            assertEquals(D.getDisplayName(), jenkinsRule.jenkins.getQueue().getItems()[1].task.getDisplayName());
        } catch (Exception e) {
            fail();
        }
    }

}