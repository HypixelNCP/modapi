package cc.rewound.modapi;

import cc.rewound.modapi.error.ErrorReason;
import cc.rewound.modapi.handler.ClientboundPacketHandler;
import cc.rewound.modapi.handler.ErrorHandler;
import cc.rewound.modapi.handler.RegisteredHandler;
import cc.rewound.modapi.packet.ClientboundRewoundPacket;
import cc.rewound.modapi.packet.EventPacket;
import cc.rewound.modapi.packet.RewoundPacket;
import cc.rewound.modapi.packet.PacketRegistry;
import cc.rewound.modapi.packet.impl.clientbound.*;
import cc.rewound.modapi.packet.impl.clientbound.event.ClientboundLocationPacket;
import cc.rewound.modapi.packet.impl.serverbound.ServerboundPartyInfoPacket;
import cc.rewound.modapi.packet.impl.serverbound.ServerboundPingPacket;
import cc.rewound.modapi.packet.impl.serverbound.ServerboundPlayerInfoPacket;
import cc.rewound.modapi.packet.impl.serverbound.ServerboundRegisterPacket;
import cc.rewound.modapi.serializer.PacketSerializer;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class RewoundModAPI {
    private static final RewoundModAPI INSTANCE = new RewoundModAPI();

    public static RewoundModAPI getInstance() {
        return INSTANCE;
    }

    private final PacketRegistry registry = new PacketRegistry();
    private final Map<String, Collection<RegisteredHandlerImpl<?>>> handlers = new ConcurrentHashMap<>();
    private final Set<String> subscribedEvents = ConcurrentHashMap.newKeySet();
    private Set<String> lastSubscribedEvents = Collections.emptySet();
    @Nullable
    private RewoundModAPIImplementation modImplementation;

    private RewoundModAPI() {
        registerRewoundPackets();
        registerEventPackets();
    }

    private void registerRewoundPackets() {
        registry.define("rewound:hello")
                .clientbound(ClientboundHelloPacket.class, ClientboundHelloPacket::new)
                .register();

        registry.define("rewound:ping")
                .clientbound(ClientboundPingPacket.class, ClientboundPingPacket::new)
                .serverbound(ServerboundPingPacket.class, ServerboundPingPacket::new)
                .register();

        registry.define("rewound:party_info")
                .clientbound(ClientboundPartyInfoPacket.class, ClientboundPartyInfoPacket::new)
                .serverbound(ServerboundPartyInfoPacket.class, ServerboundPartyInfoPacket::new)
                .register();

        registry.define("rewound:player_info")
                .clientbound(ClientboundPlayerInfoPacket.class, ClientboundPlayerInfoPacket::new)
                .serverbound(ServerboundPlayerInfoPacket.class, ServerboundPlayerInfoPacket::new)
                .register();

        registry.define("rewound:register")
                .serverbound(ServerboundRegisterPacket.class, ServerboundRegisterPacket::new)
                .register();
    }

    private void registerEventPackets() {
        registry.define("rewound:location")
                .clientBoundEvent(ClientboundLocationPacket.CURRENT_VERSION, ClientboundLocationPacket.class, ClientboundLocationPacket::new)
                .register();
    }

    private void registerDefaultHandler() {
        createHandler(ClientboundHelloPacket.class, p -> sendRegisterPacket(true));
    }

    private void sendRegisterPacket(boolean alwaysSendIfNotEmpty) {
        if (modImplementation == null || !modImplementation.isConnectedToRewound()) {
            // Allow registering events when not connected to Rewound
            return;
        }

        if (lastSubscribedEvents.equals(subscribedEvents) && !(alwaysSendIfNotEmpty && !subscribedEvents.isEmpty())) {
            return;
        }

        Set<String> lastSubscribedEvents = new HashSet<>(subscribedEvents);
        if (sendPacket(new ServerboundRegisterPacket(registry, lastSubscribedEvents))) {
            this.lastSubscribedEvents = lastSubscribedEvents;
        }
    }

    @ApiStatus.Internal
    public PacketRegistry getRegistry() {
        return registry;
    }

    @ApiStatus.Internal
    public void handle(String identifier, PacketSerializer serializer) {
        if (handlers.isEmpty()) {
            return;
        }

        if (!registry.isRegistered(identifier)) {
            return;
        }

        // All responses contain a boolean of if the response is a success, if not then a further var int is included to identify the error
        if (!serializer.readBoolean()) {
            ErrorReason reason = ErrorReason.getById(serializer.readVarInt());
            handleError(identifier, reason);
            return;
        }

        ClientboundRewoundPacket packet = registry.createClientboundPacket(identifier, serializer);
        if (packet instanceof ClientboundVersionedPacket && !((ClientboundVersionedPacket) packet).isExpectedVersion()) {
            // Ignore packets that don't match our expected version, these could be received due to other mods requesting them
            return;
        }

        handle(packet);
    }

    @ApiStatus.Internal
    @SuppressWarnings("unchecked")
    public void handle(ClientboundRewoundPacket packet) {
        Collection<RegisteredHandlerImpl<?>> typedHandlers = handlers.get(packet.getIdentifier());
        // nothing registered for this packet.
        if (typedHandlers == null) return;
        for (RegisteredHandlerImpl<?> handler : typedHandlers) {
            handler.handle(packet);
        }
    }

    @ApiStatus.Internal
    public void handleError(String identifier, ErrorReason reason) {
        Collection<RegisteredHandlerImpl<?>> handlers = this.handlers.get(identifier);
        if (handlers == null) return;
        for (RegisteredHandlerImpl<?> handler : handlers) {
            handler.handleError(reason);
        }
    }

    @ApiStatus.Internal
    public void setModImplementation(RewoundModAPIImplementation modImplementation) {
        if (this.modImplementation != null) {
            throw new IllegalStateException("Mod implementation already set");
        }

        if (modImplementation == null) {
            throw new NullPointerException("modImplementation cannot be null");
        }

        this.modImplementation = modImplementation;
        this.modImplementation.onInit();
        registerDefaultHandler();
    }

    /**
     * @deprecated Use {@link #createHandler(Class, ClientboundPacketHandler)} instead
     */
    @Deprecated
    public <T extends ClientboundRewoundPacket> void registerHandler(Class<T> packetClass, ClientboundPacketHandler<T> handler) {
        createHandler(packetClass, handler);
    }

    public <T extends ClientboundRewoundPacket> RegisteredHandler<T> createHandler(Class<T> packetClass, ClientboundPacketHandler<T> handler) {
        if (packetClass == null) {
            throw new NullPointerException("packetClass cannot be null");
        }

        if (handler == null) {
            throw new NullPointerException("handler cannot be null");
        }

        RegisteredHandlerImpl<T> registeredHandler = new RegisteredHandlerImpl<>(handler);
        handlers.computeIfAbsent(getRegistry().getIdentifier(packetClass), cls -> new CopyOnWriteArrayList<>())
                .add(registeredHandler);
        return registeredHandler;
    }

    public void subscribeToEventPacket(Class<? extends EventPacket> packet) {
        if (subscribedEvents.add(getRegistry().getIdentifier(packet))) {
            sendRegisterPacket(false);
        }
    }

    /**
     * @return whether the packet was sent successfully
     */
    public boolean sendPacket(RewoundPacket packet) {
        if (modImplementation == null) {
            throw new IllegalStateException("Mod implementation not set");
        }

        return modImplementation.sendPacket(packet);
    }

    private static class RegisteredHandlerImpl<T extends ClientboundRewoundPacket> implements RegisteredHandler<T> {
        private final ClientboundPacketHandler<T> handler;
        private ErrorHandler errorHandler;

        RegisteredHandlerImpl(ClientboundPacketHandler<T> handler) {
            this.handler = handler;
        }

        @Override
        public void onError(ErrorHandler errorHandler) {
            if (this.errorHandler != null) {
                throw new IllegalStateException("Error handler already set");
            }
            this.errorHandler = errorHandler;
        }

        void handle(ClientboundRewoundPacket packet) {
            // this cast is safe as we ensure its type when it is added to the handlers list in the first place.
            handler.handle((T) packet);
        }

        void handleError(ErrorReason reason) {
            if (errorHandler != null) {
                errorHandler.onError(reason);
            }
        }
    }
}
