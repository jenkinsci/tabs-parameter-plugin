package io.jenkins.plugins;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import hudson.model.*;
import java.util.ArrayList;
import java.util.Iterator;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class TabsGroupParameterDefinitionTest {

    @Test
    void configRoundtrip(JenkinsRule jenkins) throws Exception {
        FreeStyleProject p = jenkins.createFreeStyleProject();

        p.addProperty(new ParametersDefinitionProperty(generateTabConfig()));
        jenkins.configRoundtrip(p);
        TabsGroupParameterDefinition tabsParamDefinition = (TabsGroupParameterDefinition)
                p.getProperty(ParametersDefinitionProperty.class).getParameterDefinition("tabsParam");
        assertEquals(2, tabsParamDefinition.getTabs().size());

        Iterator<TabParametersDefinition> iterator =
                tabsParamDefinition.getTabs().iterator();
        TabParametersDefinition next = iterator.next();
        assertEquals("tab'1", next.getName());
        assertEquals("toto", next.getParameters().stream().findFirst().get().getName());

        next = iterator.next();
        assertEquals("tab2", next.getName());
        assertEquals("my-bool", next.getParameters().stream().findFirst().get().getName());
    }

    @Test
    void testScriptedPipeline(JenkinsRule jenkins) throws Exception {
        var tabs = new ArrayList<TabParametersValue>();

        var tab1Params = new ArrayList<ParameterValue>();
        tab1Params.add(new StringParameterValue("toto", "tata"));
        tabs.add(new TabParametersValue("tab'1", tab1Params));

        var tab2Params = new ArrayList<ParameterValue>();
        tab2Params.add(new BooleanParameterValue("my-bool", true, "some boolean"));
        tabs.add(new TabParametersValue("tab2", tab2Params));

        var tabsGroupValue = new TabsGroupParameterValue("tabsParam", tabs, "tab'1");

        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-scripted-pipeline");
        job.addProperty(new ParametersDefinitionProperty(generateTabConfig()));
        String pipelineScript = """
                echo "Param toto equals : ${params.tabsParam["tab'1"].toto}"
                echo "Selected tab : ${params.tabsParam.selectedTab}"
                """;

        job.setDefinition(new CpsFlowDefinition(pipelineScript, true));
        WorkflowRun completedBuild =
                jenkins.assertBuildStatusSuccess(job.scheduleBuild2(0, new ParametersAction(tabsGroupValue)));
        jenkins.assertLogContains("Param toto equals : tata", completedBuild);
        jenkins.assertLogContains("Selected tab : tab'1", completedBuild);
    }

    @Test
    void createValueFromStringIsRejected() {
        UnsupportedOperationException exception = assertThrows(
                UnsupportedOperationException.class, () -> generateTabConfig().createValue("raw"));
        assertEquals(
                "String-based parameter parsing is not supported for 'tabsParam'. Use form submission instead.",
                exception.getMessage());
    }

    private TabsGroupParameterDefinition generateTabConfig() {
        var tabs = new ArrayList<TabParametersDefinition>();

        var tab1Params = new ArrayList<ParameterDefinition>();
        tab1Params.add(new StringParameterDefinition("toto", "def"));
        tabs.add(new TabParametersDefinition("tab'1", tab1Params));

        var tab2Params = new ArrayList<ParameterDefinition>();
        tab2Params.add(new BooleanParameterDefinition("my-bool", true, "some boolean"));
        tabs.add(new TabParametersDefinition("tab2", tab2Params));

        return new TabsGroupParameterDefinition("tabsParam", tabs);
    }
}
