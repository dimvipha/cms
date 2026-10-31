package com.vipha.cms.feature.visit;

/**
 * OPEN → IN_PROGRESS → COMPLETED, with CANCELLED reachable from either
 * non-terminal state. A closed visit's status stays; its doctor can still correct the record of a
 * completed visit, while a cancelled visit stays as it is.
 */
public enum VisitStatus {
    OPEN,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED;

    public boolean isClosed() {
        return this == COMPLETED || this == CANCELLED;
    }

    public boolean canTransitionTo(VisitStatus target) {
        return switch (this) {
            case OPEN -> target == IN_PROGRESS || target == CANCELLED;
            case IN_PROGRESS -> target == COMPLETED || target == CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };
    }
}
