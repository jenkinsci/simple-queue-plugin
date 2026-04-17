package cz.mendelu.xotradov;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.Extension;
import hudson.model.*;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;
import org.kohsuke.stapler.interceptor.RequirePOST;

/**
 * Handles request to move one buildable item in a build queue, which is the core functionality of this plugin.
 * @author Jaroslav Otradovec
 */
@SuppressWarnings("unused")
@Extension
public class MoveAction extends MoveActionWorker implements RootAction {

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
        if (Jenkins.get().hasPermission(PermissionHandler.SIMPLE_QUEUE_MOVE_PERMISSION)) {
            return "simpleMove";
        } else {
            return null;
        }
    }

    /**
     * Main method responsible for receiving request from user
     * @param request Stapler request from user
     * @param response Stapler response send back to users browser
     */
    @RequirePOST
    public void doMove(final StaplerRequest2 request, final StaplerResponse2 response) {
        Jenkins j = Jenkins.get();
        if (!j.hasPermission(PermissionHandler.SIMPLE_QUEUE_MOVE_PERMISSION)) {
            response.setStatus(StaplerResponse2.SC_FORBIDDEN);
            return;
        }
        try {
            Queue queue = j.getQueue();
            if (queue != null & j.hasPermission(PermissionHandler.SIMPLE_QUEUE_MOVE_PERMISSION)) {
                moveImpl(request, response, queue, j);
            }
        } catch (Exception e) {
            logger.warning(e.toString());
            response.setStatus(StaplerResponse2.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Print the current queue as plaintext list
     * @param request Stapler request from user
     * @param response Stapler response send back to users browser
     */
    @RequirePOST
    public void doPrintQueue(final StaplerRequest2 request, final StaplerResponse2 response) {
        Jenkins j = Jenkins.get();
        if (!j.hasPermission(PermissionHandler.SIMPLE_QUEUE_MOVE_PERMISSION)) {
            response.setStatus(StaplerResponse2.SC_FORBIDDEN);
            return;
        }
        try {
            Queue queue = j.getQueue();
            if (queue != null) {
                printQueueImpl(request, response, queue, j);
            }
        } catch (Exception e) {
            logger.warning(e.toString());
            response.setStatus(StaplerResponse2.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
