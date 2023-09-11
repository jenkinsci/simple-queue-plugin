package cz.mendelu.xotradov;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.ExtensionComponent;
import hudson.model.View;
import hudson.widgets.Widget;
import java.util.Collection;
import java.util.List;
import jenkins.ExtensionFilter;
import jenkins.widgets.BuildQueueWidget;
import jenkins.widgets.WidgetFactory;


/**
 * SimpleQueueWidget replaces the default BuildQueueWidget and adds arrows to each buildable item.
 */
public class SimpleQueueWidget extends Widget {
    @SuppressWarnings("unused") // jelly
    public static String getMoveTypeName(){return MoveAction.MOVE_TYPE_PARAM_NAME;}
    @SuppressWarnings("unused") // jelly
    public static String getItemIdName(){return MoveAction.ITEM_ID_PARAM_NAME;}
    @SuppressWarnings("unused") // jelly
    public static String getViewNameParamName(){return MoveAction.VIEW_NAME_PARAM_NAME;}

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
            return List.of(new SimpleQueueWidget());
        }
    }

    @Extension
    public static final class RemoveDefaultQueueWidget extends ExtensionFilter {
        @Override
        public <T> boolean allows(Class<T> type, ExtensionComponent<T> component) {
            return WidgetFactory.class != type || !BuildQueueWidget.ViewFactoryImpl.class.isInstance(component.getInstance());
        }
    }
}
