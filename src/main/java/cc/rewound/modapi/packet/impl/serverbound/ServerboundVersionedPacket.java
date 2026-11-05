package cc.rewound.modapi.packet.impl.serverbound;

import cc.rewound.modapi.packet.RewoundPacket;
import cc.rewound.modapi.packet.impl.VersionedPacket;
import cc.rewound.modapi.serializer.PacketSerializer;
import org.jetbrains.annotations.ApiStatus;

public abstract class ServerboundVersionedPacket extends VersionedPacket implements RewoundPacket {

    @ApiStatus.Internal
    public ServerboundVersionedPacket(int version) {
        super(version);
    }

    @ApiStatus.Internal
    public ServerboundVersionedPacket(PacketSerializer serializer) {
        super(serializer);
    }

    protected boolean read(PacketSerializer serializer) {
        this.version = serializer.readVarInt();
        return true; // Always continue reading serverbound, we should handle the packet based on the version
    }

    @Override
    public void write(PacketSerializer serializer) {
        serializer.writeVarInt(version);
    }

}
