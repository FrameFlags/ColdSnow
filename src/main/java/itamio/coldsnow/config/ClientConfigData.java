package itamio.coldsnow.config;

/** Client-side config, written to {@code config/coldsnow-client.json}. */
public final class ClientConfigData {

    public int schemaVersion = 1;
    public float particleDensity = 1.0f;
    public boolean respectVanillaParticleSetting = true;
    public boolean disableWindTrails = false;
    public boolean disableScreenFlash = true;

    /** Clamps density to [0, 2] in place; never throws, even for NaN/Infinity. */
    public ClientConfigData validate() {
        if (Float.isNaN(particleDensity)) {
            particleDensity = 1.0f;
        } else if (particleDensity < 0.0f) {
            particleDensity = 0.0f;
        } else if (particleDensity > 2.0f) {
            particleDensity = 2.0f;
        }
        return this;
    }
}
