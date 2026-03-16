package cz.mendelu.xotradov.test.basic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cz.mendelu.xotradov.MoveAction;
import cz.mendelu.xotradov.test.TestHelper;
import hudson.model.FreeStyleProject;
import hudson.model.Queue;
import java.util.logging.Logger;
import jenkins.model.Jenkins;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
public class BasicTest_backAndForthTest {
    public static Logger logger = Logger.getLogger(BasicTest_backAndForthTest.class.getName());

    private TestHelper helper;

    @AfterEach
    public void waitForClean() throws Exception {
        helper.cleanup();
    }

    @Test
    public void backAndForthTest(JenkinsRule jenkinsRule) throws Exception {
        helper = new TestHelper(jenkinsRule);
        helper.fillQueueFor(20000);
        Queue queue = Queue.getInstance();
        // now can be queue filled predictably
        FreeStyleProject projectC = helper.createAndSchedule("projectC", 20000);
        FreeStyleProject projectD = helper.createAndSchedule("projectD", 20000);
        while (queue.getBuildableItems().size() != 2) {
            Thread.sleep(5);
        }
        assertEquals(projectD.getDisplayName(), queue.getItems()[0].task.getDisplayName());
        assertEquals(projectC.getDisplayName(), queue.getItems()[1].task.getDisplayName());
        assertTrue(jenkinsRule.jenkins.hasPermission(Jenkins.ADMINISTER));
        assertEquals(2, queue.getBuildableItems().size());
        assertEquals(2, queue.getItems().length);
        MoveAction moveAction = helper.getMoveAction();
        moveAction.moveUp(queue.getItems()[1], queue);
        queue.maintain();
        assertEquals(projectC.getDisplayName(), queue.getItems()[0].task.getDisplayName());
        assertEquals(projectD.getDisplayName(), queue.getItems()[1].task.getDisplayName());
        moveAction.moveDown(queue.getItems()[0], queue);
        queue.maintain();
        assertEquals(projectD.getDisplayName(), queue.getItems()[0].task.getDisplayName());
        assertEquals(projectC.getDisplayName(), queue.getItems()[1].task.getDisplayName());
    }
}
