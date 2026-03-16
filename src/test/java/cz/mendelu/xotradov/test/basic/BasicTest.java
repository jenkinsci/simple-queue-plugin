package cz.mendelu.xotradov.test.basic;

import static org.junit.jupiter.api.Assertions.assertTrue;

import cz.mendelu.xotradov.SimpleQueueWidget;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
public class BasicTest {

    @Test
    public void widgetPresenceTest(JenkinsRule jenkinsRule) {
        assertTrue(jenkinsRule.jenkins.getPrimaryView().getWidgets().stream()
                .anyMatch(SimpleQueueWidget.class::isInstance));
    }
}
