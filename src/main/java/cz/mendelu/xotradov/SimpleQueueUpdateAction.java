package cz.mendelu.xotradov;


import hudson.Extension;
import hudson.model.*;
import hudson.security.Permission;
import jenkins.model.Jenkins;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.Stapler;

import java.util.Collection;
import java.util.logging.Logger;

/**
 * Used to enable updating of queue widget.
 * @author Jaroslav Otradovec
 */
@SuppressWarnings("unused")
@Extension
public class SimpleQueueUpdateAction implements RootAction {
    private final static Logger logger = Logger.getLogger(SimpleQueueUpdateAction.class.getName());

    ///For compilation reasons
    public SimpleQueueUpdateAction() {
    }
    public static String getMoveTypeName(){return MoveAction.MOVE_TYPE_PARAM_NAME;}
    public static String getItemIdName(){return MoveAction.ITEM_ID_PARAM_NAME;}
    public static String getViewNameParamName(){return MoveAction.VIEW_NAME_PARAM_NAME;}
    ///Used by Jelly
    @Restricted(NoExternalUse.class)
    public Queue.Item[] getItems(){
        if (!Jenkins.get().hasPermission(Permission.READ)){
            return null;
        }else {
            View view = Jenkins.get().getView(Stapler.getCurrentRequest().getParameter("name"));
            Collection<Queue.Item> x;
            if (view != null) {
                x = view.getQueueItems();
                return x.toArray(new Queue.Item[x.size()]);
            }else {
                return null;
            }
        }
    }
    @Restricted(NoExternalUse.class) // Jelly
    public boolean isFilterQueue(){
        if (!Jenkins.get().hasPermission(Permission.READ)){
            return false;
        }else {
            View view = Jenkins.get().getView(Stapler.getCurrentRequest().getParameter("name"));
            return view != null && view.isFilterQueue();
        }
    }
    @Restricted(NoExternalUse.class) // Jelly
    public String getViewName(){
        if (!Jenkins.get().hasPermission(Permission.READ)){
            return "";
        }else {
            View view = Jenkins.get().getView(Stapler.getCurrentRequest().getParameter("name"));
            return view!=null?view.getViewName():"";
        }
    }
    public String getIconFileName() {
        return null;
    }
    public String getDisplayName() {
        return null;
    }
    public String getUrlName() {
        if (Jenkins.get().hasPermission(Permission.READ)){
            return "updateQueue";
        }else {
            return null;
        }
    }
}
