package cz.mendelu.xotradov;

import cz.mendelu.xotradov.test.TestHelper;
import hudson.model.Action;
import hudson.model.FreeStyleProject;
import hudson.model.Queue;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.mockito.Mockito;
import java.util.List;

import static cz.mendelu.xotradov.MoveAction.ITEM_ID_PARAM_NAME;
import static cz.mendelu.xotradov.MoveAction.MOVE_TYPE_PARAM_NAME;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MoveActionTest {

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();
    TestHelper helper = new TestHelper(jenkinsRule);

    @Test
    public void doMove() throws Exception {
        long maxTestTime = 10000;
        helper.fillQueueFor(maxTestTime);
        FreeStyleProject C = helper.createAndSchedule("C",maxTestTime);
        FreeStyleProject D = helper.createAndSchedule("D",maxTestTime);
        assertEquals(C.getDisplayName(),jenkinsRule.jenkins.getQueue().getItems()[1].task.getDisplayName());
        List<Action> list = jenkinsRule.jenkins.getActions();
        MoveAction moveAction = null;
        for (Action action: list){
            if (action instanceof MoveAction){
                moveAction = (MoveAction) action;
                break;
            }
        }
        assertNotNull(moveAction);
        StaplerRequest request = Mockito.mock(StaplerRequest.class);
        StaplerResponse response = Mockito.mock(StaplerResponse.class);
        when(request.getParameter(MOVE_TYPE_PARAM_NAME)).thenReturn(MoveType.UP.toString());
        when(request.getParameter(ITEM_ID_PARAM_NAME))
                .thenReturn(String.valueOf(jenkinsRule.jenkins.getQueue().getItems()[1].getId()));
        moveAction.doMove(request,response);
        assertEquals(C.getDisplayName(),jenkinsRule.jenkins.getQueue().getItems()[0].task.getDisplayName());
        assertEquals(D.getDisplayName(),jenkinsRule.jenkins.getQueue().getItems()[1].task.getDisplayName());
    }

    @Test
    public void moveDown() throws Exception {
        long maxTestTime = 10000;
        helper.fillQueueFor(maxTestTime);
        FreeStyleProject C = helper.createAndSchedule("C",maxTestTime);
        FreeStyleProject D = helper.createAndSchedule("D",maxTestTime);
        MoveAction moveAction = null;
        for (Action action: jenkinsRule.jenkins.getActions()){
            if (action instanceof MoveAction){
                moveAction = (MoveAction) action;
                break;
            }
        }
        assertNotNull(moveAction);
        assertNotNull(C.getQueueItem());
        Queue queue =jenkinsRule.jenkins.getQueue();
        assertEquals(C.getDisplayName(),queue.getItems()[1].task.getDisplayName());
        moveAction.moveDown(D.getQueueItem(),queue);
        queue.maintain();
        assertEquals(C.getDisplayName(),queue.getItems()[0].task.getDisplayName());
    }

    @Test
    public void moveUp() throws Exception {
        long maxTestTime = 10000;
        helper.fillQueueFor(maxTestTime);
        FreeStyleProject C = helper.createAndSchedule("C",maxTestTime);
        FreeStyleProject D = helper.createAndSchedule("D",maxTestTime);
        MoveAction moveAction = null;
        for (Action action: jenkinsRule.jenkins.getActions()){
            if (action instanceof MoveAction){
                moveAction = (MoveAction) action;
                break;
            }
        }
        assertNotNull(moveAction);
        assertNotNull(C.getQueueItem());
        Queue queue = jenkinsRule.jenkins.getQueue();
        assertEquals(C.getDisplayName(),queue.getItems()[1].task.getDisplayName());
        moveAction.moveUp(C.getQueueItem(),queue);
        queue.maintain();
        assertEquals(C.getDisplayName(),queue.getItems()[0].task.getDisplayName());
    }

    @Test
    public void moveToTop() {
        try {
            long maxTestTime = 20000;
            helper.fillQueueFor(maxTestTime);
            FreeStyleProject C = helper.createAndSchedule("C",maxTestTime);
            FreeStyleProject D = helper.createAndSchedule("D",maxTestTime);
            FreeStyleProject E = helper.createAndSchedule("E",maxTestTime);
            MoveAction moveAction = null;
            for (Action action: jenkinsRule.jenkins.getActions()){
                if (action instanceof MoveAction){
                    moveAction = (MoveAction) action;
                    break;
                }
            }
            Queue queue = jenkinsRule.jenkins.getQueue();
            assertEquals(C.getDisplayName(),queue.getItems()[2].task.getDisplayName());
            moveAction.moveToTop(C.getQueueItem(),queue);
            queue.maintain();
            assertEquals(C.getDisplayName(),queue.getItems()[0].task.getDisplayName());
        }catch (Exception e){
            fail();
        }
    }

    @Test
    public void moveToBottom() {
        try {
            long maxTestTime = 20000;
            helper.fillQueueFor(maxTestTime);
            FreeStyleProject C = helper.createAndSchedule("C",maxTestTime);
            FreeStyleProject D = helper.createAndSchedule("D",maxTestTime);
            FreeStyleProject E = helper.createAndSchedule("E",maxTestTime);
            MoveAction moveAction = null;
            for (Action action: jenkinsRule.jenkins.getActions()){
                if (action instanceof MoveAction){
                    moveAction = (MoveAction) action;
                    break;
                }
            }
            Queue queue = jenkinsRule.jenkins.getQueue();
            assertEquals(E.getDisplayName(),queue.getItems()[0].task.getDisplayName());
            moveAction.moveToBottom(C.getQueueItem(),queue);
            queue.maintain();
            assertEquals(C.getDisplayName(),queue.getItems()[2].task.getDisplayName());
        }catch (Exception e){
            fail();
        }
    }
}