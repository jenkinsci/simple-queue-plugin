package cz.mendelu.xotradov.test.basic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import cz.mendelu.xotradov.MoveAction;
import cz.mendelu.xotradov.test.TestHelper;
import hudson.model.FreeStyleProject;
import hudson.model.Queue;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
public class BasicTest_fourFDownDDownCUpUpFDownTest {
    public static Logger logger =
            Logger.getLogger(BasicTest_fourFDownDDownCUpUpFDownTest.class.getName());

    private TestHelper helper;

    @AfterEach
    public void waitForClean() throws Exception {
        helper.cleanup();
    }

    @Test
    public void fourFDownDDownCUpUpFDown(JenkinsRule jenkinsRule) throws Exception {
        helper = new TestHelper(jenkinsRule);
        helper.fillQueueFor(35000);
        FreeStyleProject projectC = helper.createAndSchedule("projectC", 35000);
        FreeStyleProject projectD = helper.createAndSchedule("projectD", 35000);
        FreeStyleProject projectE = helper.createAndSchedule("projectE", 35000);
        FreeStyleProject projectF = helper.createAndSchedule("projectF", 35000);
        Queue queue = Queue.getInstance();
        MoveAction moveAction = helper.getMoveAction();
        assertEquals(projectF.getDisplayName(), queue.getItems()[0].task.getDisplayName());
        moveAction.moveDown(queue.getItems()[0], queue); // F
        queue.maintain();
        assertEquals(projectD.getDisplayName(), queue.getItems()[2].task.getDisplayName());
        moveAction.moveDown(queue.getItems()[2], queue); // D
        queue.maintain();
        assertEquals(projectC.getDisplayName(), queue.getItems()[2].task.getDisplayName());
        moveAction.moveUp(queue.getItems()[2], queue); // C
        queue.maintain();
        assertEquals(projectC.getDisplayName(), queue.getItems()[1].task.getDisplayName());
        moveAction.moveUp(queue.getItems()[1], queue); // C
        queue.maintain();
        assertEquals(projectF.getDisplayName(), queue.getItems()[2].task.getDisplayName());
        moveAction.moveDown(queue.getItems()[2], queue); // F
        queue.maintain();
        assertEquals(projectC.getDisplayName(), queue.getItems()[0].task.getDisplayName());
        assertEquals(projectE.getDisplayName(), queue.getItems()[1].task.getDisplayName());
        assertEquals(projectD.getDisplayName(), queue.getItems()[2].task.getDisplayName());
        assertEquals(projectF.getDisplayName(), queue.getItems()[3].task.getDisplayName());
    }
}
