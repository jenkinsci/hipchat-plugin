package jenkins.plugins.hipchat.ext.tokens;

import static jenkins.plugins.hipchat.model.Constants.*;

import com.google.common.collect.ListMultimap;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.test.AbstractTestResultAction;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;

@Extension
public class TestReportUrlMacro extends TokenMacro {

    @Override
    public boolean acceptsMacroName(String macroName) {
        return TEST_REPORT_URL.equals(macroName);
    }

    @Override
    public String evaluate(AbstractBuild<?, ?> context, TaskListener listener, String macroName,
            Map<String, String> arguments, ListMultimap<String, String> argumentMultimap) {
        return evaluate(context, null, listener, macroName, arguments, argumentMultimap);
    }

    @Override
    public String evaluate(Run<?, ?> run, FilePath workspace, TaskListener listener, String macroName,
            Map<String, String> arguments, ListMultimap<String, String> argumentMultimap) {
        AbstractTestResultAction testResults = run.getAction(AbstractTestResultAction.class);
        if (testResults != null) {
            return BUILD_URL_MACRO + testResults.getUrlName();
        } else {
            return "";
        }
    }

    @Override
    public boolean hasNestedContent() {
        return true;
    }

    @Override
    public List<String> getAcceptedMacroNames() {
        return Collections.singletonList(TEST_REPORT_URL);
    }
}
