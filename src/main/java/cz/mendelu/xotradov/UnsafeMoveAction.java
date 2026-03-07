package cz.mendelu.xotradov;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;

import java.util.logging.Logger;

import hudson.Extension;
import hudson.model.Queue;
import hudson.model.RootAction;
import jenkins.model.Jenkins;

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
        Jenkins j;
        if ((j = Jenkins.getInstanceOrNull()) != null) {
            Queue queue = j.getQueue();
            moveImpl(request, queue, j);
        }
        try {
            response.forwardToPreviousPage(request);
        } catch (Exception e) {
            logger.warning(e.toString());
        }
    }
}
