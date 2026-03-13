package cz.mendelu.xotradov.test.basic;

import cz.mendelu.xotradov.test.TestHelper;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Queue;
import hudson.model.queue.QueueTaskFuture;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class BasicTest_oneBuildSuccessTest {
    public static Logger logger = Logger.getLogger(BasicTest_oneBuildSuccessTest.class.getName());

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    private TestHelper helper = new TestHelper(jenkinsRule);

    @After
    public void waitForClean() throws Exception {
        jenkinsRule.jenkins.getQueue().clear();
        jenkinsRule.waitUntilNoActivity();
    }

    @Test
    public void oneBuildSuccessTest() throws Exception {
        FreeStyleProject projectA = helper.createProject("projectA", 1000);
        QueueTaskFuture<FreeStyleBuild> futureA = helper.schedule(projectA);
        while (!Queue.getInstance().getBuildableItems().isEmpty()) {
            Thread.sleep(10);
        }
        jenkinsRule.assertBuildStatusSuccess(futureA);
    }
}
