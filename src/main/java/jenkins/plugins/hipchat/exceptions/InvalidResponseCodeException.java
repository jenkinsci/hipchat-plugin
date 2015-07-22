package jenkins.plugins.hipchat.exceptions;

import jenkins.plugins.hipchat.Messages;

public class InvalidResponseCodeException extends NotificationException {

    public InvalidResponseCodeException(int responseCode) {
        super(Messages.InvalidResponseCode(responseCode));
    }
}
