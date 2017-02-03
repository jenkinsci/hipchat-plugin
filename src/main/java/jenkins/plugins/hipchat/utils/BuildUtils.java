package jenkins.plugins.hipchat.utils;

import hudson.model.Result;
import hudson.model.Run;
import javax.inject.Singleton;

@Singleton
public class BuildUtils {

    public Result findPreviousBuildResult(Run<?, ?> run) {
        do {
            run = run.getPreviousBuild();
            if (run == null || run.isBuilding()) {
                return null;
            }
        } while (run.getResult() == Result.ABORTED || run.getResult() == Result.NOT_BUILT);
        return run.getResult();
    }
}
