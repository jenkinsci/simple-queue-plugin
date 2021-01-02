package cz.mendelu.xotradov.test;

import cz.mendelu.xotradov.MoveAction;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.assertNotNull;

public class TestHelperTest {
    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    private TestHelper helper = new TestHelper(jenkinsRule);

    @Test
    public void getMoveAction(){
        MoveAction moveAction = helper.getMoveAction();
        assertNotNull(moveAction);
    }
}
