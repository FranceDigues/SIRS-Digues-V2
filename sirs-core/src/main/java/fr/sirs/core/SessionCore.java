/**
 * This file is part of SIRS-Digues 2.
 *
 * Copyright (C) 2016, FRANCE-DIGUES,
 *
 * SIRS-Digues 2 is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * SIRS-Digues 2 is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * SIRS-Digues 2. If not, see <http://www.gnu.org/licenses/>
 */
package fr.sirs.core;

import fr.sirs.core.component.AbstractPositionableRepository;
import fr.sirs.core.component.AbstractSIRSRepository;
import fr.sirs.core.component.DatabaseRegistry;
import fr.sirs.core.component.Previews;
import fr.sirs.core.component.UtilisateurRepository;
import fr.sirs.core.component.ReferenceUsageRepository;
import fr.sirs.core.h2.H2Helper;
import fr.sirs.core.model.AbstractPhoto;
import fr.sirs.core.model.AbstractPositionDocument;
import fr.sirs.core.model.AvecForeignParent;
import fr.sirs.core.model.AvecPhotos;
import fr.sirs.core.model.Desordre;
import fr.sirs.core.model.Element;
import fr.sirs.core.model.ElementCreator;
import fr.sirs.core.model.GardeTroncon;
import fr.sirs.core.model.Identifiable;
import fr.sirs.core.model.Objet;
import fr.sirs.core.model.Observation;
import fr.sirs.core.model.Positionable;
import fr.sirs.core.model.Preview;
import fr.sirs.core.model.ProprieteTroncon;
import fr.sirs.core.model.Role;
import fr.sirs.core.model.Utilisateur;
import fr.sirs.index.ElementHit;
import fr.sirs.util.ClosingDaemon;
import fr.sirs.util.StreamingIterable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.logging.Level;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.util.collection.Cache;
import org.ektorp.CouchDbConnector;
import org.ektorp.DocumentOperationResult;
import org.ektorp.http.StdHttpClient;
import org.geotoolkit.gui.javafx.util.TaskManager;
import org.geotoolkit.util.collection.CloseableIterator;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * La session contient toutes les données chargées dans l'instance courante de
 * l'application.
 *
 * Notamment, elle doit réferencer l'ensemble des thèmes ouvert, ainsi que les
 * onglets associés. De même pour les {@link Element}s et leurs éditeurs.
 *
 * La session fournit également un point d'accès centralisé à tous les documents
 * de la base CouchDB.
 *
 * @author Johann Sorel
 */
public class SessionCore implements ApplicationContextAware {

    ////////////////////////////////////////////////////////////////////////////
    // GESTION DES DROITS
    ////////////////////////////////////////////////////////////////////////////
    private final ObjectProperty<Utilisateur> utilisateurProperty = new SimpleObjectProperty<>(null);
    public ObjectProperty<Utilisateur> utilisateurProperty() {return utilisateurProperty;}

    public Utilisateur getUtilisateur() {return utilisateurProperty.get();}
    public void setUtilisateur(final Utilisateur utilisateur) {
        utilisateurProperty.set(utilisateur);
    }

    private final ReadOnlyBooleanWrapper geometryEditionProperty = new ReadOnlyBooleanWrapper(false);
    /**
     *
     * @return A flag indicating if current user can modify geometries (true) or
     * not (false).
     */
    public ReadOnlyBooleanProperty geometryEditionProperty() {return geometryEditionProperty.getReadOnlyProperty();}

    /**
     *
     * @deprecated {@link SessionCore#validDocuments().not()}
     */
    @Deprecated
    private final ReadOnlyBooleanWrapper needValidationProperty = new ReadOnlyBooleanWrapper(true);
    /**
     *
     * @return A flag indicating if current user's work must be marked as 'not
     * validated yet'. It means that this user won't be able to modify any data
     * already validated, and it won't be able to edit work of other users.
     * @deprecated {@link SessionCore#createValidDocuments().not()}
     */
    @Deprecated
    public ReadOnlyBooleanProperty needValidationProperty() {return needValidationProperty;}

