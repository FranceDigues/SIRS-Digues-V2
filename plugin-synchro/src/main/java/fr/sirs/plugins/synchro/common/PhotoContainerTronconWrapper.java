
package fr.sirs.plugins.synchro.common;

import fr.sirs.core.model.AbstractPhoto;
import fr.sirs.core.model.AvecPhotos;
import java.util.List;
import java.util.Optional;

/**
 *
 * @author Matthieu Bastianelli (Geomatys)
 */
class PhotoContainerTronconWrapper extends TronconWrapper implements AvecPhotos<AbstractPhoto> {
//    implements AvecPhotos {

    private final AvecPhotos photoContainer;

    PhotoContainerTronconWrapper(final AvecPhotos photoContainer) {
        super(photoContainer);
        this.photoContainer = photoContainer;
    }

    PhotoContainerTronconWrapper(final AvecPhotos photoContainer, final Optional<String> tronconId) {
        super(photoContainer, tronconId);
        this.photoContainer = photoContainer;
    }

    @Override
    public List<AbstractPhoto> getPhotos() {
        return photoContainer.getPhotos();
    }

    @Override
    public void setPhotos(List<AbstractPhoto> photos) {
        photoContainer.setPhotos(photos);
    }

    public AvecPhotos getPhotoContainer() {
        return photoContainer;
    }

}
