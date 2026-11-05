package cc.rewound.modapi.packet.impl.serverbound;

import cc.rewound.modapi.serializer.PacketSerializer;
import org.jetbrains.annotations.ApiStatus;

public class ServerboundPingPacket extends ServerboundVersionedPacket {
    private static final int CURRENT_VERSION = 1;

    public ServerboundPingPacket() {
        super(CURRENT_VERSION);
    }

    @ApiStatus.Internal
    public ServerboundPingPacket(PacketSerializer serializer) {
        super(serializer);
    }

}
