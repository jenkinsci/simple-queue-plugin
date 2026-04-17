package cz.mendelu.xotradov;

import com.google.common.annotations.VisibleForTesting;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.model.Queue;
import hudson.model.View;
import hudson.model.queue.QueueSorter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.jspecify.annotations.Nullable;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;

public class MoveActionWorker {
    protected static final Logger logger = Logger.getLogger(MoveActionWorker.class.getName());
    // shared
    public static final String MOVE_TYPE_PARAM_NAME = "moveType";
    public static final String VIEW_NAME_PARAM_NAME = "viewName";
    // versatile
    public static final String ITEM_ID_PARAM_NAME = "itemId";
    // strict
    public static final String ITEM_ID_EXT_PARAM_NAME = "itemIdExt";
    public static final String ITEM_ID_EXT_PARAM_MODE =
            "itemMode"; // how: id,exact, regex, gregex (regex with .*  pref and suffix
    public static final String ITEM_ID_EXT_PARAM_TARGET =
            "itemTarget"; // in: d(isplayName),(full)D(isplayName),(job)n(ame),(case)I(insensitive) - is expeced
    // multiple times

    protected boolean isSorterSet = false;

    protected void moveImpl(StaplerRequest2 request, StaplerResponse2 response, Queue queue, Jenkins j)
            throws IOException {
        try {
            final String idExtParam = request.getParameter(ITEM_ID_EXT_PARAM_NAME);
            final String idParam = request.getParameter(ITEM_ID_PARAM_NAME);
            String id = "unknown";
            if (idExtParam != null && idParam != null) {
                setResponseToError(
                        ITEM_ID_PARAM_NAME + " and " + ITEM_ID_EXT_PARAM_NAME + " are mutually exclusive",
                        response,
                        StaplerResponse2.SC_BAD_REQUEST);
                return;
            }
            Queue.Item[] items = null;
            if (idParam != null) {
                id = idParam;
                items = getItemsByVersatileApi(response, queue, idParam);
            }
            if (idExtParam != null) {
                id = idExtParam;
                items = getItemsByStrictApi(response, request, queue, idExtParam);
            }

            if (items == null || items.length == 0) {
                setResponseToError(
                        "Queue item with id '" + id + "' not found", response, StaplerResponse2.SC_NOT_FOUND);
                return;
            }

            MoveType moveType = null;
            try {
                moveType = MoveType.valueOf(request.getParameter(MOVE_TYPE_PARAM_NAME));
            } catch (IllegalArgumentException iae) {
                setResponseToError(
                        "Wrong move type " + request.getParameter(MOVE_TYPE_PARAM_NAME),
                        response,
                        StaplerResponse2.SC_BAD_REQUEST);
                return;
            }

            View view = j.getView(request.getParameter(VIEW_NAME_PARAM_NAME));
            move(queue, items, moveType, view);
            Queue.getInstance().maintain();
            response.setStatus(StaplerResponse2.SC_OK);
        } catch (Exception ex) {
            setResponseToError(
                    "unable to simple-queue item "
                            + request.getParameterMap().entrySet().stream()
                                    .map(a -> a.getKey() + ": "
                                            + Arrays.stream(a.getValue()).collect(Collectors.joining(",")))
                                    .collect(Collectors.joining("; "))
                            + " ;"
                            + ex.getMessage(),
                    response,
                    StaplerResponse2.SC_BAD_REQUEST);
        }
    }

    private Queue.Item[] getItemsByStrictApi(
            StaplerResponse2 response, StaplerRequest2 request, Queue queue, String idExtParam) throws IOException {
        final String mode = request.getParameter(ITEM_ID_EXT_PARAM_MODE);
        final String[] target = request.getParameterValues(ITEM_ID_EXT_PARAM_TARGET);
        if (mode == null || target == null) {
            setResponseToError(
                    "One  " + ITEM_ID_EXT_PARAM_MODE + " and at least one " + ITEM_ID_EXT_PARAM_TARGET
                            + " is mandatory in strict mode ",
                    response,
                    StaplerResponse2.SC_BAD_REQUEST);
            return null;
        }
        if (idExtParam == null || idExtParam.isBlank()) {
            setResponseToError(ITEM_ID_EXT_PARAM_NAME + " can not be empty", response, StaplerResponse2.SC_BAD_REQUEST);
            return null;
        }
        ItemMode itemMode;
        try {
            itemMode = ItemMode.valueOf(mode.toUpperCase());
        } catch (IllegalArgumentException iae) {
            setResponseToError(
                    "invalid mode  " + mode + " expected: " + Arrays.toString(ItemMode.values()),
                    response,
                    StaplerResponse2.SC_BAD_REQUEST);
            return null;
        }
        List<ItemTarget> itemTargets = new ArrayList<>();
        for (String targetItem : target) {
            try {
                itemTargets.add(ItemTarget.valueOf(targetItem.toUpperCase()));
            } catch (IllegalArgumentException iae) {
                setResponseToError(
                        "invalid target  " + targetItem + " expected: " + Arrays.toString(ItemTarget.values()),
                        response,
                        StaplerResponse2.SC_BAD_REQUEST);
                return null;
            }
        }
        switch (itemMode) {
            case QID -> {
                Queue.Item item = queue.getItem(Long.parseLong(idExtParam));
                return toSingleItemArray(item);
            }
            case EXACT -> {
                Queue.Item item = findItemByName(queue, idExtParam, RegexWithParams.exact("", itemTargets));
                return toSingleItemArray(item);
            }
            case REGEX -> {
                Pattern pattern;
                RegexWithParams rp = RegexWithParams.exact(idExtParam.trim(), itemTargets);
                try {
                    pattern = rp.getPattern();
                } catch (Exception ex) {
                    setResponseToError(
                            "Can not compile  " + rp.regex + ": " + ex.getMessage(),
                            response,
                            StaplerResponse2.SC_BAD_REQUEST);
                    return null;
                }
                return findItemsByPattern(queue, pattern, rp);
            }
            case GREGEX -> {
                Pattern pattern;
                RegexWithParams rp = RegexWithParams.exact(".*" + idExtParam.trim() + ".*", itemTargets);
                try {
                    pattern = rp.getPattern();
                } catch (Exception ex) {
                    setResponseToError(
                            "Can not compile  " + rp.regex + ": " + ex.getMessage(),
                            response,
                            StaplerResponse2.SC_BAD_REQUEST);
                    return null;
                }
                return findItemsByPattern(queue, pattern, rp);
            }
        }
        return null;
    }

