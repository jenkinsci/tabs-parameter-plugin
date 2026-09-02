package io.jenkins.plugins;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.ParameterDefinition;
import hudson.util.FormValidation;
import java.io.Serializable;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

/**
 * Describe a tab and parameters contained inside
 */
public class TabParametersDefinition implements Describable<TabParametersDefinition>, Serializable {

    /**
     * UUID for tab lookup
     */
    private final long uid;
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
        this.uid = ThreadLocalRandom.current().nextLong();
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

    public long getUid() {
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

        // lgtm[jenkins/no-permission-check]
        @POST
        public FormValidation doCheckName(@QueryParameter String name) {
            if (name.isEmpty())
                return FormValidation.error(Messages.TabParametersDefinition_DescriptorImpl_NameEmpty());
            return FormValidation.ok();
        }
    }
}
