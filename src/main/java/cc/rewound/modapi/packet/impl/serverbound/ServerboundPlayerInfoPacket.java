package cc.rewound.modapi.packet.impl.serverbound;

import cc.rewound.modapi.serializer.PacketSerializer;
import org.jetbrains.annotations.ApiStatus;

public class ServerboundPlayerInfoPacket extends ServerboundVersionedPacket {
    private static final int CURRENT_VERSION = 1;

    public ServerboundPlayerInfoPacket() {
        super(CURRENT_VERSION);
    }

    @ApiStatus.Internal
    public ServerboundPlayerInfoPacket(PacketSerializer serializer) {
        super(serializer);
    }

}
