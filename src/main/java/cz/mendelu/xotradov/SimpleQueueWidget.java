package cz.mendelu.xotradov;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.ExtensionComponent;
import hudson.model.Queue;
import hudson.model.View;
import hudson.widgets.Widget;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
            if (items.size() <= 1) {
                return;
            }

            // Collect blocked and buildable items into a NotWaitingItem list
            // NOTE: Technically there might be more NotWaitingItem classes
            //  introduced in the future Jenkins core or by some plugins.
            //  Currently, this change is part of stepping up from sorting
            //  of only BuildableItem to also handling BlockedItem entries.
            //  This involves API conformance to QueueSorter interface and
            //  UI (Jelly) nuances like icons and tooltips. So any other
            //  class, if it ever comes up into scope, would need more work
            //  to handle than just recognizing `instanceof NotWaitingItem`.
            List<Queue.NotWaitingItem> notWaitingItems = new ArrayList<>();
            for (QueueItem item : items) {
                if (item instanceof Queue.BlockedItem) {
                    notWaitingItems.add((Queue.BlockedItem) item);
                } else if (item instanceof Queue.BuildableItem) {
                    notWaitingItems.add((Queue.BuildableItem) item);
                }
            }

            if (notWaitingItems.isEmpty()) {
                return;
            }

            // Use the SimpleQueueComparator to sort the combined list
            // We always use the comparator directly rather than relying on the
            // queue's sorter, because the sorter might not be a SimpleQueueSorter
            // (e.g., if no move has been made yet).
            SimpleQueueComparator comparator = SimpleQueueComparator.getInstance();

            // Sort the combined list using the comparator.
            // The comparator returns -1 when the first item is more important
            // (higher priority, should run earlier with other aspects equal).
            // In queue.getItems(), items at the END of list have higher priority
            // (built first). The widget displays items in the same order:
            //   * top = lower priority,
            //   * bottom = higher priority.
            // So we need to sort in reverse: less important items first (top of widget),
            // more important items last (bottom of widget).
            notWaitingItems.sort((a, b) -> comparator.compare(b, a));

            // Separate items into three groups:
            // 1. Items before the first NotWaitingItem (waiting items, etc.)
            // 2. NotWaitingItems (sorted)
            // 3. Items after the last NotWaitingItem
            List<QueueItem> before = new ArrayList<>();
            List<QueueItem> after = new ArrayList<>();

            int firstNotWaitingIndex = -1;
            int lastNotWaitingIndex = -1;
            for (int i = 0; i < items.size(); i++) {
                QueueItem item = items.get(i);
                if (item instanceof Queue.NotWaitingItem) {
                    if (firstNotWaitingIndex == -1) {
                        firstNotWaitingIndex = i;
                    }
                    lastNotWaitingIndex = i;
                }
            }

            for (int i = 0; i < items.size(); i++) {
                QueueItem item = items.get(i);
                if (item instanceof Queue.NotWaitingItem) {
                    // Skip - will be added in sorted order
                    continue;
                }
                if (firstNotWaitingIndex != -1 && i < firstNotWaitingIndex) {
                    before.add(item);
                } else if (lastNotWaitingIndex != -1 && i > lastNotWaitingIndex) {
                    after.add(item);
                } else {
                    // Items between first and last NotWaitingItem that are not NotWaitingItem
                    before.add(item);
                }
            }

            // Rebuild the items list: before + sorted NotWaitingItems + after
            items.clear();
            items.addAll(before);
            items.addAll(notWaitingItems);
            items.addAll(after);
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
