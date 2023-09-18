package cz.mendelu.xotradov.test.basic;

import static org.junit.Assert.assertTrue;

import cz.mendelu.xotradov.SimpleQueueWidget;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class BasicTest {
    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();
    @Test
    public void widgetPresenceTest() {
        assertTrue(jenkinsRule.jenkins.getPrimaryView().getWidgets().stream().anyMatch(SimpleQueueWidget.class::isInstance));
    }
}
