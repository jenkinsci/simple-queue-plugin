package cz.mendelu.xotradov.test.moves;

import static cz.mendelu.xotradov.MoveAction.MOVE_TYPE_PARAM_NAME;
import static cz.mendelu.xotradov.MoveActionWorker.ITEM_ID_EXT_PARAM_MODE;
import static cz.mendelu.xotradov.MoveActionWorker.ITEM_ID_EXT_PARAM_NAME;
import static cz.mendelu.xotradov.MoveActionWorker.ITEM_ID_EXT_PARAM_TARGET;
import static cz.mendelu.xotradov.MoveActionWorker.ITEM_ID_PARAM_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import cz.mendelu.xotradov.ItemMode;
import cz.mendelu.xotradov.ItemTarget;
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
public class MoveAction_doMoveNewApi {

    private TestHelper helper;

    @AfterEach
    public void waitForClean() throws Exception {
        helper.cleanup();
    }

    @Test
    public void doClash(JenkinsRule jenkinsRule) throws Exception {
        helper = new TestHelper(jenkinsRule);
        List<Integer> results = new ArrayList<>();
        long maxTestTime = 10000;
        helper.fillQueueFor(maxTestTime);
        FreeStyleProject C = helper.createAndSchedule("C", maxTestTime);
        FreeStyleProject D = helper.createAndSchedule("D", maxTestTime);
        Queue queue = jenkinsRule.jenkins.getQueue();

        assertEquals(D.getDisplayName(), queue.getItems()[0].task.getDisplayName());
        assertEquals(C.getDisplayName(), queue.getItems()[1].task.getDisplayName());

        List<Action> list = jenkinsRule.jenkins.getActions();
        MoveAction moveAction = helper.getMoveAction();
        StaplerResponse2 response = Mockito.mock(StaplerResponse2.class);
        doAnswer(invocation -> {
                    int code = invocation.getArgument(0, Integer.class);
                    results.add(code);
                    return null;
                })
                .when(response)
                .setStatus(anyInt());
        StaplerRequest2 request = Mockito.mock(StaplerRequest2.class);
        when(request.getParameter(MOVE_TYPE_PARAM_NAME)).thenReturn(MoveType.UP.toString());
        when(request.getParameter(ITEM_ID_EXT_PARAM_NAME))
                .thenReturn(String.valueOf(queue.getItems()[1].task.getDisplayName()));
        when(request.getParameter(ITEM_ID_PARAM_NAME))
                .thenReturn(String.valueOf(queue.getItems()[1].task.getDisplayName()));

        moveAction.doMove(request, response);
        assertTrue(results.get(0) > 0);
        assertTrue(results.size() == 1);
    }

    @Test
    public void missingParam1(JenkinsRule jenkinsRule) throws Exception {
        helper = new TestHelper(jenkinsRule);
        List<Integer> results = new ArrayList<>();
        long maxTestTime = 10000;
        helper.fillQueueFor(maxTestTime);
        FreeStyleProject C = helper.createAndSchedule("C", maxTestTime);
        FreeStyleProject D = helper.createAndSchedule("D", maxTestTime);
        Queue queue = jenkinsRule.jenkins.getQueue();

        assertEquals(D.getDisplayName(), queue.getItems()[0].task.getDisplayName());
        assertEquals(C.getDisplayName(), queue.getItems()[1].task.getDisplayName());

        List<Action> list = jenkinsRule.jenkins.getActions();
        MoveAction moveAction = helper.getMoveAction();
        StaplerResponse2 response = Mockito.mock(StaplerResponse2.class);
        doAnswer(invocation -> {
                    int code = invocation.getArgument(0, Integer.class);
                    results.add(code);
                    return null;
                })
                .when(response)
                .setStatus(anyInt());
        StaplerRequest2 request = Mockito.mock(StaplerRequest2.class);
        when(request.getParameter(MOVE_TYPE_PARAM_NAME)).thenReturn(MoveType.UP.toString());
        when(request.getParameter(ITEM_ID_EXT_PARAM_NAME))
                .thenReturn(String.valueOf(queue.getItems()[1].task.getDisplayName()));
        moveAction.doMove(request, response);
        assertTrue(results.get(0) > 0); // the exits and messages are chaining. initial issue, final resolution
        assertTrue(results.get(1) > 0);

        when(request.getParameter(ITEM_ID_EXT_PARAM_MODE)).thenReturn(ItemMode.EXACT.toString());
        moveAction.doMove(request, response);
        assertTrue(results.get(2) > 0);
        assertTrue(results.get(3) > 0);
    }

