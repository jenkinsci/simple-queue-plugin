package cz.mendelu.xotradov.test;

import cz.mendelu.xotradov.MoveAction;
import cz.mendelu.xotradov.ResetAction;
import hudson.model.*;
import hudson.model.queue.QueueTaskFuture;
import jenkins.model.Jenkins;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.SleepBuilder;

import javax.annotation.Nonnull;
import java.io.IOException;

public class TestHelper {
    private JenkinsRule r;
    public TestHelper(JenkinsRule r){
        this.r=r;
    }
    public QueueTaskFuture<FreeStyleBuild> schedule(@Nonnull FreeStyleProject projectA) throws Exception {
        QueueTaskFuture<FreeStyleBuild> futureA = projectA.scheduleBuild2(0);
        if (futureA == null) {
            throw new Exception("the task could not be scheduled");
        }
        boolean enteredTheQueueA = false;
        while (!enteredTheQueueA) {
            for (Queue.BuildableItem item : Queue.getInstance().getBuildableItems()) {
                if (item.task.getDisplayName() != null && item.task.getDisplayName().equals(projectA.getDisplayName())) {
                    enteredTheQueueA = true;
                }
            }
            for (Computer computer: Jenkins.get().getComputers()){
                if (computer!=null){
                    for (Executor executor: computer.getExecutors()){
                        if(executor.getCurrentWorkUnit()!=null && executor.getCurrentWorkUnit().context.task.getDisplayName().equals(projectA.getDisplayName()))return futureA;
                    }
                }
            }
            Thread.sleep(5);
        }
        return futureA;
    }

    public FreeStyleProject createProject(@Nonnull String projectName, long time) throws IOException {
        FreeStyleProject projectA = r.createFreeStyleProject(projectName);
        projectA.getBuildersList().add(new SleepBuilder(time));
        projectA.setDisplayName(projectName);
        return projectA;
    }
    public FreeStyleProject createAndSchedule(@Nonnull String projectName, long millisOfBuild)  throws Exception {
        FreeStyleProject projectA = createProject(projectName,millisOfBuild);
        schedule(projectA);
        return projectA;
    }
    public void fillQueueFor(long millis) throws Exception {
        FreeStyleProject projectA = createProject("projectA",millis);
        schedule(projectA); //projectA.doBuild(req,rsp,7000);
        FreeStyleProject projectB = createProject("projectB", millis);
        schedule(projectB);
        Queue queue = Queue.getInstance();
        while (!queue.getBuildableItems().isEmpty()){
            Thread.sleep(10);
        }
    }

    public ResetAction getResetAction() {
        for (Action action: r.jenkins.getActions()){
            if (action instanceof ResetAction){
                return (ResetAction) action;
            }
        }
        return null;
    }

    public MoveAction getMoveAction() {
        for (Action action: r.jenkins.getActions()){
            if (action instanceof MoveAction){
                return (MoveAction) action;
            }
        }
        return null;
    }
}
