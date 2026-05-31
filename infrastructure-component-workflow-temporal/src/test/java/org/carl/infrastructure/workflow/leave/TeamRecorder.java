package org.carl.infrastructure.workflow.leave;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Test recorder for the team-leave (gathering) tests. */
public final class TeamRecorder {

    public static final List<String> EVENTS = new CopyOnWriteArrayList<>();

    private TeamRecorder() {}

    public static void reset() {
        EVENTS.clear();
    }
}
