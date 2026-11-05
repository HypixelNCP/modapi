package cc.rewound.modapi.handler;

import cc.rewound.modapi.packet.ClientboundRewoundPacket;

public interface RegisteredHandler<T extends ClientboundRewoundPacket> {
    /**
     * Handling for when an error is received for this registered handler.
     * <br>
     * Note: This error may be received due to any modification requesting the same packet type.
     */
    void onError(ErrorHandler errorHandler);
}
