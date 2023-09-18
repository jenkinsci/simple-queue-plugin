package cz.mendelu.xotradov;

import cz.mendelu.xotradov.test.TestHelper;
import hudson.model.FreeStyleProject;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.mockito.Mockito;

import static org.junit.Assert.*;

public class ResetActionTest {

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();
    private TestHelper helper = new TestHelper(jenkinsRule);

    @After
    public void waitForClean() throws Exception {
        jenkinsRule.jenkins.getQueue().clear();
        jenkinsRule.waitUntilNoActivity();
    }

    @Test
    public void doReset() throws Exception {
        ResetAction resetAction = helper.getResetAction();
        StaplerRequest request =  Mockito.mock(StaplerRequest.class);
        StaplerResponse response = Mockito.mock(StaplerResponse.class);
        helper.fillQueueFor(20000);
        FreeStyleProject c = helper.createAndSchedule("C",20000);
        helper.createAndSchedule("D",20000);
        assertEquals(c.getDisplayName(),jenkinsRule.jenkins.getQueue().getItems()[1].task.getDisplayName());
        helper.getMoveAction().moveUp(c.getQueueItem(),jenkinsRule.jenkins.getQueue());
        jenkinsRule.jenkins.getQueue().maintain();
        assertEquals(c.getDisplayName(),jenkinsRule.jenkins.getQueue().getItems()[0].task.getDisplayName());
        resetAction.doReset(request,response);
        assertEquals(c.getDisplayName(),jenkinsRule.jenkins.getQueue().getItems()[1].task.getDisplayName());
    }
}