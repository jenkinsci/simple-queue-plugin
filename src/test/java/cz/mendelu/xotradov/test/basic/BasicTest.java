package cz.mendelu.xotradov.test.basic;

import cz.mendelu.xotradov.MoveAction;
import cz.mendelu.xotradov.SimpleQueueWidget;
import cz.mendelu.xotradov.test.TestHelper;
import hudson.model.*;
import hudson.model.queue.QueueTaskFuture;
import hudson.widgets.Widget;
import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.logging.Logger;

import static org.junit.Assert.*;

public class BasicTest {
    public static Logger logger = Logger.getLogger(BasicTest.class.getName());
    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();
    private TestHelper helper = new TestHelper(jenkinsRule);

    @Test
    public void widgetPresenceTest(){
        boolean presence = false;
        for (Widget widget1: jenkinsRule.jenkins.getWidgets()){
            if (widget1 instanceof SimpleQueueWidget) {
                presence = true;
                break;
            }
        }
        assertTrue(presence);
    }
    }
