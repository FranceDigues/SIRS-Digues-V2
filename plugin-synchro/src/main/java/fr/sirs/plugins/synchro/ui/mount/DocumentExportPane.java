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
package fr.sirs.plugins.synchro.ui.mount;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.sirs.Injector;
import fr.sirs.SIRS;
import fr.sirs.Session;
import fr.sirs.core.SirsCore;
import fr.sirs.core.component.AbstractPositionableRepository;
import fr.sirs.core.component.AbstractSIRSRepository;
import fr.sirs.core.component.DesordreRepository;
import fr.sirs.core.component.OuvrageHydrauliqueAssocieRepository;
import fr.sirs.core.component.ReseauHydrauliqueCielOuvertRepository;
import fr.sirs.core.component.ReseauHydrauliqueFermeRepository;
import fr.sirs.core.component.StationPompageRepository;
import fr.sirs.core.model.AbstractPhoto;
import fr.sirs.core.model.AvecPhotos;
import fr.sirs.core.model.Photo;
import fr.sirs.core.model.Preview;
import fr.sirs.core.model.SIRSFileReference;
import fr.sirs.plugins.synchro.SynchroPlugin;
import fr.sirs.plugins.synchro.common.TextCell;
import fr.sirs.plugins.synchro.ui.DocumentSelector;
import fr.sirs.util.CopyTask;
import fr.sirs.util.property.DocumentRoots;
import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.scene.layout.BorderPane;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.util.StringConverter;
import org.geotoolkit.gui.javafx.util.TaskManager;
import org.geotoolkit.internal.GeotkFX;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * TODO : detect mobile emplacement. TODO : manage documents already on mobile
 *
 * @author Alexis Manin (Geomatys)
 */
public class DocumentExportPane extends StackPane {

    @FXML
    private BorderPane uiConfigPane;

    @FXML
    private ListView<SIRSFileReference> uiDesktopList;

    @FXML
    private Button uiDelete;

    @FXML
    private ListView<SIRSFileReference> uiMobileList;

    @FXML
    private Button uiDesktopToMobile;

    @FXML
    private Button uiExportBtn;

    @FXML
    private Button uiOutputDriveBtn;

    @FXML
    private Label uiOutputDriveLabel;

    @FXML
    private Label uiRemainingSpace;

    @FXML
    private Label uiCopyTitle;

    @FXML
    private ProgressBar uiCopyProgress;

    @FXML
    private Label uiCopyMessage;

    @FXML
    private BorderPane uiCopyPane;

    @FXML
    private BorderPane uiLoadingPane;

    @FXML
    private Label uiLoadingLabel;

    @FXML
    private ChoiceBox<Integer> uiPhotoChoice;

    @FXML
    private Spinner<Integer> uiPhotoSpinner;

    @Autowired
    private Session session;

    @Autowired
    private DesordreRepository desordreRepo;

    @Autowired
    private OuvrageHydrauliqueAssocieRepository ouvrageHydrauliqueAssocieRepository;

    @Autowired
    private ReseauHydrauliqueFermeRepository reseauHydrauliqueFermeRepository;

    @Autowired
    private ReseauHydrauliqueCielOuvertRepository reseauHydrauliqueCielOuvertRepository;

    @Autowired
    private StationPompageRepository stationPompageRepository;

    private final Tooltip copyMessageTooltip = new Tooltip();

    private final SimpleLongProperty outputSize = new SimpleLongProperty();
    private final SimpleObjectProperty<Path> mobileDocumentDir = new SimpleObjectProperty<>();
    private final SimpleObjectProperty<CopyTask> copyTaskProperty = new SimpleObjectProperty<>();

    private final ObservableMap<Class, AbstractSIRSRepository<SIRSFileReference>> repositories = FXCollections.observableHashMap();

    private final DocumentSelector selector;

