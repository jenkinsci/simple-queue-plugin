package cz.mendelu.xotradov;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.ExtensionComponent;
import hudson.model.Queue;
import hudson.model.View;
import hudson.widgets.Widget;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jenkins.ExtensionFilter;
import jenkins.model.queue.QueueItem;
import jenkins.widgets.BuildQueueWidget;
import jenkins.widgets.WidgetFactory;

/**
 * SimpleQueueWidget replaces the default BuildQueueWidget and adds arrows to each buildable item.
 */
public class SimpleQueueWidget extends Widget {

    @NonNull
    private String ownerUrl;

    @NonNull
    private List<QueueItem> queueItems;

    private boolean filtered;

    @NonNull
    private String viewName;

    public SimpleQueueWidget(
            @NonNull String ownerUrl, @NonNull List<QueueItem> queueItems, boolean filtered, String viewName) {
        this.ownerUrl = ownerUrl;
        this.queueItems = new ArrayList<>(queueItems);
        this.filtered = filtered;
        this.viewName = viewName;
    }

    public boolean isFiltered() {
        return filtered;
    }

    @Override
    @NonNull
    public String getOwnerUrl() {
        return ownerUrl;
    }

    @NonNull
    public List<QueueItem> getQueueItems() {
        return queueItems;
    }

    @NonNull
    public String getViewName() {
        return viewName;
    }

    @SuppressWarnings("unused") // jelly
    public static String getMoveTypeName() {
        return MoveAction.MOVE_TYPE_PARAM_NAME;
    }

    @SuppressWarnings("unused") // jelly
    public static String getItemIdName() {
        return MoveAction.ITEM_ID_PARAM_NAME;
    }

    @SuppressWarnings("unused") // jelly
    public static String getViewNameParamName() {
        return MoveAction.VIEW_NAME_PARAM_NAME;
    }

    @SuppressWarnings("unused") // jelly
    public static String getItemIdExtName() {
        return MoveAction.ITEM_ID_EXT_PARAM_NAME;
    }

    @SuppressWarnings("unused") // jelly
    public static String getItemIdExtParamMode() {
        return MoveAction.ITEM_ID_EXT_PARAM_MODE;
    }

    @SuppressWarnings("unused") // jelly
    public static String getItemIdExtParamTarget() {
        return MoveAction.ITEM_ID_EXT_PARAM_TARGET;
    }

    /**
     * Helper for Jelly templates: checks whether the given queue item is a WaitingItem
     * (waiting for its quiet period to pass). Jelly does not support {@code instanceof},
     * so this method performs the type check in Java.
     *
     * @param item the queue item to check
     * @return true if the item is a {@link Queue.WaitingItem}
     */
    @SuppressWarnings("unused") // jelly
    public static boolean isWaiting(QueueItem item) {
        return item instanceof Queue.WaitingItem;
    }

    /**
     * Helper for Jelly templates: checks whether the given queue item is pending
     * (assigned to an executor, about to start). Only BuildableItem subclasses can
     * be pending, so this safely returns false for non-BuildableItem types.
     *
     * @param item the queue item to check
     * @return true if the item is a pending BuildableItem
     */
    @SuppressWarnings("unused") // jelly
    public static boolean isPending(QueueItem item) {
        return item instanceof Queue.BuildableItem && ((Queue.BuildableItem) item).isPending();
    }

    @Extension(ordinal = 100)
    public static final class ViewFactoryImpl extends WidgetFactory<View, SimpleQueueWidget> {
        @Override
        public Class<View> type() {
            return View.class;
        }

        @Override
        public Class<SimpleQueueWidget> widgetType() {
            return SimpleQueueWidget.class;
        }

        @NonNull
        @Override
        public Collection<SimpleQueueWidget> createFor(@NonNull View target) {
            List<QueueItem> items = new ArrayList<>(target.getQueueItems());

            // Sort the combined list of blocked and buildable items so that
            // the widget displays them in the correct priority order based on
            // user desires. Jenkins Queue's getItems() always returns blocked
            // items before buildable items, but the user may have moved a
            // blocked item below buildable items via the UI -- so it would
            // get corresponding higher priority as soon as it becomes buildable.
            sortQueueItems(items);

            return List.of(new SimpleQueueWidget(
                    target.getUrl(),
                    items,
                    target.isFilterQueue(),
                    target.getViewName()));
        }

        /**
         * Sorts the combined list of queue items so that blocked and buildable
         * items are ordered together according to user desires stored in the
         * SimpleQueueComparator. This is necessary because Jenkins Queue's
         * getItems() always returns blocked items before buildable items,
         * regardless of the user's priority preferences.
         */
        private static void sortQueueItems(List<QueueItem> items) {
            // QueueItem is an interface implemented by Queue.Item, so we can cast directly.
            // Build a list of Queue.Item for sorting.
            List<Queue.Item> queueItems = new ArrayList<>();
            for (QueueItem item : items) {
                queueItems.add((Queue.Item) item);
            }
            SimpleQueueComparator.getInstance().sortNotWaitingItems(queueItems);

            // Build a map from id to QueueItem wrapper for rebuilding
            Map<Long, QueueItem> wrapperMap = new HashMap<>();
            for (QueueItem wrapper : items) {
                wrapperMap.put(((Queue.Item) wrapper).getId(), wrapper);
            }

            // Rebuild the items list with the sorted order
            items.clear();
            for (Queue.Item qi : queueItems) {
                QueueItem wrapper = wrapperMap.get(qi.getId());
                if (wrapper != null) {
                    items.add(wrapper);
                }
            }
        }
    }

    @Extension
    public static final class RemoveDefaultQueueWidget extends ExtensionFilter {
        @Override
        public <T> boolean allows(Class<T> type, ExtensionComponent<T> component) {
            return WidgetFactory.class != type
                    || !BuildQueueWidget.ViewFactoryImpl.class.isInstance(component.getInstance());
        }
    }
}
