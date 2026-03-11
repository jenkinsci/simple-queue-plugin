package cz.mendelu.xotradov.test.moves;

import static cz.mendelu.xotradov.MoveActionWorker.ITEM_ID_PARAM_MODE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import static cz.mendelu.xotradov.MoveAction.ITEM_ID_PARAM_NAME;
import static cz.mendelu.xotradov.MoveAction.MOVE_TYPE_PARAM_NAME;
import static cz.mendelu.xotradov.MoveAction.VIEW_NAME_PARAM_NAME;

import hudson.model.Queue;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.mockito.Mockito;

import java.util.ArrayList;
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

    @After
    public void waitForClean() throws Exception {
        jenkinsRule.jenkins.getQueue().clear();
        jenkinsRule.waitUntilNoActivity();
    }

    @Test
    public void doMoveByName() throws Exception {
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
            StaplerRequest request = Mockito.mock(StaplerRequest.class);
            StaplerResponse response = Mockito.mock(StaplerResponse.class);
            when(request.getParameter(MOVE_TYPE_PARAM_NAME)).thenReturn(MoveType.UP.toString());
            when(request.getParameter(VIEW_NAME_PARAM_NAME)).thenReturn("all");
            when(request.getParameter(ITEM_ID_PARAM_NAME)).thenReturn(
                    String.valueOf(queue.getItems()[1].task.getDisplayName()));
            moveAction.doMove(request, response);

            // We asked to move C up (lower its priority, closer to [0]), so it
            // becomes just above whoever was at just one point lower priority (D):
            assertEquals(C.getDisplayName(), queue.getItems()[0].task.getDisplayName());
            assertEquals(D.getDisplayName(), queue.getItems()[1].task.getDisplayName());
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void doMoveByNameManyByRegexMatches() throws Exception {
        try {
            long maxTestTime = 10000;
            helper.fillQueueFor(maxTestTime);
            FreeStyleProject C = helper.createAndSchedule("C", maxTestTime);
            FreeStyleProject D = helper.createAndSchedule("D", maxTestTime);
            FreeStyleProject E = helper.createAndSchedule("E", maxTestTime);
            FreeStyleProject F = helper.createAndSchedule("F", maxTestTime);
            FreeStyleProject G = helper.createAndSchedule("G", maxTestTime);
            FreeStyleProject H = helper.createAndSchedule("H", maxTestTime);

            Queue queue = jenkinsRule.jenkins.getQueue();

            assertEquals(H.getDisplayName(), queue.getItems()[0].task.getDisplayName());
            assertEquals(G.getDisplayName(), queue.getItems()[1].task.getDisplayName());
            assertEquals(F.getDisplayName(), queue.getItems()[2].task.getDisplayName());
            assertEquals(E.getDisplayName(), queue.getItems()[3].task.getDisplayName());
            assertEquals(D.getDisplayName(), queue.getItems()[4].task.getDisplayName());
            assertEquals(C.getDisplayName(), queue.getItems()[5].task.getDisplayName());

            List<Action> list = jenkinsRule.jenkins.getActions();
            MoveAction moveAction = helper.getMoveAction();
            StaplerRequest request = Mockito.mock(StaplerRequest.class);
            StaplerResponse response = Mockito.mock(StaplerResponse.class);
            when(request.getParameter(MOVE_TYPE_PARAM_NAME)).thenReturn(MoveType.UP.toString());
            // Use direct Java regex syntax for Matcher::find() :
            when(request.getParameter(ITEM_ID_PARAM_MODE)).thenReturn("regex");
            when(request.getParameter(ITEM_ID_PARAM_NAME)).thenReturn(".*(?i)[Fd](?-i).*");

            // Debugging aid:
            ArrayList<String> namesBefore = new ArrayList<>();
            for (Queue.Item item: queue.getItems())
                namesBefore.add(item.getDisplayName());

            moveAction.doMove(request, response);

            // Debugging aid:
            ArrayList<String> namesAfter = new ArrayList<>();
            for (Queue.Item item: queue.getItems())
                namesAfter.add(item.getDisplayName());

            // We asked to move F and D up (lower their priority, closer to [0]), so
            // they become just above whoever was at just one point lower priority
            // than the lowest-prio of these two (G):
            assertEquals(H.getDisplayName(), queue.getItems()[0].task.getDisplayName());
            assertEquals(F.getDisplayName(), queue.getItems()[1].task.getDisplayName());
            assertEquals(D.getDisplayName(), queue.getItems()[2].task.getDisplayName());
            assertEquals(G.getDisplayName(), queue.getItems()[3].task.getDisplayName());
            assertEquals(E.getDisplayName(), queue.getItems()[4].task.getDisplayName());
            assertEquals(C.getDisplayName(), queue.getItems()[5].task.getDisplayName());
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void doMoveByNameManyByRegexFind() throws Exception {
        try {
            long maxTestTime = 10000;
            helper.fillQueueFor(maxTestTime);
            FreeStyleProject C = helper.createAndSchedule("C", maxTestTime);
            FreeStyleProject D = helper.createAndSchedule("D", maxTestTime);
            FreeStyleProject E = helper.createAndSchedule("E", maxTestTime);
            FreeStyleProject F = helper.createAndSchedule("F", maxTestTime);
            FreeStyleProject G = helper.createAndSchedule("G", maxTestTime);
            FreeStyleProject H = helper.createAndSchedule("H", maxTestTime);

            Queue queue = jenkinsRule.jenkins.getQueue();

            assertEquals(H.getDisplayName(), queue.getItems()[0].task.getDisplayName());
            assertEquals(G.getDisplayName(), queue.getItems()[1].task.getDisplayName());
            assertEquals(F.getDisplayName(), queue.getItems()[2].task.getDisplayName());
            assertEquals(E.getDisplayName(), queue.getItems()[3].task.getDisplayName());
            assertEquals(D.getDisplayName(), queue.getItems()[4].task.getDisplayName());
            assertEquals(C.getDisplayName(), queue.getItems()[5].task.getDisplayName());

            List<Action> list = jenkinsRule.jenkins.getActions();
            MoveAction moveAction = helper.getMoveAction();
            StaplerRequest request = Mockito.mock(StaplerRequest.class);
            StaplerResponse response = Mockito.mock(StaplerResponse.class);
            when(request.getParameter(MOVE_TYPE_PARAM_NAME)).thenReturn(MoveType.UP.toString());
            // Use ~/.../ syntax for Matcher::matches() :
            when(request.getParameter(ITEM_ID_PARAM_MODE)).thenReturn("regex");
            when(request.getParameter(ITEM_ID_PARAM_NAME)).thenReturn("~/[Fd]/i");

            // Debugging aid:
            ArrayList<String> namesBefore = new ArrayList<>();
            for (Queue.Item item: queue.getItems())
                namesBefore.add(item.getDisplayName());

            moveAction.doMove(request, response);

            // Debugging aid:
            ArrayList<String> namesAfter = new ArrayList<>();
            for (Queue.Item item: queue.getItems())
                namesAfter.add(item.getDisplayName());

            // We asked to move F and D up (lower their priority, closer to [0]), so
            // they become just above whoever was at just one point lower priority
            // than the lowest-prio of these two (G):
            assertEquals(H.getDisplayName(), queue.getItems()[0].task.getDisplayName());
            assertEquals(F.getDisplayName(), queue.getItems()[1].task.getDisplayName());
            assertEquals(D.getDisplayName(), queue.getItems()[2].task.getDisplayName());
            assertEquals(G.getDisplayName(), queue.getItems()[3].task.getDisplayName());
            assertEquals(E.getDisplayName(), queue.getItems()[4].task.getDisplayName());
            assertEquals(C.getDisplayName(), queue.getItems()[5].task.getDisplayName());
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void doMoveByNameManyByRegexFindNoMode() throws Exception {
        try {
            long maxTestTime = 10000;
            helper.fillQueueFor(maxTestTime);
            FreeStyleProject C = helper.createAndSchedule("C", maxTestTime);
            FreeStyleProject D = helper.createAndSchedule("D", maxTestTime);
            FreeStyleProject E = helper.createAndSchedule("E", maxTestTime);
            FreeStyleProject F = helper.createAndSchedule("F", maxTestTime);
            FreeStyleProject G = helper.createAndSchedule("G", maxTestTime);
            FreeStyleProject H = helper.createAndSchedule("H", maxTestTime);

            Queue queue = jenkinsRule.jenkins.getQueue();

            assertEquals(H.getDisplayName(), queue.getItems()[0].task.getDisplayName());
            assertEquals(G.getDisplayName(), queue.getItems()[1].task.getDisplayName());
            assertEquals(F.getDisplayName(), queue.getItems()[2].task.getDisplayName());
            assertEquals(E.getDisplayName(), queue.getItems()[3].task.getDisplayName());
            assertEquals(D.getDisplayName(), queue.getItems()[4].task.getDisplayName());
            assertEquals(C.getDisplayName(), queue.getItems()[5].task.getDisplayName());

            List<Action> list = jenkinsRule.jenkins.getActions();
            MoveAction moveAction = helper.getMoveAction();
            StaplerRequest request = Mockito.mock(StaplerRequest.class);
            StaplerResponse response = Mockito.mock(StaplerResponse.class);
            when(request.getParameter(MOVE_TYPE_PARAM_NAME)).thenReturn(MoveType.UP.toString());
            // Use ~/.../ syntax for Matcher::matches() :
            when(request.getParameter(ITEM_ID_PARAM_NAME)).thenReturn("~/[Fd]/i");

            // Debugging aid:
            ArrayList<String> namesBefore = new ArrayList<>();
            for (Queue.Item item: queue.getItems())
                namesBefore.add(item.getDisplayName());

            moveAction.doMove(request, response);

            // Debugging aid:
            ArrayList<String> namesAfter = new ArrayList<>();
            for (Queue.Item item: queue.getItems())
                namesAfter.add(item.getDisplayName());

            // We did not ask for regex mode, and the string should not have matched
            // anything as an exact name hit, so no changes in the list:
            assertEquals(H.getDisplayName(), queue.getItems()[0].task.getDisplayName());
            assertEquals(G.getDisplayName(), queue.getItems()[1].task.getDisplayName());
            assertEquals(F.getDisplayName(), queue.getItems()[2].task.getDisplayName());
            assertEquals(E.getDisplayName(), queue.getItems()[3].task.getDisplayName());
            assertEquals(D.getDisplayName(), queue.getItems()[4].task.getDisplayName());
            assertEquals(C.getDisplayName(), queue.getItems()[5].task.getDisplayName());
        } catch (Exception e) {
            fail();
        }
    }

}
