package fr.sirs.plugins.synchro.common;

import fr.sirs.Session;
import fr.sirs.core.component.AbstractPositionableRepository;
import fr.sirs.core.component.AbstractSIRSRepository;
import fr.sirs.core.model.AbstractObservation;
import fr.sirs.core.model.AbstractPhoto;
import fr.sirs.core.model.AvecBornesTemporelles;
import fr.sirs.core.model.AvecPhotos;
import fr.sirs.core.model.Desordre;
import fr.sirs.core.model.OuvrageHydrauliqueAssocie;
import fr.sirs.core.model.ReseauHydrauliqueCielOuvert;
import fr.sirs.core.model.ReseauHydrauliqueFerme;
import fr.sirs.core.model.StationPompage;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javafx.beans.property.ObjectProperty;
import org.apache.sis.util.ArgumentChecks;

/**
 * Note : No computing is done here, we simply wrap operators in a stream. It
 * implies that fetching will be done when browsing objects. So, you should
 * avoid using this component inside rendering thread. The advantage of this
 * approach is that nothing will be loaded until you actually need it. The
 * second advantage is that resources are loaded as you browse it, and release
 * as you release them.
 *
 * @author Alexis Manin (Geomatys)
 */
public class PhotoFinder implements Supplier<Stream<AbstractPhoto>> {

    final Session session;
    Collection<String> tronconIds;
    LocalDate docDateFilter;

    UnaryOperator<Stream<AbstractPhoto>> preProcessor;

    public PhotoFinder(Session session) {
        ArgumentChecks.ensureNonNull("Session", session);
        this.session = session;
    }

    public Collection<String> getTronconIds() {
        return tronconIds;
    }

    public PhotoFinder setTronconIds(Collection<String> tronconIds) {
        this.tronconIds = tronconIds;
        return this;
    }

    public LocalDate getDocDateFilter() {
        return docDateFilter;
    }

    public PhotoFinder setDocDateFilter(LocalDate docDateFilter) {
        this.docDateFilter = docDateFilter;
        return this;
    }

    public UnaryOperator<Stream<AbstractPhoto>> getPreProcessor() {
        return preProcessor;
    }

    public PhotoFinder setPreProcessor(UnaryOperator<Stream<AbstractPhoto>> preProcessor) {
        this.preProcessor = preProcessor;
        return this;
    }

    @Override
    public Stream<AbstractPhoto> get() {
        return filter(getPhotoContainers());
    }

    private Stream<AvecPhotos> getPhotoContainers() {
        return Stream.of(session)
                .flatMap((Session source) -> {
                    return Stream.concat(stream(source, OuvrageHydrauliqueAssocie.class)
                                    .map(OuvrageHydrauliqueAssocieWrapper::new),
                            Stream.concat(stream(source, ReseauHydrauliqueFerme.class)
                                    .map(ReseauHydrauliqueFermeWrapper::new),
                            Stream.concat(stream(source, ReseauHydrauliqueCielOuvert.class)
                                    .map(ReseauHydrauliqueCielOuvertWrapper::new),
                            Stream.concat(stream(source, StationPompage.class)
                                    .map(StationPompageWrapper::new),
                            Stream.concat(stream(source, Desordre.class)
                                    .map(DesordreWrapper::new),
                            stream(source, AvecPhotos.class))))));
                });
    }

    private <T> Stream<T> stream(final Session session, final Class<T> docType) {
        Stream<AbstractSIRSRepository> stream = session.getRepositoriesForClass(docType).stream();
        if (tronconIds == null || tronconIds.isEmpty()) {
            return stream.flatMap(repo -> repo.getAll().stream());
        } else {
            return stream.filter(AbstractPositionableRepository.class::isInstance)
                    .map(AbstractPositionableRepository.class::cast)
                    .flatMap(this::getForTroncons);
        }
    }

    private Stream getForTroncons(final AbstractPositionableRepository repo) {
        return tronconIds.stream()
                .flatMap(id -> repo.getByLinearId(id).stream());
    }

