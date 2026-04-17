package cz.mendelu.xotradov.test.moves;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import cz.mendelu.xotradov.MoveAction;
import cz.mendelu.xotradov.test.TestHelper;
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
public class MoveAction_doPrintQueueTest {

    private TestHelper helper;

    @AfterEach
    public void waitForClean() throws Exception {
        helper.cleanup();
    }

    @Test
    public void doPrintQueueWithBuildableItems(JenkinsRule jenkinsRule) throws Exception {
        helper = new TestHelper(jenkinsRule);
        try {
            long maxTestTime = 10000;
            helper.fillQueueFor(maxTestTime);
            FreeStyleProject C = helper.createAndSchedule("C", maxTestTime);
            FreeStyleProject D = helper.createAndSchedule("D", maxTestTime);
            Queue queue = jenkinsRule.jenkins.getQueue();

            MoveAction moveAction = helper.getMoveAction();
            StaplerRequest2 request = Mockito.mock(StaplerRequest2.class);
            StaplerResponse2 response = Mockito.mock(StaplerResponse2.class);
            
            StringWriter stringWriter = new StringWriter();
            PrintWriter writer = new PrintWriter(stringWriter);
            when(response.getWriter()).thenReturn(writer);
            when(request.getParameter("buildable")).thenReturn(null); // default to true

            moveAction.doPrintQueue(request, response);
            writer.flush();

            String output = stringWriter.toString();
            assertTrue(output.contains("D"), "Output should contain project D");
            assertTrue(output.contains("C"), "Output should contain project C");
            
            // Verify format - each project on its own line
            String[] lines = output.trim().split("\n");
            assertEquals(2, lines.length, "Should have 2 buildable items");
            assertEquals("D", lines[0].trim());
            assertEquals("C", lines[1].trim());
        } catch (Exception e) {
            fail("Test failed with exception: " + e.getMessage());
        }
    }

    @Test
    public void doPrintQueueWithBuildableParamTrue(JenkinsRule jenkinsRule) throws Exception {
        helper = new TestHelper(jenkinsRule);
        try {
            long maxTestTime = 10000;
            helper.fillQueueFor(maxTestTime);
            FreeStyleProject C = helper.createAndSchedule("C", maxTestTime);
            FreeStyleProject D = helper.createAndSchedule("D", maxTestTime);

            MoveAction moveAction = helper.getMoveAction();
            StaplerRequest2 request = Mockito.mock(StaplerRequest2.class);
            StaplerResponse2 response = Mockito.mock(StaplerResponse2.class);
            
            StringWriter stringWriter = new StringWriter();
            PrintWriter writer = new PrintWriter(stringWriter);
            when(response.getWriter()).thenReturn(writer);
            when(request.getParameter("buildable")).thenReturn("true");

            moveAction.doPrintQueue(request, response);
            writer.flush();

            String output = stringWriter.toString();
            assertTrue(output.contains("D"), "Output should contain project D");
            assertTrue(output.contains("C"), "Output should contain project C");
        } catch (Exception e) {
            fail("Test failed with exception: " + e.getMessage());
        }
    }

    @Test
    public void doPrintQueueWithBuildableParamFalse(JenkinsRule jenkinsRule) throws Exception {
        helper = new TestHelper(jenkinsRule);
        try {
            long maxTestTime = 10000;
            helper.fillQueueFor(maxTestTime);
            FreeStyleProject C = helper.createAndSchedule("C", maxTestTime);
            FreeStyleProject D = helper.createAndSchedule("D", maxTestTime);

            MoveAction moveAction = helper.getMoveAction();
            StaplerRequest2 request = Mockito.mock(StaplerRequest2.class);
            StaplerResponse2 response = Mockito.mock(StaplerResponse2.class);
            
            StringWriter stringWriter = new StringWriter();
            PrintWriter writer = new PrintWriter(stringWriter);
            when(response.getWriter()).thenReturn(writer);
            when(request.getParameter("buildable")).thenReturn("false");

            moveAction.doPrintQueue(request, response);
            writer.flush();

            String output = stringWriter.toString();
            // With buildable=false, should show all items including non-buildable
            assertTrue(output.contains("D"), "Output should contain project D");
            assertTrue(output.contains("C"), "Output should contain project C");
        } catch (Exception e) {
            fail("Test failed with exception: " + e.getMessage());
        }
    }

    @Test
    public void doPrintQueueEmptyQueue(JenkinsRule jenkinsRule) throws Exception {
        helper = new TestHelper(jenkinsRule);
        try {
            // Don't add any items to queue
            Queue queue = jenkinsRule.jenkins.getQueue();
            assertEquals(0, queue.getItems().length, "Queue should be empty");

            MoveAction moveAction = helper.getMoveAction();
            StaplerRequest2 request = Mockito.mock(StaplerRequest2.class);
            StaplerResponse2 response = Mockito.mock(StaplerResponse2.class);
            
            StringWriter stringWriter = new StringWriter();
            PrintWriter writer = new PrintWriter(stringWriter);
            when(response.getWriter()).thenReturn(writer);
            when(request.getParameter("buildable")).thenReturn(null);

            moveAction.doPrintQueue(request, response);
            writer.flush();

            String output = stringWriter.toString();
            assertTrue(output.trim().isEmpty(), "Output should be empty for empty queue");
        } catch (Exception e) {
            fail("Test failed with exception: " + e.getMessage());
        }
    }
}

// Made with Bob
