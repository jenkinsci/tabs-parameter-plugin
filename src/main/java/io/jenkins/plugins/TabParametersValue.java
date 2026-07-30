package io.jenkins.plugins;

import hudson.model.ParameterValue;
import java.io.Serializable;
import java.util.List;
import org.kohsuke.stapler.DataBoundConstructor;

public class TabParametersValue implements Serializable {
    private final String name;
    private final List<ParameterValue> parameters;

    @DataBoundConstructor
    public TabParametersValue(String name, List<ParameterValue> parameters) {
        this.name = name;
        this.parameters = parameters;
    }

    public String getName() {
        return name;
    }

    public List<ParameterValue> getParameters() {
        return parameters;
    }

    @Override
    public String toString() {
        return "TabParametersValue{" + "name='" + name + '\'' + ", parameters=" + parameters + '}';
    }
}
