package cz.mendelu.xotradov;

import com.google.common.annotations.VisibleForTesting;
import hudson.Extension;
import hudson.model.Queue;
import hudson.model.RootAction;
import hudson.model.queue.QueueSorter;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Handles request to move one buildable item in a build queue, which is the core functionality of this plugin.
 * @author Jaroslav Otradovec
 */
@SuppressWarnings("unused")
@Extension
public class MoveAction implements RootAction {
    private static Logger logger = Logger.getLogger(MoveAction.class.getName());
    public static final String MOVE_TYPE_PARAM_NAME= "moveType";
    public static final String ITEM_ID_PARAM_NAME="itemId";
    private boolean isSorterSet=false;
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

    public void doMove(final StaplerRequest request, final StaplerResponse response) {
        Jenkins j;
        if ((j = Jenkins.getInstanceOrNull()) != null) {
            Queue queue = j.getQueue();
            if (queue != null & j.hasPermission(PermissionHandler.SIMPLE_QUEUE_MOVE_PERMISSION)) {
                try {
                    Queue.Item item = queue.getItem(Long.parseLong(request.getParameter(ITEM_ID_PARAM_NAME)));
                    MoveType moveType = MoveType.valueOf(request.getParameter(MOVE_TYPE_PARAM_NAME));
                    if (item != null){
                        switch (moveType){
                            case UP_FAST:
                                moveToTop(item,queue);
                                break;
                            case UP:
                                moveUp(item,queue);
                                break;
                            case DOWN:
                                moveDown(item,queue);
                                break;
                            case DOWN_FAST:
                                moveToBottom(item,queue);
                                break;
                        }
                        Queue.getInstance().maintain();
                    }
                }catch (NumberFormatException nfe){
                    logger.info("Wrong item id");
                }catch (IllegalArgumentException iae){
                    logger.info("Wrong move type");
                }
            }
        }
        try {
            response.sendRedirect2(request.getRootPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @VisibleForTesting
    public void moveToTop(@Nonnull Queue.Item itemA,@Nonnull Queue queue){
        Queue.Item[] items = queue.getItems();
        List<Queue.Item> itemsB = getItemsBefore(itemA, items);
        if (itemsB.size()!=0){
            if (!isSorterSet){
                setSorter(queue);
            }
            QueueSorter queueSorter = queue.getSorter();
            if (queueSorter instanceof SimpleQueueSorter){
                SimpleQueueComparator comparator = ((SimpleQueueSorter) queueSorter).getSimpleQueueComparator();
                for (Queue.Item itemB: itemsB){
                    comparator.addDesire(itemB.getId(),itemA.getId());
                }
                resort(queue);
            }
        }
    }

    @VisibleForTesting
    public void moveUp(Queue.Item itemA, Queue queue) {
        Queue.Item[] items = queue.getItems();
        Queue.Item itemB = getItemBefore(itemA, items);
        if (itemB!=null){
            if (!isSorterSet){
                setSorter(queue);
            }
            QueueSorter queueSorter = queue.getSorter();
            if (queueSorter instanceof SimpleQueueSorter){
                ((SimpleQueueSorter) queueSorter).getSimpleQueueComparator().addDesire(itemB.getId(),itemA.getId());
                resort(queue);
            }
        }
    }

    @VisibleForTesting
    public void moveDown(Queue.Item itemA, Queue queue) {
        Queue.Item[] items = queue.getItems();
        Queue.Item itemB = getItemAfter(itemA, items);
        if (itemB!=null){
            if (!isSorterSet){
                setSorter(queue);
            }
            QueueSorter queueSorter = queue.getSorter();
            if (queueSorter instanceof SimpleQueueSorter){
                ((SimpleQueueSorter) queueSorter).getSimpleQueueComparator().addDesire(itemA.getId(),itemB.getId());
                resort(queue);
            }
        }
    }

    @VisibleForTesting
    public void moveToBottom(@Nonnull Queue.Item itemA,@Nonnull Queue queue){
        Queue.Item[] items = queue.getItems();
        List<Queue.Item> itemsB = getItemsAfter(itemA, items);
        if (itemsB.size()!=0){
            if (!isSorterSet){
                setSorter(queue);
            }
            QueueSorter queueSorter = queue.getSorter();
            if (queueSorter instanceof SimpleQueueSorter){
                SimpleQueueComparator comparator = ((SimpleQueueSorter) queueSorter).getSimpleQueueComparator();
                for (Queue.Item itemB : itemsB){
                    comparator.addDesire(itemA.getId(),itemB.getId());
                }
                resort(queue);
            }
        }
    }

    @Nonnull
    private List<Queue.Item> getItemsBefore(@Nonnull Queue.Item itemA,@Nonnull Queue.Item[] items) {
        List<Queue.Item> returnList = new ArrayList<>();
        if (items.length >= 2) {
            boolean seenItemA= false;
            for (Queue.Item item:items){
                if (!seenItemA){
                    if (item.getId()==itemA.getId()){
                        seenItemA = true;
                    }else {
                        returnList.add(item);
                    }
                }
            }
        }
        return returnList;
    }

    @Nonnull
    private List<Queue.Item> getItemsAfter(@Nonnull Queue.Item itemA,@Nonnull Queue.Item[] items) {
        List<Queue.Item> returnList = new ArrayList<>();
        if (items.length >= 2) {
            boolean seenItemA = false;
            for (Queue.Item item: items){
                if (seenItemA){
                    //add item
                    returnList.add(item);
                }else {
                    //check for item
                    if (item.getId() == itemA.getId()) seenItemA = true;
                }
            }
        }
        return returnList;
    }

    @CheckForNull
    private Queue.Item getItemAfter(Queue.Item itemA, Queue.Item[] items) {
        if (items.length >= 2) {
            Queue.Item previous = null;
            for (Queue.Item itemB : items) {
                if ((previous!=null) && (previous.getId() == itemA.getId())) {
                    return itemB;
                }
                previous=itemB;
            }
        }
        return null;
    }

    @CheckForNull
    private Queue.Item getItemBefore(Queue.Item itemA, Queue.Item[] items) {
        if (items.length >= 2) {
            Queue.Item itemB = null;
            for (Queue.Item itemFor : items) {
                if (itemFor.getId() == itemA.getId()) {
                    return itemB;
                }
                itemB=itemFor;
            }
        }
        return null;
    }

    private void setSorter(Queue queue) {
        if (!isSorterSet){
            QueueSorter originalQueueSorter = queue.getSorter();
            if (originalQueueSorter == null) originalQueueSorter = new DefaultSorter();
            SimpleQueueSorter simpleQueueSorter = new SimpleQueueSorter(originalQueueSorter);
            queue.setSorter(simpleQueueSorter);
            isSorterSet=true;
        }
    }



    private void resort(Queue queue) {
        queue.getSorter().sortBuildableItems(queue.getBuildableItems());
    }

}
