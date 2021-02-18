package cz.mendelu.xotradov;

import hudson.init.Initializer;
import hudson.widgets.Widget;
import jenkins.model.Jenkins;

import java.util.logging.Logger;

import static hudson.init.InitMilestone.*;


/**
 * Used for initialization
 * @author Jaroslav Otradovec
 */
public class SimpleQueueInit {
    private final static Logger logger = Logger.getLogger(SimpleQueueInit.class.getName());

    /**
    * From hudson.init.Initializer
     * after PLUGINS_PREPARED is the earlies InitMilestone where it can be done
    */
    @SuppressWarnings("unused") //justification: is used because of the annotation Initializer below
    @Initializer(after=PLUGINS_PREPARED)
    public void postInitialize() {
        if (Jenkins.get().getWidgets().isEmpty()){
            logger.warning("Empty widget list!");
        }else {
            Widget queueWidget = Jenkins.get().getWidgets().get(0);
            if (queueWidget.getClass()==jenkins.widgets.BuildQueueWidget.class){
                logger.info("Removing " + queueWidget.getClass().toString());
                Jenkins.get().getWidgets().remove(queueWidget);
            }else {
                logger.warning("SimpleQueuePlugin unable to remove default widget, reason: not normal widget setup");
            }

        }
    }

}
