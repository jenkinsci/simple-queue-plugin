package cz.mendelu.xotradov;

import static org.junit.jupiter.api.Assertions.*;

import cz.mendelu.xotradov.test.TestHelper;
import org.htmlunit.html.DomNode;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
public class UITest {

    private TestHelper helper;

    @AfterEach
    public void waitForClean() throws Exception {
        helper.cleanup();
    }

    @Test
    public void HoverText(JenkinsRule jenkinsRule) throws Exception {
        helper = new TestHelper(jenkinsRule);
        long maxTestTime = 30000;
        helper.fillQueueFor(maxTestTime);
        helper.createAndSchedule("C", maxTestTime);
        helper.createAndSchedule("D", maxTestTime);
        HtmlPage page = jenkinsRule.createWebClient().goTo("");
        DomNode queueElement = page.getElementById("buildSimpleQueue");
        DomNode div = queueElement.getChildren().iterator().next().getNextSibling();
        DomNode a = div.getFirstChild()
                .getFirstChild()
                .getFirstChild()
                .getFirstChild()
                .getFirstChild();
        assertFalse(a.asXml().contains("WaitingFor"));
        assertTrue(a.asXml().contains("sec"));
    }

    @Test
    public void initWidgetTest(JenkinsRule jenkinsRule) throws Exception {
        helper = new TestHelper(jenkinsRule);
        long maxTestTime = 30000;
        helper.fillQueueFor(maxTestTime);
        helper.createAndSchedule("C", maxTestTime);
        HtmlPage page = jenkinsRule.createWebClient().goTo("");
        DomNode queueDefaultElement = page.getElementById("buildQueue");
        DomNode queueSimpleElement = page.getElementById("buildSimpleQueue");
        assertNull(queueDefaultElement);
        assertNotNull(queueSimpleElement);
    }
}