    @Test
    public void missingParam2(JenkinsRule jenkinsRule) throws Exception {
        helper = new TestHelper(jenkinsRule);
        List<Integer> results = new ArrayList<>();
        long maxTestTime = 10000;
        helper.fillQueueFor(maxTestTime);
        FreeStyleProject C = helper.createAndSchedule("C", maxTestTime);
        FreeStyleProject D = helper.createAndSchedule("D", maxTestTime);
        Queue queue = jenkinsRule.jenkins.getQueue();

        assertEquals(D.getDisplayName(), queue.getItems()[0].task.getDisplayName());
        assertEquals(C.getDisplayName(), queue.getItems()[1].task.getDisplayName());

        List<Action> list = jenkinsRule.jenkins.getActions();
        MoveAction moveAction = helper.getMoveAction();
        StaplerResponse2 response = Mockito.mock(StaplerResponse2.class);
        doAnswer(invocation -> {
                    int code = invocation.getArgument(0, Integer.class);
                    results.add(code);
                    return null;
                })
                .when(response)
                .setStatus(anyInt());
        StaplerRequest2 request = Mockito.mock(StaplerRequest2.class);
        when(request.getParameter(MOVE_TYPE_PARAM_NAME)).thenReturn(MoveType.UP.toString());
        when(request.getParameter(ITEM_ID_EXT_PARAM_NAME))
                .thenReturn(String.valueOf(queue.getItems()[1].task.getDisplayName()));
        moveAction.doMove(request, response);
        assertTrue(results.get(0) > 0); // the exits and messages are chaining. initial issue, final resolution
        assertTrue(results.get(1) > 0);

        when(request.getParameterValues(ITEM_ID_EXT_PARAM_TARGET))
                .thenReturn(new String[] {ItemTarget.DISPLAY.toString()});
        moveAction.doMove(request, response);
        assertTrue(results.get(2) > 0);
        assertTrue(results.get(3) > 0);
    }

    @Test
    public void doMoveByName(JenkinsRule jenkinsRule) throws Exception {
        helper = new TestHelper(jenkinsRule);
        List<Integer> results = new ArrayList<>();
        long maxTestTime = 10000;
        helper.fillQueueFor(maxTestTime);
        FreeStyleProject C = helper.createAndSchedule("C", maxTestTime);
        FreeStyleProject D = helper.createAndSchedule("D", maxTestTime);
        Queue queue = jenkinsRule.jenkins.getQueue();

        assertEquals(D.getDisplayName(), queue.getItems()[0].task.getDisplayName());
        assertEquals(C.getDisplayName(), queue.getItems()[1].task.getDisplayName());

        List<Action> list = jenkinsRule.jenkins.getActions();
        MoveAction moveAction = helper.getMoveAction();
        StaplerResponse2 response = Mockito.mock(StaplerResponse2.class);
        doAnswer(invocation -> {
                    int code = invocation.getArgument(0, Integer.class);
                    results.add(code);
                    return null;
                })
                .when(response)
                .setStatus(anyInt());
        StaplerRequest2 request = Mockito.mock(StaplerRequest2.class);
        when(request.getParameter(MOVE_TYPE_PARAM_NAME)).thenReturn(MoveType.UP.toString());
        when(request.getParameter(ITEM_ID_EXT_PARAM_NAME))
                .thenReturn(String.valueOf(queue.getItems()[1].task.getDisplayName()));
        when(request.getParameter(ITEM_ID_EXT_PARAM_MODE)).thenReturn(ItemMode.EXACT.toString());
        when(request.getParameterValues(ITEM_ID_EXT_PARAM_TARGET))
                .thenReturn(new String[] {ItemTarget.DISPLAY.toString()});
        moveAction.doMove(request, response);
        assertEquals(200, (int) (results.get(0)));

        // We asked to move C up (lower its priority, closer to [0]), so it
        // becomes just above whoever was at just one point lower priority (D):
        assertEquals(C.getDisplayName(), queue.getItems()[0].task.getDisplayName());
        assertEquals(D.getDisplayName(), queue.getItems()[1].task.getDisplayName());
    }

