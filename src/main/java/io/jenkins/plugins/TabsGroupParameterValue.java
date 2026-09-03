package io.jenkins.plugins;

import hudson.EnvVars;
import hudson.model.ParameterValue;
import hudson.model.Run;
import java.util.*;
import org.kohsuke.stapler.DataBoundConstructor;

public class TabsGroupParameterValue extends ParameterValue {

    /**
     * Reserved key used to expose the selected tab name in the map returned by {@link #getValue()}.
     * Tab names must not collide with this key.
     */
    public static final String SELECTED_TAB_KEY = "selectedTab";
    /**
     * To prevent users from overriding selected tab key, we use another key to put the params values
     */
    public static final String SELECTED_TAB_PARAMS_KEY = "selectedParams";

    private final List<TabParametersValue> tabsValues;

    private final UUID selectedTabUid;

    @DataBoundConstructor
    public TabsGroupParameterValue(String name, List<TabParametersValue> tabsValues, UUID selectedTabUid) {
        super(name);
        this.tabsValues = Objects.requireNonNull(tabsValues, "tabsValues must not be null");
        this.selectedTabUid = selectedTabUid;
    }

    /**
     * Returns a nested map for Groovy map-style access in Jenkinsfile.
     * e.g. params.groupTab.tab1.myarg
     */
    @Override
    public Map<String, Object> getValue() {
        var result = new LinkedHashMap<String, Object>();
        tabsValues.stream()
                .filter(tabParametersValue -> Objects.equals(tabParametersValue.getUid(), selectedTabUid))
                .findFirst()
                .ifPresent(tab -> {
                    Map<String, Object> paramMap = new LinkedHashMap<>();
                    for (ParameterValue param : tab.getParameters()) {
                        paramMap.put(param.getName(), param.getValue());
                    }
                    result.put(SELECTED_TAB_PARAMS_KEY, paramMap);
                    result.put(SELECTED_TAB_KEY, tab.getName());
                });
        return result;
    }

    @Override
    public void buildEnvironment(Run<?, ?> build, EnvVars env) {
        tabsValues.stream()
                .filter(tabParametersValue -> Objects.equals(tabParametersValue.getUid(), selectedTabUid))
                .findFirst()
                .ifPresent(tab -> {
                    for (ParameterValue param : tab.getParameters()) {
                        param.buildEnvironment(build, env);
                        var value = env.get(param.getName());
                        env.put(name + "." + SELECTED_TAB_PARAMS_KEY + "." + param.getName(), value);
                    }
                    env.put(name + "." + SELECTED_TAB_KEY, tab.getName());
                });
    }

    public List<TabParametersValue> getTabsValues() {
        return tabsValues;
    }

    public UUID getSelectedTabUid() {
        return selectedTabUid;
    }

    public boolean isSelectedTab(TabParametersValue tab) {
        return Objects.equals(tab.getUid(), selectedTabUid);
    }

    public TabParametersValue getSelectedTab() {
        return tabsValues.stream()
                .filter(tabParametersValue -> Objects.equals(tabParametersValue.getUid(), selectedTabUid))
                .findFirst()
                .orElse(null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        TabsGroupParameterValue that = (TabsGroupParameterValue) o;
        return Objects.equals(tabsValues, that.tabsValues) && Objects.equals(selectedTabUid, that.selectedTabUid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), tabsValues, selectedTabUid);
    }
}
