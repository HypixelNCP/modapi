package cc.rewound.modapi.packet;

/**
 * Represents a packet that is automatically sent based automatically based on specific conditions.
 * <p>
 * Event driven packets do not have a serverbound instance, as they can only be triggered by the server.
 * You will however, need to subscribe to wanted events, see {@link cc.rewound.modapi.RewoundModAPI#subscribeToEventPacket(Class)}.
 */
public interface EventPacket extends ClientboundRewoundPacket {
}
