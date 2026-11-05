package cc.rewound.modapi.packet.handler;

import cc.rewound.modapi.packet.ClientboundRewoundPacket;
import cc.rewound.modapi.packet.PacketHandler;
import cc.rewound.modapi.packet.impl.clientbound.ClientboundPartyInfoPacket;
import cc.rewound.modapi.packet.impl.serverbound.ServerboundPartyInfoPacket;

import java.util.Collections;

public class PartyInfoHandler implements PacketHandler<ServerboundPartyInfoPacket> {
    @Override
    public ClientboundRewoundPacket handle(String identifier, ServerboundPartyInfoPacket packet) {
        return new ClientboundPartyInfoPacket(
                2,
                false,
                Collections.emptyMap()
        );
    }
}
