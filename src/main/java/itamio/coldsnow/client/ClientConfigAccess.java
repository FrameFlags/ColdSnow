package itamio.coldsnow.client;

import itamio.coldsnow.config.ClientConfigData;
import itamio.coldsnow.config.ConfigStore;
import net.minecraftforge.fml.loading.FMLPaths;

/** Lazily loads and caches {@code config/coldsnow-client.json} for the client tick handler. */
public final class ClientConfigAccess {
    private static final ConfigStore<ClientConfigData> STORE = new ConfigStore<>(
            FMLPaths.CONFIGDIR.get().resolve("coldsnow-client.json"),
            ClientConfigData.class,
            ClientConfigData::new);

    private static volatile ClientConfigData cached;

    private ClientConfigAccess() {
    }

    public static ClientConfigData get() {
        ClientConfigData value = cached;
        if (value == null) {
            synchronized (ClientConfigAccess.class) {
                value = cached;
                if (value == null) {
                    value = STORE.load();
                    if (value == null) {
                        value = new ClientConfigData().validate();
                    }
                    cached = value;
                }
            }
        }
        return value;
    }

    /** Call after the config file is rewritten so the next tick picks up new values. */
    public static void invalidate() {
        cached = null;
    }
}
