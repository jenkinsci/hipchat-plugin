package jenkins.plugins.hipchat.model;

public enum Color {

    YELLOW("yellow"),
    GREEN("green"),
    RED("red"),
    PURPLE("purple"),
    GRAY("gray"),
    RANDOM("random");

    private final String name;

    private Color(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
