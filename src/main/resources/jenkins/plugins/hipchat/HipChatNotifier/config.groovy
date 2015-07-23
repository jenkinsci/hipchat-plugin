// Namespaces
l = namespace("/lib/layout")
st = namespace("jelly:stapler")
j = namespace("jelly:core")
t = namespace("/lib/hudson")
f = namespace("/lib/form")
d = namespace("jelly:define")

def configured = instance != null

f.entry(title: _("Auth Token"), help: "/plugin/hipchat/help-projectConfig-token.html") {
    f.textbox(name: "hipchat.token", value: instance?.token)
}

f.entry(title: _("Project Room"), help: "/plugin/hipchat/help-projectConfig-hipChatRoom.html") {
    f.textbox(name: "hipchat.room", value: instance?.room)
}

f.section(title: _("Notification settings")) {
    f.entry(title: _("Notify Build Start")) {
        f.checkbox(name: "hipchat.startNotification", checked: instance?.startNotification)
    }
    f.entry(title: _("Notify Aborted")) {
        f.checkbox(name: "hipchat.notifyAborted", checked: instance?.notifyAborted)
    }
    f.entry(title: _("Notify Failure")) {
        f.checkbox(name: "hipchat.notifyFailure", checked: instance?.notifyFailure)
    }
    f.entry(title: _("Notify Not Built")) {
        f.checkbox(name: "hipchat.notifyNotBuilt", checked: instance?.notifyNotBuilt)
    }
    f.entry(title: _("Notify Success")) {
        f.checkbox(name: "hipchat.notifySuccess", checked: instance?.notifySuccess)
    }
    f.entry(title: _("Notify Unstable")) {
        f.checkbox(name: "hipchat.notifyUnstable", checked: instance?.notifyUnstable)
    }
    f.entry(title: _("Notify Back To Normal")) {
        f.checkbox(name: "hipchat.notifyBackToNormal", checked: instance?.notifyBackToNormal)
    }
}

if (descriptor.isMatrixProject(my)) {
    f.entry(field: "matrixTriggerMode", title: _("Trigger for matrix projects")) {
        f.enum { 
            raw(my.description)
        }
    }
}

f.section(title: _("Message Templates")) {
    f.entry(title: _("Job started"), help: "/plugin/hipchat/help-projectConfig-hipChatMessages.html") {
        f.textbox(name: "hipchat.startJobMessage", value: instance?.startJobMessage)
        text("Default: '" + descriptor.getStartJobMessageDefault() + "'")
    }
    f.entry(title: _("Job completed"), help: "/plugin/hipchat/help-projectConfig-hipChatMessages.html") {
        f.textbox(name: "hipchat.completeJobMessage", value: instance?.completeJobMessage)
        text("Default: '" + descriptor.getCompleteJobMessageDefault() + "'")
    }
}
