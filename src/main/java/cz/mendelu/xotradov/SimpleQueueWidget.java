package cz.mendelu.xotradov;

import hudson.Extension;
import hudson.ExtensionComponent;
import hudson.widgets.Widget;

import java.util.logging.Logger;
import jenkins.ExtensionFilter;
import jenkins.widgets.BuildQueueWidget;

/**
 * SimpleQueueWidget replaces the default BuildQueueWidget and adds arrows to each buildable item.
 */
@SuppressWarnings("unused") //justification: used because of the @Extension below
@Extension(ordinal=199)
public class SimpleQueueWidget extends Widget {
    private final static Logger logger = Logger.getLogger(SimpleQueueWidget.class.getName());
    public static String getMoveTypeName(){return MoveAction.MOVE_TYPE_PARAM_NAME;}
    public static String getItemIdName(){return MoveAction.ITEM_ID_PARAM_NAME;}
    public static String getViewNameParamName(){return MoveAction.VIEW_NAME_PARAM_NAME;}

    @Extension
    public static final class RemoveDefaultQueueWidget extends ExtensionFilter {
        @Override
        public <T> boolean allows(Class<T> type, ExtensionComponent<T> component) {
            return Widget.class != type || !BuildQueueWidget.class.isInstance(component.getInstance());
        }
    }
}
