package cz.mendelu.xotradov.test.basic;

import cz.mendelu.xotradov.test.TestHelper;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Queue;
import hudson.model.queue.QueueTaskFuture;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
public class BasicTest_oneBuildSuccessTest {
    public static Logger logger = Logger.getLogger(BasicTest_oneBuildSuccessTest.class.getName());

    private TestHelper helper;

    @AfterEach
    public void waitForClean() throws Exception {
        helper.cleanup();
    }

    @Test
    public void oneBuildSuccessTest(JenkinsRule jenkinsRule) throws Exception {
        helper = new TestHelper(jenkinsRule);
        FreeStyleProject projectA = helper.createProject("projectA", 1000);
        QueueTaskFuture<FreeStyleBuild> futureA = helper.schedule(projectA);
        while (!Queue.getInstance().getBuildableItems().isEmpty()) {
            Thread.sleep(10);
        }
        jenkinsRule.assertBuildStatusSuccess(futureA);
    }
}
