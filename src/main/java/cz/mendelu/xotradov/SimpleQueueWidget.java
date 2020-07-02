package cz.mendelu.xotradov;

import hudson.Extension;
import hudson.widgets.Widget;

import java.util.logging.Logger;

/**
 * SimpleQueueWidget replaces the default BuildQueueWidget and adds arrows to each buildable item.
 */
@SuppressWarnings("unused")
@Extension(ordinal=199)
public class SimpleQueueWidget extends Widget {
    private final static Logger logger = Logger.getLogger(SimpleQueueWidget.class.getName());
    public static String getMoveTypeName(){return MoveAction.MOVE_TYPE_PARAM_NAME;}
    public static String getItemIdName(){return MoveAction.ITEM_ID_PARAM_NAME;}
}
