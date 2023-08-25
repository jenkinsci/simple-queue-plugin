package cz.mendelu.xotradov.test.basic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.logging.Logger;

import cz.mendelu.xotradov.MoveAction;
import cz.mendelu.xotradov.test.TestHelper;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Queue;
import hudson.model.queue.QueueTaskFuture;
import jenkins.model.Jenkins;

public class BasicTest_oneBuildSuccessTest {
    public static Logger logger = Logger.getLogger(BasicTest_oneBuildSuccessTest.class.getName());
    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();
    private TestHelper helper = new TestHelper(jenkinsRule);

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