package cc.rewound.modapi;

import cc.rewound.modapi.packet.RewoundPacket;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public interface RewoundModAPIImplementation {

    void onInit();

    boolean sendPacket(RewoundPacket packet);

    boolean isConnectedToRewound();

}
