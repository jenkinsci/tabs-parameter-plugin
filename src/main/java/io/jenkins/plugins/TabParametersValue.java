package io.jenkins.plugins;

import hudson.model.ParameterValue;
import java.io.Serializable;
import java.util.List;
import java.util.UUID;
import org.kohsuke.stapler.DataBoundConstructor;

public class TabParametersValue implements Serializable {
    private final UUID uid;
    private final String name;
    private final List<ParameterValue> parameters;

    @DataBoundConstructor
    public TabParametersValue(UUID uid, String name, List<ParameterValue> parameters) {
        this.uid = uid;
        this.name = name;
        this.parameters = parameters;
    }

    public String getName() {
        return name;
    }

    public List<ParameterValue> getParameters() {
        return parameters;
    }

    public UUID getUid() {
        return uid;
    }

    @Override
    public String toString() {
        return "TabParametersValue{uid=" + uid + " name='" + name + "', parameters=" + parameters + '}';
    }
}
