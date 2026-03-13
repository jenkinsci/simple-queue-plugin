package cz.mendelu.xotradov;

import static org.junit.Assert.*;

import cz.mendelu.xotradov.test.TestHelper;
import org.htmlunit.html.DomNode;
import org.htmlunit.html.HtmlPage;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class UITest {
    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    TestHelper helper = new TestHelper(jenkinsRule);

    @After
    public void waitForClean() throws Exception {
        jenkinsRule.jenkins.getQueue().clear();
        jenkinsRule.waitUntilNoActivity();
    }

    @Test
    public void HoverText() throws Exception {
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
    public void initWidgetTest() throws Exception {
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
