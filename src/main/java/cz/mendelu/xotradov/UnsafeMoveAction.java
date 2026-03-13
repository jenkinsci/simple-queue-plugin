package cz.mendelu.xotradov;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.Extension;
import hudson.model.Queue;
import hudson.model.RootAction;
import java.util.logging.Logger;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;

@SuppressWarnings("unused")
@Extension
public class UnsafeMoveAction extends MoveActionWorker implements RootAction {
    private static Logger logger = Logger.getLogger(UnsafeMoveAction.class.getName());

    @CheckForNull
    @Override
    public String getIconFileName() {
        return null;
    }

    @CheckForNull
    @Override
    public String getDisplayName() {
        return null;
    }

    @CheckForNull
    @Override
    public String getUrlName() {
        return "simpleMoveUnsafe";
    }

    public void doMove(final StaplerRequest2 request, final StaplerResponse2 response) {
        if (!SimpleQueueConfig.getInstance().isEnableUnsafe()) {
            throw new IllegalArgumentException("Unsafe reset api attempted without being enabled");
        }
        Jenkins j = Jenkins.get();
        try {
            Queue queue = j.getQueue();
            if (queue != null & j.hasPermission(PermissionHandler.SIMPLE_QUEUE_MOVE_PERMISSION)) {
                moveImpl(request, response, queue, j);
            }
        } catch (Exception e) {
            response.setStatus(StaplerResponse2.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
