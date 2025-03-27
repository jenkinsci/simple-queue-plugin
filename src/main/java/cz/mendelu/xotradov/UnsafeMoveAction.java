package cz.mendelu.xotradov;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.util.logging.Logger;

import javax.annotation.CheckForNull;

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


    public void doMove(final StaplerRequest request, final StaplerResponse response) {
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
