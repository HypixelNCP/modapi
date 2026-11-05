package cc.rewound.modapi.packet;

import cc.rewound.modapi.RewoundModAPI;
import cc.rewound.modapi.serializer.PacketSerializer;
import org.jetbrains.annotations.ApiStatus;

public interface RewoundPacket {

    @ApiStatus.Internal
    void write(PacketSerializer serializer);

    @ApiStatus.Internal
    default String getIdentifier() {
        return RewoundModAPI.getInstance().getRegistry().getIdentifier(getClass());
    }

}
