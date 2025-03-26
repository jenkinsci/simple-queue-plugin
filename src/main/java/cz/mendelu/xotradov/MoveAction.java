package cz.mendelu.xotradov;

import com.google.common.annotations.VisibleForTesting;
import hudson.Extension;
import hudson.model.*;
import hudson.model.queue.QueueSorter;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.kohsuke.stapler.interceptor.RequirePOST;

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
    public static final String VIEW_NAME_PARAM_NAME="viewName";
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
                try {
                    String idParam = request.getParameter(ITEM_ID_PARAM_NAME);
                    Queue.Item item = null;
                    try {
                        item = queue.getItem(Long.parseLong(idParam));
                    } catch (NumberFormatException nfe) {
                        item = findIemByName(queue, idParam);
                    }
                    MoveType moveType = MoveType.valueOf(request.getParameter(MOVE_TYPE_PARAM_NAME));
                    View view = j.getView(request.getParameter(VIEW_NAME_PARAM_NAME));
                    if (item != null) {
                        move(queue, item, moveType, view);
                        Queue.getInstance().maintain();
                    } else {
                        logger.info("Wrong item id " + idParam + " (or not found in view " + request.getParameter(VIEW_NAME_PARAM_NAME) + ")");
                    }
                } catch (IllegalArgumentException iae) {
                    logger.info("Wrong move type " + request.getParameter(MOVE_TYPE_PARAM_NAME));
                } catch (Exception ex) {
                    logger.info("unable to simple-queue item " + request.getParameterMap().entrySet().stream().map(a -> a.getKey() + ": " + Arrays.stream(a.getValue()).collect(
                            Collectors.joining(","))).collect(Collectors.joining("; ")));
                }
            }
        }
        try {
            response.forwardToPreviousPage(request);
        } catch (Exception e) {
            logger.warning(e.toString());
        }
    }

    private Queue.Item findIemByName(Queue queue, String idParam) {
        for (Queue.Item item : queue.getItems()) {
            if (item.isBuildable()) {
                if (item.task != null && item.task.getDisplayName().equals(idParam)) {
                    return item;
                }
            }
        }
        return null;
    }

    private void move(@Nonnull Queue queue, @Nonnull Queue.Item item, @Nonnull MoveType moveType, View view) {
        if (view == null || !view.isFilterQueue()) {
            moveUnfiltered(queue, item, moveType);
        } else {
            moveFiltered(queue, item, moveType, view);
        }
    }

    private void moveUnfiltered(@Nonnull Queue queue, @Nonnull Queue.Item item, @Nonnull MoveType moveType) {
        switch (moveType) {
            case UP_FAST:
                moveToTop(item, queue);
                break;
            case UP:
                moveUp(item, queue);
                break;
            case DOWN:
                moveDown(item, queue);
                break;
            case DOWN_FAST:
                moveToBottom(item,queue);
                break;
            case TOP:
            case BOTTOM:
                break;
        }
    }

    private void moveFiltered(@Nonnull Queue queue,@Nonnull Queue.Item item,@Nonnull MoveType moveType,@Nonnull View view) {
        switch (moveType){
            case TOP:
                moveToTop(item,queue);
                break;
            case UP_FAST:
                moveToTopFiltered(item,queue,view);
                break;
            case UP:
                moveUpFiltered(item,queue,view);
                break;
            case DOWN:
                moveDownFiltered(item,queue,view);
                break;
            case DOWN_FAST:
                moveToBottomFiltered(item,queue,view);
                break;
            case BOTTOM:
                moveToBottom(item,queue);
                break;
        }
    }

    @VisibleForTesting
    public void moveToBottomFiltered(Queue.Item itemToBottom, Queue queue,@Nonnull View view) {
        Queue.Item oldBottomItem = getBottom(view.getQueueItems());
        if (oldBottomItem!=null){
            putABelowB(itemToBottom,oldBottomItem,queue);
        }
    }

    private void putABelowB(Queue.Item itemToBottom, Queue.Item oldBottomItem, Queue queue) {
        Queue.Item[] items = queue.getItems();
        List<Queue.Item> itemsC = getItemsBetween(itemToBottom,oldBottomItem, items);
        if (!isSorterSet){
            setSorter(queue);
        }
        QueueSorter queueSorter = queue.getSorter();
        if (queueSorter instanceof SimpleQueueSorter){
            SimpleQueueComparator comparator = ((SimpleQueueSorter) queueSorter).getSimpleQueueComparator();
            comparator.addDesire(itemToBottom.getId(),oldBottomItem.getId());
            for (Queue.Item itemC: itemsC){
                comparator.addDesire(itemToBottom.getId(),itemC.getId());
            }
            resort(queue);
        }
    }

    @VisibleForTesting
    public @CheckForNull Queue.Item getBottom(@Nonnull List<Queue.Item> queueItems) {
        if (queueItems.size()>0){
            return queueItems.get(queueItems.size()-1);
        }else {
            return null;
        }
    }

    @VisibleForTesting
    public void moveDownFiltered(Queue.Item itemToDown, Queue queue, View view) {
        Queue.Item oldItemBelow = getItemAfter(itemToDown,view.getQueueItems().toArray(new Queue.Item[view.getQueueItems().size()]));
        if (oldItemBelow!=null){
            putABelowB(itemToDown,oldItemBelow,queue);
        }
    }

    /**
     * Handles move of item when view is filtered.
     * @param itemToUp Item to be moved up
     * @param queue Main queue from jenkins
     * @param view View in which was request produced
     */
    @VisibleForTesting
    public void moveUpFiltered(Queue.Item itemToUp, Queue queue, View view) {
        Queue.Item oldItemAbove = getItemBefore(itemToUp,view.getQueueItems().toArray(new Queue.Item[view.getQueueItems().size()]));
        if (oldItemAbove!=null){
            putAOnTopOfB(itemToUp,oldItemAbove,queue);
        }
    }


    private void moveToTopFiltered(@Nonnull Queue.Item item, @Nonnull Queue queue, @Nonnull View view) {
            Queue.Item oldTopItem = getTop(view.getQueueItems());
            if (oldTopItem!=null){
                putAOnTopOfB(item,oldTopItem,queue);
            }
    }

    @VisibleForTesting
    public void putAOnTopOfB(@Nonnull Queue.Item itemA, @Nonnull Queue.Item itemB,@Nonnull Queue queue) {
        Queue.Item[] items = queue.getItems();
        List<Queue.Item> itemsC = getItemsBetween(itemA,itemB, items);
            if (!isSorterSet){
                setSorter(queue);
            }
            QueueSorter queueSorter = queue.getSorter();
            if (queueSorter instanceof SimpleQueueSorter){
                SimpleQueueComparator comparator = ((SimpleQueueSorter) queueSorter).getSimpleQueueComparator();
                comparator.addDesire(itemB.getId(),itemA.getId());
                for (Queue.Item itemC: itemsC){
                    comparator.addDesire(itemC.getId(),itemA.getId());
                }
                resort(queue);
            }
    }

    private List<Queue.Item> getItemsBetween(Queue.Item itemA, Queue.Item itemB, Queue.Item[] items) {
        if (isABeforeB(itemA,itemB,items)){
            return getItemsBetweenTopFirst(itemB,itemA,items);
        }else {
            return getItemsBetweenTopFirst(itemA,itemB,items);
        }
    }

    ///We suppose that both items are in the queue present
    private boolean isABeforeB(Queue.Item itemA, Queue.Item itemB, Queue.Item[] items) {
        List<Queue.Item> itemsBefore = getItemsBefore(itemA,items);
         for(Queue.Item item:itemsBefore){
             if (item.getId()==itemB.getId()){
                 return false;
             }
         }
        return true;
    }


    private List<Queue.Item> getItemsBetweenTopFirst(Queue.Item topItem, Queue.Item bottomItem, Queue.Item[] items) {
        List<Queue.Item> returnList = new ArrayList<>();
        if (items.length > 2) {
            boolean seenBottom = false;
            boolean seenTop = false;
            for (Queue.Item item:items){
                if (!seenTop){
                    if (item.getId()==bottomItem.getId()){
                        seenBottom=true;
                    }
                    if (seenBottom){
                        if (item.getId()==topItem.getId()){
                            seenTop = true;
                        }else {
                            returnList.add(item);
                        }
                    }
                }
            }
        }
        return returnList;
    }

    /**
     * @return Returns last item from collection, in queue it has the least priority
     */
    @VisibleForTesting
    public @CheckForNull Queue.Item getTop(Collection<Queue.Item> items) {
        int size = items.size();
        if (size>0){
            for (int i = size; i > 1 ; i--) {
                items.iterator().next();
            }
            return items.iterator().next();
        }else {
            return null;
        }
    }

    /**
     * @param itemA Item with least importance
     */
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

    /**
      @param itemA Item to be moved up in list = more away from execution
     */
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

    /**
     * @param itemA The most important item
     * */
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

    /**
     * @param itemA Item after which should be returned item that has lower priority
     * @param items Has on [0] the top item, with the lowest priority
     * @return Returns item that is after in the queue order = the with higher priority = goes before to execution
     */
    @CheckForNull
    private Queue.Item getItemAfter(@Nonnull Queue.Item itemA,@Nonnull Queue.Item[] items) {
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
