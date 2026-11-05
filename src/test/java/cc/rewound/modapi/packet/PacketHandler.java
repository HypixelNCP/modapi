package cc.rewound.modapi.packet;

import cc.rewound.modapi.packet.ClientboundRewoundPacket;
import cc.rewound.modapi.packet.RewoundPacket;

public interface PacketHandler<T extends RewoundPacket> {

    ClientboundRewoundPacket handle(String identifier, T packet);

}
