package cc.rewound.modapi.handler;

import cc.rewound.modapi.error.ErrorReason;

@FunctionalInterface
public interface ErrorHandler {
    void onError(ErrorReason reason);
}
