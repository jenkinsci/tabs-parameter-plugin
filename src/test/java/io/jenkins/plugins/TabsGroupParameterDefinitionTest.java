package io.jenkins.plugins;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import hudson.model.*;
import java.lang.reflect.Proxy;
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
import org.kohsuke.stapler.StaplerRequest2;

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
    void groupValueRejectsOutOfRangeSelectedTabIndex() {
        for (int index : new int[] {-1, 1}) {
            var tabs = List.of(new TabParametersValue("tab", List.of()));
            assertThrows(IllegalArgumentException.class, () -> new TabsGroupParameterValue("tabsParam", tabs, index));
        }
    }

    @Test
    void groupValueKeepsExistingTabUid() {
        var uid = UUID.randomUUID();
        var tab = new TabParametersValue(uid, "tab", List.of());

        var value = new TabsGroupParameterValue("tabsParam", List.of(tab), 0);

        assertEquals(uid, value.getSelectedTabUid());
    }

    @Test
    void tabUidsAreSetWhenLoadedFromConfig(JenkinsRule jenkins) throws Exception {
        FreeStyleProject p = jenkins.createFreeStyleProject();
        p.addProperty(new ParametersDefinitionProperty(generateTabConfig().get(0)));
        p.save();
        jenkins.getInstance().reload();

        var reloaded = (TabsGroupParameterDefinition) jenkins.getInstance()
                .getItemByFullName(p.getFullName(), FreeStyleProject.class)
                .getProperty(ParametersDefinitionProperty.class)
                .getParameterDefinition("tabsParam");
        assertEquals(
                TabsGroupParameterDefinition.tabUid("tabsParam", 1),
                reloaded.getTabs().get(1).getUid());
    }

    @Test
    void buildStepAcceptsTabsDeclaration(JenkinsRule jenkins) throws Exception {
        WorkflowJob downstream = jenkins.createProject(WorkflowJob.class, "downstream");
        downstream.addProperty(
                new ParametersDefinitionProperty(generateTabConfig().get(0)));
        downstream.setDefinition(new CpsFlowDefinition(
                "echo \"my-bool=${params.tabsParam.selectedParams.'my-bool'} tab=${params.tabsParam.selectedTab}\"",
                true));

        WorkflowJob upstream = jenkins.createProject(WorkflowJob.class, "upstream");
        upstream.setDefinition(new CpsFlowDefinition("""
                build job: 'downstream', wait: true, parameters: [
                    [$class: 'TabsGroupParameterValue', name: 'tabsParam', selectedTabIndex: 1, tabsValues: [
                        [name: "tab'1", parameters: [
                            [$class: 'StringParameterValue', name: 'toto', value: 'remote']
                        ]],
                        [name: 'tab2', parameters: [
                            [$class: 'BooleanParameterValue', name: 'my-bool', value: false]
                        ]]
                    ]]
                ]
                """, true));

        jenkins.assertBuildStatusSuccess(upstream.scheduleBuild2(0));
        jenkins.assertLogContains("my-bool=false tab=tab2", downstream.getLastBuild());
    }

    @Test
    void createValueFromRequestReadsRequestParameters(JenkinsRule jenkins) {
        var definition = generateTabConfig().get(0);
        var req = requestWithParameters(Map.of("tabsParam", "1", "toto", "custom"));

        var value = definition.createValue(req);

        assertEquals("tabsParam", value.getName());
        assertEquals(2, value.getTabsValues().size());
        // "1" selects the second tab, tab2
        assertEquals("tab2", value.getSelectedTab().getName());
        // the selected tab has no value in the request, so it keeps its default
        assertEquals(Map.of("my-bool", true), value.getValue().get(TabsGroupParameterValue.SELECTED_TAB_PARAMS_KEY));
        // the other tab uses the value from the request
        var firstTabParameters = value.getTabsValues().get(0).getParameters();
        assertEquals("custom", firstTabParameters.get(0).getValue());
    }

    @Test
    void createValueFromRequestRejectsInvalidTabIndex(JenkinsRule jenkins) {
        var definition = generateTabConfig().get(0);

        assertThrows(
                IllegalArgumentException.class,
                () -> definition.createValue(requestWithParameters(Map.of("tabsParam", "5"))));
        assertThrows(
                IllegalArgumentException.class,
                () -> definition.createValue(requestWithParameters(Map.of("tabsParam", "abc"))));
        assertThrows(
                IllegalArgumentException.class,
                () -> definition.createValue(requestWithParameters(Map.of("tabsParam", "-1"))));
        assertEquals(
                "tab'1",
                definition
                        .createValue(requestWithParameters(Map.of("toto", "x")))
                        .getSelectedTab()
                        .getName());
        assertEquals(
                "tab'1",
                definition
                        .createValue(requestWithParameters(Map.of("tabsParam", " ")))
                        .getSelectedTab()
                        .getName());
    }

    @Test
    void invalidTabsAreRejected(JenkinsRule jenkins) {
        var nested = new TabsGroupParameterDefinition("inner", List.of());
        assertThrows(IllegalArgumentException.class, () -> new TabParametersDefinition("tab", List.of(nested)));
        assertThrows(
                IllegalArgumentException.class,
                () -> new TabParametersDefinition(
                        "tab", List.of(new StringParameterDefinition(TabsGroupParameterValue.SELECTED_TAB_KEY, ""))));
        assertThrows(
                IllegalArgumentException.class,
                () -> new TabsGroupParameterValue(
                        "tabsParam", new ArrayList<>(List.of(new TabParametersValue("tab", List.of()))), 1));
        assertThrows(
                IllegalArgumentException.class,
                () -> new TabsGroupParameterValue(
                        "tabsParam", new ArrayList<>(List.of(new TabParametersValue("tab", List.of()))), -1));
    }

    @Test
    void valueKeepsExistingTabUids(JenkinsRule jenkins) {
        var uid = UUID.randomUUID();
        var value = new TabsGroupParameterValue(
                "tabsParam", new ArrayList<>(List.of(new TabParametersValue(uid, "tab", List.of()))), 0);

        assertEquals(uid, value.getSelectedTabUid());
    }

    @Test
    void copyWithDefaultValueKeepsDefaultsWithoutPreviousValue(JenkinsRule jenkins) {
        var definition = generateTabConfig().get(0);
        var previousTab = new TabParametersValue(
                definition.getTabs().get(0).getUid(), "tab'1", List.of(new StringParameterValue("other", "x")));
        var unknownTab = new TabParametersValue(UUID.randomUUID(), "gone", List.of());
        var previous = new TabsGroupParameterValue(
                "tabsParam", new ArrayList<>(List.of(previousTab, unknownTab)), previousTab.getUid());

        var copy = (TabsGroupParameterDefinition) definition.copyWithDefaultValue(previous);

        assertEquals(2, copy.getTabs().size());
        assertEquals(
                "def",
                copy.getTabs()
                        .get(0)
                        .getParameters()
                        .get(0)
                        .getDefaultParameterValue()
                        .getValue());
        assertEquals(previousTab.getUid(), copy.getInitialTabUid());
        assertNull(new TabsGroupParameterDefinition("empty", List.of()).getInitialTabUid());
    }

    @Test
    void copyWithDefaultValueIgnoresOtherValueTypes(JenkinsRule jenkins) {
        var definition = generateTabConfig().get(0);

        assertEquals(definition, definition.copyWithDefaultValue(new StringParameterValue("tabsParam", "x")));
        assertEquals(definition.getTabs().get(0).getUid(), definition.getInitialTabUid());
    }

    private StaplerRequest2 requestWithParameters(Map<String, String> parameters) {
        return (StaplerRequest2) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[] {StaplerRequest2.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getParameter" -> {
                            return parameters.get((String) args[0]);
                        }
                        case "getParameterValues" -> {
                            var value = parameters.get((String) args[0]);
                            return value == null ? null : new String[] {value};
                        }
                        default -> {
                            return null;
                        }
                    }
                });
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

    @Test
    void rebuildPagePrefillsPreviousValues(JenkinsRule jenkins) throws Exception {
        var definition = generateTabConfig().get(0);
        FreeStyleProject p = jenkins.createFreeStyleProject();
        p.addProperty(new ParametersDefinitionProperty(definition));

        var secondTab = definition.getTabs().get(1);
        var tabs = List.of(
                new TabParametersValue(
                        definition.getTabs().get(0).getUid(),
                        "tab'1",
                        List.of(new StringParameterValue("toto", "previous"))),
                new TabParametersValue(
                        secondTab.getUid(), "tab2", List.of(new BooleanParameterValue("my-bool", false, "d"))));
        var value = new TabsGroupParameterValue("tabsParam", new ArrayList<>(tabs), secondTab.getUid());
        FreeStyleBuild build = jenkins.assertBuildStatusSuccess(p.scheduleBuild2(0, new ParametersAction(value)));

        JenkinsRule.WebClient webClient = jenkins.createWebClient().withThrowExceptionOnFailingStatusCode(false);
        HtmlPage page = webClient.getPage(build, "rebuild/parameterized");

        HtmlInput selectedTabInput = (HtmlInput) page.getElementById("selected-tab-input-tabsParam");
        assertEquals(secondTab.getUid().toString(), selectedTabInput.getValue());
        assertEquals(
                "previous",
                page.getElementsByName("value").stream()
                        .map(e -> ((HtmlInput) e))
                        .filter(e -> "text".equals(e.getTypeAttribute()))
                        .findFirst()
                        .orElseThrow()
                        .getValue());
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
