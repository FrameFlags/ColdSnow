package itamio.coldsnow.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigStoreTest {

    @Test
    void missingFileReturnsDefaults(@TempDir Path dir) {
        ConfigStore<StormConfigData> store = new ConfigStore<>(
                dir.resolve("coldsnow.json"), StormConfigData.class, StormConfigData::new);

        StormConfigData loaded = store.load();
        assertNotNull(loaded);
        assertTrue(loaded.enabled);
        assertEquals(1, loaded.schemaVersion);
        assertEquals(14, loaded.cycleDaysMin);
        assertEquals(20, loaded.cycleDaysMax);
        assertEquals(42L, loaded.seed);
        assertEquals(16, loaded.maxLayers);
        assertEquals(20, loaded.accumulationIntervalTicks);
    }

    @Test
    void saveIsPrettyPrintedAndRoundTrips(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("nested").resolve("coldsnow.json");
        ConfigStore<StormConfigData> store =
                new ConfigStore<>(file, StormConfigData.class, StormConfigData::new);

        StormConfigData data = new StormConfigData();
        data.maxLayers = 24;
        data.seed = 7L;
        data.enabled = false;
        store.save(data);

        String text = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(text.contains("\n"), "Gson pretty printing should use newlines");
        assertTrue(text.contains("\"maxLayers\": 24"), text);

        StormConfigData loaded = store.load();
        assertEquals(24, loaded.maxLayers);
        assertEquals(7L, loaded.seed);
        assertFalse(loaded.enabled);
        assertEquals(1, loaded.schemaVersion, "missing fields keep their Java defaults");
        assertEquals(14, loaded.cycleDaysMin);
        try (var entries = Files.list(file.getParent())) {
            assertEquals(0, entries.filter(p -> p.getFileName().toString().endsWith(".tmp")).count());
        }
    }

    @Test
    void malformedConfigIsBackedUpAndDefaultsAreReturned(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("coldsnow.json");
        String malformed = "{\"schemaVersion\": 1, \"maxLayers\": ";
        Files.writeString(file, malformed, StandardCharsets.UTF_8);

        ConfigStore<StormConfigData> store =
                new ConfigStore<>(file, StormConfigData.class, StormConfigData::new);
        StormConfigData loaded = store.load();

        assertEquals(16, loaded.maxLayers);
        assertTrue(loaded.enabled);
        Path invalid = dir.resolve("coldsnow.json.invalid");
        assertTrue(Files.exists(invalid), "malformed original must be preserved as .invalid");
        assertEquals(malformed, Files.readString(invalid, StandardCharsets.UTF_8));

        // A second failure must overwrite the backup, not throw.
        assertEquals(16, store.load().maxLayers);
        assertEquals(malformed, Files.readString(invalid, StandardCharsets.UTF_8));
    }

    @Test
    void validConfigIsClampedOnLoad(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("coldsnow.json");
        Files.writeString(file,
                "{\"maxLayers\":999,\"cycleDaysMin\":1,\"cycleDaysMax\":99,\"seed\":5}",
                StandardCharsets.UTF_8);

        ConfigStore<StormConfigData> store =
                new ConfigStore<>(file, StormConfigData.class, StormConfigData::new);
        StormConfigData loaded = store.load();

        assertEquals(64, loaded.maxLayers);
        assertEquals(14, loaded.cycleDaysMin);
        assertEquals(20, loaded.cycleDaysMax);
        assertEquals(5L, loaded.seed);
        assertTrue(loaded.enabled, "missing boolean keeps its Java default");
        assertFalse(Files.exists(dir.resolve("coldsnow.json.invalid")));
    }

    @Test
    void clientConfigClampsAndKeepsDefaultsForMissingFields(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("coldsnow-client.json");
        Files.writeString(file, "{\"particleDensity\":9.0,\"disableWindTrails\":true}",
                StandardCharsets.UTF_8);

        ConfigStore<ClientConfigData> store =
                new ConfigStore<>(file, ClientConfigData.class, ClientConfigData::new);
        ClientConfigData loaded = store.load();

        assertEquals(2.0f, loaded.particleDensity, 0.0f);
        assertTrue(loaded.disableWindTrails);
        assertTrue(loaded.respectVanillaParticleSetting, "missing boolean keeps its Java default");
        assertTrue(loaded.disableScreenFlash);
    }

    @Test
    void validateNeverThrowsOnBadNumbers() {
        StormConfigData storm = new StormConfigData();
        storm.maxLayers = -10;
        storm.cycleDaysMin = 0;
        storm.cycleDaysMax = 100;
        assertEquals(storm, storm.validate());
        assertEquals(8, storm.maxLayers);
        assertEquals(14, storm.cycleDaysMin);
        assertEquals(20, storm.cycleDaysMax);

        ClientConfigData client = new ClientConfigData();
        client.particleDensity = Float.NaN;
        assertEquals(client, client.validate());
        assertEquals(1.0f, client.particleDensity, 0.0f);
        client.particleDensity = Float.NEGATIVE_INFINITY;
        client.validate();
        assertEquals(0.0f, client.particleDensity, 0.0f);
    }
}
