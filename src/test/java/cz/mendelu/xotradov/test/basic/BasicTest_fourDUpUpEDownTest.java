package cz.mendelu.xotradov.test.basic;

import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.logging.Logger;

import cz.mendelu.xotradov.MoveAction;
import cz.mendelu.xotradov.test.TestHelper;
import hudson.model.FreeStyleProject;
import hudson.model.Queue;

public class BasicTest_fourDUpUpEDownTest {
    public static Logger logger = Logger.getLogger(BasicTest_fourDUpUpEDownTest.class.getName());
    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();
    private TestHelper helper = new TestHelper(jenkinsRule);


    @Test
    public void fourDUpUpEDown() throws Exception {
        helper.fillQueueFor(30000);
        FreeStyleProject projectC = helper.createAndSchedule("projectC",25000);
        FreeStyleProject projectD = helper.createAndSchedule("projectD",25000);
        FreeStyleProject projectE = helper.createAndSchedule("projectE",25000);
        FreeStyleProject projectF = helper.createAndSchedule("projectF",25000);
        Queue queue = Queue.getInstance();
        MoveAction moveAction = (MoveAction)jenkinsRule.jenkins.getActions().get(1);
        assertEquals(projectD.getDisplayName(),queue.getItems()[2].task.getDisplayName());
        moveAction.moveUp(queue.getItems()[2],queue);//D
        queue.maintain();
        assertEquals(projectD.getDisplayName(),queue.getItems()[1].task.getDisplayName());
        moveAction.moveUp(queue.getItems()[1],queue);//D
        queue.maintain();
        assertEquals(projectE.getDisplayName(),queue.getItems()[2].task.getDisplayName());
        moveAction.moveDown(queue.getItems()[2],queue);//E
        queue.maintain();
        assertEquals(projectD.getDisplayName(),queue.getItems()[0].task.getDisplayName());
        assertEquals(projectF.getDisplayName(),queue.getItems()[1].task.getDisplayName());
        assertEquals(projectC.getDisplayName(),queue.getItems()[2].task.getDisplayName());
        assertEquals(projectE.getDisplayName(),queue.getItems()[3].task.getDisplayName());
    }

}
