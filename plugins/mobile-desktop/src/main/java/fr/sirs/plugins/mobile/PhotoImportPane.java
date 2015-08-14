package fr.sirs.plugins.mobile;

import fr.sirs.SIRS;
import fr.sirs.Session;
import fr.sirs.core.SirsCore;
import fr.sirs.core.model.AvecPhotos;
import fr.sirs.core.model.Desordre;
import fr.sirs.core.model.Element;
import fr.sirs.core.model.LabelMapper;
import fr.sirs.core.model.Photo;
import fr.sirs.util.CopyTask;
import fr.sirs.util.property.SirsPreferences;
import java.awt.Color;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.FileStore;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.util.StringConverter;
import org.apache.sis.util.ArgumentChecks;
import org.geotoolkit.font.FontAwesomeIcons;
import org.geotoolkit.font.IconBuilder;
import org.geotoolkit.gui.javafx.util.TaskManager;
import org.geotoolkit.internal.GeotkFX;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Alexis Manin (Geomatys)
 */
public class PhotoImportPane extends StackPane {

    private static final Path PHOTO_FOLDER = Paths.get("files", "nouvellesPhotos");

    /**
     * Expected structure for the name of images to import. It should start with
     * the related document id, followed by any suffix judged convinient by mobile
     * application to differentiate different images owned by the same document.
     * We also ensure that file extension matches one famous image format.
     */
    private static final Pattern IMG_PATTERN = Pattern.compile("(?i)^(\\w{32})(.*)(\\.(jpe?g|png|bmp|tiff?))$");

