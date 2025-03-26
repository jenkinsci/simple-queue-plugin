package cz.mendelu.xotradov;

import hudson.Extension;
import hudson.model.RootAction;
import hudson.model.queue.QueueSorter;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.util.logging.Logger;
import org.kohsuke.stapler.interceptor.RequirePOST;

@SuppressWarnings("unused")
@Extension
public class ResetAction implements RootAction {
    private static Logger logger = Logger.getLogger(ResetAction.class.getName());

    @RequirePOST
    public void doReset(final StaplerRequest request, final StaplerResponse response) {
        if (!Jenkins.get().hasPermission(PermissionHandler.SIMPLE_QUEUE_RESET_PERMISSION)) return;
        QueueSorter queueSorter = Jenkins.get().getQueue().getSorter();
        if (queueSorter instanceof SimpleQueueSorter){
            ((SimpleQueueSorter) queueSorter).reset();
        }
        try {
            response.sendRedirect2(request.getRootPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    @CheckForNull
    @Override
    public String getIconFileName() {
        if (Jenkins.get().hasPermission(PermissionHandler.SIMPLE_QUEUE_RESET_PERMISSION)){
            return "/plugin/simple-queue/images/reset_64.png";
        }else {
            return null;
        }
    }

    @CheckForNull
    @Override
    public String getDisplayName() {
        if (Jenkins.get().hasPermission(PermissionHandler.SIMPLE_QUEUE_RESET_PERMISSION)){
            return "Reset Simple-Queue";
        }else {
            return null;
        }
    }

    @CheckForNull
    @Override
    public String getUrlName() {
        if (Jenkins.get().hasPermission(PermissionHandler.SIMPLE_QUEUE_RESET_PERMISSION)){
            return "simpleQueueReset";
        }else {
            return null;
        }
    }

}
