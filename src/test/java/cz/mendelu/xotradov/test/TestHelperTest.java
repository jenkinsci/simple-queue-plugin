package cz.mendelu.xotradov.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import cz.mendelu.xotradov.MoveAction;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
public class TestHelperTest {

    private TestHelper helper;

    @Test
    public void getMoveAction(JenkinsRule jenkinsRule) {
        helper = new TestHelper(jenkinsRule);
        MoveAction moveAction = helper.getMoveAction();
        assertNotNull(moveAction);
    }
}
