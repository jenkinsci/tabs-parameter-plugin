package io.jenkins.plugins;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.model.SimpleParameterDefinition;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
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
public class TabsGroupParameterDefinition extends SimpleParameterDefinition {

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
        long selectedTabUidRaw = jo.getLong("selectedTabUid");
        var groupParameterValue = new TabsGroupParameterValue(name, new ArrayList<>(), selectedTabUidRaw);

        Object rawTabsValues = Objects.requireNonNull(jo.get("tabsValues"), "tabsValues must not be null");
        Iterable<Object> tabsValues = toIterable(rawTabsValues);

        tabsValues.forEach(tab -> {
            var tabJSONObject = JSONObject.fromObject(tab);
            var tabUid = tabJSONObject.getLong("uid");
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
    public TabsGroupParameterValue createValue(String value) {
        throw new UnsupportedOperationException("String-based parameter parsing is not supported for '" + getName()
                + "'. Use form submission instead.");
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
