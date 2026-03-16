package cz.mendelu.xotradov.test.moves;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
public class MoveAction_moveDownFilteredTest {

    private TestHelper helper;

    @AfterEach
    public void waitForClean() throws Exception {
        helper.cleanup();
    }

    @Test
    public void moveDownFiltered(JenkinsRule jenkinsRule) {
        helper = new TestHelper(jenkinsRule);
        try {
            long maxTestTime = 10000;
            helper.fillQueueFor(maxTestTime);
            FreeStyleProject C = helper.createAndSchedule("C", maxTestTime);
            FreeStyleProject D = helper.createAndSchedule("D", maxTestTime);
            FreeStyleProject E = helper.createAndSchedule("E", maxTestTime);
            FreeStyleProject F = helper.createAndSchedule("F", maxTestTime);
            FreeStyleProject G = helper.createAndSchedule("G", maxTestTime);
            MoveAction moveAction = helper.getMoveAction();
            assertNotNull(C.getQueueItem());
            Queue queue = jenkinsRule.jenkins.getQueue();
            assertEquals(F.getDisplayName(), queue.getItems()[1].task.getDisplayName());
            assertEquals(C.getDisplayName(), queue.getItems()[4].task.getDisplayName());
            View view = Mockito.mock(View.class);
            when(view.isFilterQueue()).thenReturn(true);
            List<Queue.Item> list = Arrays.asList(queue.getItems());
            when(view.getQueueItems()).thenReturn(list);
            moveAction.moveDownFiltered(F.getQueueItem(), queue, view);
            queue.maintain();
            assertEquals(F.getDisplayName(), queue.getItems()[2].task.getDisplayName());
        } catch (Exception e) {
            fail();
        }
    }
}
