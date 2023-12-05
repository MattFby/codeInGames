package MarsLanderEpisode3.extracted;

enum RocketStatus {
    FLYING("FLYING"),
    LANDED("LANDED"),
    LOST_IN_SPACE("LOST_IN_SPACE"),
    CRASHED("CRASHED");

    private final String desc;

    RocketStatus(String desc) {
        this.desc = desc;
    }

    public String toString() {
        return this.desc;
    }
}