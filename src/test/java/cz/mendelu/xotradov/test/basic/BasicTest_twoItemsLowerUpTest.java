package cz.mendelu.xotradov.test.basic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.logging.Logger;

import cz.mendelu.xotradov.MoveAction;
import cz.mendelu.xotradov.test.TestHelper;
import hudson.model.FreeStyleProject;
import hudson.model.Queue;
import jenkins.model.Jenkins;

public class BasicTest_twoItemsLowerUpTest {
    public static Logger logger = Logger.getLogger(BasicTest_twoItemsLowerUpTest.class.getName());
    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();
    private TestHelper helper = new TestHelper(jenkinsRule);

    @After
    public void waitForClean() throws Exception {
        jenkinsRule.jenkins.getQueue().clear();
        jenkinsRule.waitUntilNoActivity();
    }

    @Test
    public void twoItemsLowerUpTest() throws Exception {
        helper.fillQueueFor(20000);
        Queue queue = Queue.getInstance();
        //now can be queue filled predictably
        FreeStyleProject projectC = helper.createAndSchedule("projectC", 20000);
        FreeStyleProject projectD = helper.createAndSchedule("projectD",20000);
        while (queue.getBuildableItems().size() != 2){
            Thread.sleep(5);
        }
        assertEquals(projectD.getDisplayName(),queue.getItems()[0].task.getDisplayName());
        assertEquals(projectC.getDisplayName(),queue.getItems()[1].task.getDisplayName());
        assertTrue(jenkinsRule.jenkins.hasPermission(Jenkins.ADMINISTER));
        assertEquals(2,queue.getBuildableItems().size());
        assertEquals(2,queue.getItems().length);
        MoveAction moveAction = (MoveAction)jenkinsRule.jenkins.getActions().get(1);
        moveAction.moveUp(queue.getItems()[1],queue);
        queue.maintain();
        assertEquals(projectC.getDisplayName(),queue.getItems()[0].task.getDisplayName());
        assertEquals(projectD.getDisplayName(),queue.getItems()[1].task.getDisplayName());
    }

}
