package cz.mendelu.xotradov;

import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import cz.mendelu.xotradov.test.TestHelper;
import hudson.model.FreeStyleProject;
import hudson.model.Queue;
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
}