    @Test
    public void doMoveByNameManyByRegexMatches(JenkinsRule jenkinsRule) throws Exception {
        helper = new TestHelper(jenkinsRule);
        List<Integer> results = new ArrayList<>();
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
        doAnswer(invocation -> {
                    int code = invocation.getArgument(0, Integer.class);
                    results.add(code);
                    return null;
                })
                .when(response)
                .setStatus(anyInt());
        when(request.getParameter(MOVE_TYPE_PARAM_NAME)).thenReturn(MoveType.UP.toString());
        when(request.getParameter(ITEM_ID_EXT_PARAM_MODE)).thenReturn(ItemMode.REGEX.toString());
        when(request.getParameterValues(ITEM_ID_EXT_PARAM_TARGET))
                .thenReturn(new String[] {ItemTarget.NAME.toString()});
        when(request.getParameter(ITEM_ID_EXT_PARAM_NAME)).thenReturn(".*(?i)[Fd](?-i).*");

        moveAction.doMove(request, response);
        assertEquals(200, (int) (results.get(0)));

        assertEquals(H.getDisplayName(), queue.getItems()[0].task.getDisplayName());
        assertEquals(F.getDisplayName(), queue.getItems()[1].task.getDisplayName());
        assertEquals(D.getDisplayName(), queue.getItems()[2].task.getDisplayName());
        assertEquals(G.getDisplayName(), queue.getItems()[3].task.getDisplayName());
        assertEquals(E.getDisplayName(), queue.getItems()[4].task.getDisplayName());
        assertEquals(C.getDisplayName(), queue.getItems()[5].task.getDisplayName());
    }

    @Test
    public void doMoveByNameManyByRegexFind(JenkinsRule jenkinsRule) throws Exception {
        helper = new TestHelper(jenkinsRule);
        List<Integer> results = new ArrayList<>();
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
        doAnswer(invocation -> {
                    int code = invocation.getArgument(0, Integer.class);
                    results.add(code);
                    return null;
                })
                .when(response)
                .setStatus(anyInt());
        when(request.getParameter(MOVE_TYPE_PARAM_NAME)).thenReturn(MoveType.UP.toString());
        when(request.getParameter(ITEM_ID_EXT_PARAM_MODE)).thenReturn(ItemMode.GREGEX.toString());
        when(request.getParameterValues(ITEM_ID_EXT_PARAM_TARGET))
                .thenReturn(new String[] {ItemTarget.FULLDISPLAY.toString(), ItemTarget.I.toString()});
        when(request.getParameter(ITEM_ID_EXT_PARAM_NAME)).thenReturn("[Fd]");

        moveAction.doMove(request, response);
        assertEquals(200, (int) (results.get(0)));

        assertEquals(H.getDisplayName(), queue.getItems()[0].task.getDisplayName());
        assertEquals(F.getDisplayName(), queue.getItems()[1].task.getDisplayName());
        assertEquals(D.getDisplayName(), queue.getItems()[2].task.getDisplayName());
        assertEquals(G.getDisplayName(), queue.getItems()[3].task.getDisplayName());
        assertEquals(E.getDisplayName(), queue.getItems()[4].task.getDisplayName());
        assertEquals(C.getDisplayName(), queue.getItems()[5].task.getDisplayName());
    }
}
