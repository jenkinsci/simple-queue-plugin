package cz.mendelu.xotradov;

import hudson.model.Action;
import hudson.model.TransientViewActionFactory;
import hudson.model.View;
import java.util.Collections;
import java.util.List;

public class TVAF extends TransientViewActionFactory {

    @Override
    public List<Action> createFor(View view) {
        return Collections.singletonList(new SimpleQueueUpdateAction());
    }
}
