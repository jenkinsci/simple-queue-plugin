package cz.mendelu.xotradov.test.moves;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import cz.mendelu.xotradov.SimpleQueueConfig;
import cz.mendelu.xotradov.UnsafeMoveAction;
import cz.mendelu.xotradov.test.TestHelper;
import hudson.model.Action;
import hudson.model.FreeStyleProject;
import hudson.model.Queue;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;
import org.mockito.Mockito;

@WithJenkins
public class UnsafeMoveAction_doPrintQueueTest {

    private TestHelper helper;

    @AfterEach
    public void waitForClean() throws Exception {
        helper.cleanup();
    }

    private UnsafeMoveAction getUnsafeMoveAction(JenkinsRule jenkinsRule) {
        for (Action action : jenkinsRule.jenkins.getActions()) {
            if (action instanceof UnsafeMoveAction) {
                return (UnsafeMoveAction) action;
            }
        }
        return null;
    }

    @Test
    public void doPrintQueueWithUnsafeEnabled(JenkinsRule jenkinsRule) throws Exception {
        helper = new TestHelper(jenkinsRule);
        try {
            // Enable unsafe API
            SimpleQueueConfig.getInstance().setEnableUnsafe(true);

            long maxTestTime = 10000;
            helper.fillQueueFor(maxTestTime);
            FreeStyleProject C = helper.createAndSchedule("C", maxTestTime);
            FreeStyleProject D = helper.createAndSchedule("D", maxTestTime);
            Queue queue = jenkinsRule.jenkins.getQueue();

            UnsafeMoveAction unsafeMoveAction = getUnsafeMoveAction(jenkinsRule);
            assertNotNull(unsafeMoveAction, "UnsafeMoveAction should be available");

            StaplerRequest2 request = Mockito.mock(StaplerRequest2.class);
            StaplerResponse2 response = Mockito.mock(StaplerResponse2.class);
            
            StringWriter stringWriter = new StringWriter();
            PrintWriter writer = new PrintWriter(stringWriter);
            when(response.getWriter()).thenReturn(writer);
            when(request.getParameter("buildable")).thenReturn(null);

            unsafeMoveAction.doPrintQueue(request, response);
            writer.flush();

            String output = stringWriter.toString();
            assertTrue(output.contains("D"), "Output should contain project D");
            assertTrue(output.contains("C"), "Output should contain project C");
            
            // Verify format
            String[] lines = output.trim().split("\n");
            assertEquals(2, lines.length, "Should have 2 buildable items");
        } catch (Exception e) {
            fail("Test failed with exception: " + e.getMessage());
        } finally {
            SimpleQueueConfig.getInstance().setEnableUnsafe(false);
        }
    }

    @Test
    public void doPrintQueueWithUnsafeDisabled(JenkinsRule jenkinsRule) throws Exception {
        helper = new TestHelper(jenkinsRule);
        try {
            // Disable unsafe API
            SimpleQueueConfig.getInstance().setEnableUnsafe(false);

            long maxTestTime = 10000;
            helper.fillQueueFor(maxTestTime);
            FreeStyleProject C = helper.createAndSchedule("C", maxTestTime);
            FreeStyleProject D = helper.createAndSchedule("D", maxTestTime);

            UnsafeMoveAction unsafeMoveAction = getUnsafeMoveAction(jenkinsRule);
            assertNotNull(unsafeMoveAction, "UnsafeMoveAction should be available");

            StaplerRequest2 request = Mockito.mock(StaplerRequest2.class);
            StaplerResponse2 response = Mockito.mock(StaplerResponse2.class);
            
            StringWriter stringWriter = new StringWriter();
            PrintWriter writer = new PrintWriter(stringWriter);
            when(response.getWriter()).thenReturn(writer);
            when(request.getParameter("buildable")).thenReturn(null);

            // Should throw IllegalArgumentException when unsafe is disabled
            assertThrows(IllegalArgumentException.class, () -> {
                unsafeMoveAction.doPrintQueue(request, response);
            }, "Should throw IllegalArgumentException when unsafe API is disabled");

        } catch (Exception e) {
            fail("Test failed with exception: " + e.getMessage());
        }
    }

    @Test
    public void doPrintQueueWithBuildableParamFalse(JenkinsRule jenkinsRule) throws Exception {
        helper = new TestHelper(jenkinsRule);
        try {
            // Enable unsafe API
            SimpleQueueConfig.getInstance().setEnableUnsafe(true);

            long maxTestTime = 10000;
            helper.fillQueueFor(maxTestTime);
            FreeStyleProject C = helper.createAndSchedule("C", maxTestTime);
            FreeStyleProject D = helper.createAndSchedule("D", maxTestTime);

            UnsafeMoveAction unsafeMoveAction = getUnsafeMoveAction(jenkinsRule);
            assertNotNull(unsafeMoveAction, "UnsafeMoveAction should be available");

            StaplerRequest2 request = Mockito.mock(StaplerRequest2.class);
            StaplerResponse2 response = Mockito.mock(StaplerResponse2.class);
            
            StringWriter stringWriter = new StringWriter();
            PrintWriter writer = new PrintWriter(stringWriter);
            when(response.getWriter()).thenReturn(writer);
            when(request.getParameter("buildable")).thenReturn("false");

            unsafeMoveAction.doPrintQueue(request, response);
            writer.flush();

            String output = stringWriter.toString();
            assertTrue(output.contains("D"), "Output should contain project D");
            assertTrue(output.contains("C"), "Output should contain project C");
        } catch (Exception e) {
            fail("Test failed with exception: " + e.getMessage());
        } finally {
            SimpleQueueConfig.getInstance().setEnableUnsafe(false);
        }
    }

    @Test
    public void doPrintQueueEmptyQueue(JenkinsRule jenkinsRule) throws Exception {
        helper = new TestHelper(jenkinsRule);
        try {
            // Enable unsafe API
            SimpleQueueConfig.getInstance().setEnableUnsafe(true);

            // Don't add any items to queue
            Queue queue = jenkinsRule.jenkins.getQueue();
            assertEquals(0, queue.getItems().length, "Queue should be empty");

            UnsafeMoveAction unsafeMoveAction = getUnsafeMoveAction(jenkinsRule);
            assertNotNull(unsafeMoveAction, "UnsafeMoveAction should be available");

            StaplerRequest2 request = Mockito.mock(StaplerRequest2.class);
            StaplerResponse2 response = Mockito.mock(StaplerResponse2.class);
            
            StringWriter stringWriter = new StringWriter();
            PrintWriter writer = new PrintWriter(stringWriter);
            when(response.getWriter()).thenReturn(writer);
            when(request.getParameter("buildable")).thenReturn(null);

            unsafeMoveAction.doPrintQueue(request, response);
            writer.flush();

            String output = stringWriter.toString();
            assertTrue(output.trim().isEmpty(), "Output should be empty for empty queue");
        } catch (Exception e) {
            fail("Test failed with exception: " + e.getMessage());
        } finally {
            SimpleQueueConfig.getInstance().setEnableUnsafe(false);
        }
    }
}

// Made with Bob
