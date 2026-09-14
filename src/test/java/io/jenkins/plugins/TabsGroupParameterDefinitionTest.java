package io.jenkins.plugins;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import hudson.model.*;
import java.util.*;
import jenkins.plugins.parameter_separator.ParameterSeparatorValue;
import org.htmlunit.html.HtmlAnchor;
import org.htmlunit.html.HtmlInput;
import org.htmlunit.html.HtmlPage;
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

        List<TabsGroupParameterDefinition> definitions = generateTabConfig();
        for (TabsGroupParameterDefinition tabsGroupParameterDefinition : definitions) {
            p.addProperty(new ParametersDefinitionProperty(tabsGroupParameterDefinition));
        }
        jenkins.configRoundtrip(p);
        TabsGroupParameterDefinition tabsParamDefinition = definitions.get(0);
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
        tab1Params.add(new ParameterSeparatorValue("nullable", null, null, null));
        UUID uid = UUID.randomUUID();
        tabs.add(new TabParametersValue(uid, "tab'1", tab1Params));

        var tab2Params = new ArrayList<ParameterValue>();
        tab2Params.add(new BooleanParameterValue("my-bool", true, "some boolean"));
        tabs.add(new TabParametersValue(UUID.randomUUID(), "tab2", tab2Params));

        var tabsGroupValue = new TabsGroupParameterValue("tabsParam", tabs, uid);

        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-scripted-pipeline");
        for (TabsGroupParameterDefinition tabsGroupParameterDefinition : generateTabConfig()) {
            job.addProperty(new ParametersDefinitionProperty(tabsGroupParameterDefinition));
        }
        String pipelineScript = """
                echo "Param toto equals : ${params.tabsParam.selectedParams.toto}"
                echo "Param nullable equals : ${params.tabsParam.selectedParams.nullable}"
                echo "Selected tab : ${params.tabsParam.selectedTab}"
                echo "Env toto equals : ${env.('tabsParam.selectedParams.toto')}"
                echo "Env nullable equals : ${env.('tabsParam.selectedParams.nullable')}"
                echo "Env Selected tab : ${env.('tabsParam.selectedTab')}"
                """;

        job.setDefinition(new CpsFlowDefinition(pipelineScript, true));
        WorkflowRun completedBuild =
                jenkins.assertBuildStatusSuccess(job.scheduleBuild2(0, new ParametersAction(tabsGroupValue)));
        jenkins.assertLogContains("Param toto equals : tata", completedBuild);
        jenkins.assertLogContains("Param nullable equals : ", completedBuild);
        jenkins.assertLogContains("Selected tab : tab'1", completedBuild);
        jenkins.assertLogContains("Env toto equals : tata", completedBuild);
        jenkins.assertLogContains("Env nullable equals : ", completedBuild);
        jenkins.assertLogContains("Env Selected tab : tab'1", completedBuild);
    }

    @Test
    void createValueFromStringIsRejected() {
        var tabsGroupParameterDefinition =
                generateTabConfig().stream().findFirst().get();
        UnsupportedOperationException exception = assertThrows(
                UnsupportedOperationException.class, () -> tabsGroupParameterDefinition.createValue("raw"));
        assertEquals(
                "String-based parameter parsing is not supported for 'tabsParam'. Use form submission instead.",
                exception.getMessage());
    }

    @Test
    void whenTabIsClicked_thenSelectedTabIsUpdated(JenkinsRule jenkins) throws Exception {
        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-tab-click");
        TabsGroupParameterDefinition tabsConfig = null;
        for (TabsGroupParameterDefinition tabsGroupParameterDefinition : generateTabConfig()) {
            job.addProperty(new ParametersDefinitionProperty(tabsGroupParameterDefinition));
            if (tabsGroupParameterDefinition.getName().equals("tabsParam")) {
                tabsConfig = tabsGroupParameterDefinition;
            }
        }

        List<TabParametersDefinition> tabs = new ArrayList<>(tabsConfig.getTabs());
        String firstTabUid = tabs.get(0).getUid().toString();
        String secondTabUid = tabs.get(1).getUid().toString();

        JenkinsRule.WebClient webClient = jenkins.createWebClient()
                // ParametersDefinitionProperty/index.jelly sends a 405 but really it is OK
                .withThrowExceptionOnFailingStatusCode(false);
        ;
        HtmlPage page = webClient.getPage(job, "build");

        HtmlInput selectedTabInput = (HtmlInput) page.getElementById("selected-tab-input-tabsParam");
        assertEquals(firstTabUid, selectedTabInput.getValue());

        HtmlAnchor secondTabLink = page.getAnchors().stream()
                .filter(anchor -> "tab2".equals(anchor.getTextContent()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Could not find tab link for 'tab2'"));

        secondTabLink.click();

        assertEquals(secondTabUid, selectedTabInput.getValue());
    }

    private List<TabsGroupParameterDefinition> generateTabConfig() {
        var tabs = new ArrayList<TabParametersDefinition>();

        var tab1Params = new ArrayList<ParameterDefinition>();
        tab1Params.add(new StringParameterDefinition("toto", "def"));
        tabs.add(new TabParametersDefinition("tab'1", tab1Params));

        var tab2Params = new ArrayList<ParameterDefinition>();
        tab2Params.add(new BooleanParameterDefinition("my-bool", true, "some boolean"));
        tabs.add(new TabParametersDefinition("tab2", tab2Params));

        // Test duplicated tabs with same names should not interfere with the rest of the UI
        var tabsDuplicate = new ArrayList<TabParametersDefinition>();
        var tab2ParamsDuplicate = new ArrayList<ParameterDefinition>();
        tab2ParamsDuplicate.add(new BooleanParameterDefinition("my-bool", true, "some boolean"));
        tabsDuplicate.add(new TabParametersDefinition("tab2", tab2ParamsDuplicate));

        var returnSet = new ArrayList<TabsGroupParameterDefinition>();
        returnSet.add(new TabsGroupParameterDefinition("tabsParam", tabs));
        returnSet.add(new TabsGroupParameterDefinition("tabsParamDuplicate", tabsDuplicate));
        return returnSet;
    }
}
