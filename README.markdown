# HipChat plugin for Jenkins

A Jenkins plugin that sends notifications to HipChat chat rooms for build events.

## Features

* Supports both v1 and v2 API (v1 API support to be removed in next major version)
* Can send notifications for the following build statuses
 * Build Started
 * Build Aborted
 * Build Failed
 * Not Built (e.g. when an earlier stage prevented a multi-stage build to finish)
 * Build Successful
 * Build Unstable (e.g. tests failed, but compilation was successful)
 * Back To Normal (e.g. when the current build was successful, but the previous wasn't for some reason)
* The room name can be parameterized
* Supports different notification modes for matrix builds
* Can be used from pipeline builds to send notifications
* Supports HipChat [card notifications](https://developer.atlassian.com/hipchat/guide/sending-messages)

## Configuration

The plugin allows two levels of configuration, each explained in the below sections. The assumption should be that if a project level setting can not be found, the plugin will fall back to the global configuration.

### Global configuration

These settings can be found under **Manage Jenkins** -> **Configure System** and look for **Global HipChat Notifier Settings**. The settings listed here are:

* **HipChat Server Host**: The hostname (and optionally the port number) for the HipChat server in use. Note that the server will *always* be accessed via HTTPS. Defaults to api.hipchat.com for cloud installations. The plugin will only connect to the server using TLS protocol, access via SSL protocol is disabled.
* **Use v2 API**: Whether to use HipChat v2 API when interacting with the HipChat server. Note: that support for v1 version of the API is going to be removed in the next major version.
* **Credentials**: The 'secret text' kind of credential to be used when accessing HipChat REST endpoints. When using v2 API, the Credentials must be a valid OAuth2 token obtained as described in the [v2 API documentation](https://developer.atlassian.com/hipchat/guide/hipchat-rest-api). When v2 API is disabled, the plugin will use v1 API to perform its operations. The v1 API requires API Tokens (which are different from OAuth2 tokens!).
* **Room**: Allows to specify the name or ID of the room where the notification(s) should be sent. If the name of the room is not known at the time of the configuration, you can also use build variables (e.g. $HIPCHAT_ROOM). Multiple values can be provided, in which case use comma to separate the values.
* **Send As**: Specifies the sender of the notification when using the v1 API. The value must be less than 15 characters long.
* **Card Provider**: Here you can select a card provider implementation which is responsible to render a HipChat Card as needed. See below for more information on custom Card providers. Cards are only supported when v2 version of the HipChat API is in use.
* **Default notifications**: Configure the default set of notifications. These will be used when there is no notification configured at the job level. Note that the default notifications are only sent if the HipChat Notifications Post Build Action is added to the job (otherwise the plugin won't get invoked).

### Job level configuration

To set up the plugin for an individual job, go to the job's configuration page and add the **HipChat Notifications** post-build action. The settings listed there are:

* **Credentials**: Use in case you have a room-specific token to override the globally set Credentials.
* **Project Room**: Allows to specify the name or ID of the room where the notification(s) should be sent. If the name of the room is not known at the time of the configuration, you can also use build variables (e.g. $HIPCHAT_ROOM). Multiple values can be provided, in which case use comma to separate the values.
* **Notifications**: The list of notifications that should be sent out upon build events. If this setting is left empty, the plugin will fall back to the Default notifications setting in the Global configuration. The settings available for each notification entry:
  * **Notify Room**: Whether the message should trigger a HipChat notification
  * **Text Format**: When checked, the message will be sent in text format, instead of the default HTML format. When using text format, emoticons will be properly displayed in messages, but links must be printed in full length.
  * **Notification Type**: Select which build status/result should trigger this notification.
  * **Color**: Select the color of the notification message.
  * **Message template**: The specific message template to use for this notification
* **Message templates**: These templates are used as default values when the notification-specific message template is not defined.
  * **Job started**: The message to use when the build starts.
  * **Job completed**: The message to use when the build completes regardless of the build result.

### Message template format

The message templates used by the plugin now fully support [token-macro](https://github.com/jenkinsci/token-macro-plugin) tokens, for a comprehensive list of the available tokens, please check out the help texts for the message template settings.

In addition to the out of the box supported tokens from the token-macro-plugin, the plugin can also utilize various other tokens provided by other plugins. Having the [email-ext-plugin](https://wiki.jenkins-ci.org/display/JENKINS/Email-ext+plugin) installed on Jenkins will make the following (non-comprehensive list of) tokens available for example:
* TRIGGER_NAME
* TEST_COUNTS
* FAILED_TESTS

If you find that one of the above listed tokens do not work with the plugin, you should probably check first whether the email-ext-plugin is installed on your Jenkins instance. The same rule applies for other third party token provider plugins.

The HipChat plugin also provides the following token implementations:

| Token name | Content | Example value |
| --- | --- | --- |
| BLUE_OCEAN_URL | [Blue Ocean UI](https://jenkins.io/projects/blueocean/) friendly link to the currently built job | http://localhost:8080/jenkins/job/foobar/1/display/redirect |
| BUILD_DURATION | The duration of the build in human readable format | 42 min |
| COMMIT_MESSAGE | The first line of the last changeset's commit message | Initial commit |
| HIPCHAT_CHANGES | Human readable details of the new changesets or "No changes" if changesets weren't computed for this build | Started by changes from bjensen (1 file(s) changed) |
| HIPCHAT_CHANGES_OR_CAUSE | Returns HIPCHAT_CHANGES if it was successfully calculated, otherwise returns the cause of the build | Started by user Admin |
| TEST_REPORT_URL | Direct link to the test reports | http://localhost:8080/jenkins/job/foobar/1/testReport |

### Proxy support

The plugin utilizes the proxy configuration in Jenkins when making external HTTPS connections. To configure proxy in Jenkins, follow the [Jenkins documentation](https://wiki.jenkins-ci.org/display/JENKINS/JenkinsBehindProxy).

The currently supported features are:
* authenticated proxies
* "No Proxy Host" setting

### Pipeline support

When using pipeline projects, HipChat messages can be sent using the following DSL:

```
hipchatSend color: 'YELLOW', credentialId: 'myid', failOnError: true, message: 'test', notify: true, room: 'Jenkins', sendAs: 'Jenkins', server: 'api.hipchat.com', textFormat: true, v2enabled: true
```

Note that the following parameters for the hipchatSend step are planned to be deprecated in the next major version:

* sendAs
* server
* token
* v2enabled

## Support for custom Card Providers

HipChat supports various kinds of cards for its notifications, as such the card implementation in the Jenkins HipChat plugin has been done in a pluggable manner. In case the out of the box available card implementations do not fit your needs, the following extension will need to be written:

```
@Extension
public class MyCoolCardProvider extends CardProvider {

    private static final Logger LOGGER = Logger.getLogger(MyCoolCardProvider.class.getName());

    @Override
    public Card getCard(Run<?, ?> run, TaskListener taskListener, String message) {
        // implement magic
    }

    @Override
    public CardProviderDescriptor getDescriptor() {
        return new DescriptorImpl();
    }

    @Extension
    public static class DescriptorImpl extends CardProviderDescriptor {

        @Override
        public String getDisplayName() {
            return "My cool card provider";
        }
    }
}
```

The return value type Card represents a HipChat card and exposes all of its available properties as it is defined in the [HipChat API documentation](https://developer.atlassian.com/hipchat/guide/sending-messages).

The idea behind the extensible approach is that lots of different card implementations can be made available. If you do end up writing a custom CardProvider, please open a pull request, so that others can benefit from it too. Having these custom implementations contributed should also ensure (to a reasonable degree) that future API changes will be reflected on these implementations as the changes are being made.
