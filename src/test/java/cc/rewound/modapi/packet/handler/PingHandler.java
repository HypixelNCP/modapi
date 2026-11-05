package cc.rewound.modapi.packet.handler;

import cc.rewound.modapi.packet.PacketHandler;
import cc.rewound.modapi.packet.ClientboundRewoundPacket;
import cc.rewound.modapi.packet.impl.clientbound.ClientboundPingPacket;
import cc.rewound.modapi.packet.impl.serverbound.ServerboundPingPacket;

public class PingHandler implements PacketHandler<ServerboundPingPacket> {
    @Override
    public ClientboundRewoundPacket handle(String identifier, ServerboundPingPacket packet) {
        return new ClientboundPingPacket("pong");
    }
}
