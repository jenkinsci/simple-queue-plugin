package cz.mendelu.xotradov.test.moves;

import static cz.mendelu.xotradov.MoveAction.ITEM_ID_PARAM_NAME;
import static cz.mendelu.xotradov.MoveAction.MOVE_TYPE_PARAM_NAME;
import static cz.mendelu.xotradov.MoveAction.VIEW_NAME_PARAM_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

import cz.mendelu.xotradov.MoveAction;
import cz.mendelu.xotradov.MoveType;
import cz.mendelu.xotradov.test.TestHelper;
import hudson.model.Action;
import hudson.model.FreeStyleProject;
import hudson.model.Queue;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;
import org.mockito.Mockito;

@WithJenkins
public class MoveAction_doMoveByNameTest {

    private TestHelper helper;

    @AfterEach
    public void waitForClean() throws Exception {
        helper.cleanup();
    }

    @Test
    public void doMoveByName(JenkinsRule jenkinsRule) throws Exception {
        helper = new TestHelper(jenkinsRule);
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
            when(request.getParameter(VIEW_NAME_PARAM_NAME)).thenReturn("all");
            when(request.getParameter(ITEM_ID_PARAM_NAME))
                    .thenReturn(String.valueOf(queue.getItems()[1].task.getDisplayName()));
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
    public void doMoveByNameManyByRegexMatches(JenkinsRule jenkinsRule) throws Exception {
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

            Queue queue = jenkinsRule.jenkins.getQueue();

            assertEquals(H.getDisplayName(), queue.getItems()[0].task.getDisplayName());
            assertEquals(G.getDisplayName(), queue.getItems()[1].task.getDisplayName());
            assertEquals(F.getDisplayName(), queue.getItems()[2].task.getDisplayName());
            assertEquals(E.getDisplayName(), queue.getItems()[3].task.getDisplayName());
            assertEquals(D.getDisplayName(), queue.getItems()[4].task.getDisplayName());
            assertEquals(C.getDisplayName(), queue.getItems()[5].task.getDisplayName());

            List<Action> list = jenkinsRule.jenkins.getActions();
            MoveAction moveAction = helper.getMoveAction();
            StaplerRequest2 request = Mockito.mock(StaplerRequest2.class);
            StaplerResponse2 response = Mockito.mock(StaplerResponse2.class);
            when(request.getParameter(MOVE_TYPE_PARAM_NAME)).thenReturn(MoveType.UP.toString());
            // Use direct Java regex syntax for Matcher::find() :
            when(request.getParameter(ITEM_ID_PARAM_NAME)).thenReturn(".*(?i)[Fd](?-i).*");

            // Debugging aid:
            ArrayList<String> namesBefore = new ArrayList<>();
            for (Queue.Item item : queue.getItems()) namesBefore.add(item.getDisplayName());

            moveAction.doMove(request, response);

            // Debugging aid:
            ArrayList<String> namesAfter = new ArrayList<>();
            for (Queue.Item item : queue.getItems()) namesAfter.add(item.getDisplayName());

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
    public void doMoveByNameManyByRegexFind(JenkinsRule jenkinsRule) throws Exception {
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

            Queue queue = jenkinsRule.jenkins.getQueue();

            assertEquals(H.getDisplayName(), queue.getItems()[0].task.getDisplayName());
            assertEquals(G.getDisplayName(), queue.getItems()[1].task.getDisplayName());
            assertEquals(F.getDisplayName(), queue.getItems()[2].task.getDisplayName());
            assertEquals(E.getDisplayName(), queue.getItems()[3].task.getDisplayName());
            assertEquals(D.getDisplayName(), queue.getItems()[4].task.getDisplayName());
            assertEquals(C.getDisplayName(), queue.getItems()[5].task.getDisplayName());

            List<Action> list = jenkinsRule.jenkins.getActions();
            MoveAction moveAction = helper.getMoveAction();
            StaplerRequest2 request = Mockito.mock(StaplerRequest2.class);
            StaplerResponse2 response = Mockito.mock(StaplerResponse2.class);
            when(request.getParameter(MOVE_TYPE_PARAM_NAME)).thenReturn(MoveType.UP.toString());
            // Use ~/.../ syntax for Matcher::matches() :
            when(request.getParameter(ITEM_ID_PARAM_NAME)).thenReturn("~/[Fd]/i");

            // Debugging aid:
            ArrayList<String> namesBefore = new ArrayList<>();
            for (Queue.Item item : queue.getItems()) namesBefore.add(item.getDisplayName());

            moveAction.doMove(request, response);

            // Debugging aid:
            ArrayList<String> namesAfter = new ArrayList<>();
            for (Queue.Item item : queue.getItems()) namesAfter.add(item.getDisplayName());

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
}
