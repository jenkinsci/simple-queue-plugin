package cz.mendelu.xotradov.test.moves;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;

import cz.mendelu.xotradov.MoveAction;
import cz.mendelu.xotradov.test.TestHelper;
import hudson.model.FreeStyleProject;
import hudson.model.Queue;
import hudson.model.View;

public class MoveAction_moveDownFilteredTest {

    @Rule
    public final JenkinsRule jenkinsRule = new JenkinsRule();
    public final TestHelper helper = new TestHelper(jenkinsRule);

    @After
    public void waitForClean() throws Exception {
        jenkinsRule.jenkins.getQueue().clear();
        jenkinsRule.waitUntilNoActivity();
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

}