    private static Queue.Item @Nullable [] toSingleItemArray(Queue.Item item) {
        if (item != null) {
            return new Queue.Item[] {item};
        } else {
            return null;
        }
    }

    private Queue.Item[] getItemsByVersatileApi(StaplerResponse2 response, Queue queue, String idParam)
            throws IOException {
        Queue.Item[] items = null;
        try {
            // This handles queue item numbers; to search
            // by the number of a build etc. use the regex mode!
            Queue.Item item = queue.getItem(Long.parseLong(idParam));
            if (item != null) {
                items = new Queue.Item[1];
                items[0] = item;
            }
        } catch (NumberFormatException nfe) {
            // this handles search by name
            Queue.Item item = findItemByName(queue, idParam, null);
            if (item != null) {
                items = new Queue.Item[1];
                items[0] = item;
            } else {
                // regex mode
                RegexWithParams rp;
                if (RegexWithParams.isGroovy(idParam)) {
                    rp = RegexWithParams.groovyLikeRegexWithParams(idParam);
                    if (rp.regex.equals(".*.*")) {
                        setResponseToError(
                                "Empty grovy-like regex  " + rp.regex + "/" + idParam,
                                response,
                                StaplerResponse2.SC_BAD_REQUEST);
                        return null;
                    }
                } else {
                    rp = RegexWithParams.javaLikeRegexWithParams(idParam);
                }
                Pattern pattern;
                try {
                    pattern = rp.getPattern();
                } catch (Exception ex) {
                    setResponseToError(
                            "Can not compile  " + rp.regex + ": " + ex.getMessage(),
                            response,
                            StaplerResponse2.SC_BAD_REQUEST);
                    return null;
                }
                items = findItemsByPattern(queue, pattern, rp);
            }
        }
        return items;
    }

    private static void setResponseToError(String info, StaplerResponse2 response, int status) throws IOException {
        logger.info(info);
        response.setStatus(status);
        PrintWriter writer = response.getWriter();
        JSONObject message = new JSONObject();
        message.put("error", info);
        if (writer != null) { // mocked writer was returning null, killing tests
            writer.println(message.toString(2));
        }
    }

