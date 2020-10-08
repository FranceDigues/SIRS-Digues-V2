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

import fr.sirs.Injector;
import fr.sirs.SIRS;
import fr.sirs.Session;
import fr.sirs.core.SirsCore;
import fr.sirs.core.model.AbstractPhoto;
import fr.sirs.core.model.AvecPhotos;
import fr.sirs.core.model.Desordre;
import fr.sirs.core.model.Element;
import fr.sirs.core.model.LabelMapper;
import fr.sirs.core.model.OuvrageHydrauliqueAssocie;
import fr.sirs.core.model.ReseauHydrauliqueCielOuvert;
import fr.sirs.core.model.ReseauHydrauliqueFerme;
import fr.sirs.core.model.SIRSFileReference;
import fr.sirs.core.model.StationPompage;
import fr.sirs.plugins.synchro.SynchroPlugin;
import fr.sirs.plugins.synchro.ui.PhotoDestination;
import fr.sirs.plugins.synchro.ui.PrefixComposer;
import fr.sirs.util.CopyTask;
import java.awt.Color;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
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
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.apache.sis.util.ArgumentChecks;
import org.geotoolkit.font.FontAwesomeIcons;
import org.geotoolkit.font.IconBuilder;
import org.geotoolkit.gui.javafx.util.TaskManager;
import org.geotoolkit.internal.GeotkFX;

/**
 *
 * @author Alexis Manin (Geomatys)
 */
public class PhotoImportPane extends StackPane {

    /**
     * Expected structure for the name of images to import. It should start with
     * the related document id, followed by any suffix judged convinient by mobile
     * application to differentiate different images owned by the same document.
     * We also ensure that file extension matches one famous image format.
     */
    private static final Pattern IMG_PATTERN = Pattern.compile("(?i)^(\\w{32}|[\\w-]{36})(.*)(\\.(jpe?g|png|bmp|tiff?))$");

