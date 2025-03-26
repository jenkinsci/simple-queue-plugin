package cz.mendelu.xotradov;

import hudson.Extension;
import hudson.model.*;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.annotation.CheckForNull;
import org.kohsuke.stapler.interceptor.RequirePOST;

/**
 * Handles request to move one buildable item in a build queue, which is the core functionality of this plugin.
 * @author Jaroslav Otradovec
 */
@SuppressWarnings("unused")
@Extension
public class MoveAction extends MoveActionWorker implements RootAction  {
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
        if (Jenkins.get().hasPermission(PermissionHandler.SIMPLE_QUEUE_MOVE_PERMISSION)){
            return "simpleMove";
        }else {
            return null;
        }
    }

    /**
     * Main method responsible for receiving request from user
     * @param request Stapler request from user
     * @param response Stapler response send back to users browser
     */
    @RequirePOST
    public void doMove(final StaplerRequest request, final StaplerResponse response) {
        Jenkins j;
        if ((j = Jenkins.getInstanceOrNull()) != null) {
            Queue queue = j.getQueue();
            if (queue != null & j.hasPermission(PermissionHandler.SIMPLE_QUEUE_MOVE_PERMISSION)) {
                doMoveImpl(request, queue, j);
            }
        }
        try {
            response.forwardToPreviousPage(request);
        } catch (Exception e) {
            logger.warning(e.toString());
        }
    }

}
