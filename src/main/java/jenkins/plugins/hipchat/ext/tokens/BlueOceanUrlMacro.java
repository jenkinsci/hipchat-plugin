package jenkins.plugins.hipchat.ext.tokens;

import static jenkins.plugins.hipchat.model.Constants.*;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.model.TaskListener;
import java.util.Collections;
import java.util.List;
import org.jenkinsci.plugins.displayurlapi.DisplayURLProvider;
import org.jenkinsci.plugins.tokenmacro.DataBoundTokenMacro;

@Extension
public class BlueOceanUrlMacro extends DataBoundTokenMacro {

    @Override
    public String evaluate(AbstractBuild<?, ?> context, TaskListener listener, String macroName) {
        return evaluate(context, null, listener, macroName);
    }

    @Override
    public String evaluate(Run<?, ?> run, FilePath workspace, TaskListener listener, String macroName) {
        return DisplayURLProvider.get().getRunURL(run);
    }

    @Override
    public boolean acceptsMacroName(String macroName) {
        return BLUE_OCEAN_URL.equals(macroName);
    }

    @Override
    public List<String> getAcceptedMacroNames() {
        return Collections.singletonList(BLUE_OCEAN_URL);
    }
}