    /**
     * @deprecated Utiliser {@link SessionCore#roleBinding() }
     */
    @Deprecated
    private final ReadOnlyObjectWrapper<Role> role = new ReadOnlyObjectWrapper<>();

    /**
     * @deprecated Utiliser {@link SessionCore#roleBinding() }
     */
    @Deprecated
    public ReadOnlyObjectProperty<Role> roleProperty() {
        return role.getReadOnlyProperty();
    }

    /**
     * @deprecated Utiliser {@link SessionCore#roleBinding() }
     */
    @Deprecated
    public Role getRole(){return role.get();}

    private final ObjectBinding<Role> roleBinding = new ObjectBinding<Role>(){

        {
            bind(utilisateurProperty);
        }

        @Override
        protected Role computeValue() {
            if(utilisateurProperty.get()==null) return null;
            return utilisateurProperty.get().getRole();
        }
    };

    /**
     * @return un binding indiquant le rôle de l'utilisateur
     */
    public ObjectBinding<Role> roleBinding(){return roleBinding;}


    private final BooleanBinding adminOrUserOrExtern = new BooleanBinding() {

        {
            bind(roleBinding);
        }

        @Override
        protected boolean computeValue() {
            final Role userRole = roleBinding.get();
            return Role.ADMIN.equals(userRole)
                    || Role.USER.equals(userRole)
                    || Role.EXTERN.equals(userRole);
        }
    };

    /**
     * @return un binding booléen indiquant si l'utilisateur connecté est administrateur, utilisateur ou externe
     */
    public BooleanBinding adminOrUserOrExtern(){return adminOrUserOrExtern;}

    private final StringBinding userIdBinding = new StringBinding() {
        {
            bind(utilisateurProperty);
        }

        @Override
        protected String computeValue() {
            if(utilisateurProperty.get()==null) return null;
            return utilisateurProperty.get().getId();
        }
    };

    /**
     * @return un binding indiquant l'identifiant de l'utilisateur connecté
     */
    public StringBinding userIdBinding(){return userIdBinding;}

    private final BooleanBinding createValidDocuments = new BooleanBinding() {

        {
            bind(roleBinding);
        }

        @Override
        protected boolean computeValue() {
            final Role userRole = roleBinding.get();
            return Role.ADMIN.equals(userRole) || Role.USER.equals(userRole);
        }
    };

    /**
     *
     * @return un binding indiquant si l'utilisateur crée des documents déjà valides.
     * (s'il s'agit d'un administrateur ou d'un utilisateur normal)
     */
    public BooleanBinding createValidDocuments() {return createValidDocuments;}

    @Autowired
    ElementCreator elementCreator;

    public ElementCreator getElementCreator(){return elementCreator;}

