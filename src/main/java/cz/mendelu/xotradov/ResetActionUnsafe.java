package cz.mendelu.xotradov;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.interceptor.RequirePOST;

import java.io.IOException;
import java.util.logging.Logger;

import javax.annotation.CheckForNull;

import hudson.Extension;
import hudson.model.RootAction;
import hudson.model.queue.QueueSorter;
import jenkins.model.Jenkins;

@SuppressWarnings("unused")
@Extension
public class ResetActionUnsafe implements RootAction {
    private static Logger logger = Logger.getLogger(ResetActionUnsafe.class.getName());

    //curl http://my.url:8080/simpleQueueResetUnsafe/reset
    public void doReset(final StaplerRequest request, final StaplerResponse response) {
        doImpl(request, response);
    }

    public static void  doImpl(final StaplerRequest request, final StaplerResponse response) {
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


    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @CheckForNull
    @Override
    public String getUrlName() {
        return "simpleQueueResetUnsafe";
    }

}
