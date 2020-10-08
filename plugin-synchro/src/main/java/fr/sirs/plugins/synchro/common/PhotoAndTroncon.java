
package fr.sirs.plugins.synchro.common;

import fr.sirs.core.model.AbstractPhoto;
import java.util.Optional;
import org.apache.sis.util.ArgumentChecks;

/**
 *
 * @author Matthieu Bastianelli (Geomatys)
 */
public class PhotoAndTroncon {

    private final AbstractPhoto photo;
    private final Optional<String> tronconId;

    public PhotoAndTroncon(final AbstractPhoto photo, final Optional<String> tronconId) {
        ArgumentChecks.ensureNonNull("Photo", photo);
        this.photo = photo;
        this.tronconId = tronconId;
    }

    public AbstractPhoto getPhoto() {
        return photo;
    }

    public Optional<String> getTronconId() {
        return tronconId;
    }


}
