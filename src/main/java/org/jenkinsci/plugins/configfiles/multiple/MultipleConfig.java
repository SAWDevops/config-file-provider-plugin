package org.jenkinsci.plugins.configfiles.multiple;

import hudson.Extension;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.lib.configprovider.AbstractConfigProviderImpl;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.lib.configprovider.model.ContentType;
import org.jenkinsci.plugins.configfiles.Messages;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.*;

/**
 * Created by frostn on 01/06/2014.
 */
public class MultipleConfig extends Config implements Iterable<String> {
    private static final long serialVersionUID = 1L;
    private static final String SKIP = "#skip";

    private List<String> contents;
    private List<String> enableContents;
    private Iterator<String> iterator;

    @DataBoundConstructor
    public MultipleConfig(String id, String name, String comment, String content, List<String> contents) {
        super(id, name, comment, "");
        this.contents = new ArrayList<String>();
        this.enableContents = new ArrayList<String>();
        if (contents != null) {
            for (String currContent : contents) {
                if (!StringUtils.isBlank(currContent)) {
                    this.contents.add(currContent);
                    if (isEnabled(currContent)) {
                        this.enableContents.add(currContent);
                    }
                }
            }
        }
        iterator = this.enableContents.iterator();

        // The first execution that use this Config will copy a random content.
        // Rest of the executions will use the content in round robin manner.
        for (int randomSteps = (int) (this.enableContents.size() * Math.random());
             iterator.hasNext() && randomSteps > 0;
             randomSteps--) {
            iterator.next();
        }
    }

    public List<String> getContents() {
        return contents;
    }

    public Iterator<String> iterator() {
        if (!iterator.hasNext()) {
            iterator = enableContents.iterator();
        }
        return iterator;
    }

    private boolean isEnabled(String content) {
        return !StringUtils.isBlank(content) && !content.trim().toLowerCase().startsWith(SKIP);
    }

    @Extension(ordinal = 550)
    public static class MultipleConfigProvider extends AbstractConfigProviderImpl {

        public MultipleConfigProvider() {
            load();
        }

        @Override
        public ContentType getContentType() {
            return null;
        }

        @Override
        public String getDisplayName() {
            return Messages.multiple_provider_name();
        }

        @Override
        public Config newConfig() {
            String id = getProviderId() + System.currentTimeMillis();
            return new MultipleConfig(id, "MyMultiple", "", "", null);
        }

        // ======================
        // start stuff for backward compatibility
        protected transient String ID_PREFIX;

        @Override
        public boolean isResponsibleFor(String configId) {
            return super.isResponsibleFor(configId) || configId.startsWith("MultipleConfigProvider.");
        }

        @Override
        protected String getXmlFileName() {
            return "multiple-config-files.xml";
        }

        static {
            Jenkins.XSTREAM.alias("org.jenkinsci.plugins.configfiles.multiple.MultipleConfigProvider", MultipleConfigProvider.class);
        }
        // end stuff for backward compatibility
        // ======================

    }
}