    public static final Image ICON_TRASH_BLACK = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_TRASH_O,16,Color.BLACK),null);

    public static enum DIRECTION {
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
    private ComboBox<Character> uiSeparatorChoice;

    @FXML
    private ProgressBar uiImportProgress;

    @FXML
    private Button uiImportBtn;

    @FXML
    private Label uiCopyMessage;

    private final Tooltip copyMessageTooltip = new Tooltip();

    /**
     * Source directory in which we'll find photos to transfer.
     */
    private final SimpleObjectProperty<Path> sourceDirProperty = new SimpleObjectProperty<>();

    private final SimpleObjectProperty<Task> taskProperty = new SimpleObjectProperty<>();

    private final PhotoDestination destPane;
    private final PrefixComposer prefixPane;

    public PhotoImportPane() {
        super();
        SIRS.loadFXML(this);

        sourceDirProperty.addListener(this::sourceChanged);
        taskProperty.addListener(this::taskUpdate);

        uiCopyMessage.managedProperty().bind(uiCopyMessage.visibleProperty());
        uiCopyMessage.visibleProperty().bind(uiCopyMessage.textProperty().isNotEmpty());

        uiImportProgress.visibleProperty().bind(taskProperty.isNotNull());

        destPane = new PhotoDestination(Injector.getSession());
        prefixPane = new PrefixComposer();
        uiParameterContainer.getChildren().addAll(destPane, prefixPane);
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

    @FXML
    void chooseSource(ActionEvent event) {
        sourceDirProperty.set(SynchroPlugin.chooseMedia(getScene().getWindow()));
    }

    /**
     * Refresh UI bindings on copy task change.
     *
     * @param obs
     * @param oldTask
     * @param newTask
     */
    void taskUpdate(final ObservableValue<? extends Task> obs, Task oldValue, Task newValue) {
        if (oldValue != null) {
            uiCopyMessage.textProperty().unbind();
            copyMessageTooltip.textProperty().unbind();
            uiImportProgress.progressProperty().unbind();
        }
        if (newValue != null) {
            uiCopyMessage.textProperty().bind(newValue.messageProperty());
            copyMessageTooltip.textProperty().bind(newValue.messageProperty());
            uiImportProgress.progressProperty().bind(newValue.progressProperty());
        } else {
            uiCopyMessage.setText(null);
            copyMessageTooltip.setText(null);
        }
    }

    @FXML
    void importPhotos(ActionEvent event) {
        // If another task is already running, import button will have "cancel" button role.
        if (taskProperty.get() != null) {
            taskProperty.get().cancel();
            taskProperty.set(null);
            return;
        }

        uiImportProgress.setProgress(-1);
        // Ensure source media is configured
        Path tmpSource = sourceDirProperty.get();
        if (tmpSource == null || !Files.isDirectory(tmpSource)) {
            warning("aucun périphérique d'entrée valide spécifié. Veuillez vérifiez vos paramètres d'import.");
            return;
        }

        tmpSource = tmpSource.resolve(SynchroPlugin.PHOTO_FOLDER);
        if (!Files.isDirectory(tmpSource)) {
            warning("Aucune photo disponible pour import sur le périphérique mobile.");
            return;
        }

        // Check destination is configured
        final Path source = tmpSource;
        final Path root = destPane.getRoot();
        if (root == null || !Files.isDirectory(root)) {
            warning("aucun dossier de sortie valide spécifié. Veuillez vérifiez vos paramètres d'import.");
            return;
        }

        // Find all images in source media
        final Task<HashSet<Path>> listTask = TaskManager.INSTANCE.submit("Recherche de fichiers images", () -> {
            final HashSet<Path> filesToCopy = new HashSet<>();
            Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (IMG_PATTERN.matcher(file.getFileName().toString()).matches()) {
                        filesToCopy.add(file);
                    }
                    return super.visitFile(file, attrs);
                }
            });
            return filesToCopy;
        });

        taskProperty.set(listTask);
        listTask.setOnFailed(listEvent -> Platform.runLater(() -> {
            GeotkFX.newExceptionDialog("Impossible de lister les photos dipoonibles sur le média source.", listTask.getException()).show();
            SirsCore.LOGGER.log(Level.WARNING, "Photo import : cannot list images on source media.", listTask.getException());
            taskProperty.set(null);
        }));

        listTask.setOnSucceeded(listEvent -> Platform.runLater(() -> {
            final HashSet<Path> files = listTask.getValue();
            if (files != null && !files.isEmpty()) {
                copyPhotos(files, root);
            } else {
                taskProperty.set(null);
                warning("Aucune nouvelle photo n'a été trouvée pour l'import.");
            }
        }));
    }

    private void copyPhotos(final Collection<Path> filesToCopy, final Path root) {
        // Build destination path
        final Path subDir = destPane.getSubDir();
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

        /* We give root directory as destination. sub-directory will be managed by
         * the resolver, because we have to update CouchDB documents accordingly.
         */
        final PathResolver resolver = new PathResolver(subDir, prefixPane.getPrefixBuilder().get(), uiSeparatorChoice.getValue());
        final CopyTask cpTask = new CopyTask(filesToCopy, root, resolver);
        taskProperty.set(cpTask);

        uiImportBtn.setText("Annuler");
        TaskManager.INSTANCE.submit(cpTask);

        // When finished, we reset task and panel.
        cpTask.runningProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
            if (Boolean.FALSE.equals(newValue)) {
                Platform.runLater(() -> {
                    uiImportBtn.setText("Importer");
                    uiParameterContainer.setDisable(false);
                    taskProperty.set(null);
                });
            }
        });
    }

    /**
     * Move elements selected in the given listview one step up or down.
     * @param source The list view to work with
     * @param direction Direction to move selected elements to (up or down).
     */
    public static void moveSelectedElements(final ListView source, final DIRECTION direction) {
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

        private final Session session;

        private final Path rootRelativeDir;
        private final Function<SIRSFileReference, String> prefixBuilder;
        private final char separator;

        private final boolean noOp;

        public PathResolver(Path rootRelativeDir, final Function<SIRSFileReference, String> prefixBuilder, Character separator) {
            session = Injector.getSession();
            this.rootRelativeDir = rootRelativeDir;
            this.prefixBuilder = prefixBuilder;
            noOp = rootRelativeDir == null && prefixBuilder == null;

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
            AbstractPhoto photo = null;
            if (e instanceof AbstractPhoto) {
                photo = (AbstractPhoto) e;
            } else {
                // ID does not point on the photo. We'll have to retrieve it by analysing input element, which should contain it.
                final HashSet<AvecPhotos> photoContainers = new HashSet();
                if (e instanceof AvecPhotos) {
                    photoContainers.add((AvecPhotos) e);
                } else if (e instanceof Desordre) {
                    photoContainers.addAll(((Desordre) e).observations);
                } else if (e instanceof OuvrageHydrauliqueAssocie) {
                    photoContainers.addAll(((OuvrageHydrauliqueAssocie) e).observations);
                } else if (e instanceof ReseauHydrauliqueFerme) {
                    photoContainers.addAll(((ReseauHydrauliqueFerme) e).observations);
                } else if (e instanceof ReseauHydrauliqueCielOuvert) {
                    photoContainers.addAll(((ReseauHydrauliqueCielOuvert) e).observations);
                } else if (e instanceof  StationPompage) {
                    photoContainers.addAll(((StationPompage) e).observations);
                }

                scan:
                for (final AvecPhotos<AbstractPhoto> obs : photoContainers) {
                    for (final AbstractPhoto o : obs.getPhotos()) {
                        final AbstractPhoto p = (AbstractPhoto) o;
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
            if (prefixBuilder == null) {
                newName = t.getFileName().toString();
            } else {
                final String prefix = prefixBuilder.apply(photo);
                if (prefix == null) {
                    newName = t.getFileName().toString();
                } else {
                    newName = prefix.concat(t.getFileName().toString());
                }
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
    public static class PrefixCell extends ListCell<PropertyDescriptor> {

        final LabelMapper mapper = LabelMapper.get(AbstractPhoto.class);

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
    public static class DescriptorConverter extends StringConverter<PropertyDescriptor> {

        final WeakHashMap<String, PropertyDescriptor> fromString = new WeakHashMap<>();

        final LabelMapper mapper = LabelMapper.get(AbstractPhoto.class);

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
