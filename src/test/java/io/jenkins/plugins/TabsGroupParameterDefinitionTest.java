package io.jenkins.plugins;

import hudson.model.*;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.util.ArrayList;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.assertEquals;

@WithJenkins
class TabsGroupParameterDefinitionTest {

    @Test
    void configRoundtrip(JenkinsRule jenkins) throws Exception {
        FreeStyleProject p = jenkins.createFreeStyleProject();


        p.addProperty(new ParametersDefinitionProperty(generateTabConfig()));
        jenkins.configRoundtrip(p);
        TabsGroupParameterDefinition tabsParamDefinition = (TabsGroupParameterDefinition) p.getProperty(ParametersDefinitionProperty.class).getParameterDefinition("tabsParam");
        assertEquals(2, tabsParamDefinition.getTabs().size());

        Iterator<TabParametersDefinition> iterator = tabsParamDefinition.getTabs().iterator();
        TabParametersDefinition next = iterator.next();
        assertEquals("tab1", next.getName());
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
        tabs.add(new TabParametersValue("tab1", tab1Params));

        var tab2Params = new ArrayList<ParameterValue>();
        tab2Params.add(new BooleanParameterValue("my-bool", true, "some boolean"));
        tabs.add(new TabParametersValue("tab2", tab2Params));

        var tabsGroupValue = new TabsGroupParameterValue("tabsParam", tabs, "tab1");


        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-scripted-pipeline");
        job.addProperty(new ParametersDefinitionProperty(generateTabConfig()));
        String pipelineScript = "echo \"Param toto equals : ${params.tabsParam.tab1.toto}\"";

        job.setDefinition(new CpsFlowDefinition(pipelineScript, true));
        WorkflowRun completedBuild = jenkins.assertBuildStatusSuccess(job.scheduleBuild2(0, new ParametersAction(tabsGroupValue)));
        String expectedString = "Param toto equals : tata";
        jenkins.assertLogContains(expectedString, completedBuild);
    }

    private TabsGroupParameterDefinition generateTabConfig() {
        var tabs = new ArrayList<TabParametersDefinition>();

        var tab1Params = new ArrayList<ParameterDefinition>();
        tab1Params.add(new StringParameterDefinition("toto", "def"));
        tabs.add(new TabParametersDefinition("tab1", tab1Params));

        var tab2Params = new ArrayList<ParameterDefinition>();
        tab2Params.add(new BooleanParameterDefinition("my-bool", true, "some boolean"));
        tabs.add(new TabParametersDefinition("tab2", tab2Params));

        return new TabsGroupParameterDefinition("tabsParam", tabs);
    }

}