    private Stream<AbstractPhoto> filter(Stream<AvecPhotos> containers) {
        if (docDateFilter != null) {
            containers = containers
                    .filter(AvecBornesTemporelles.class::isInstance)
                    .filter(obj -> DocumentUtilities.intersectsDate((AvecBornesTemporelles) obj, docDateFilter));
        }

        Function<AvecPhotos, Stream<AbstractPhoto>> photoExtractor = ap -> ap.getPhotos().stream();
        if (preProcessor != null) {
            photoExtractor = photoExtractor.andThen(preProcessor);
        }

        return containers.flatMap(photoExtractor);
    }

    /**
     * Compare observations by date, descending order (most recent to oldest).
     */
    private static int compare(final AbstractObservation o1, final AbstractObservation o2) {
        final LocalDate date1 = o1.getDate();
        final LocalDate date2 = o2.getDate();

        if (date1 == date2)
            return 0;
        if (date1 == null)
            return 1;
        else if (date2 == null)
            return -1;

        return -date1.compareTo(date2);
    }

    private static class DesordreWrapper extends ObservationContainerWrapper<Desordre> {

        public DesordreWrapper(Desordre source) {
            super(source, source.observations);
        }
    }

    private static class OuvrageHydrauliqueAssocieWrapper extends ObservationContainerWrapper<OuvrageHydrauliqueAssocie> {

        public OuvrageHydrauliqueAssocieWrapper(OuvrageHydrauliqueAssocie source) {
            super(source, source.observations);
        }
    }

    private static class ReseauHydrauliqueFermeWrapper extends ObservationContainerWrapper<ReseauHydrauliqueFerme> {

        public ReseauHydrauliqueFermeWrapper(ReseauHydrauliqueFerme source) {
            super(source, source.observations);
        }
    }

    private static class ReseauHydrauliqueCielOuvertWrapper extends ObservationContainerWrapper<ReseauHydrauliqueCielOuvert> {

        public ReseauHydrauliqueCielOuvertWrapper(ReseauHydrauliqueCielOuvert source) {
            super(source, source.observations);
        }
    }

    private static class StationPompageWrapper extends ObservationContainerWrapper<StationPompage> {

        public StationPompageWrapper(StationPompage source) {
            super(source, source.observations);
        }
    }

    /**
     * Encapsule des conteneurs d'observations en implémentations de "AvecPhotos" renvoyant les photos de leurs
     * observations.
     *
     * @param <OC> type de conteneur d'observations (désordres etc.)
     */
    private static abstract class ObservationContainerWrapper<OC extends AvecBornesTemporelles>
            implements AvecPhotos<AbstractPhoto>, AvecBornesTemporelles {

        private final OC source;
        private final List<? extends AbstractObservation> observations;

        public ObservationContainerWrapper(final OC source, List<? extends AbstractObservation> observations) {
            this.source = source;
            this.observations = observations;
        }

        /**
         * Cette méthode doit renvoyer les photos des observations du conteneur.
         * @return
         */
        @Override
        public List<AbstractPhoto> getPhotos() {
            return observations.stream()
                    .sorted(PhotoFinder::compare)
                    .flatMap(o -> o.getPhotos().stream())
                    .collect(Collectors.toList());
        }

        @Override
        public void setPhotos(List<AbstractPhoto> photos) {
            throw new UnsupportedOperationException("Read-only utility.");
        }

        @Override
        public ObjectProperty<LocalDate> date_debutProperty() {
            return source.date_debutProperty();
        }

        @Override
        public LocalDate getDate_debut() {
            return source.getDate_debut();
        }

        @Override
        public void setDate_debut(LocalDate date_debut) {
            throw new UnsupportedOperationException("Read-only utility.");
        }

        @Override
        public ObjectProperty<LocalDate> date_finProperty() {
            return source.date_finProperty();
        }

        @Override
        public LocalDate getDate_fin() {
            return source.getDate_fin();
        }

        @Override
        public void setDate_fin(LocalDate date_fin) {
            throw new UnsupportedOperationException("Read-only utility.");
        }
    }
}
