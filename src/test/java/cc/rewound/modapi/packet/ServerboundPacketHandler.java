package cc.rewound.modapi.packet;

import cc.rewound.modapi.packet.handler.PartyInfoHandler;
import cc.rewound.modapi.packet.handler.PingHandler;
import cc.rewound.modapi.packet.handler.PlayerInfoHandler;
import cc.rewound.modapi.packet.impl.serverbound.ServerboundPartyInfoPacket;
import cc.rewound.modapi.packet.impl.serverbound.ServerboundPingPacket;
import cc.rewound.modapi.packet.impl.serverbound.ServerboundPlayerInfoPacket;

import java.util.HashMap;
import java.util.Map;

class ServerboundPacketHandler {
    private final Map<Class<? extends RewoundPacket>, PacketHandler> packetHandlers = new HashMap<>();

    ServerboundPacketHandler() {
        register(ServerboundPingPacket.class, new PingHandler());
        register(ServerboundPartyInfoPacket.class, new PartyInfoHandler());
        register(ServerboundPlayerInfoPacket.class, new PlayerInfoHandler());
    }

    private <T extends RewoundPacket> void register(Class<T> packetClass, PacketHandler<T> handler) {
        packetHandlers.put(packetClass, handler);
    }

    public ClientboundRewoundPacket handle(String identifier, RewoundPacket packet) {
        PacketHandler handler = packetHandlers.get(packet.getClass());
        if (handler == null) {
            return null;
        }

        return handler.handle(identifier, packet);
    }

}
