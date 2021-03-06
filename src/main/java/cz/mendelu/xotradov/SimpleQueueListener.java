package cz.mendelu.xotradov;

import hudson.Extension;
import hudson.model.Queue;
import hudson.model.queue.QueueListener;

@SuppressWarnings("unused") //justification: is used because of the @Extension below
@Extension
public class SimpleQueueListener extends QueueListener {
    @Override
    public void onLeft(Queue.LeftItem li) {
        SimpleQueueComparator.getInstance().removeDesireOfKey(li.getId());
        super.onLeft(li);
    }
}