    public static final Image ICON_TRASH_BLACK = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_TRASH_O,16,Color.BLACK),null);

    private static enum DIRECTION {
        UP,
        DOWN;
    }

    @FXML
    private VBox uiParameterContainer;

    @FXML
    private Label uiSourceName;

    @FXML
    private Label uiSourceType;

    @FXML
    private Label uiSourceUsableSpace;

    @FXML
    private ProgressIndicator uiSoureSpaceProgress;

    @FXML
    private Label uiRootLabel;

    @FXML
    private Hyperlink uiChooseSubDir;

    @FXML
    private Label uiSubDirLabel;

    @FXML
    private ProgressIndicator uiDestSpaceProgress;

    @FXML
    private ComboBox<Character> uiSeparatorChoice;
    @FXML
    private ListView<PropertyDescriptor> uiPrefixListView;

    @FXML
    private Button uiAddPrefixBtn;

    @FXML
    private Button uiMoveUpBtn;

    @FXML
    private Button uiMoveDownBtn;

    @FXML
    private Button uiDeletePrefixBtn;

    @FXML
    private ProgressBar uiImportProgress;

    @FXML
    private Button uiImportBtn;

    @FXML
    private TitledPane uiPrefixTitledPane;

    @FXML
    private Label uiCopyMessage;
    private final Tooltip copyMessageTooltip = new Tooltip();

    /**
     * Source directory in which we'll find photos to transfer.
     */
    private final SimpleObjectProperty<Path> sourceDirProperty = new SimpleObjectProperty<>();

    /**
     * Destination root path, as it should be defined in {@link SirsPreferences.PROPERTIES#DOCUMENT_ROOT}.
     */
    private final SimpleObjectProperty<Path> rootDirProperty = new SimpleObjectProperty<>();

    /**
     * A sub-directory of {@link #rootDirProperty} to put imported photos into.
     */
    private final SimpleObjectProperty<Path> subDirProperty = new SimpleObjectProperty<>();

    private final SimpleObjectProperty<CopyTask> copyTaskProperty = new SimpleObjectProperty<>();

    private final ObservableList<PropertyDescriptor> availablePrefixes = FXCollections.observableArrayList();

    public PhotoImportPane() {
        super();
        SIRS.loadFXML(this);

        final BooleanBinding noRootConfigured = rootDirProperty.isNull();
        uiChooseSubDir.disableProperty().bind(noRootConfigured);
        uiSubDirLabel.disableProperty().bind(noRootConfigured);

        sourceDirProperty.addListener(this::sourceChanged);
        rootDirProperty.addListener(this::destinationChanged);
        subDirProperty.addListener(this::destinationChanged);

        uiCopyMessage.managedProperty().bind(uiCopyMessage.visibleProperty());
        uiCopyMessage.visibleProperty().bind(uiCopyMessage.textProperty().isNotEmpty());

        // prefix composition UIs
        final ObservableList<Character> prefixSeparators = FXCollections.observableArrayList();
        prefixSeparators.addAll(' ', '.', '-', '_');
        SIRS.initCombo(uiSeparatorChoice, prefixSeparators, '.');

        uiPrefixListView.setItems(FXCollections.observableArrayList());
        uiPrefixListView.setCellFactory(param -> new PrefixCell());
        uiPrefixListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // Set button icons
        uiAddPrefixBtn.setGraphic(new ImageView(SIRS.ICON_ADD_BLACK));
        uiAddPrefixBtn.setText(null);
        uiMoveUpBtn.setGraphic(new ImageView(SIRS.ICON_CARET_UP_BLACK));
        uiMoveUpBtn.setText(null);
        uiMoveDownBtn.setGraphic(new ImageView(SIRS.ICON_CARET_DOWN_BLACK));
        uiMoveDownBtn.setText(null);
        uiDeletePrefixBtn.setGraphic(new ImageView(ICON_TRASH_BLACK));
        uiDeletePrefixBtn.setText(null);

        try {
            availablePrefixes.addAll(SIRS.listSimpleProperties(Photo.class).values());
        } catch (IntrospectionException ex) {
            SIRS.LOGGER.log(Level.WARNING, "Cannot identify available prefixes.", ex);
            uiPrefixTitledPane.setVisible(false);
            uiPrefixTitledPane.setManaged(false);
        }
    }

    private void sourceChanged(final ObservableValue<? extends Path> obs, final Path oldValue, final Path newValue) {
        if (newValue != null) {
            try {
                final FileStore fileStore = newValue.getFileSystem().provider().getFileStore(newValue);

                uiSourceName.setText(fileStore.name());
                uiSourceType.setText(fileStore.type());

                final long usableSpace = fileStore.getUsableSpace();
                final long totalSpace = fileStore.getTotalSpace();
                uiSourceUsableSpace.setText(SIRS.toReadableSize(usableSpace));
                uiSoureSpaceProgress.setProgress(totalSpace <= 0 || usableSpace <= 0? 0.99999 : 1 - ((double)usableSpace / totalSpace));
            } catch (IOException e) {
                GeotkFX.newExceptionDialog("L'analyse du media source a échoué. Impossible de définir le périphérique choisi comme source de l'import.", e);
                sourceDirProperty.set(null);
            }
        } else {
            uiSourceName.setText("N/A");
            uiSourceType.setText("N/A");
            uiSourceUsableSpace.setText("N/A");
            uiSoureSpaceProgress.setProgress(0);
        }
    }

    /**
     * Compute back destination usable space each time root or subdirectory change.
     * We do it for both elements, in case sub-directory is not on the same filestore.
     * Ex : root is /media, and sub-directory is myUsbKey/photos
     * @param obs
     * @param oldValue
     * @param newValue
     */
    private void destinationChanged(final ObservableValue<? extends Path> obs, final Path oldValue, final Path newValue) {
        if (rootDirProperty.get() == null) {
            uiRootLabel.setText("N/A");
            uiSubDirLabel.setText("N/A");
            uiDestSpaceProgress.setProgress(0);
        } else {
            uiRootLabel.setText(rootDirProperty.get().toString());
            final Path subDir = subDirProperty.get();
            final Path absolutePath;
            if (subDir == null) {
                uiSubDirLabel.setText("N/A");
                absolutePath = rootDirProperty.get();
            } else {
                uiSubDirLabel.setText(subDir.toString());
                absolutePath = rootDirProperty.get().resolve(subDir);
            }

            try {
                final FileStore fileStore = newValue.getFileSystem().provider().getFileStore(absolutePath);

                final long usableSpace = fileStore.getUsableSpace();
                final long totalSpace = fileStore.getTotalSpace();
                // HACK : Never set to 1 to avoid message print.
                uiDestSpaceProgress.setProgress(totalSpace <= 0 || usableSpace <= 0? 0.99999 : 1 - ((double)usableSpace / totalSpace));
            } catch (IOException e) {
                GeotkFX.newExceptionDialog("L'analyse du dossier destination a échoué. Veuillez choisir un autre dossier destination.", e);
            }
        }
    }

    /*
     * UI ACTIONS
     */

    @FXML
    void addPrefix(ActionEvent event) {
        ComboBox<PropertyDescriptor> choices = new ComboBox<>();
        SIRS.initCombo(choices, availablePrefixes, null);
        choices.setConverter(new DescriptorConverter());

        final Alert alert = new Alert(Alert.AlertType.CONFIRMATION, null, ButtonType.CANCEL, ButtonType.OK);
        alert.setResizable(true);
        alert.setWidth(400);
        alert.getDialogPane().setContent(choices);

        ButtonType result = alert.showAndWait().orElse(ButtonType.CANCEL);
        if (ButtonType.OK.equals(result)) {
            PropertyDescriptor value = choices.getValue();
            if (value != null) {
                uiPrefixListView.getItems().add(value);
                availablePrefixes.remove(value);
            }
        }
    }

    @FXML
    void deletePrefix(ActionEvent event) {
        final MultipleSelectionModel<PropertyDescriptor> selectionModel = uiPrefixListView.getSelectionModel();
        final ObservableList<PropertyDescriptor> selected = selectionModel.getSelectedItems();
        if (!selected.isEmpty()) {
            uiPrefixListView.getItems().removeAll(selected);
            availablePrefixes.addAll(selected);
        }
        selectionModel.clearSelection();
    }

    @FXML
    void movePrefixDown(ActionEvent event) {
        moveSelectedElements(uiPrefixListView, DIRECTION.DOWN);
    }

    @FXML
    void movePrefixUp(ActionEvent event) {
        moveSelectedElements(uiPrefixListView, DIRECTION.UP);
    }

    @FXML
    void chooseSource(ActionEvent event) {
        sourceDirProperty.set(MobilePlugin.chooseMedia(getScene().getWindow()));
    }

    @FXML
    void chooseSubDirectory(ActionEvent event) {
        final DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Répertoire destination");
        chooser.setInitialDirectory(rootDirProperty.get().toFile());
        File chosen = chooser.showDialog(getScene().getWindow());
        if (chosen != null) {
            subDirProperty.set(chosen.toPath());
        }
    }

    @FXML
    void configureRoot(ActionEvent event) {
        final DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choisir un répertoire racine : ");
        if (rootDirProperty.get() != null) {
            chooser.setInitialDirectory(rootDirProperty.get().toFile());
        }
        File chosen = chooser.showDialog(getScene().getWindow());
        if (chosen != null) {
            rootDirProperty.set(chosen.toPath().toAbsolutePath());
            SirsPreferences.INSTANCE.setProperty(SirsPreferences.PROPERTIES.DOCUMENT_ROOT.name(), rootDirProperty.get().toString());
        }
    }

    /**
     * Refresh UI bindings on copy task change.
     *
     * @param obs
     * @param oldTask
     * @param newTask
     */
    void copyTaskUpdate(final ObservableValue<? extends CopyTask> obs, CopyTask oldValue, CopyTask newValue) {
        if (oldValue != null) {
            uiCopyMessage.textProperty().unbind();
            copyMessageTooltip.textProperty().unbind();
            uiImportProgress.progressProperty().unbind();
        }
        if (newValue != null) {
            uiCopyMessage.textProperty().bind(newValue.messageProperty());
            copyMessageTooltip.textProperty().bind(newValue.messageProperty());
            uiImportProgress.progressProperty().bind(newValue.progressProperty());
        }
    }

    @FXML
    void importPhotos(ActionEvent event) {
        // If another task is already running, import button will have "cancel" button role.
        if (copyTaskProperty.get() != null) {
            copyTaskProperty.get().cancel();
            copyTaskProperty.set(null);
            return;
        }

        uiImportProgress.setProgress(-1);
        // Ensure source media is configured
        Path source = sourceDirProperty.get();
        if (source == null || !Files.isDirectory(source)) {
            warning("aucun périphérique d'entrée valide spécifié. Veuillez vérifiez vos paramètres d'import.");
            return;
        }

        source = source.resolve(PHOTO_FOLDER);
        if (!Files.isDirectory(source)) {
            warning("Aucune photo disponible pour import sur le périphérique mobile.");
            return;
        }

        // Check destination is configured
        final Path root = rootDirProperty.get();
        if (root == null || !Files.isDirectory(root)) {
            warning("aucun dossier de sortie valide spécifié. Veuillez vérifiez vos paramètres d'import.");
            return;
        }

        // Find all images in source media
        uiCopyMessage.setText("Recherche de fichiers images");
        final HashSet<Path> filesToCopy = new HashSet<>();
        try {
            Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (IMG_PATTERN.matcher(file.getFileName().toString()).matches()) {
                        filesToCopy.add(file);
                    }
                    return super.visitFile(file, attrs);
                }
            });
        } catch (IOException e) {
            GeotkFX.newExceptionDialog("Impossible de lister les photos dipoonibles sur le média source.", e).show();
            SirsCore.LOGGER.log(Level.WARNING, "Photo import : cannot list images on source media.", e);
            return;
        }

        // Build destination path
        final Path subDir = subDirProperty.get();
        final Path destination = subDir == null ? root : root.resolve(subDir);
        if (!Files.exists(destination)) {
            try {
                Files.createDirectories(destination);
            } catch (IOException ex) {
                warning("Il est impossible de créer le dossier de sortie. Veuillez vérifier vos paramètres ou droits d'accès système.");
                Logger.getLogger(PhotoImportPane.class.getName()).log(Level.WARNING, "Cannot create output directory for photo import.", ex);
                return;
            }
        } else if (!Files.isDirectory(destination)) {
            warning("Le chemin destination ne dénote pas un dossier. Impossible de procéder à l'import.");
            return;
        }

        final LinkedHashSet prefixes = new LinkedHashSet(uiPrefixListView.getItems());
        final PathResolver resolver = new PathResolver(subDir, prefixes, uiSeparatorChoice.getValue());
        /* We give root directory as destination. sub-directory will be managed by
         * the resolver, because we have to update CouchDB documents accordingly.
         */
        if (filesToCopy.isEmpty()) {
            warning("Aucune photo n'a été trouvée pour l'import.");
            return;
        }
        final CopyTask cpTask = new CopyTask(filesToCopy, root, resolver);
        copyTaskProperty.set(cpTask);

        uiImportBtn.setText("Annuler");
        TaskManager.INSTANCE.submit(cpTask);

        // When finished, we reset task and panel.
        cpTask.runningProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
            if (Boolean.FALSE.equals(newValue)) {
                Platform.runLater(() -> {
                    uiImportBtn.setText("Importer");
                    uiParameterContainer.setDisable(false);
                    copyTaskProperty.set(null);
                });
            }
        });
    }

    /**
     * Move elements selected in the given listview one step up or down.
     * @param source The list view to work with
     * @param direction Direction to move selected elements to (up or down).
     */
    static void moveSelectedElements(final ListView source, final DIRECTION direction) {
        ArgumentChecks.ensureNonNull("Input list", source);
        ArgumentChecks.ensureNonNull("Movement direction", direction);

        final boolean up = DIRECTION.UP.equals(direction);
        final ObservableList<PropertyDescriptor> items = source.getItems();
        final MultipleSelectionModel<PropertyDescriptor> selectionModel = source.getSelectionModel();
        // defensive copy
        final ArrayList<Integer> selected = new ArrayList<>(selectionModel.getSelectedIndices());

        final int[] newSelection = new int[selected.size()];
        int counter = 0;
        /* We won't move items while browsing selected indices, to avoid messing with iterator position.
         * We'll store movements to make here (in the order they must be performed), and execute them afterhand.
         */
        final LinkedHashMap<Integer, Integer> movements = new LinkedHashMap<>();

        final ListIterator<Integer> sIt;
        if (up) {
            sIt = selected.listIterator();
        } else {
            sIt = selected.listIterator(selected.size());
        }

        while (up? sIt.hasNext() : sIt.hasPrevious()) {
            final Integer index = up? sIt.next() : sIt.previous();
            // If element is on the edge of list, we don't move it.
            if (up? index <= 0 : index >= items.size() -1) continue;

            final Integer moveTo = up? index -1 : index + 1;
            // If next element is also selected, and it won't move, we cannot move this one neither.
            if (up)
                sIt.previous();
            else
                sIt.next();
            if (up? sIt.hasPrevious() : sIt.hasNext()) {
                final Integer nextSelected = up ? sIt.previous() : sIt.next();
                // rollback iterator position
                if (up)
                    sIt.next();
                else
                    sIt.previous();

                if (moveTo >= nextSelected && !movements.containsKey(nextSelected)) {
                    if (up)
                        sIt.next();
                    else
                        sIt.previous();
                    newSelection[counter++] = index;
                    continue;
                }
            }
            if (up)
                sIt.next();
            else
                sIt.previous();

            movements.put(index, moveTo);
            newSelection[counter++] = moveTo;
        }

        final Iterator<Map.Entry<Integer, Integer>> movIt = movements.entrySet().iterator();
        // move elements
        while (movIt.hasNext()) {
            final Map.Entry<Integer, Integer> movement = movIt.next();
            items.add(movement.getValue(), items.remove((int)movement.getKey()));
        }

        // update selection to keep same objects selected.
        selectionModel.clearSelection();
        selectionModel.selectIndices(-1, newSelection);
    }

    /*
     * UTILITIES
     */

    /**
     * Display a warning dialog to user
     * TODO : replace with growls.
     * @param alertMessage
     */
    private static void warning(final String alertMessage) {
        final Alert alert = new Alert(Alert.AlertType.WARNING, alertMessage, ButtonType.OK);
        alert.setWidth(400);
        alert.setHeight(300);
        alert.setResizable(true);
        alert.show();
    }

    /**
     * A path resolver for {@link CopyTask} Responsible for photo import. It is
     * in charge of photo rename (prefix additions). It also update couchDB documents
     * to change the saved path in them.
     */
    static class PathResolver implements Function<Path, Path> {

        @Autowired
        private Session session;

        private final Path rootRelativeDir;
        private final LinkedHashSet<PropertyDescriptor> prefixes;
        private final char separator;

        private final boolean noOp;

        public PathResolver(Path rootRelativeDir, LinkedHashSet<PropertyDescriptor> prefixes, Character separator) {
            this.rootRelativeDir = rootRelativeDir;
            this.prefixes = prefixes;
            noOp = rootRelativeDir == null && (prefixes == null || prefixes.isEmpty());

            this.separator = separator == null? '.' : separator;
        }

        @Override
        public Path apply(Path t) {
            // If no sub-directory or renaming rule is given, no CouchDB update is required, we just return file name.
            if (noOp)
                return t.getFileName();

            final Matcher matcher = IMG_PATTERN.matcher(t.getFileName().toString());
            if (!matcher.matches()) {
                throw new IllegalArgumentException("A file which does not match image name convention is attempted to be imported !");
            }

            Optional<? extends Element> opt = session.getElement(matcher.group(1));
            if (!opt.isPresent()) {
                throw new IllegalStateException("No valid document can be found for input image.");
            }

            final Element e = opt.get();
            Photo photo = null;
            if (e instanceof Photo) {
                photo = (Photo) e;
            } else {
                // ID does not point on the photo. We'll have to retrieve it by analysing input element, which should contain it.
                final HashSet<AvecPhotos> photoContainers = new HashSet();
                if (e instanceof AvecPhotos) {
                    photoContainers.add((AvecPhotos) e);
                } else if (e instanceof Desordre) {
                    photoContainers.addAll(((Desordre) e).observations);
                }

                scan:
                for (final AvecPhotos obs : photoContainers) {
                    for (final Object o : obs.getPhotos()) {
                        final Photo p = (Photo) o;
                        if (p.getChemin().equals(t.getFileName().toString()) || p.getChemin().equals(t.toString())) {
                            photo = p;
                            break scan;
                        }
                    }
                }
            }

            if (photo == null) {
                throw new IllegalStateException("No valid document can be found for input image.");
            }

            final String newName;
            if (prefixes == null || prefixes.isEmpty()) {
                newName = t.getFileName().toString();
            } else {
                final StringBuilder nameBuilder = new StringBuilder();
                for (final PropertyDescriptor desc : prefixes) {
                    Method readMethod = desc.getReadMethod();
                    if (readMethod != null) {
                        try {
                            nameBuilder.append(readMethod.invoke(photo)).append(separator);
                        } catch (Exception ex) {
                            warning("Le préfixe "+LabelMapper.mapPropertyName(Photo.class, desc.getName())+" ne peut être ajouté. L'import est annulé.");
                            SirsCore.LOGGER.log(Level.WARNING, "Photo import : cannot add following prefix in file name : "+desc.getDisplayName(), ex);
                        }
                    }
                }
                nameBuilder.append(t.getFileName().toString());
                newName = nameBuilder.toString();
            }

            final Path result;
            if (rootRelativeDir == null) {
                result = Paths.get(newName);
            } else {
                result = rootRelativeDir.resolve(newName);
            }

            // Database update
            photo.setChemin(result.toString());
            final Element couchDbDocument = photo.getCouchDBDocument();
            session.getRepositoryForClass((Class<Element>)couchDbDocument.getClass()).update(couchDbDocument);

            return result;
        }
    }

    /**
     * A cell displaying proper title for a given property descriptor.
     */
    private static class PrefixCell extends ListCell<PropertyDescriptor> {

        final LabelMapper mapper = LabelMapper.get(Photo.class);

        @Override
        protected void updateItem(PropertyDescriptor item, boolean empty) {
            super.updateItem(item, empty);
            if (item == null || empty) {
                setText("");
            } else {
                setText(mapper.mapPropertyName(item.getName()));
            }
        }
    }

    /**
     * A converter displaying proper title for a given property descriptor.
     */
    private static class DescriptorConverter extends StringConverter<PropertyDescriptor> {

        final WeakHashMap<String, PropertyDescriptor> fromString = new WeakHashMap<>();

        final LabelMapper mapper = LabelMapper.get(Photo.class);

        @Override
        public String toString(PropertyDescriptor object) {
            if (object == null) return "";
            final String pName = mapper.mapPropertyName(object.getName());
            fromString.put(pName, object);
            return pName;
        }

        @Override
        public PropertyDescriptor fromString(String string) {
            return fromString.get(string);
        }

    }
}
