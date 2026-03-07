package cz.mendelu.xotradov;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;

import java.io.IOException;
import java.util.logging.Logger;

import hudson.Extension;
import hudson.model.RootAction;
import hudson.model.queue.QueueSorter;
import jenkins.model.Jenkins;

@SuppressWarnings("unused")
@Extension
public class UnsafeResetAction implements RootAction {
    private static Logger logger = Logger.getLogger(UnsafeResetAction.class.getName());

    //curl http://my.url:8080/simpleQueueResetUnsafe/reset
    public void doReset(final StaplerRequest2 request, final StaplerResponse2 response) {
        if (!SimpleQueueConfig.getInstance().isEnableUnsafe()) {
            throw new IllegalArgumentException("Unsafe reset api attempted without being enabled");
        }
        resetImpl(request, response);
    }

    public static void resetImpl(final StaplerRequest2 request, final StaplerResponse2 response) {
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
