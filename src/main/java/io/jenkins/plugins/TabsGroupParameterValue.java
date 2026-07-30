package io.jenkins.plugins;

import hudson.model.ParameterValue;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.kohsuke.stapler.DataBoundConstructor;

public class TabsGroupParameterValue extends ParameterValue {

    /**
     * Reserved key used to expose the selected tab name in the map returned by {@link #getValue()}.
     * Tab names and parameter names must not collide with this key.
     */
    public static final String SELECTED_TAB_KEY = "selectedTab";

    private final List<TabParametersValue> tabsValues;

    private final String selectedTab;

    @DataBoundConstructor
    public TabsGroupParameterValue(String name, List<TabParametersValue> tabsValues, String selectedTab) {
        super(name);
        this.tabsValues = tabsValues;
        this.selectedTab = selectedTab;
    }

    /**
     * Returns a nested map for Groovy map-style access in Jenkinsfile.
     * e.g. params.groupTab.tab1.myarg
     */
    @Override
    public Map<String, Object> getValue() {
        var result = new LinkedHashMap<String, Object>();
        for (TabParametersValue tab : tabsValues) {
            var paramMap = new LinkedHashMap<String, Object>();
            for (ParameterValue param : tab.getParameters()) {
                paramMap.put(param.getName(), param.getValue());
            }
            result.put(tab.getName(), paramMap);
        }
        result.put(SELECTED_TAB_KEY, selectedTab);
        return result;
    }

    public List<TabParametersValue> getTabsValues() {
        return tabsValues;
    }

    public String getSelectedTab() {
        return selectedTab;
    }

    public String getTabButtonId(TabParametersValue tab) {
        if (tab.getName().equals(selectedTab)) {
            return "selected";
        }
        return "not-selected";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        TabsGroupParameterValue that = (TabsGroupParameterValue) o;
        return Objects.equals(tabsValues, that.tabsValues);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), tabsValues);
    }
}
