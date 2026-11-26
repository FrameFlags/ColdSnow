package itamio.coldsnow.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Small JSON config store: pretty printed, atomically replaced, and self-healing
 * when the file on disk is unreadable or malformed.
 *
 * @param <T> plain config POJO type
 */
public final class ConfigStore<T> {

    private final Path path;
    private final Class<T> type;
    private final Supplier<T> defaults;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public ConfigStore(Path path, Class<T> type, Supplier<T> defaults) {
        this.path = Objects.requireNonNull(path, "path");
        this.type = Objects.requireNonNull(type, "type");
        this.defaults = Objects.requireNonNull(defaults, "defaults");
    }

    /**
     * Reads the config. Missing files yield defaults. Malformed files are copied to
     * {@code <name>.invalid} before defaults are returned.
     */
    public T load() {
        if (!Files.isRegularFile(path)) {
            return validated(defaults.get());
        }
        String text;
        try {
            text = Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            backupInvalid();
            return validated(defaults.get());
        }

        if (text.isBlank()) {
            backupInvalid();
            return validated(defaults.get());
        }

        try {
            T parsed;
            try (Reader reader = new StringReader(text)) {
                // Config POJOs have no-arg constructors, so Gson runs their field
                // initializers and missing JSON entries keep the Java defaults.
                parsed = gson.fromJson(reader, type);
            }
            if (parsed == null) {
                throw new JsonParseException("empty config object");
            }
            return validated(parsed);
        } catch (JsonParseException | IllegalStateException | IOException e) {
            backupInvalid();
            return validated(defaults.get());
        }
    }

    /** Writes {@code value} to a temp file in the same directory, then moves it into place. */
    public void save(T value) {
        validated(value);
        Path absolute = path.toAbsolutePath();
        Path parent = absolute.getParent();
        if (parent == null) {
            parent = Paths.get(".").toAbsolutePath();
        }
        Path temp = null;
        try {
            Files.createDirectories(parent);
            temp = Files.createTempFile(parent, absolute.getFileName().toString() + ".", ".tmp");
            try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                gson.toJson(value, writer);
            }
            try {
                Files.move(temp, absolute,
                        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, absolute, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to save config " + path, e);
        } finally {
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException ignored) {
                    // best effort only; the original config is untouched
                }
            }
        }
    }

    private Path invalidPath() {
        return path.resolveSibling(path.getFileName().toString() + ".invalid");
    }

    private void backupInvalid() {
        try {
            Path invalid = invalidPath();
            Path parent = invalid.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.copy(path, invalid, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {
            // A failed backup must not prevent returning defaults.
        }
    }

    private T validated(T value) {
        if (value == null) {
            return null;
        }
        try {
            Method validate = value.getClass().getMethod("validate");
            if (validate.getParameterCount() == 0) {
                validate.invoke(value);
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Config POJOs are expected to clamp themselves; absence is not fatal.
        }
        return value;
    }
}
