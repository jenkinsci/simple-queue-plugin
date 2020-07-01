package cz.mendelu.xotradov;

import hudson.model.Action;
import hudson.model.TransientViewActionFactory;
import hudson.model.View;
import jenkins.model.TransientActionFactory;
import java.util.Collections;
import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;

public class TVAF extends TransientViewActionFactory {

    @Override
    public List<Action> createFor(View view) {
        return Collections.singletonList(new SimpleQueueUpdateAction());
    }
}
