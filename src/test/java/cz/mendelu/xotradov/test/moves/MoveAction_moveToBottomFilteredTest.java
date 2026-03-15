package cz.mendelu.xotradov.test.moves;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

import cz.mendelu.xotradov.MoveAction;
import cz.mendelu.xotradov.test.TestHelper;
import hudson.model.FreeStyleProject;
import hudson.model.Queue;
import hudson.model.View;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.mockito.Mockito;

@WithJenkins
public class MoveAction_moveToBottomFilteredTest {

    private TestHelper helper;

    @AfterEach
    public void waitForClean() throws Exception {
        helper.cleanup();
    }

    @Test
    public void moveToBottomFiltered(JenkinsRule jenkinsRule) {
        helper = new TestHelper(jenkinsRule);
        try {
            long maxTestTime = 20000;
            helper.fillQueueFor(maxTestTime);
            FreeStyleProject C = helper.createAndSchedule("C", maxTestTime);
            FreeStyleProject D = helper.createAndSchedule("D", maxTestTime);
            FreeStyleProject E = helper.createAndSchedule("E", maxTestTime);
            MoveAction moveAction = helper.getMoveAction();
            Queue queue = jenkinsRule.jenkins.getQueue();
            assertEquals(E.getDisplayName(), queue.getItems()[0].task.getDisplayName());
            View view = Mockito.mock(View.class);
            when(view.isFilterQueue()).thenReturn(true);
            List<Queue.Item> list = Arrays.asList(queue.getItems());
            when(view.getQueueItems()).thenReturn(list);
            moveAction.moveToBottomFiltered(E.getQueueItem(), queue, view);
            queue.maintain();
            assertEquals(D.getDisplayName(), queue.getItems()[0].task.getDisplayName());
            assertEquals(C.getDisplayName(), queue.getItems()[1].task.getDisplayName());
            assertEquals(E.getDisplayName(), queue.getItems()[2].task.getDisplayName());
        } catch (Exception e) {
            fail();
        }
    }
}