    ////////////////////////////////////////////////////////////////////////////
    // GESTION DU CONTEXTE SPRING
    ////////////////////////////////////////////////////////////////////////////
    private ApplicationContext applicationContext;
    public ApplicationContext getApplicationContext(){return applicationContext;}

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
            this.applicationContext = applicationContext;
    }


    ////////////////////////////////////////////////////////////////////////////
    // GESTION DES REPOSITORIES COUCHDB
    ////////////////////////////////////////////////////////////////////////////
    /**
     * A map of all available repositories in current context. Value is a
     * specific repository, and key is the canonical name of the model class on
     * which the repository works.
     */
    private final Map<String, AbstractSIRSRepository> repositories = new HashMap<>();

    private final Cache<Class<? extends Element>, Collection<AbstractSIRSRepository>> matchingRepositoriesCache = new Cache<>(12, 12, false);

    /**
     * Retrieve all registries initialized by spring, then add them to current session.
     *
     * @param registered repositories found in application context.
     */
    @Autowired
    public void initRepositories(List<AbstractSIRSRepository> registered) {
        for (final AbstractSIRSRepository repo : registered) {
            repositories.put(repo.getModelClass().getCanonicalName(), repo);
        }
    }

    /**
     * Find a repository for update operations on {@link Element} of given type.
     * @param <T> Type of wanted model object.
     * @param elementType The class of the type we want a {@link AbstractSIRSRepository} for. (Ex : RefMateriau.class, TronconDigue.class, etc.)
     * @return A valid repository for input type, or null if we cannot find any repository for given type.
     */
    public <T extends Element> AbstractSIRSRepository<T> getRepositoryForClass(Class<T> elementType) {
        if (elementType == null) return null;
        return getRepositoryForType(elementType.getCanonicalName());
    }

    public Collection<AbstractSIRSRepository> getModelRepositories(){
        return repositories.values();
    }

    /**
     * @return the list of all classes pointed by repositories.
     */
    public Collection<Class> getAvailableModels() {
        final Set<Class> clazz = new HashSet<>();
        for (final AbstractSIRSRepository repo : repositories.values()) {
            clazz.add(repo.getModelClass());
        }
        return clazz;
    }

    /**
     * Return a collection of candidate repositories for an abstract class or an interface.
     * @param elementType Type of model object wanted.
     * @return All repositories which work on given element types or its sub-classes. Can be empty, but never null.
     */
    public Collection<AbstractSIRSRepository> getRepositoriesForClass(Class elementType) {
        if (elementType == null)
            return Collections.EMPTY_SET;
        try {
            return matchingRepositoriesCache.getOrCreate(elementType, () -> {
                final HashSet<AbstractSIRSRepository> result = new HashSet<>();
                for (final AbstractSIRSRepository repo : repositories.values()) {
                    if (elementType.isAssignableFrom(repo.getModelClass())) {
                        result.add(repo);
                    }
                }
                return result;
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Find a repository for update operations on {@link Element} of given type.
     * @param type The name of the type we want a {@link AbstractSIRSRepository} for. (Ex : RefMateriau, TronconDigue, etc.)
     * @return A valid repository for input type. Can be null if input canonical class name is unknown.
     */
    public AbstractSIRSRepository getRepositoryForType(String type) {
        return repositories.get(type);
    }


    ////////////////////////////////////////////////////////////////////////////
    // SPECIFIC REPOSITORIES
    ////////////////////////////////////////////////////////////////////////////
    private final ReferenceUsageRepository referenceUsageRepository;

    @Autowired
    private Previews previews;

    private final CouchDbConnector connector;
    private CoordinateReferenceSystem projection;
    private int srid;

    @Autowired
    public SessionCore(CouchDbConnector couchDbConnector) {
        this.connector = couchDbConnector;

        PoolingClientConnectionManager connectionPool = null;
        StdHttpClient connection = (StdHttpClient) connector.getConnection();
        if (connection.getBackend() instanceof DefaultHttpClient) {
            final DefaultHttpClient defaultConnection = (DefaultHttpClient)connection.getBackend();
            ClientConnectionManager connectionManager = defaultConnection.getConnectionManager();
            if (connectionManager instanceof PoolingClientConnectionManager) {
                connectionPool = (PoolingClientConnectionManager)connectionManager;
            }
        }

        if (connectionPool == null) {
            SirsCore.LOGGER.warning("Cannot get connection pool stats");
        } else {
            final PoolingClientConnectionManager pool = connectionPool;
            final Thread statDaemon = new Thread(() -> {
                final Thread t = Thread.currentThread();
                while (!t.isInterrupted()) {
                    SirsCore.LOGGER.fine(pool.getTotalStats().toString());
                    SirsCore.LOGGER.fine("Watched resources : "+ClosingDaemon.referenceCache.size());
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ex) {
                        SirsCore.LOGGER.log(Level.WARNING, "Stat thread interrupted !", ex);
                        t.interrupt();
                    }
                }
            });

            statDaemon.setName("Ektorp connection counter");
            statDaemon.setDaemon(true);
            statDaemon.start();
        }

        referenceUsageRepository = new ReferenceUsageRepository(connector);

        // Listen on user change
        utilisateurProperty.addListener(
                (ObservableValue<? extends Utilisateur> observable, Utilisateur oldValue, Utilisateur newValue) -> {
                    if (newValue == null || newValue.getRole() == null) {
                        role.set(Role.GUEST);
                    } else {
                        role.set(newValue.getRole());
                    }

                    // reset rights to most restricted, then unlock authorization regarding user role.
                    needValidationProperty.set(true);
                    geometryEditionProperty.set(false);
                    switch (role.get()) {
                        case USER:
                            needValidationProperty.set(false);
                            break;
                        case ADMIN:
                            geometryEditionProperty.set(true);
                            needValidationProperty.set(false);
                            break;
                        case EXTERN:
                            geometryEditionProperty.set(true);
                    }
                });
    }

    public CouchDbConnector getConnector() {
        return connector;
    }

    public int getSrid() {
        if (projection == null) {
            getProjection();
        }
        return srid;
    }

    public CoordinateReferenceSystem getProjection() {
        if (projection == null) {
            try {
                Optional<SirsDBInfo> info = DatabaseRegistry.getInfo(connector);
                if (info.isPresent()) {
                    projection = CRS.forCode(info.get().getEpsgCode());
                    srid = IdentifiedObjects.lookupEPSG(projection);
                }
            } catch (FactoryException e) {
                throw new SirsCoreRuntimeException(e);
            }
        }
        return projection;
    }

    /**
     *
     * @return the application task manager, designed to start users tasks in a
     * separate thread pool.
     */
    public TaskManager getTaskManager() {
        return SirsCore.getTaskManager();
    }

    public ReferenceUsageRepository getReferenceUsageRepository(){
        return referenceUsageRepository;
    }

    public Previews getPreviews() {
        return previews;
    }

    public List<ProprieteTroncon> getProprietesByTronconId(final String tronconId){
        final AbstractPositionableRepository<ProprieteTroncon> repo = (AbstractPositionableRepository<ProprieteTroncon>) getRepositoryForClass(ProprieteTroncon.class);
        return repo.getByLinearId(tronconId);
    }

    public List<GardeTroncon> getGardesByTronconId(final String tronconId){
        final AbstractPositionableRepository<GardeTroncon> repo = (AbstractPositionableRepository<GardeTroncon>) getRepositoryForClass(GardeTroncon.class);
        return repo.getByLinearId(tronconId);
    }

    public List<Objet> getObjetsByTronconId(final String tronconId) {
        final List<Objet> objets = new ArrayList<>();
        for(final AbstractSIRSRepository<Objet> repo : getRepositoriesForClass(Objet.class)) {
            if(repo instanceof AbstractPositionableRepository) {
                objets.addAll(((AbstractPositionableRepository<Objet>)repo).getByLinearId(tronconId));
            }
        }
        return objets;
    }

    public List<AbstractPositionDocument> getPositionDocumentsByTronconId(final String tronconId) {
        final List<AbstractPositionDocument> positions = new ArrayList<>();
                for(final AbstractSIRSRepository<AbstractPositionDocument> repo : getRepositoriesForClass(AbstractPositionDocument.class)) {
            if(repo instanceof AbstractPositionableRepository) {
                positions.addAll(((AbstractPositionableRepository<AbstractPositionDocument>)repo).getByLinearId(tronconId));
            }
        }

        return positions;
    }

    public List<? extends AbstractPhoto> getPhotoList(final String linearId) {
        final List<AbstractPhoto> photos = new ArrayList<>();

        final Collection<AbstractSIRSRepository> repos = getRepositoriesForClass(AvecPhotos.class);
        for (final AbstractSIRSRepository repo : repos) {
            if (repo instanceof AbstractPositionableRepository) {
                StreamingIterable byLinearId = ((AbstractPositionableRepository)repo).getByLinearIdStreaming(linearId);
                try (final CloseableIterator iterator = byLinearId.iterator()) {
                    while (iterator.hasNext()) {
                        photos.addAll(((AvecPhotos)iterator.next()).getPhotos());
                    }
                }
            } else {
                for (final Object photoContainer : repo.getAllStreaming()) {
                    for (final AbstractPhoto photo : ((AvecPhotos<? extends AbstractPhoto>)photoContainer).getPhotos()) {
                        Element parent = photo.getParent();
                        while (parent != null) {
                            if (parent instanceof AvecForeignParent && linearId.equalsIgnoreCase(((AvecForeignParent)parent).getForeignParentId())) {
                                photos.add(photo);
                                break;
                            }
                            parent = parent.getParent();
                        }
                    }
                }
            }
        }

        // Special case :
        StreamingIterable<Desordre> desordres = ((AbstractPositionableRepository<Desordre>) getRepositoryForClass(Desordre.class)).getByLinearIdStreaming(linearId);
        try (final CloseableIterator<Desordre> iterator = desordres.iterator()) {
            while (iterator.hasNext()) {
                final Desordre d = iterator.next();
                for (final Observation o : d.observations) {
                    photos.addAll(o.photos);
                }
            }
        }

        return photos;
    }

    public List<Positionable> getPositionableByLinearId(final String linearId) {
        final ArrayList<Positionable> positionables = new ArrayList<>();
        for (final AbstractPositionableRepository repo : applicationContext.getBeansOfType(AbstractPositionableRepository.class).values()) {
            positionables.addAll(repo.getByLinearId(linearId));
        }
        final List<? extends AbstractPhoto> photos = getPhotoList(linearId);
        for (final AbstractPhoto photo : photos) {
            if (photo instanceof Positionable)
                positionables.add((Positionable)photo);
        }

        return positionables;
    }

//    private static List<Class<? extends Element>> ELEMENT_IMPLS;
    /**
     * Search in {@link Element} {@link ServiceLoader} for all implementations of
     * a given type.
     *
     * @param <T> Type of object to search for.
     * @param target Class to find implementations for.
     * @return List of concrete classes inheriting {@link Element} and given target.
     */
    public static <T> List<Class<? extends T>>getConcreteSubTypes(final Class<T> target) {
        final SessionCore sc = InjectorCore.getBean(SessionCore.class);

        final ArrayList<Class<? extends T>> result = new ArrayList<>();
        for (final T t : sc.getApplicationContext().getBeansOfType(target).values()) {
            result.add((Class<? extends T>) t.getClass());
        }

        return result;

//        synchronized (SessionCore.class) {
//            if (ELEMENT_IMPLS == null) {
//                final Iterator<Element> registeredImpls = ServiceLoader.load(Element.class).iterator();
//                final ArrayList<Class<? extends Element>> tmpList = new ArrayList<>();
//                while (registeredImpls.hasNext()) {
//                    tmpList.add(registeredImpls.next().getClass());
//                }
//                ELEMENT_IMPLS = Collections.unmodifiableList(tmpList);
//            }
//        }
//
//        if (target == null || target.equals(Element.class)) {
//            return (List) ELEMENT_IMPLS;
//        }
//
//        final ArrayList<Class<? extends T>> result = new ArrayList<>();
//        for (final Class c : ELEMENT_IMPLS) {
//            if (target.isAssignableFrom(c))
//                result.add(c);
//        }
//
//        return result;
    }

    /**
     * Take an element in input, and return the same, but with its {@link Element#parentProperty() }
     * and {@link Element#getCouchDBDocument() } set.
     *
     * @param e The element we want to get parent for.
     * @return The same element, completed with its parent, Or a null value if we
     * cannot get full version of the element.
     */
    public Optional<? extends Element> getCompleteElement(Element e) {
        if (e != null) {
            if (e.getCouchDBDocument() != null) {
                // For objects like {@link tronconDigue}, we force reload, because
                //they're root objects. It means checking their document do not
                //ensure they're complete.
                if (e.getCouchDBDocument() == e) {
                    return Optional.of((Element)getRepositoryForClass(e.getClass()).get(e.getId()));
                } else {
                    return Optional.of(e);
                }
            } else {
                String documentId = e.getDocumentId();
                Identifiable parent;
                if (documentId != null && !documentId.isEmpty()) {
                    parent = getFromCaches(documentId);
                } else {
                    parent = null;
                }

                if (parent == null) {
                    Preview label = previews.get(e.getId());
                    AbstractSIRSRepository targetRepo = getRepositoryForType(label.getDocClass());
                    if (targetRepo != null) {
                        parent = targetRepo.get(documentId);
                    }
                }

                if (parent instanceof Element) {
                    return Optional.ofNullable(((Element) parent).getChildById(e.getId()));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Search in repository caches for document with given Id.
     * @param docId Id of the document to retrieve.
     * @return Found cached doc, or null if we find nothing in caches.
     */
    private Element getFromCaches(final String docId) {
        for (final AbstractSIRSRepository repo : repositories.values()) {
            Identifiable fromCache = repo.getFromCache(docId);
            if (fromCache instanceof Element) {
                return (Element) fromCache;
            }
        }
        return null;
    }

    /**
     * Analyse input object to find a matching {@link Element} registered in database.
     * @param toGetElementFor The object which represents the Element to retrieve.
     * Can be a {@link Preview}, {@link  ElementHit}, or a {@link String} (in which
     * case it must represent a valid element ID).
     * @return An optional which contains the found value, if any.
     */
    public Optional<? extends Element> getElement(Object toGetElementFor) {
        if (toGetElementFor instanceof Element) {
            return getCompleteElement((Element)toGetElementFor);
        }

        if (toGetElementFor instanceof String) {
            final Element fromCaches = getFromCaches((String)toGetElementFor);
            if (fromCaches != null) {
                return Optional.of(fromCaches);
            } else {
                toGetElementFor = previews.get((String)toGetElementFor);
            }
        }

        if (toGetElementFor instanceof Preview) {
            final Preview summary = (Preview) toGetElementFor;
            if (summary.getDocClass() != null) {
                final AbstractSIRSRepository repository = getRepositoryForType(summary.getDocClass());
                if (repository != null) {
                    final Identifiable tmp = repository.get(summary.getDocId() == null ? summary.getElementId() : summary.getDocId());
                    if (tmp instanceof Element) {
                        if (summary.getElementId() != null) {
                            return Optional.of(((Element) tmp).getChildById(summary.getElementId()));
                        } else {
                            return Optional.of((Element) tmp);
                        }
                    }
                }
            }
        } else if (toGetElementFor instanceof ElementHit) {
            final ElementHit hit = (ElementHit) toGetElementFor;
            if (hit.getElementClassName() != null) {
                final AbstractSIRSRepository repository = getRepositoryForType(hit.getElementClassName());
                if (repository != null) {
                    final Identifiable tmp = repository.get(hit.getDocumentId());
                    if (tmp instanceof Element) {
                        return Optional.of((Element) tmp);
                    }
                }
            }
        }
        return Optional.empty();
    }

    public String getElementType(final Object o) {
        if (o instanceof Element) {
            return o.getClass().getCanonicalName();
        } else if (o instanceof Preview) {
            return ((Preview)o).getElementClass();
        } else if (o instanceof ElementHit) {
            return ((ElementHit)o).getElementClassName();
        } else {
            return null;
        }
    }

    /**
     * Update or add given elements to database. If one of the given elements is
     * a fragment, we'll try to get back complete element before updating.
     * @param target Collection of objects to update.
     * @return A list of failed operations. Can be empty, but never null.
     */
    public List<DocumentOperationResult> executeBulk(Collection<Element> target) {
        if (target == null || target.isEmpty()) {
            return Collections.EMPTY_LIST;
        }

        // Sort input documents by type to delegate bulk to repositories (to avoid cache problems)
        final HashMap<Class, HashSet<Element>> toUpdate = new HashMap<>();
        final ArrayList<DocumentOperationResult> result = new ArrayList<>();
        Iterator<Element> iterator = target.iterator();
        while (iterator.hasNext()) {
            Element e = iterator.next();
            if (e.getDocumentId() != null && !e.getDocumentId().equals(e.getId())) {
                // input object was only part of a couchdb document, so we try to retrieve complete element.
                Optional<? extends Element> completeElement = getCompleteElement(e);
                if (completeElement.isPresent()) {
                    e = completeElement.get().getCouchDBDocument();
                } else {
                    result.add(DocumentOperationResult.newInstance(e.getDocumentId(), "Cannot update document (client side error).", "Impossible to retrieve complete document to update from fragment."));
                }
            }

            HashSet<Element> tmpList = toUpdate.get(e.getClass());
            if (tmpList == null) {
                tmpList = new HashSet<>();
                toUpdate.put(e.getClass(), tmpList);
            }
            tmpList.add(e);
        }

        // Finally, we call bulk executor of each concerned repository.
        for (final Map.Entry<Class, HashSet<Element>> entry : toUpdate.entrySet()) {
            final HashSet<Element> docs = entry.getValue();
            final Class docClass = entry.getKey();
            if (!docs.isEmpty()) {
                try {
                    final AbstractSIRSRepository repo = getRepositoryForClass(docClass);
                    if (repo == null) {
                        SirsCore.LOGGER.log(Level.WARNING, "No repository found for " + docClass);
                        for (final Element inError : docs) {
                            result.add(DocumentOperationResult.newInstance(inError.getDocumentId(), "Cannot update document (client side error).", "Cannot find any repository for class " + docClass.getCanonicalName()));
                        }
                    } else {
                        result.addAll(repo.executeBulk(docs));
                    }
                } catch (NoSuchBeanDefinitionException e) {
                    SirsCore.LOGGER.log(Level.WARNING, "No repository found for " + docClass, e);
                    for (final Element inError : docs) {
                        result.add(DocumentOperationResult.newInstance(inError.getDocumentId(), "Cannot update document (client side error).", "Cannot find any repository for class " + docClass.getCanonicalName()));
                    }
                } catch (Exception e) {
                    SirsCore.LOGGER.log(Level.WARNING, "Unexpected error on bulk execution.", e);
                    for (final Element inError : docs) {
                        result.add(DocumentOperationResult.newInstance(inError.getDocumentId(), "Unexpected error on position update", e.getMessage()));
                    }
                }
            }
        }

        return result;
    }

    /**
     * Create an user in CouchDB database.
     * @param login Login to set to thee user.
     * @param password Password of the user. It will be hashed before being sent.
     * @param role Role to give to the user
     * @throws IllegalArgumentException If an user with the same login already exists.
     */
    public void createUser(final String login, final String password, final Role role) {
        UtilisateurRepository repo = applicationContext.getBean(UtilisateurRepository.class);
        if (repo.getByLogin(login).isEmpty()) {
            final Utilisateur user = ElementCreator.createAnonymValidElement(Utilisateur.class);
            user.setLogin(login);
            user.setPassword(SirsCore.hexaMD5(password));
            user.setRole(role);
            repo.add(user);
        } else {
            throw new IllegalArgumentException("An user already exists for login " + login);
        }
    }


    public H2Helper getH2Helper() {
        return applicationContext.getBean(H2Helper.class);
    }

    /**
     * Check that given user has modification rights over given element.
     * @param input The element to check modification rights for.
     * @return True if current session owner can edit it, false otherwise.
     */
    public boolean editionAuthorized(final Element input) {
        // les utilisateurs qui peuvent éditer un élément sont ceux qui peuvent en créer de valides OU ceux qui sont l'auteur d'un document invalide.
        return createValidDocuments().get() || (!input.getValid() && getUtilisateur().getId().equals(input.getAuthor()));
    }
}
