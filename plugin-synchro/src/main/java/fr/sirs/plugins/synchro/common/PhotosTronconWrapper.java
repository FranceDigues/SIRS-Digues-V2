package fr.sirs.plugins.synchro.common;

import fr.sirs.core.model.AbstractPhoto;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * Object carryinf {@link Stream} of {@linkplain AbstractPhoto photos} and the
 * id of the associated tronçon.
 * @author Matthieu Bastianelli (Geomatys)
 */
public class PhotosTronconWrapper extends TronconWrapper {

    private Stream<AbstractPhoto> photosStream;

    PhotosTronconWrapper(final PhotoContainerTronconWrapper photoContainer) {
        super(photoContainer);
        this.photosStream = photoContainer.getPhotos().stream();
    }

    public Stream<AbstractPhoto> getPhotosStream() {
        return photosStream;
    }

    public PhotosTronconWrapper applyUnaryOperator(UnaryOperator<Stream<AbstractPhoto>> prepocess) {
        photosStream = prepocess.apply(photosStream);
        return this;
    }

    public PhotosTronconWrapper applyFilter(Predicate<AbstractPhoto> predicate) {
        photosStream = photosStream.filter(predicate);
        return this;
    }

    public Stream<PhotoAndTroncon> getPhotosAndTronçons() {
        return photosStream.map(photo -> new PhotoAndTroncon(photo, tronconId));
    }

}
