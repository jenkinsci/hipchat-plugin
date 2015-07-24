## HipChat plugin for Jenkins

A Jenkins plugin that can send notifications to HipChat chat rooms for build events.

### Features

* Supports both v1 and v2 API
* Can send notifications for the following build events:
 * Build Start
 * Aborted
 * Failure
 * Not Built
 * Success
 * Unstable
 * Back To Normal
* The room name can be parameterized
* Supports different notification modes for matrix builds

### Proxy settings

The plugin utilizes the proxy configuration in Jenkins when making external HTTPS connections.

### Configuration

When using v1 API, an API token needs to be provided, otherwise an OAuth2 access token with send_notification scope
 shall be used.

#### Build-flow-plugin support

When the flow project does not have a workspace, the HipChat post build action will not send out messages, in that case
 it is recommended to modify the DSL script (as can be seen below) to send out the notification as part of the flow
 itself (see also [#45](https://github.com/jenkinsci/hipchat-plugin/issues/45))

The following DSL script can be used to send out notifications as part of the flow build:

    def hipChatV1 = new jenkins.plugins.hipchat.impl.HipChatV1Service("api.hipchat.com", "v1_token", "room name list that can be separated by comma, or just a single room name", "sendAs");
    hipChatV1.publish("This is a V1 notification", "green", /*notify?*/true);
    def hipChatV2 = new jenkins.plugins.hipchat.impl.HipChatV2Service("api.hipchat.com", "v2_token", "room name list that can be separated by comma, or just a single room name");
    hipChatV2.publish("This is a V2 notification", "green", /*notify?*/true);

Note, that the API may change between versions potentially causing build failures for such projects.
