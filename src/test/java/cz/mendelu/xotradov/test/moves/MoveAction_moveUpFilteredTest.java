package cz.mendelu.xotradov.test.moves;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

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

public class MoveAction_moveUpFilteredTest {

    @Rule
    public final JenkinsRule jenkinsRule = new JenkinsRule();
    public final TestHelper helper = new TestHelper(jenkinsRule);


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

}