    public DocumentExportPane() {
        super();
        SIRS.loadFXML(this);
        Injector.injectDependencies(this);

        selector = new DocumentSelector(session);
        uiConfigPane.setTop(selector);

        // Prevent actions when a copy or a loading is running.
        FocusListener focusListener = new FocusListener();
        uiCopyPane.visibleProperty().addListener(focusListener);
        uiLoadingPane.visibleProperty().addListener(focusListener);
        uiDesktopList.setCellFactory(list -> new TextCell());
        uiDesktopList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        uiDesktopList.setItems(selector.getDocuments().filtered(obj -> !uiMobileList.getItems().contains(obj)));
        uiMobileList.setCellFactory(list -> new TextCell());
        uiMobileList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        uiDesktopToMobile.setText(null);
        uiDesktopToMobile.setGraphic(new ImageView(SIRS.ICON_ARROW_RIGHT_BLACK));
        uiDelete.setText(null);
        uiDelete.setGraphic(new ImageView(GeotkFX.ICON_DELETE));

        mobileDocumentDir.addListener(this::updateOutputDriveInfo);

        uiCopyMessage.setTooltip(copyMessageTooltip);
        copyTaskProperty.addListener(this::copyTaskUpdate);

        ObservableList<Integer> photoList = FXCollections.observableArrayList();
        photoList.addAll(0, 1, -1, Integer.MAX_VALUE);
        uiPhotoChoice.setItems(photoList);
        uiPhotoChoice.setConverter(new PhotoNumberConverter());
        uiPhotoChoice.getSelectionModel().select(0);

        uiPhotoChoice.valueProperty().addListener((ObservableValue<? extends Integer> observable, Integer oldValue, Integer newValue) -> {
            if (newValue < 0) {
                uiPhotoSpinner.setVisible(true);
            } else {
                uiPhotoSpinner.setVisible(false);
            }
        });
        uiPhotoSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, Integer.MAX_VALUE));
    }

    /**
     * Open a directory chooser to allow user to specify wanted output drive.
     */
    @FXML
    private void chooseOutputDir() {
        mobileDocumentDir.set(SynchroPlugin.chooseMedia(getScene().getWindow()));
    }

    /**
     * Try to retrieve available space on chosen mobile folder.
     *
     * @param obs source observable value
     * @param oldValue previous selected folder
     * @param newValue currently selected folder
     */
    void updateOutputDriveInfo(final ObservableValue<? extends Path> obs, Path oldValue, Path newValue) {
        if (newValue == null) {
            uiOutputDriveLabel.setText("");
            uiRemainingSpace.setText("inconnu");
        } else {
            try {
                final FileStore fileStore = Files.getFileStore(newValue);
                uiOutputDriveLabel.setText("Périphérique de type " + fileStore.type() + " : " + fileStore.name());
                final long usableSpace = fileStore.getUsableSpace();
                outputSize.set(usableSpace);
                uiRemainingSpace.setText(SIRS.toReadableSize(usableSpace));
            } catch (IOException ex) {
                uiOutputDriveLabel.setText("Impossible de récupérer le type ou le nom du périphérique.");
                uiRemainingSpace.setText("inconnu");
            }
        }
    }

    /**
     * Refresh UI bindings on copy task.
     *
     * @param obs
     * @param oldTask
     * @param newTask
     */
    void copyTaskUpdate(final ObservableValue<? extends CopyTask> obs, CopyTask oldValue, CopyTask newValue) {
        if (oldValue != null) {
            uiCopyPane.visibleProperty().unbind();
            uiCopyTitle.textProperty().unbind();
            uiCopyMessage.textProperty().unbind();
            copyMessageTooltip.textProperty().unbind();
            uiCopyProgress.progressProperty().unbind();
        }
        if (newValue != null) {
            uiCopyPane.visibleProperty().bind(newValue.runningProperty());
            uiCopyTitle.textProperty().bind(newValue.titleProperty());
            uiCopyMessage.textProperty().bind(newValue.messageProperty());
            copyMessageTooltip.textProperty().bind(newValue.messageProperty());
            uiCopyProgress.progressProperty().bind(newValue.progressProperty());
        }
    }

    /*
     * UI ACTIONS
     */
    @FXML
    void sendToMobileList(ActionEvent event) {
        ObservableList<SIRSFileReference> selectedItems = uiDesktopList.getSelectionModel().getSelectedItems();
        if (selectedItems.isEmpty())
            return;

        uiMobileList.getItems().addAll(selectedItems);
        uiDesktopList.setItems(selector.getDocuments().filtered(obj -> !uiMobileList.getItems().contains(obj)));
    }

    @FXML
    void deleteFromMobile(ActionEvent event) {
        ObservableList<SIRSFileReference> selectedItems = uiMobileList.getSelectionModel().getSelectedItems();
        if (selectedItems.isEmpty())
            return;

        uiMobileList.getItems().removeAll(selectedItems);
        uiDesktopList.setItems(selector.getDocuments().filtered(obj -> !uiMobileList.getItems().contains(obj)));
    }

    @FXML
    void cancelTask(ActionEvent event) {
        final Task tmp = copyTaskProperty.get();
        if (tmp != null)
            tmp.cancel();
    }

    /**
     * Fire document export process when user click on "export" button.
     * Steps are :
     * 1 - Find all documents / photos to export.
     * 2 - Compute total size to copy.
     * 3 - Compare size to copy with output available space.
     * 4 - if available space is large enough, ask user if he's sure before
     * proceeding the copy, return otherwise (after showing a warning message).
     * 5 - Lock panel
     * 6 - Proceed ccopy
     * 7 - return to main panel.
     *
     * @param event
     */
    @FXML
    void exportToMobile(ActionEvent event) {
        final Path destination = mobileDocumentDir.get();
        if (destination == null || !Files.isDirectory(destination)) {
            new Alert(Alert.AlertType.WARNING, "Impossible de déterminer le répertoire de sortie.", ButtonType.OK).show();
            return;
        }

        final Path[] toCheck = new Path[]{
            destination.resolve(SynchroPlugin.DOCUMENT_FOLDER),
            destination.resolve(SynchroPlugin.PHOTO_FOLDER)
        };
        for (final Path tmpPath : toCheck) {
            if (!Files.isDirectory(tmpPath)) {
                try {
                    Files.createDirectories(tmpPath);
                } catch (Exception ex) {
                    SirsCore.LOGGER.log(Level.WARNING, "Cannot create following directory on mobile device : " + tmpPath.toString(), ex);
                    new Alert(Alert.AlertType.WARNING, "Impossible de déterminer le répertoire de sortie.", ButtonType.OK).show();
                    return;
                }
            }
        }

        // Before launching copy, we check if user has selected troncons for photo export.
        final Integer photoNumber = uiPhotoChoice.getValue() < 0 ? uiPhotoSpinner.getValue() : uiPhotoChoice.getValue();
        if (photoNumber > 0) {
            ObservableList<Preview> tronconItems = selector.getSelectedTroncons();
            if (tronconItems.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Aucun tronçon sélectionné. Les photos ne seront pas exportées. Êtes-vous sûr ?", ButtonType.NO, ButtonType.YES);
                alert.setResizable(true);
                // cancel copy to let user select tronçons to export photos for.
                if (ButtonType.NO.equals(alert.showAndWait().orElse(ButtonType.NO))) {
                    return;
                }
            }
        }

        // 1
        uiLoadingPane.setVisible(true);
        uiLoadingLabel.setText("Liste les documents à copier");
        // List each files which must be copied (map key), along with their precise destination, relative to destination folder (map value).
        final Task<Map<Path, Path>> listFileTask = new TaskManager.MockTask<>(uiLoadingLabel.getText(), () -> {
            final Map<Path, Path> docMap = new HashMap<>();
            // Find document list size
            String chemin;
            Path root, docInput, docDest;
            for (final SIRSFileReference ref : uiMobileList.getItems()) {
                chemin = ref.getChemin();
                if (chemin == null || chemin.isEmpty())
                    continue;
                root = DocumentRoots.getRoot(ref).orElseThrow(() -> new IllegalStateException("Aucune dossier racine trouvé pour les documents !"));
                docInput = SIRS.concatenatePaths(root, chemin);
                if (Files.isRegularFile(docInput)) {
                    docDest = SynchroPlugin.DOCUMENT_FOLDER.resolve(root.relativize(docInput));
                    docMap.put(docInput, docDest);
                }
            }

            /* We must analyze database documents to find all photos to export.
             * There's two distinct cases :
             * - Disorders, which don't implement {AvecPhoto} interface, but
             * contains observations which are {AvecPhoto} objects.
             * - Others, which are {AvecPhoto} objects for which we can directly
             * get from repository.
             */
            if (photoNumber > 0) {
                final ValidPhotoPredicate photoFilter = new ValidPhotoPredicate();
                final PhotoDateComparator photoComparator = new PhotoDateComparator();
                ObservableList<Preview> tronconItems = selector.getSelectedTroncons();
                if (!tronconItems.isEmpty()) {
                    final List<AbstractPhoto> photos = new ArrayList<>();
                    List<AbstractPositionableRepository> repos = (List) session.getRepositoriesForClass(AvecPhotos.class)
                            .stream().filter(repo -> repo instanceof AbstractPositionableRepository).collect(Collectors.toList());

                    for (final Preview tdPreview : tronconItems) {
                        final String linearId = tdPreview.getElementId();
                        photos.addAll(desordreRepo.getByLinearId(linearId).stream()
                                .flatMap(dd -> dd.observations.stream())
                                // for each object, we take the keep only valid
                                // photo, and keep only the most recent ones.
                                .flatMap(obs -> obs.photos.stream()
                                        .filter(photoFilter)
                                        .sorted(photoComparator)
                                        .limit(photoNumber))
                                .limit(photoNumber)
                                .collect(Collectors.toList())
                        );
                        photos.addAll(ouvrageHydrauliqueAssocieRepository.getByLinearId(linearId).stream()
                                .flatMap(dd -> dd.observations.stream())
                                // for each object, we take the keep only valid
                                // photo, and keep only the most recent ones.
                                .flatMap(obs -> obs.photos.stream()
                                        .filter(photoFilter)
                                        .sorted(photoComparator)
                                        .limit(photoNumber))
                                .limit(photoNumber)
                                .collect(Collectors.toList())
                        );
                        photos.addAll(reseauHydrauliqueFermeRepository.getByLinearId(linearId).stream()
                                .flatMap(dd -> dd.observations.stream())
                                // for each object, we take the keep only valid
                                // photo, and keep only the most recent ones.
                                .flatMap(obs -> obs.photos.stream()
                                        .filter(photoFilter)
                                        .sorted(photoComparator)
                                        .limit(photoNumber))
                                .limit(photoNumber)
                                .collect(Collectors.toList())
                        );
                        photos.addAll(reseauHydrauliqueCielOuvertRepository.getByLinearId(linearId).stream()
                                .flatMap(dd -> dd.observations.stream())
                                // for each object, we take the keep only valid
                                // photo, and keep only the most recent ones.
                                .flatMap(obs -> obs.photos.stream()
                                        .filter(photoFilter)
                                        .sorted(photoComparator)
                                        .limit(photoNumber))
                                .limit(photoNumber)
                                .collect(Collectors.toList())
                        );
                        photos.addAll(stationPompageRepository.getByLinearId(linearId).stream()
                                .flatMap(dd -> dd.observations.stream())
                                // for each object, we take the keep only valid
                                // photo, and keep only the most recent ones.
                                .flatMap(obs -> obs.photos.stream()
                                        .filter(photoFilter)
                                        .sorted(photoComparator)
                                        .limit(photoNumber))
                                .limit(photoNumber)
                                .collect(Collectors.toList())
                        );

                        photos.addAll((Collection<? extends AbstractPhoto>) repos.stream()
                                .flatMap(repo -> repo.getByLinearId(linearId).stream())
                                // for each object, we take the most recent valid photos.
                                .flatMap(pos -> ((AvecPhotos) pos).getPhotos().stream()
                                        .filter(photoFilter)
                                        .sorted(photoComparator)
                                        .limit(photoNumber))
                                .collect(Collectors.toList()));
                    }

                    // keep only photos defined with an accessible file.
                    for (final AbstractPhoto photo : photos) {
                        chemin = photo.getChemin();
                        if (chemin == null || chemin.isEmpty())
                            continue;
                        root = DocumentRoots.getRoot(photo).orElseThrow(() -> new IllegalStateException("Aucune dossier racine trouvé pour les photographies !"));
                        docInput = SIRS.concatenatePaths(root, chemin);
                        if (Files.isRegularFile(docInput)) {
                            docDest = SynchroPlugin.PHOTO_FOLDER.resolve(root.relativize(docInput));
                            docMap.put(docInput, docDest);
                        }
                    }
                }
            }
            return docMap;
        });

        listFileTask.setOnFailed(evt -> SIRS.fxRun(false, () -> {
            uiLoadingPane.setVisible(false);
            GeotkFX.newExceptionDialog("Impossible de lister tous les fichiers pour la copie", listFileTask.getException()).show();
        }));

        listFileTask.setOnCancelled(evt -> SIRS.fxRun(false, () -> {
            uiLoadingPane.setVisible(false);
        }));

        listFileTask.setOnSucceeded(listEvt -> SIRS.fxRun(false, () -> {
            final Map<Path, Path> toCopy = listFileTask.getValue();
            // 2
            uiLoadingLabel.setText("Calcul de la quantité de donnée à copier");
            final Task<Long> sizeTask = new TaskManager.MockTask<>(uiLoadingLabel.getText(), () -> {
                return toCopy.keySet().stream()
                        .map(doc -> {
                            try {
                                return Files.getFileAttributeView(doc, BasicFileAttributeView.class).readAttributes().size();
                            } catch (IOException ex) {
                                throw new RuntimeException("Impossible de calculer une taille pour " + doc, ex);
                            }
                        })
                        .reduce(0l, (first, second) -> first + second);
            });

            sizeTask.setOnFailed(evt -> SIRS.fxRun(false, () -> {
                uiLoadingPane.setVisible(false);
                GeotkFX.newExceptionDialog("Impossible de calculer la taille totale à copier", sizeTask.getException()).show();
            }));

            sizeTask.setOnCancelled(evt -> SIRS.fxRun(false, () -> {
                uiLoadingPane.setVisible(false);
            }));

            sizeTask.setOnSucceeded(evt -> SIRS.fxRun(false, () -> {
                try {
                    // 3
                    final long sizeToCopy = sizeTask.getValue();
                    if (sizeToCopy <= 0) {
                        new Alert(Alert.AlertType.INFORMATION, "Aucune donnée à copier.", ButtonType.OK).show();
                    } else if (outputSize.get() < sizeToCopy) {
                        new Alert(Alert.AlertType.ERROR, "Espace insuffisant sur le media de sortie.", ButtonType.OK).show();
                    } else {
                        // 4
                        final String sourceReadable = SIRS.toReadableSize(sizeToCopy);
                        final String outputName = mobileDocumentDir.get().toString();
                        final String outputReadable = uiRemainingSpace.getText();
                        final Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                                "Vous allez copier " + sourceReadable + " sur " + outputName + " (Espace restant : " + outputReadable + ").\nÊtes-vous sûr ?",
                                ButtonType.NO, ButtonType.YES);
                        alert.setResizable(true);
                        ButtonType choice = alert.showAndWait().orElse(ButtonType.NO);

                        if (ButtonType.YES.equals(choice)) {
                            final CopyTask copyTask = new DocumentCopy(toCopy.keySet(), destination, input -> toCopy.get(input));
                            // 5
                            copyTaskProperty.set(copyTask);
                            // 6 & 7 : Let's do it !
                            TaskManager.INSTANCE.submit(copyTask);
                        }
                    }
                } finally {
                    uiLoadingPane.setVisible(false);
                }
            }));

            TaskManager.INSTANCE.submit(sizeTask);
        }));

        TaskManager.INSTANCE.submit(listFileTask);
    }

    private class FocusListener implements ChangeListener<Boolean> {

        private Node focused;

        @Override
        public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
            if (uiCopyPane.isVisible() || uiLoadingPane.isVisible()) {
                focused = getScene().getFocusOwner();
                uiConfigPane.setDisable(true);
            } else {
                uiConfigPane.setDisable(false);
                if (focused != null) {
                    focused.requestFocus();
                }
            }
        }

    }

    /**
     * A convinient converter to display number of photograph to export to user.
     */
    public static class PhotoNumberConverter extends StringConverter<Integer> {

        protected static final String ALL = "Toutes";
        protected static final String LAST = "La dernière";
        protected static final String NONE = "Aucune";
        protected static final String CHOOSE = "Choisir le nombre à importer";

        @Override
        public String toString(Integer object) {
            if (object < 0) {
                return CHOOSE;
            } else if (object == null || object == 0) {
                return NONE;
            } else if (object == 1) {
                return LAST;
            } else if (object == Integer.MAX_VALUE) {
                return ALL;
            } else {
                return object.toString();
            }
        }

        @Override
        public Integer fromString(String string) {
            if (CHOOSE.equals(string)) {
                return -1;
            } else if (NONE.equals(string)) {
                return 0;
            } else if (LAST.equals(string)) {
                return 1;
            } else if (ALL.equals(string)) {
                return Integer.MAX_VALUE;
            } else if (string == null || string.isEmpty()) {
                return 0;
            } else {
                return Integer.valueOf(string);
            }
        }
    }

    /**
     * A predicate to know if a photo file is accessible or not. Return true if
     * the given photo contains a path pointing to a regular file, as defined by
     * {@link Files#isRegularFile(java.nio.file.Path, java.nio.file.LinkOption...) }
     */
    private static class ValidPhotoPredicate implements Predicate<Photo> {

        @Override
        public boolean test(Photo t) {
            if (t == null || t.getChemin() == null || t.getChemin().isEmpty()) {
                return false;
            } else {
                return Files.isRegularFile(SIRS.getDocumentAbsolutePath(t));
            }
        }
    }

    /**
     * A comparator which aim is to put most recent photo at the beginning of a
     * sorted list, to keep only new photos when we truncate photo list.
     */
    public static class PhotoDateComparator implements Comparator<AbstractPhoto> {
        @Override
        public int compare(AbstractPhoto o1, AbstractPhoto o2) {
            if ((o1 == null || o1.getDate() == null) && (o2 == null || o2.getDate() == null)) {
                return 0;
            } else if (o1 == null  || o1.getDate() == null) {
                return 1;
            } else if (o2 == null || o2.getDate() == null) {
                return -1;
            } else {
                return -o1.getDate().compareTo(o2.getDate());
            }
        }
    }

    private static class DocumentCopy extends CopyTask {

        public DocumentCopy(Collection<Path> toCopy, Path destination) {
            super(toCopy, destination);
        }

        public DocumentCopy(final Collection<Path> toCopy, final Path destination, final Function<Path, Path> resolver) {
            super(toCopy, destination, resolver);
        }

        @Override
        protected Boolean call() throws Exception {
            final boolean success = super.call();

            // Create a JSON object mirroring file structure, to ease mobile app future scans.
            if (success) {
                final List<JsonPath> files = new ArrayList<>();
                Files.walkFileTree(destination, new SimpleFileVisitor<Path>(){

                    JsonPath parentDir = null;
                    JsonPath currentDir = null;

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        // TODO : check mime-type using Files.probeContentType
                        final JsonPath jsonFile = new JsonPath(file.getFileName().toString(), attrs.creationTime().toMillis(), attrs.lastModifiedTime().toMillis(), attrs.size(), attrs.isSymbolicLink(), attrs.isDirectory());
                        if (currentDir == null) {
                            files.add(jsonFile);
                        } else {
                            currentDir.children.add(jsonFile);
                        }
                        return super.visitFile(file, attrs);
                    }

                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                        currentDir = new JsonPath(dir.getFileName().toString(), attrs.creationTime().toMillis(), attrs.lastModifiedTime().toMillis(), attrs.size(), attrs.isSymbolicLink(), attrs.isDirectory());
                        if (parentDir != null) {
                            parentDir.children.add(currentDir);
                            currentDir.parent = parentDir;
                        } else {
                            files.add(currentDir);
                        }
                        return super.preVisitDirectory(dir, attrs);
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        if (currentDir != null) {
                            parentDir = currentDir.parent;
                        }
                        currentDir = null;
                        return super.postVisitDirectory(dir, exc);
                    }
                });
                new ObjectMapper().writeValue(Files.newOutputStream(destination.resolve("index.json"), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING), files);
            }
            return success;
        }




    }

    /**
     * Simple structure giving information about a file-tree item.
     */
    private static class JsonPath {
        public String name;
        public long creationTime;
        public long lastModified;
        public long size;
        public boolean isDir;
        public boolean isSymbolicLink;
        public transient JsonPath parent;
        public final List<JsonPath> children = new ArrayList<>();

        public JsonPath() {};

        public JsonPath(final String name, final long creationTime, final long lastModified, final long size, final boolean isSymbolicLink, final boolean isDir) {
            this.name = name;
            this.creationTime = creationTime;
            this.lastModified = lastModified;
            this.size = size;
            this.isSymbolicLink = isSymbolicLink;
            this.isDir = isDir;
        }
    }
}
