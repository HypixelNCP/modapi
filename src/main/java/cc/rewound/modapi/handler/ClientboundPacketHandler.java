package cc.rewound.modapi.handler;

import cc.rewound.modapi.packet.ClientboundRewoundPacket;

@FunctionalInterface
public interface ClientboundPacketHandler<T extends ClientboundRewoundPacket> {
    void handle(T packet);
}
