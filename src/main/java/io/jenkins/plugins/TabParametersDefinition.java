package io.jenkins.plugins;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.ParameterDefinition;
import java.io.Serializable;
import java.util.List;
import java.util.UUID;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Describe a tab and parameters contained inside
 */
public class TabParametersDefinition implements Describable<TabParametersDefinition>, Serializable {

    /**
     * UUID for tab lookup
     * Transient because we don't want it in the config.xml, to keep history of "config history plugin" clean
     */
    private transient UUID uid;
    /**
     * Name of the tab
     */
    private final String name;
    /**
     * All parameters inside the tab
     */
    private final List<ParameterDefinition> parameters;

    @DataBoundConstructor
    public TabParametersDefinition(String name, List<ParameterDefinition> parameters) {
        this.uid = null;
        this.name = name;
        this.parameters = parameters;
        if (this.parameters.stream().anyMatch(param -> param instanceof TabsGroupParameterDefinition)) {
            throw new IllegalArgumentException(
                    "TabsGroupParameterDefinition cannot be nested inside TabParametersDefinition");
        }
        if (this.parameters.stream()
                .anyMatch(param -> TabsGroupParameterValue.SELECTED_TAB_KEY.equals(param.getName()))) {
            throw new IllegalArgumentException("Parameter name '" + TabsGroupParameterValue.SELECTED_TAB_KEY
                    + "' is reserved and cannot be used inside a tab");
        }
    }

    public List<ParameterDefinition> getParameters() {
        return parameters;
    }

    public String getName() {
        return name;
    }

    public UUID getUid() {
        if (uid == null) {
            uid = UUID.randomUUID();
        }
        return uid;
    }

    @Override
    @NonNull
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) Jenkins.get().getDescriptorOrDie(getClass());
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<TabParametersDefinition> {
        @NonNull
        @Override
        public String getDisplayName() {
            return Messages.TabParametersDefinition_DescriptorImpl_DisplayName();
        }

        public List<ParameterDefinition.ParameterDescriptor> getParametersDescriptors() {
            return ExtensionList.lookup(ParameterDefinition.ParameterDescriptor.class).stream()
                    .filter(descriptor -> descriptor.clazz != TabsGroupParameterDefinition.class)
                    .toList();
        }
    }
}
