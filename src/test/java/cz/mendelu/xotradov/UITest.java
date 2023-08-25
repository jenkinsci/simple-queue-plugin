package cz.mendelu.xotradov;

import org.htmlunit.html.DomNode;
import org.htmlunit.html.HtmlElement;
import org.htmlunit.html.HtmlPage;
import cz.mendelu.xotradov.test.TestHelper;
import hudson.model.FreeStyleProject;
import hudson.model.Queue;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.w3c.dom.Node;

import static org.junit.Assert.*;

public class UITest {
    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();
    TestHelper helper = new TestHelper(jenkinsRule);

    @Test
    @Ignore("seems to be not supported in newer jenkins")
    public void HoverText() throws Exception{
        long maxTestTime = 30000;
        helper.fillQueueFor(maxTestTime);
        helper.createAndSchedule("C",maxTestTime);
        helper.createAndSchedule("D",maxTestTime);
        HtmlPage page = jenkinsRule.createWebClient().goTo("");
        DomNode queueElement = page.getElementById("buildSimpleQueue");
        DomNode div = queueElement.getChildren().iterator().next().getNextSibling();
        DomNode a = div.getFirstChild().getNextSibling().getFirstChild().getFirstChild().getFirstChild().getFirstChild();
        assertFalse(a.asXml().contains("WaitingFor"));
        assertTrue(a.asXml().contains("sec"));
    }

    @Test
    @Ignore("seems to be not supported in newer jenkins")
    public void initWidgetTest() throws Exception {
        long maxTestTime = 30000;
        helper.fillQueueFor(maxTestTime);
        helper.createAndSchedule("C",maxTestTime);
        HtmlPage page = jenkinsRule.createWebClient().goTo("");
        DomNode queueDefaultElement = page.getElementById("buildQueue");
        DomNode queueSimpleElement = page.getElementById("buildSimpleQueue");
        assertNull(queueDefaultElement);
        assertNotNull(queueSimpleElement);
    }
}
