package itamio.coldsnow.network;

import itamio.coldsnow.Constants;
import itamio.coldsnow.core.StormSnapshot;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * The single {@code coldsnow:storm} channel. Registered during common setup
 * (via {@code enqueueWork}) so registration happens on the main thread.
 */
public final class ColdSnowNetwork {
    private static final String PROTOCOL_VERSION = "1";

    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Constants.MODID, "storm"),
            () -> PROTOCOL_VERSION,
            version -> true,
            version -> true);

    private static boolean registered;

    public static void register() {
        if (registered) {
            return;
        }
        try {
            int id = 0;
            CHANNEL.registerMessage(id++, StormStatePacket.class,
                    StormStatePacket::encode,
                    StormStatePacket::decode,
                    StormStatePacket::handle);
            registered = true;
        } catch (RuntimeException ignored) {
            // A duplicate registration must not break world loading.
            registered = true;
        }
    }

    /** Broadcast the storm state to every connected player. */
    public static void sendToAll(ServerLevel level, StormSnapshot snapshot) {
        if (level == null || snapshot == null) {
            return;
        }
        try {
            CHANNEL.send(PacketDistributor.ALL.noArg(),
                    new StormStatePacket(snapshot.stage(), snapshot.intensity()));
        } catch (RuntimeException ignored) {
            // Networking must never interrupt the server tick loop.
        }
    }

    private ColdSnowNetwork() {
        throw new AssertionError("No instances");
    }
}
