package cz.mendelu.xotradov;


import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

import hudson.Extension;

import jenkins.model.GlobalConfiguration;

@Extension
public class SimpleQueueConfig extends GlobalConfiguration {

    boolean enableUnsafe = false;

    public static SimpleQueueConfig getInstance() {
        return GlobalConfiguration.all().get(SimpleQueueConfig.class);
    }

    public boolean isEnableUnsafe() {
        return enableUnsafe;
    }

    public boolean getEnableUnsafe() {
        return enableUnsafe;
    }

    @DataBoundSetter
    public void setEnableUnsafe(boolean enableUnsafe) {
        this.enableUnsafe = enableUnsafe;
    }

    public SimpleQueueConfig() {
        load();
    }


    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
        req.bindJSON(this, json);
        save();
        return super.configure(req, json);
    }
}
