package jenkins.plugins.hipchat.utils;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.common.AbstractIdCredentialsListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.domains.DomainSpecification;
import com.cloudbees.plugins.credentials.domains.HostnameSpecification;
import com.cloudbees.plugins.credentials.domains.SchemeSpecification;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import jenkins.plugins.hipchat.HipChatNotifier;
import jenkins.plugins.hipchat.HipChatNotifier.DescriptorImpl;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;

import javax.annotation.CheckForNull;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * This class is here to help with credential related tasks, such as credential lookup and migration of insecurely
 * stored credentials.
 */
@Singleton
public class CredentialUtils {

    /**
     * Finds the credential with the given credentialId in the CredentialStore.
     *
     * @param context The context (job) to be used to find the right credential.
     * @param credentialId The ID of the credential.
     * @param server The URL to the HipChat server to ensure that we find the credential under the right security
     * domain.
     * @return The found credential, or null if the credential cannot be found.
     */
    public StringCredentials resolveCredential(Item context, @CheckForNull String credentialId, String server) {
        return credentialId == null ? null
                : CredentialsMatchers.firstOrNull(CredentialsProvider.lookupCredentials(StringCredentials.class,
                context, ACL.SYSTEM, requirements(server)),
                CredentialsMatchers.withId(credentialId));
    }

    /**
     * Retrieves the UI model object containing all acceptable credentials. This method can operate in two modes
     * essentially:
     * <ul>
     *     <li>When item is null: in this case the credentials will be looked up globally. In this case the assumption
     *     is that we are displaying the credential dropdown on the global config page.</li>
     *     <li>When item is not null: in this case the credentials will be looked up within the context of the job. In
     *     this case the assumption is that we are displaying the credential dropdown on the job config page.</li>
     * </ul>
     *
     * @param item The context (job) to use to find the the credentials. May be null. In job config mode, the current
     * value of the credential setting will be extracted from this item.
     * @param globalCredentialId In global config mode, use this as the currently selected credential.
     * @param server The URL to the HipChat server to ensure that we find the credentials under the right security
     * domain.
     * @return The UI model containing all matching credentials, or only the current selection if the user does not have
     * the right set of permissions.
     */
    public ListBoxModel getAvailableCredentials(@CheckForNull Item item, String globalCredentialId, String server) {
        String currentValue = getCurrentlySelectedCredentialId(item, globalCredentialId);
        if ((item == null && !Jenkins.getInstance().hasPermission(Jenkins.ADMINISTER))
                || item != null && !item.hasPermission(Item.EXTENDED_READ)) {
            return new StandardListBoxModel().includeCurrentValue(currentValue);
        }
        AbstractIdCredentialsListBoxModel<StandardListBoxModel, StandardCredentials> model = new StandardListBoxModel()
                .includeEmptyValue();
        if (item == null) {
            model = model.includeAs(ACL.SYSTEM, Jenkins.getInstance(), StringCredentials.class, requirements(server));
        } else {
            model = model.includeAs(ACL.SYSTEM, item, StringCredentials.class, requirements(server));
        }
        if (currentValue != null) {
            model = model.includeCurrentValue(currentValue);
        }
        return model;
    }

    /**
     * Migrates the credential stored in global config from the old insecure format to the Credential system.
     *
     * @param descriptor The descriptor of this plugin.
     * @throws IOException If there was an error whilst migrating the credential.
     */
    public void migrateGlobalCredential(DescriptorImpl descriptor) throws IOException {
        List<StringCredentials> credentials = CredentialsProvider.lookupCredentials(StringCredentials.class,
                Jenkins.getInstance(), ACL.SYSTEM, requirements(descriptor.getServer()));
        String room = Util.fixEmpty(descriptor.getRoom()) != null ? descriptor.getRoom() : "Global";
        String credentialId = storeCredential(descriptor, credentials, room, descriptor.getToken());

        descriptor.setToken(null);
        descriptor.setCredentialId(credentialId);
    }

    /**
     * Migrates the credential stored in a job config from the old insecure format to the Credential system.
     *
     * @param descriptor The descriptor of this plugin.
     * @param item The job where the plugin is configured.
     * @param notifier The plugin instance corresponding to this job.
     * @throws IOException If there was an error whilst migrating the credential.
     */
    public void migrateJobCredential(DescriptorImpl descriptor, Item item, HipChatNotifier notifier)
            throws IOException {
        List<StringCredentials> credentials = CredentialsProvider.lookupCredentials(StringCredentials.class, item,
                ACL.SYSTEM, requirements(descriptor.getServer()));
        String room = Util.fixEmpty(notifier.getRoom()) != null ? notifier.getRoom() : descriptor.getRoom();
        String credentialId = storeCredential(descriptor, credentials, room, notifier.getToken());

        notifier.setToken(null);
        notifier.setCredentialId(credentialId);
    }

    private String storeCredential(DescriptorImpl descriptor, List<StringCredentials> credentials, String room,
            String token) throws IOException {
        String server = descriptor.getServer();
        List<String> takenIds = new ArrayList<String>();
        for (StringCredentials credential : credentials) {
            takenIds.add(credential.getId());
            if (credential.getId().startsWith("HipChat-") && token.equals(Secret.toString(credential.getSecret()))) {
                return credential.getId();
            }
        }

        CredentialsStore store = CredentialsProvider.lookupStores(Jenkins.getInstance()).iterator().next();
        String id = generateCredentialId(descriptor, takenIds, room);
        BaseStandardCredentials credential = new StringCredentialsImpl(CredentialsScope.GLOBAL, id, id,
                Secret.fromString(token));
        if (store.isDomainsModifiable()) {
            Domain domain = store.getDomainByName(server);
            if (domain == null) {
                List<DomainSpecification> specs = new ArrayList<DomainSpecification>();
                specs.add(new HostnameSpecification(server, null));
                specs.add(new SchemeSpecification("https"));
                domain = new Domain(server, null, specs);
                store.addDomain(domain, credential);
            } else {
                store.addCredentials(domain, credential);
            }
        } else {
            store.addCredentials(Domain.global(), credential);
        }

        return credential.getId();
    }

    private String getCurrentlySelectedCredentialId(Item item, String globalCredentialId) {
        if (item == null) {
            return globalCredentialId;
        } else if (item instanceof AbstractProject) {
            HipChatNotifier notifier = ((AbstractProject<?, ?>) item).getPublishersList().get(HipChatNotifier.class);
            return notifier == null ? null : notifier.getCredentialId();
        } else {
            return null;
        }
    }

    private String generateCredentialId(DescriptorImpl descriptor, List<String> takenIds, String room) {
        //Simplify the room name
        room = Util.fixEmpty(room) == null ? UUID.randomUUID().toString() : room.split(",")[0];
        String id = "HipChat-" + (descriptor.isV2Enabled() ? room + "-Token" : "API-Token")
                .replaceAll("[^a-zA-Z0-9_.-]", "_");
        String candidate = id;
        int i = 2;
        while (takenIds.contains(candidate)) {
            candidate = id + "-" + i++;
        }
        return candidate;
    }

    private List<DomainRequirement> requirements(String server) {
        return URIRequirementBuilder.fromUri("https://" + server).build();
    }
}
