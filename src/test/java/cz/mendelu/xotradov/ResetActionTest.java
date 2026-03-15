package cz.mendelu.xotradov;

import static org.junit.jupiter.api.Assertions.assertEquals;

import cz.mendelu.xotradov.test.TestHelper;
import hudson.model.FreeStyleProject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;
import org.mockito.Mockito;

@WithJenkins
public class ResetActionTest {

    private TestHelper helper;

    @AfterEach
    public void waitForClean() throws Exception {
        helper.cleanup();
    }

    @Test
    public void doReset(JenkinsRule jenkinsRule) throws Exception {
        helper = new TestHelper(jenkinsRule);
        ResetAction resetAction = helper.getResetAction();
        StaplerRequest2 request = Mockito.mock(StaplerRequest2.class);
        StaplerResponse2 response = Mockito.mock(StaplerResponse2.class);
        helper.fillQueueFor(20000);
        FreeStyleProject c = helper.createAndSchedule("C", 20000);
        helper.createAndSchedule("D", 20000);
        assertEquals(
                c.getDisplayName(),
                jenkinsRule.jenkins.getQueue().getItems()[1].task.getDisplayName());
        helper.getMoveAction().moveUp(c.getQueueItem(), jenkinsRule.jenkins.getQueue());
        jenkinsRule.jenkins.getQueue().maintain();
        assertEquals(
                c.getDisplayName(),
                jenkinsRule.jenkins.getQueue().getItems()[0].task.getDisplayName());
        resetAction.doReset(request, response);
        assertEquals(
                c.getDisplayName(),
                jenkinsRule.jenkins.getQueue().getItems()[1].task.getDisplayName());
    }
}
