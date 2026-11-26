package itamio.coldsnow.network;

import itamio.coldsnow.client.ClientStormState;
import itamio.coldsnow.core.StormSnapshot;
import itamio.coldsnow.core.StormStage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server -> client notification of the current storm stage and intensity.
 * The packet intentionally carries only the two values the client renderer
 * needs; all timing/authority stays on the server.
 */
public record StormStatePacket(StormStage stage, float intensity) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(this.stage == null ? StormStage.CLEAR : this.stage);
        buf.writeFloat(Float.isFinite(this.intensity) ? this.intensity : 0.0F);
    }

    public static StormStatePacket decode(FriendlyByteBuf buf) {
        try {
            StormStage stage = buf.readEnum(StormStage.class);
            float intensity = buf.readFloat();
            return new StormStatePacket(stage == null ? StormStage.CLEAR : stage,
                    Float.isFinite(intensity) ? intensity : 0.0F);
        } catch (RuntimeException ignored) {
            // Malformed packet: fail open to a clear sky.
            return new StormStatePacket(StormStage.CLEAR, 0.0F);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        if (contextSupplier == null) {
            return;
        }
        NetworkEvent.Context context = contextSupplier.get();
        if (context == null) {
            return;
        }
        context.enqueueWork(() -> {
            StormStage stage = this.stage == null ? StormStage.CLEAR : this.stage;
            float intensity = Float.isFinite(this.intensity) ? this.intensity : 0.0F;
            StormSnapshot snapshot = new StormSnapshot(stage, -1, 0L, 0L, 0L, intensity);
            // The lambda body is only resolved/executed on the client, so a
            // dedicated server never loads the client-only state holder.
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientStormState.set(snapshot));
        });
        context.setPacketHandled(true);
    }
}