    protected Queue.Item findItemByName(Queue queue, String idParam, RegexWithParams params) {
        if (params == null) {
            for (Queue.Item item : queue.getItems()) {
                if (item.isBuildable()) {
                    if (item.task != null && item.task.getDisplayName().equals(idParam)) {
                        return item;
                    }
                }
            }
        } else {
            for (Queue.Item item : queue.getItems()) {
                if (item.isBuildable() && item.task != null) {
                    if (params.isDisplayName()) {
                        if (params.isCaseInsensitive()) {
                            if (item.task.getDisplayName().equalsIgnoreCase(idParam)) {
                                return item;
                            }
                        } else {
                            if (item.task.getDisplayName().equals(idParam)) {
                                return item;
                            }
                        }
                    }
                    if (params.isFullDisplayName()) {
                        if (params.isCaseInsensitive()) {
                            if (item.task.getFullDisplayName().equalsIgnoreCase(idParam)) {
                                return item;
                            }
                        } else {
                            if (item.task.getFullDisplayName().equals(idParam)) {
                                return item;
                            }
                        }
                    }
                    if (params.isName()) {
                        if (params.isCaseInsensitive()) {
                            if (item.task.getName().equalsIgnoreCase(idParam)) {
                                return item;
                            }
                        } else {
                            if (item.task.getName().equals(idParam)) {
                                return item;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Finds items in the queue that match the given pattern.
     * @param queue The queue to search in.
     * @param idParamPattern The pattern to match against task display names.
     * @return An array of matching Queue.Item objects, or null if no matches are found.
     */
    protected Queue.Item[] findItemsByPattern(Queue queue, Pattern idParamPattern, RegexWithParams rp) {
        List<Queue.Item> items = new ArrayList<>();
        for (Queue.Item item : queue.getItems()) {
            if (item.isBuildable()) {
                if (item.task != null) {
                    boolean matched = false;
                    if (rp.isDisplayName()) {
                        matched = isMatched(idParamPattern, item.task.getDisplayName(), items, item);
                    }
                    if (!matched && rp.isFullDisplayName()) {
                        matched = isMatched(idParamPattern, item.task.getFullDisplayName(), items, item);
                    }
                    if (!matched && rp.isName()) {
                        isMatched(idParamPattern, item.task.getName(), items, item);
                    }
                }
            }
        }
        if (items.isEmpty()) return null;
        return items.toArray(new Queue.Item[0]);
    }

    private static boolean isMatched(Pattern pattern, String taskpart, List<Queue.Item> items, Queue.Item item) {
        Matcher matcher = pattern.matcher(taskpart);
        if (matcher.matches()) {
            items.add(item);
            return true;
        }
        return false;
    }

    protected void move(@NonNull Queue queue, @NonNull Queue.Item item, @NonNull MoveType moveType, View view) {
        if (view == null || !view.isFilterQueue()) {
            moveUnfiltered(queue, item, moveType);
        } else {
            moveFiltered(queue, item, moveType, view);
        }
    }

    protected void move(@NonNull Queue queue, @NonNull Queue.Item[] items, @NonNull MoveType moveType, View view) {
        if (view == null || !view.isFilterQueue()) {
            moveUnfiltered(queue, items, moveType);
        } else {
            moveFiltered(queue, items, moveType, view);
        }
    }

    private void moveUnfiltered(@NonNull Queue queue, @NonNull Queue.Item item, @NonNull MoveType moveType) {
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
                moveToBottom(item, queue);
                break;
            case TOP:
            case BOTTOM:
                break;
        }
    }

    private void moveUnfiltered(@NonNull Queue queue, @NonNull Queue.Item[] items, @NonNull MoveType moveType) {
        switch (moveType) {
            case UP_FAST:
                moveToTop(items, queue);
                break;
            case UP:
                moveUp(items, queue);
                break;
            case DOWN:
                moveDown(items, queue);
                break;
            case DOWN_FAST:
                moveToBottom(items, queue);
                break;
            case TOP:
            case BOTTOM:
                break;
        }
    }

    private void moveFiltered(
            @NonNull Queue queue, @NonNull Queue.Item item, @NonNull MoveType moveType, @NonNull View view) {
        switch (moveType) {
            case TOP:
                moveToTop(item, queue);
                break;
            case UP_FAST:
                moveToTopFiltered(item, queue, view);
                break;
            case UP:
                moveUpFiltered(item, queue, view);
                break;
            case DOWN:
                moveDownFiltered(item, queue, view);
                break;
            case DOWN_FAST:
                moveToBottomFiltered(item, queue, view);
                break;
            case BOTTOM:
                moveToBottom(item, queue);
                break;
        }
    }

    private void moveFiltered(
            @NonNull Queue queue, @NonNull Queue.Item[] items, @NonNull MoveType moveType, @NonNull View view) {
        switch (moveType) {
            case TOP:
                moveToTop(items, queue);
                break;
            case UP_FAST:
                moveToTopFiltered(items, queue, view);
                break;
            case UP:
                moveUpFiltered(items, queue, view);
                break;
            case DOWN:
                moveDownFiltered(items, queue, view);
                break;
            case DOWN_FAST:
                moveToBottomFiltered(items, queue, view);
                break;
            case BOTTOM:
                moveToBottom(items, queue);
                break;
        }
    }

    @VisibleForTesting
    public void moveToBottomFiltered(Queue.Item itemToBottom, Queue queue, @NonNull View view) {
        Queue.Item oldBottomItem = getBottom(view.getQueueItems());
        if (oldBottomItem != null) {
            putABelowB(itemToBottom, oldBottomItem, queue);
        }
    }

    @VisibleForTesting
    public void moveToBottomFiltered(Queue.Item[] itemsToBottom, Queue queue, @NonNull View view) {
        Queue.Item oldBottomItem = getBottom(view.getQueueItems());
        if (oldBottomItem != null) {
            putABelowB(itemsToBottom, oldBottomItem, queue);
        }
    }

    private void putABelowB(Queue.Item itemToBottom, Queue.Item oldBottomItem, Queue queue) {
        Queue.Item[] items = queue.getItems();
        List<Queue.Item> itemsC = getItemsBetween(itemToBottom, oldBottomItem, items);
        if (!isSorterSet) {
            setSorter(queue);
        }
        QueueSorter queueSorter = queue.getSorter();
        if (queueSorter instanceof SimpleQueueSorter) {
            SimpleQueueComparator comparator = ((SimpleQueueSorter) queueSorter).getSimpleQueueComparator();
            comparator.addDesire(itemToBottom.getId(), oldBottomItem.getId());
            for (Queue.Item itemC : itemsC) {
                comparator.addDesire(itemToBottom.getId(), itemC.getId());
            }
            resort(queue);
        }
    }

    /**
     * Similar to single-item putABelowB(), this method puts a whole array
     * of specified more-important items "under" the oldBottomItem in the
     * resulting queue, keeping the order they had in the original queue.
     *
     * @param itemsToBottom
     * @param oldBottomItem
     * @param queue
     */
    private void putABelowB(Queue.Item[] itemsToBottom, Queue.Item oldBottomItem, Queue queue) {
        if (itemsToBottom.length < 2) return;

        Queue.Item[] items = queue.getItems();
        List<Queue.Item> itemsC = getItemsBetween(itemsToBottom, oldBottomItem, items);
        if (!isSorterSet) {
            setSorter(queue);
        }
        QueueSorter queueSorter = queue.getSorter();
        if (queueSorter instanceof SimpleQueueSorter) {
            SimpleQueueComparator comparator = ((SimpleQueueSorter) queueSorter).getSimpleQueueComparator();
            for (Queue.Item itemToBottom : itemsToBottom) {
                comparator.addDesire(itemToBottom.getId(), oldBottomItem.getId());
                for (Queue.Item itemC : itemsC) {
                    comparator.addDesire(itemToBottom.getId(), itemC.getId());
                }
            }
            resort(queue);
        }
    }

    @VisibleForTesting
    public @CheckForNull Queue.Item getBottom(@NonNull List<Queue.Item> queueItems) {
        if (queueItems.size() > 0) {
            return queueItems.get(queueItems.size() - 1);
        } else {
            return null;
        }
    }

    @VisibleForTesting
    public void moveDownFiltered(Queue.Item itemToDown, Queue queue, View view) {
        Queue.Item oldItemBelow = getItemAfter(
                itemToDown,
                view.getQueueItems().toArray(new Queue.Item[view.getQueueItems().size()]));
        if (oldItemBelow != null) {
            putABelowB(itemToDown, oldItemBelow, queue);
        }
    }

    @VisibleForTesting
    public void moveDownFiltered(Queue.Item[] itemsToDown, Queue queue, View view) {
        Queue.Item oldItemBelow = getItemAfter(
                itemsToDown,
                view.getQueueItems().toArray(new Queue.Item[view.getQueueItems().size()]));
        if (oldItemBelow != null) {
            putABelowB(itemsToDown, oldItemBelow, queue);
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
        Queue.Item oldItemAbove = getItemBefore(
                itemToUp,
                view.getQueueItems().toArray(new Queue.Item[view.getQueueItems().size()]));
        if (oldItemAbove != null) {
            putAOnTopOfB(itemToUp, oldItemAbove, queue);
        }
    }

    @VisibleForTesting
    public void moveUpFiltered(Queue.Item[] itemsToUp, Queue queue, View view) {
        Queue.Item oldItemAbove = getItemBefore(
                itemsToUp,
                view.getQueueItems().toArray(new Queue.Item[view.getQueueItems().size()]));
        if (oldItemAbove != null) {
            putAOnTopOfB(itemsToUp, oldItemAbove, queue);
        }
    }

    private void moveToTopFiltered(@NonNull Queue.Item item, @NonNull Queue queue, @NonNull View view) {
        Queue.Item oldTopItem = getTop(view.getQueueItems());
        if (oldTopItem != null) {
            putAOnTopOfB(item, oldTopItem, queue);
        }
    }

    private void moveToTopFiltered(@NonNull Queue.Item[] items, @NonNull Queue queue, @NonNull View view) {
        Queue.Item oldTopItem = getTop(view.getQueueItems());
        if (oldTopItem != null) {
            putAOnTopOfB(items, oldTopItem, queue);
        }
    }

    @VisibleForTesting
    public void putAOnTopOfB(@NonNull Queue.Item itemA, @NonNull Queue.Item itemB, @NonNull Queue queue) {
        Queue.Item[] items = queue.getItems();
        List<Queue.Item> itemsC = getItemsBetween(itemA, itemB, items);
        if (!isSorterSet) {
            setSorter(queue);
        }
        QueueSorter queueSorter = queue.getSorter();
        if (queueSorter instanceof SimpleQueueSorter) {
            SimpleQueueComparator comparator = ((SimpleQueueSorter) queueSorter).getSimpleQueueComparator();
            comparator.addDesire(itemB.getId(), itemA.getId());
            for (Queue.Item itemC : itemsC) {
                comparator.addDesire(itemC.getId(), itemA.getId());
            }
            resort(queue);
        }
    }

    /**
     * Similar to single-item putAOnTopOfB(), this method puts a whole array of
     * specified items "above" the itemB in the resulting queue, keeping the
     * order they have in the original queue.
     *
     * @param itemsA all go above itemB
     * @param itemB
     * @param queue
     */
    @VisibleForTesting
    public void putAOnTopOfB(@NonNull Queue.Item[] itemsA, @NonNull Queue.Item itemB, @NonNull Queue queue) {
        if (itemsA.length < 2) return;

        Queue.Item[] items = queue.getItems();
        List<Queue.Item> itemsC = getItemsBetween(itemsA, itemB, items);
        if (!isSorterSet) {
            setSorter(queue);
        }
        QueueSorter queueSorter = queue.getSorter();
        if (queueSorter instanceof SimpleQueueSorter) {
            SimpleQueueComparator comparator = ((SimpleQueueSorter) queueSorter).getSimpleQueueComparator();
            for (Queue.Item itemA : itemsA) {
                comparator.addDesire(itemB.getId(), itemA.getId());
                for (Queue.Item itemC : itemsC) {
                    comparator.addDesire(itemC.getId(), itemA.getId());
                }
            }
            resort(queue);
        }
    }

    private List<Queue.Item> getItemsBetween(Queue.Item itemA, Queue.Item itemB, Queue.Item[] items) {
        if (isABeforeB(itemA, itemB, items)) {
            return getItemsBetweenTopFirst(itemB, itemA, items);
        } else {
            return getItemsBetweenTopFirst(itemA, itemB, items);
        }
    }

    /**
     * For each item in array itemA, find items between it and itemB in array items.
     * Combine the results, excluding hits present in array itemA itself.
     *
     * @param itemsA
     * @param itemB
     * @param items
     * @return
     */
    private List<Queue.Item> getItemsBetween(Queue.Item[] itemsA, Queue.Item itemB, Queue.Item[] items) {
        if (itemsA.length < 1 || items.length < 2) {
            // Too short, nothing in between
            List<Queue.Item> returnList = new ArrayList<>();
            return returnList;
        }
        if (itemsA.length == 1) return getItemsBetween(itemsA[0], itemB, items);

        // Cache IDs to ignore in a quick-to-search Set:
        HashSet<Long> itemsAid = new HashSet<>();
        for (Queue.Item itemA : itemsA) {
            if (itemA != null) itemsAid.add(itemA.getId());
        }

        // Collect IDs of items that are between each itemA and the itemB,
        // which themselves are not in itemA
        HashSet<Long> inBetweens = new HashSet<>();
        for (Queue.Item itemA : itemsA) {
            List<Queue.Item> tmp = null;
            if (isABeforeB(itemA, itemB, items)) {
                tmp = getItemsBetweenTopFirst(itemB, itemA, items);
            } else {
                tmp = getItemsBetweenTopFirst(itemA, itemB, items);
            }
            for (Queue.Item itemC : tmp) {
                if (itemC != null) {
                    long idC = itemC.getId();
                    if (!(itemsAid.contains(idC))) inBetweens.add(idC);
                }
            }
        }

        // Produce the result in original items[] order
        List<Queue.Item> returnList = new ArrayList<>();
        for (Queue.Item item : items) {
            if (item != null && inBetweens.contains(item.getId())) returnList.add(item);
        }
        return returnList;
    }

    /// We suppose that both items are in the queue present
    private boolean isABeforeB(Queue.Item itemA, Queue.Item itemB, Queue.Item[] items) {
        List<Queue.Item> itemsBefore = getItemsBefore(itemA, items);
        for (Queue.Item item : itemsBefore) {
            if (item.getId() == itemB.getId()) {
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
            for (Queue.Item item : items) {
                if (!seenTop) {
                    if (item.getId() == bottomItem.getId()) {
                        seenBottom = true;
                    }
                    if (seenBottom) {
                        if (item.getId() == topItem.getId()) {
                            seenTop = true;
                        } else {
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
        if (size > 0) {
            for (int i = size; i > 1; i--) {
                items.iterator().next();
            }
            return items.iterator().next();
        } else {
            return null;
        }
    }

    /**
     * @param itemA Item with least importance
     */
    @VisibleForTesting
    public void moveToTop(@NonNull Queue.Item itemA, @NonNull Queue queue) {
        Queue.Item[] items = queue.getItems();
        List<Queue.Item> itemsB = getItemsBefore(itemA, items);
        if (itemsB.size() != 0) {
            if (!isSorterSet) {
                setSorter(queue);
            }
            QueueSorter queueSorter = queue.getSorter();
            if (queueSorter instanceof SimpleQueueSorter) {
                SimpleQueueComparator comparator = ((SimpleQueueSorter) queueSorter).getSimpleQueueComparator();
                for (Queue.Item itemB : itemsB) {
                    comparator.addDesire(itemB.getId(), itemA.getId());
                }
                resort(queue);
            }
        }
    }

    /**
     * @param itemsA Array of Items all with least importance (kept in same order they had in original queue)
     */
    @VisibleForTesting
    public void moveToTop(@NonNull Queue.Item[] itemsA, @NonNull Queue queue) {
        Queue.Item[] items = queue.getItems();
        List<Queue.Item> itemsB = getItemsBeforeLast(itemsA, items);
        if (!itemsB.isEmpty()) {
            if (!isSorterSet) {
                setSorter(queue);
            }
            QueueSorter queueSorter = queue.getSorter();
            if (queueSorter instanceof SimpleQueueSorter) {
                SimpleQueueComparator comparator = ((SimpleQueueSorter) queueSorter).getSimpleQueueComparator();
                for (Queue.Item itemA : itemsA) {
                    for (Queue.Item itemB : itemsB) {
                        comparator.addDesire(itemB.getId(), itemA.getId());
                    }
                }
                resort(queue);
            }
        }
    }

    /**
     * @param itemA Item to be moved up in list = more away from execution
     */
    @VisibleForTesting
    public void moveUp(Queue.Item itemA, Queue queue) {
        Queue.Item[] items = queue.getItems();
        Queue.Item itemB = getItemBefore(itemA, items);
        if (itemB != null) {
            if (!isSorterSet) {
                setSorter(queue);
            }
            QueueSorter queueSorter = queue.getSorter();
            if (queueSorter instanceof SimpleQueueSorter) {
                ((SimpleQueueSorter) queueSorter).getSimpleQueueComparator().addDesire(itemB.getId(), itemA.getId());
                resort(queue);
            }
        }
    }

    /**
     * @param itemsA Array of Items to be moved up in list = more away from execution
     */
    @VisibleForTesting
    public void moveUp(Queue.Item[] itemsA, Queue queue) {
        Queue.Item[] items = queue.getItems();
        Queue.Item itemBedge = getItemBefore(itemsA, items);
        List<Queue.Item> itemsB = getItemsBeforeLast(itemsA, items);
        if (!(itemsB.isEmpty())) {
            if (!isSorterSet) {
                setSorter(queue);
            }
            QueueSorter queueSorter = queue.getSorter();
            if (queueSorter instanceof SimpleQueueSorter) {
                SimpleQueueComparator comparator = ((SimpleQueueSorter) queueSorter).getSimpleQueueComparator();
                boolean sawBedge = (itemBedge == null); // start out considering all itemsB if we have no edge
                for (Queue.Item itemB : itemsB) {
                    if (!sawBedge && itemBedge != null && itemB.getId() == itemBedge.getId())
                        // else: skip items that are over the edge in priority - closer to minimum prio (start of list)
                        sawBedge = true;
                    if (sawBedge) {
                        for (Queue.Item itemA : itemsA) {
                            comparator.addDesire(itemB.getId(), itemA.getId());
                        }
                    }
                }
                resort(queue);
            }
        }
    }

    @VisibleForTesting
    public void moveDown(Queue.Item itemA, Queue queue) {
        Queue.Item[] items = queue.getItems();
        Queue.Item itemB = getItemAfter(itemA, items);
        if (itemB != null) {
            if (!isSorterSet) {
                setSorter(queue);
            }
            QueueSorter queueSorter = queue.getSorter();
            if (queueSorter instanceof SimpleQueueSorter) {
                ((SimpleQueueSorter) queueSorter).getSimpleQueueComparator().addDesire(itemA.getId(), itemB.getId());
                resort(queue);
            }
        }
    }

    /** Relocate ALL of itemsA[] to have priority just one higher than the previously
     * highest-priority other item just above the highest-priority element in itemsA[],
     * keeping relative order of relocated entries as they were in original items[].
     *
     * @param itemsA
     * @param queue
     */
    @VisibleForTesting
    public void moveDown(Queue.Item[] itemsA, Queue queue) {
        Queue.Item[] items = queue.getItems();
        Queue.Item itemBedge = getItemAfter(itemsA, items);
        List<Queue.Item> itemsB = getItemsAfterFirst(itemsA, items);
        if (!(itemsB.isEmpty())) {
            if (!isSorterSet) {
                setSorter(queue);
            }
            QueueSorter queueSorter = queue.getSorter();
            if (queueSorter instanceof SimpleQueueSorter) {
                SimpleQueueComparator comparator = ((SimpleQueueSorter) queueSorter).getSimpleQueueComparator();
                boolean sawBedge = false; // start out considering all itemsB
                for (Queue.Item itemB : itemsB) {
                    if (!sawBedge) {
                        for (Queue.Item itemA : itemsA) {
                            comparator.addDesire(itemA.getId(), itemB.getId());
                        }
                        if (itemBedge != null && itemB.getId() == itemBedge.getId())
                            // skip further items that are over the edge in priority
                            sawBedge = true;
                    }
                }
                resort(queue);
            }
        }
    }

    /**
     * @param itemA The most important item
     */
    @VisibleForTesting
    public void moveToBottom(@NonNull Queue.Item itemA, @NonNull Queue queue) {
        Queue.Item[] items = queue.getItems();
        List<Queue.Item> itemsB = getItemsAfter(itemA, items);
        if (itemsB.size() != 0) {
            if (!isSorterSet) {
                setSorter(queue);
            }
            QueueSorter queueSorter = queue.getSorter();
            if (queueSorter instanceof SimpleQueueSorter) {
                SimpleQueueComparator comparator = ((SimpleQueueSorter) queueSorter).getSimpleQueueComparator();
                for (Queue.Item itemB : itemsB) {
                    comparator.addDesire(itemA.getId(), itemB.getId());
                }
                resort(queue);
            }
        }
    }

    /**
     * @param itemsA Array of the most important Items
     * */
    @VisibleForTesting
    public void moveToBottom(@NonNull Queue.Item[] itemsA, @NonNull Queue queue) {
        Queue.Item[] items = queue.getItems();
        List<Queue.Item> itemsB = getItemsAfterFirst(itemsA, items);
        if (itemsB.size() > 0) {
            if (!isSorterSet) {
                setSorter(queue);
            }
            QueueSorter queueSorter = queue.getSorter();
            if (queueSorter instanceof SimpleQueueSorter) {
                SimpleQueueComparator comparator = ((SimpleQueueSorter) queueSorter).getSimpleQueueComparator();
                for (Queue.Item itemA : itemsA) {
                    for (Queue.Item itemB : itemsB) {
                        comparator.addDesire(itemA.getId(), itemB.getId());
                    }
                }
                resort(queue);
            }
        }
    }

    @NonNull
    private List<Queue.Item> getItemsBefore(@NonNull Queue.Item itemA, @NonNull Queue.Item[] items) {
        List<Queue.Item> returnList = new ArrayList<>();
        if (items.length >= 2) {
            boolean seenItemA = false;
            for (Queue.Item item : items) {
                if (!seenItemA) {
                    if (item.getId() == itemA.getId()) {
                        seenItemA = true;
                    } else {
                        returnList.add(item);
                    }
                }
            }
        }
        return returnList;
    }

    /** Get items which are before ALL entries in itemsA[] (except entries in itemsA itself) */
    @NonNull
    @SuppressFBWarnings(
            value = "UPM_UNCALLED_PRIVATE_METHOD",
            justification =
                    "It is currently not certain that this logic will never be used - new use-cases or corner-case fallbacks may appear later")
    private List<Queue.Item> getItemsBefore(@NonNull Queue.Item[] itemsA, @NonNull Queue.Item[] items) {
        if (itemsA.length == 1) return getItemsBefore(itemsA[0], items);

        List<Queue.Item> returnList = new ArrayList<>();
        if (items.length >= 2 && itemsA.length > 0) {
            // Cache IDs to ignore in a quick-to-search Set:
            HashSet<Long> itemsAid = new HashSet<>();
            for (Queue.Item itemA : itemsA) {
                if (itemA != null) itemsAid.add(itemA.getId());
            }

            int countdown = itemsAid.size();
            for (Queue.Item item : items) {
                if (itemsAid.contains(item.getId())) {
                    countdown--;
                    // Have we seen all itemsA[] entries yet?
                    if (countdown < 1) break;
                } else {
                    returnList.add(item);
                }
            }
        }
        return returnList;
    }

    /** Get items which are before the latest seen entry in itemsA[] (except entries in itemsA itself) */
    @NonNull
    private List<Queue.Item> getItemsBeforeLast(@NonNull Queue.Item[] itemsA, @NonNull Queue.Item[] items) {
        if (itemsA.length == 1) return getItemsBefore(itemsA[0], items);

        List<Queue.Item> returnList = new ArrayList<>();
        if (items.length >= 2 && itemsA.length > 0) {
            // Cache IDs to ignore in a quick-to-search Set:
            HashSet<Long> itemsAid = new HashSet<>();
            for (Queue.Item itemA : itemsA) {
                if (itemA != null) itemsAid.add(itemA.getId());
            }

            int countdown = itemsAid.size();
            for (Queue.Item item : items) {
                if (itemsAid.contains(item.getId())) {
                    countdown--;
                    // Have we seen all itemsA[] entries yet?
                    if (countdown < 1) break;
                } else {
                    returnList.add(item);
                }
            }
        }
        return returnList;
    }

    /** Return all items after itemA (as walking the items[] from [0] upwards) excluding the itemA itself */
    @NonNull
    private List<Queue.Item> getItemsAfter(@NonNull Queue.Item itemA, @NonNull Queue.Item[] items) {
        List<Queue.Item> returnList = new ArrayList<>();
        if (items.length >= 2) {
            boolean seenItemA = false;
            for (Queue.Item item : items) {
                if (seenItemA) {
                    // add item
                    returnList.add(item);
                } else {
                    // check for item
                    if (item.getId() == itemA.getId()) seenItemA = true;
                }
            }
        }
        return returnList;
    }

    /** Return all items after the bottom-most itemsA[] entry
     *  (nearest to end of items[], encountered as walking the
     *  items[] from [0] to higher offsets) excluding the
     *  itemsA[] entries themselves
     */
    @NonNull
    @SuppressFBWarnings(
            value = "UPM_UNCALLED_PRIVATE_METHOD",
            justification =
                    "It is currently not certain that this logic will never be used - new use-cases or corner-case fallbacks may appear later")
    private List<Queue.Item> getItemsAfter(@NonNull Queue.Item[] itemsA, @NonNull Queue.Item[] items) {
        if (itemsA.length == 1) return getItemsAfter(itemsA[0], items);

        List<Queue.Item> returnList = new ArrayList<>();
        if (items.length >= 2 && itemsA.length > 0) {
            // Cache IDs to ignore in a quick-to-search Set:
            HashSet<Long> itemsAid = new HashSet<>();
            for (Queue.Item itemA : itemsA) {
                if (itemA != null) itemsAid.add(itemA.getId());
            }

            boolean seenItemA = false;
            for (Queue.Item item : items) {
                if (itemsAid.contains(item.getId())) {
                    // keep checking for item always, we never know where's the last one
                    seenItemA = true;
                    // Ultimately return whatever is after the last-most item in itemA
                    // excluding this item itself
                    returnList.clear();
                } else if (seenItemA) {
                    // add item
                    returnList.add(item);
                }
            }
        }
        return returnList;
    }

    /** Return all items after the top-most itemsA[] entry
     *  (first seen in items[], encountered as walking the
     *  items[] from [0] to higher offsets) excluding the
     *  itemsA[] entries themselves
     */
    @NonNull
    private List<Queue.Item> getItemsAfterFirst(@NonNull Queue.Item[] itemsA, @NonNull Queue.Item[] items) {
        if (itemsA.length == 1) return getItemsAfter(itemsA[0], items);

        List<Queue.Item> returnList = new ArrayList<>();
        if (items.length >= 2 && itemsA.length > 0) {
            // Cache IDs to ignore in a quick-to-search Set:
            HashSet<Long> itemsAid = new HashSet<>();
            for (Queue.Item itemA : itemsA) {
                if (itemA != null) itemsAid.add(itemA.getId());
            }

            boolean seenItemA = false;
            for (Queue.Item item : items) {
                if (itemsAid.contains(item.getId())) {
                    // keep checking for item ID always, so we can skip those in itemsA[] array
                    seenItemA = true;
                } else if (seenItemA) {
                    // add item
                    returnList.add(item);
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
    private Queue.Item getItemAfter(@NonNull Queue.Item itemA, @NonNull Queue.Item[] items) {
        if (items.length >= 2) {
            Queue.Item previous = null;
            for (Queue.Item itemB : items) {
                if ((previous != null) && (previous.getId() == itemA.getId())) {
                    return itemB;
                }
                previous = itemB;
            }
        }
        return null;
    }

    /**
     * @param itemsA Array of items, the next after the last one of which should be returned as the item that has lower priority
     * @param items Has on [0] the top item, with the lowest priority
     * @return Returns item that is after in the queue order = the with higher priority = goes before to execution
     */
    @CheckForNull
    private Queue.Item getItemAfter(@NonNull Queue.Item[] itemsA, @NonNull Queue.Item[] items) {
        if (itemsA.length < 1 || items.length < 2) {
            return null;
        }
        if (itemsA.length == 1) return getItemAfter(itemsA[0], items);

        // Cache IDs to ignore in a quick-to-search Set:
        HashSet<Long> itemsAid = new HashSet<>();
        for (Queue.Item itemA : itemsA) {
            if (itemA != null) itemsAid.add(itemA.getId());
        }

        // Find the next item which is below each item in the argument array
        // e.g. just below the last one of those
        Queue.Item previous = null;
        Queue.Item latestAfter = null;
        for (Queue.Item itemB : items) {
            if ((previous != null) && (itemsAid.contains(previous.getId()))) {
                latestAfter = itemB;
            }
            previous = itemB;
        }
        return latestAfter;
    }

    @CheckForNull
    private Queue.Item getItemBefore(Queue.Item itemA, Queue.Item[] items) {
        if (items.length >= 2) {
            Queue.Item itemB = null;
            for (Queue.Item itemFor : items) {
                if (itemFor.getId() == itemA.getId()) {
                    return itemB;
                }
                itemB = itemFor;
            }
        }
        return null;
    }

    /** Find the first item in items[] which is above each itemA in the argument itemsA[] array */
    @CheckForNull
    private Queue.Item getItemBefore(@NonNull Queue.Item[] itemsA, @NonNull Queue.Item[] items) {
        if (itemsA.length < 1 || items.length < 2) {
            return null;
        }
        if (itemsA.length == 1) return getItemBefore(itemsA[0], items);

        // Cache IDs to ignore in a quick-to-search Set:
        HashSet<Long> itemsAid = new HashSet<>();
        for (Queue.Item itemA : itemsA) {
            if (itemA != null) itemsAid.add(itemA.getId());
        }

        // Find the first item which is above each item in the argument array
        Queue.Item itemB = null;
        for (Queue.Item itemFor : items) {
            if ((itemFor != null) && (itemsAid.contains(itemFor.getId()))) {
                return itemB;
            }
            itemB = itemFor;
        }

        return null;
    }

    private void setSorter(Queue queue) {
        if (!isSorterSet) {
            QueueSorter originalQueueSorter = queue.getSorter();
            if (originalQueueSorter == null) originalQueueSorter = new DefaultSorter();
            SimpleQueueSorter simpleQueueSorter = new SimpleQueueSorter(originalQueueSorter);
            queue.setSorter(simpleQueueSorter);
            isSorterSet = true;
        }
    }

    private void resort(Queue queue) {
        queue.getSorter().sortBuildableItems(queue.getBuildableItems());
    }

    /**
     * Print the current queue state as plaintext list
     * @param request Stapler request from user
     * @param response Stapler response to write the queue information
     * @param queue The Jenkins queue
     * @throws IOException if writing to response fails
     */
    protected void printQueueImpl(StaplerRequest2 request, StaplerResponse2 response, Queue queue) throws IOException {
        response.setContentType("text/plain; charset=UTF-8");
        response.setStatus(StaplerResponse2.SC_OK);
        PrintWriter writer = response.getWriter();
        
        if (writer != null) {
            String buildableParam = request.getParameter("buildable");
            boolean onlyBuildable = buildableParam == null || !buildableParam.equalsIgnoreCase("false");
            
            Queue.Item[] queueItems = queue.getItems();
            for (Queue.Item item : queueItems) {
                if (item.task != null) {
                    if (onlyBuildable) {
                        if (item.isBuildable()) {
                            writer.println(item.task.getDisplayName());
                        }
                    } else {
                        writer.println(item.task.getDisplayName());
                    }
                }
            }
        }
    }
}
