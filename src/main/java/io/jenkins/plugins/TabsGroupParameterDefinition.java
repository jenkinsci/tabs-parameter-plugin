package io.jenkins.plugins;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import java.util.*;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest2;

/**
 * Entrypoint of tab parameters
 * Define a list of tabs {@link TabParametersDefinition} the contains other parameters
 * Parameters can be any Jenkins {@link ParameterDefinition}
 * <p>
 * The method {@link TabsGroupParameterDefinition#createValue(StaplerRequest2, JSONObject)} make the work of generating values of parameters inside the tabs
 */
public class TabsGroupParameterDefinition extends ParameterDefinition {

    private final List<TabParametersDefinition> tabs;

    @DataBoundConstructor
    public TabsGroupParameterDefinition(String name, List<TabParametersDefinition> tabs) {
        super(name);
        this.tabs = Objects.requireNonNull(tabs, "tabs must not be null");
    }

    @Override
    public TabsGroupParameterValue createValue(StaplerRequest2 req, JSONObject jo) {
        Objects.requireNonNull(req, "request must not be null");
        Objects.requireNonNull(jo, "request payload must not be null");

        String name = Objects.requireNonNull(jo.getString("name"), "parameter group name must not be null");
        var selectedTabUidRaw = UUID.fromString(jo.getString("selectedTabUid"));
        var groupParameterValue = new TabsGroupParameterValue(name, new ArrayList<>(), selectedTabUidRaw);

        Object rawTabsValues = Objects.requireNonNull(jo.get("tabsValues"), "tabsValues must not be null");
        Iterable<Object> tabsValues = toIterable(rawTabsValues);

        tabsValues.forEach(tab -> {
            var tabJSONObject = JSONObject.fromObject(tab);
            var tabUid = UUID.fromString(tabJSONObject.getString("uid"));
            var tabName = tabJSONObject.getString("name");

            var parametersValues = new ArrayList<ParameterValue>();
            if (tabJSONObject.containsKey("parameter")) {
                Iterable<Object> parameters = toIterable(tabJSONObject.get("parameter"));
                parameters.forEach(parameter -> {
                    JSONObject jsonParameter = JSONObject.fromObject(parameter);
                    var parameterName = jsonParameter.getString("name");
                    var paramDefinition = getParamDefinitionFromTab(parameterName);
                    parametersValues.add(paramDefinition.createValue(req, jsonParameter));
                });
            }
            groupParameterValue.getTabsValues().add(new TabParametersValue(tabUid, tabName, parametersValues));
        });

        return groupParameterValue;
    }

    private List<Object> toIterable(Object maybeIterable) {
        if (maybeIterable instanceof JSONArray jsonArray) {
            return jsonArray;
        } else {
            return Collections.singletonList(maybeIterable);
        }
    }

    private ParameterDefinition getParamDefinitionFromTab(String name) {
        for (TabParametersDefinition tab : tabs) {
            for (ParameterDefinition parameter : tab.getParameters()) {
                if (parameter.getName().equals(name)) {
                    return parameter;
                }
            }
        }
        throw new IllegalArgumentException(
                "Cannot find parameter definition " + name + " in " + this.getName() + " tab definition");
    }

    @Override
    public TabsGroupParameterValue createValue(StaplerRequest2 req) {
        Objects.requireNonNull(req, "request must not be null");

        var selectedIndex = parseSelectedTabIndex(req.getParameter(getName()));
        var tabsValues = new ArrayList<TabParametersValue>();
        for (TabParametersDefinition tab : tabs) {
            var parametersValues = new ArrayList<ParameterValue>();
            for (ParameterDefinition parameter : tab.getParameters()) {
                var value = parameter.createValue(req);
                if (value != null) {
                    parametersValues.add(value);
                }
            }
            tabsValues.add(new TabParametersValue(tab.getUid(), tab.getName(), parametersValues));
        }
        return new TabsGroupParameterValue(
                getName(), tabsValues, tabsValues.get(selectedIndex).getUid());
    }

    /** Zero-based index of the selected tab, the first tab when absent. */
    private int parseSelectedTabIndex(String raw) {
        if (raw == null || raw.isBlank()) {
            return 0;
        }
        try {
            int index = Integer.parseInt(raw.trim());
            if (index >= 0 && index < tabs.size()) {
                return index;
            }
        } catch (NumberFormatException ignored) {
            // reported below
        }
        throw new IllegalArgumentException(
                "Invalid tab index '" + raw + "' for '" + getName() + "', expected 0 to " + (tabs.size() - 1));
    }

    public List<TabParametersDefinition> getTabs() {
        return tabs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        TabsGroupParameterDefinition that = (TabsGroupParameterDefinition) o;
        return Objects.equals(tabs, that.tabs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), tabs);
    }

    @Override
    public String toString() {
        return "TabsGroupParameterDefinition{" + "tabs=" + tabs + '}';
    }

    @Extension
    @Symbol("tabsParam")
    public static final class ParameterDescriptorImpl extends ParameterDescriptor {
        @NonNull
        @Override
        public String getDisplayName() {
            return Messages.TabsGroupParameterDefinition_DisplayName();
        }
    }
}
