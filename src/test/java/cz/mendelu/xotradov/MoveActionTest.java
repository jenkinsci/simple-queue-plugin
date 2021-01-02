package cz.mendelu.xotradov;

import cz.mendelu.xotradov.test.TestHelper;
import hudson.model.Action;
import hudson.model.FreeStyleProject;
import hudson.model.Queue;
import hudson.model.View;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;

import static cz.mendelu.xotradov.MoveAction.ITEM_ID_PARAM_NAME;
import static cz.mendelu.xotradov.MoveAction.MOVE_TYPE_PARAM_NAME;
import static org.junit.Assert.*;
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
        MoveAction moveAction = helper.getMoveAction();
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
        MoveAction moveAction = helper.getMoveAction();
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
        MoveAction moveAction = helper.getMoveAction();
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
            MoveAction moveAction = helper.getMoveAction();
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

    @Test
    public void moveToBottom() {
        try {
            long maxTestTime = 20000;
            helper.fillQueueFor(maxTestTime);
            FreeStyleProject C = helper.createAndSchedule("C",maxTestTime);
            FreeStyleProject D = helper.createAndSchedule("D",maxTestTime);
            FreeStyleProject E = helper.createAndSchedule("E",maxTestTime);
            MoveAction moveAction = helper.getMoveAction();
            Queue queue = jenkinsRule.jenkins.getQueue();
            assertEquals(E.getDisplayName(),queue.getItems()[0].task.getDisplayName());
            moveAction.moveToBottom(C.getQueueItem(),queue);
            queue.maintain();
            assertEquals(C.getDisplayName(),queue.getItems()[2].task.getDisplayName());
        }catch (Exception e){
            fail();
        }
    }

    @Test
    public void getTop() {
        try {
            long maxTestTime = 20000;
            helper.fillQueueFor(maxTestTime);
            FreeStyleProject C = helper.createAndSchedule("C",maxTestTime);
            FreeStyleProject D = helper.createAndSchedule("D",maxTestTime);
            FreeStyleProject E = helper.createAndSchedule("E",maxTestTime);
            MoveAction moveAction = helper.getMoveAction();
            Queue queue = jenkinsRule.jenkins.getQueue();
            assertEquals(E.getDisplayName(),queue.getItems()[0].task.getDisplayName());
            assertEquals(E.getDisplayName(),moveAction.getTop(Arrays.asList(queue.getItems())).task.getDisplayName());
            moveAction.moveUp(D.getQueueItem(),queue);
            queue.maintain();
            assertEquals(D.getDisplayName(),moveAction.getTop(Arrays.asList(queue.getItems())).task.getDisplayName());
            moveAction.moveToTop(C.getQueueItem(),queue);
            queue.maintain();
            assertEquals(C.getDisplayName(),moveAction.getTop(Arrays.asList(queue.getItems())).task.getDisplayName());
        }catch (Exception e){
            fail();
        }
    }

    @Test
    public void moveUpFiltered() {
        try {
            long maxTestTime = 20000;
            helper.fillQueueFor(maxTestTime);
            FreeStyleProject C = helper.createAndSchedule("C",maxTestTime);
            FreeStyleProject D = helper.createAndSchedule("D",maxTestTime);
            MoveAction moveAction = helper.getMoveAction();
            assertNotNull(C.getQueueItem());
            Queue queue = jenkinsRule.jenkins.getQueue();
            assertEquals(D.getDisplayName(),queue.getItems()[0].task.getDisplayName());
            assertEquals(C.getDisplayName(),queue.getItems()[1].task.getDisplayName());
            View view = Mockito.mock(View.class);
            when(view.isFilterQueue()).thenReturn(true);
            List<Queue.Item> list = Arrays.asList(queue.getItems());
            when(view.getQueueItems()).thenReturn(list);
            moveAction.moveUpFiltered(C.getQueueItem(),queue,view);
            queue.maintain();
            assertEquals(C.getDisplayName(),queue.getItems()[0].task.getDisplayName());
        }catch (Exception e){
            fail();
        }
    }

    @Test
    public void moveDownFiltered() {
        try {
            long maxTestTime = 10000;
            helper.fillQueueFor(maxTestTime);
            FreeStyleProject C = helper.createAndSchedule("C",maxTestTime);
            FreeStyleProject D = helper.createAndSchedule("D",maxTestTime);
            FreeStyleProject E = helper.createAndSchedule("E",maxTestTime);
            FreeStyleProject F = helper.createAndSchedule("F",maxTestTime);
            FreeStyleProject G = helper.createAndSchedule("G",maxTestTime);
            MoveAction moveAction = helper.getMoveAction();
            assertNotNull(C.getQueueItem());
            Queue queue =jenkinsRule.jenkins.getQueue();
            assertEquals(F.getDisplayName(),queue.getItems()[1].task.getDisplayName());
            assertEquals(C.getDisplayName(),queue.getItems()[4].task.getDisplayName());
            View view = Mockito.mock(View.class);
            when(view.isFilterQueue()).thenReturn(true);
            List<Queue.Item> list = Arrays.asList(queue.getItems());
            when(view.getQueueItems()).thenReturn(list);
            moveAction.moveDownFiltered(F.getQueueItem(),queue,view);
            queue.maintain();
            assertEquals(F.getDisplayName(),queue.getItems()[2].task.getDisplayName());
        }catch (Exception e){
            fail();
        }
    }

    @Test
    public void moveToBottomFiltered() {
        try {
            long maxTestTime = 20000;
            helper.fillQueueFor(maxTestTime);
            FreeStyleProject C = helper.createAndSchedule("C",maxTestTime);
            FreeStyleProject D = helper.createAndSchedule("D",maxTestTime);
            FreeStyleProject E = helper.createAndSchedule("E",maxTestTime);
            MoveAction moveAction = helper.getMoveAction();
            Queue queue = jenkinsRule.jenkins.getQueue();
            assertEquals(E.getDisplayName(),queue.getItems()[0].task.getDisplayName());
            View view = Mockito.mock(View.class);
            when(view.isFilterQueue()).thenReturn(true);
            List<Queue.Item> list = Arrays.asList(queue.getItems());
            when(view.getQueueItems()).thenReturn(list);
            moveAction.moveToBottomFiltered(E.getQueueItem(),queue,view);
            queue.maintain();
            assertEquals(D.getDisplayName(),queue.getItems()[0].task.getDisplayName());
            assertEquals(C.getDisplayName(),queue.getItems()[1].task.getDisplayName());
            assertEquals(E.getDisplayName(),queue.getItems()[2].task.getDisplayName());
        }catch (Exception e){
            fail();
        }
    }

    @Test
    public void getBottom() {
        try {
            long maxTestTime = 20000;
            helper.fillQueueFor(maxTestTime);
            FreeStyleProject C = helper.createAndSchedule("C",maxTestTime);
            FreeStyleProject D = helper.createAndSchedule("D",maxTestTime);
            FreeStyleProject E = helper.createAndSchedule("E",maxTestTime);
            MoveAction moveAction = helper.getMoveAction();
            Queue queue = jenkinsRule.jenkins.getQueue();
            assertEquals(C.getDisplayName(),queue.getItems()[2].task.getDisplayName());
            assertEquals(C.getDisplayName(),moveAction.getBottom(Arrays.asList(queue.getItems())).task.getDisplayName());
            moveAction.moveUp(C.getQueueItem(),queue);
            queue.maintain();
            assertEquals(D.getDisplayName(),moveAction.getBottom(Arrays.asList(queue.getItems())).task.getDisplayName());
            moveAction.moveToBottom(E.getQueueItem(),queue);
            queue.maintain();
            assertEquals(E.getDisplayName(),moveAction.getBottom(Arrays.asList(queue.getItems())).task.getDisplayName());
        }catch (Exception e){
            fail();
        }
    }
}