package fr.sirs.plugins.mobile;

import fr.sirs.Injector;
import fr.sirs.SIRS;
import fr.sirs.Session;
import fr.sirs.core.SirsCore;
import fr.sirs.core.TronconUtils;
import fr.sirs.core.component.AbstractSIRSRepository;
import fr.sirs.core.model.AbstractPositionDocument;
import fr.sirs.core.model.AbstractPositionDocumentAssociable;
import fr.sirs.core.model.AvecBornesTemporelles;
import fr.sirs.core.model.LabelMapper;
import fr.sirs.core.model.LevePositionProfilTravers;
import fr.sirs.core.model.PositionProfilTravers;
import fr.sirs.core.model.Preview;
import fr.sirs.core.model.ProfilLong;
import fr.sirs.core.model.SIRSFileReference;
import fr.sirs.core.model.SIRSReference;
import fr.sirs.core.model.TronconDigue;
import fr.sirs.util.SirsStringConverter;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.scene.layout.BorderPane;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SelectionMode;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.util.ArgumentChecks;
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
    private ListView<Preview> uiTronconList;

    @FXML
    private DatePicker uiDate;

    @FXML
    private ComboBox<Class> uiDocumentType;

    @FXML
    private Button uiExportBtn;

    @FXML
    private ChoiceBox<Path> uiOutputChoice;

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

    @Autowired
    private Session session;

    private final SimpleLongProperty outputSize = new SimpleLongProperty();
    private final SimpleObjectProperty<Path> mobileDocumentDir = new SimpleObjectProperty<>();
    private final SimpleObjectProperty<CopyTask> copyTaskProperty = new SimpleObjectProperty<>();

    private final ObservableMap<Class, AbstractSIRSRepository<SIRSFileReference>> repositories = FXCollections.observableHashMap();

    public DocumentExportPane() {
        super();
        SIRS.loadFXML(this);
        Injector.injectDependencies(this);

        // Prevent actions when a copy or a loading is running.
        //uiConfigPane.disableProperty().bind(uiCopyPane.visibleProperty().or(uiLoadingPane.visibleProperty()));

        uiDocumentType.setConverter(new ClassNameConverter());

        uiTronconList.setItems(FXCollections.observableList(session.getPreviews().getByClass(TronconDigue.class)));
        uiTronconList.setCellFactory((previews) -> new TextCell());
        uiTronconList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        uiDesktopList.setCellFactory(list -> new TextCell());
        uiDesktopList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        uiMobileList.setCellFactory(list -> new TextCell());
        uiMobileList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        initRepositories();
        final ObservableList<Class> availableTypes = FXCollections.observableArrayList(repositories.keySet());
        availableTypes.add(0, SIRSFileReference.class);
        uiDocumentType.setItems(availableTypes);

        uiTronconList.getSelectionModel().getSelectedItems().addListener((ListChangeListener.Change<? extends Preview> c) -> {
            updateDocumentList();
        });
        uiDocumentType.valueProperty().addListener((ObservableValue<? extends Object> observable, Object oldValue, Object newValue) -> {
            updateDocumentList();
        });
        uiDate.valueProperty().addListener((ObservableValue<? extends Object> observable, Object oldValue, Object newValue) -> {
            updateDocumentList();
        });

        uiDesktopToMobile.setText(null);
        uiDesktopToMobile.setGraphic(new ImageView(SIRS.ICON_ARROW_RIGHT_BLACK));
        uiDelete.setText(null);
        uiDelete.setGraphic(new ImageView(GeotkFX.ICON_DELETE));

        mobileDocumentDir.addListener(this::updateAvailableSpace);

        // DEBUG PURPOSE
        mobileDocumentDir.set(Paths.get("/home/geomatys/temp"));
    }

    /**
     * Initialize map of available document providers from Spring context.
     */
    private final void initRepositories() {
        repositories.clear();
        final Collection<AbstractSIRSRepository> registeredRepositories = session.getRepositoriesForClass(SIRSFileReference.class);
        for (final AbstractSIRSRepository repo : registeredRepositories) {
            repositories.put(repo.getModelClass(), repo);
        }
    }

    /**
     * Build list of available documents in desktop application when a filter
     * change.
     */
    private void updateDocumentList() {
        uiLoadingPane.setVisible(true);

        final Task updater = TaskManager.INSTANCE.submit("Recherche de documents", () -> {
            final Class docClass = uiDocumentType.getValue() == null ? SIRSFileReference.class : uiDocumentType.getValue();
            final List<TronconDigue> selectedTroncons = session.getRepositoryForClass(TronconDigue.class).get(
                    uiTronconList.getSelectionModel().getSelectedItems().stream().map(p -> p.getElementId()).collect(Collectors.toList()));

            // Use an hash set to avoid doublons.
            final HashSet<SIRSFileReference> items = new HashSet();

            // A filter which check that a given document really points on a file reference
            final Predicate fileFilter = element -> {
                if (element instanceof SIRSFileReference) {
                    String chemin = ((SIRSFileReference)element).getChemin();
                    if (chemin != null && !chemin.isEmpty()) {
                        return Files.isRegularFile(SIRS.getDocumentAbsolutePath(chemin));
                    }
                }
                return false;
            };

            // Prepare temporal filter
            final LocalDate date = uiDate.getValue();
            final Long dateMilli = date == null ? null : Timestamp.valueOf(date.atStartOfDay()).getTime();
            final Predicate temporalFilter = dateMilli == null ? null : e -> {
                if (e instanceof AvecBornesTemporelles) {
                    AvecBornesTemporelles temp = (AvecBornesTemporelles) e;
                    final Long start = temp.getDate_debut() == null ? Long.MIN_VALUE : Timestamp.valueOf(temp.getDate_debut().atStartOfDay()).getTime();
                    final Long end = temp.getDate_fin() == null ? Long.MIN_VALUE : Timestamp.valueOf(temp.getDate_fin().atStartOfDay()).getTime();
                    final NumberRange range = new NumberRange(Long.class, start, true, end, true);
                    return range.contains(dateMilli);
                } else {
                    return false;
                }
            };

            /*
            * We will browse each troncon to get back all of its documents of queried
            * type and at the selected date.
            */
            for (final TronconDigue troncon : selectedTroncons) {
                // First, we test if the current troncon is in the wanted time period.
                if (temporalFilter != null && !temporalFilter.test(troncon)) {
                    continue;
                }

                final List<Preview> docPreviews = getDocumentPreviews(troncon);

                // utility container whose key is a type of elements, and value is the id of all elements found for this type.
                final HashMap<String, HashSet<String>> docs = new HashMap();
                for (final Preview docPreview : docPreviews) {
                    // If a document type filter is set, we filter directly on previews to avoid unnecessary queries.
                    if (SIRSFileReference.class.equals(docClass) || docClass.getCanonicalName().equals(docPreview.getElementClass())) {
                        HashSet<String> ids = docs.get(docPreview.getElementClass());
                        if (ids == null) {
                            ids = new HashSet();
                            docs.put(docPreview.getElementClass(), ids);
                        }
                        ids.add(docPreview.getElementId());
                    }
                }

                for (final Map.Entry<String, HashSet<String>> docGroup : docs.entrySet()) {
                    final AbstractSIRSRepository repo = session.getRepositoryForType(docGroup.getKey());
                    if (repo == null)
                        continue;
                    final List elements = repo.get(docGroup.getValue().toArray(new String[0]));
                    if (temporalFilter != null) {
                        items.addAll((Collection) elements.stream().filter(temporalFilter).filter(fileFilter).collect(Collectors.toList()));
                    } else {
                        items.addAll((Collection) elements.stream().filter(fileFilter).collect(Collectors.toList()));
                    }
                }
            }

            TaskManager.MockTask uiUpdate = new TaskManager.MockTask(() -> uiDesktopList.setItems(FXCollections.observableArrayList(items)));
            Platform.runLater(uiUpdate);
            uiUpdate.get();
            return true;
        });

        updater.runningProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
            if (Boolean.FALSE.equals(newValue)) {
                final Runnable r = () -> uiLoadingPane.setVisible(false);
                if (Platform.isFxApplicationThread()) {
                    r.run();
                } else {
                    Platform.runLater(r);
                }
            }
        });
    }

    /**
     * Analyse all {@link AbstractPositionDocument} placed on input {@link TronconDigue}
     * to retrieve associated {@link SIRSFileReference}. The aim is to perform a minimum amount
     * of queries to avoid IO/CPU overhead.
     *
     * There's 3 cases here :
     * - {@link AbstractPositionDocumentAssociable} : document position contains
     * an id of another document which is the wanted {@link SIRSFileReference}
     * - {@link AbstractPositionDocument} which are {@link SIRSFileReference} themselves
     * (Ex: {@link ProfilLong}.
     * - {@link PositionProfilTravers}, which contains a link to multiple {@link LevePositionProfilTravers},
     * each of them being associatded to one wanted {@link SIRSFileReference}
     *
     * @param troncon The linear we have to find documents for.
     * @return a preview of each document found.
     */
    private List<Preview> getDocumentPreviews(final TronconDigue troncon) {
        final HashSet<String> docIds = new HashSet<>();
        // Store ids found for leves, to perform a single query to retrieve all.
        final HashSet<String> levePositionIds = new HashSet<>();
        final List<AbstractPositionDocument> docPositions = TronconUtils.getPositionDocumentList(troncon.getId());
        for (final AbstractPositionDocument docPosition : docPositions) {
            if (docPosition instanceof SIRSFileReference) {
                docIds.add(docPosition.getId());
            } else if (docPosition instanceof AbstractPositionDocumentAssociable) {
                final String docId = ((AbstractPositionDocumentAssociable)docPosition).getSirsdocument();
                if (docId != null && !docId.isEmpty()) {
                    docIds.add(docId);
                }
            } else if (docPosition instanceof PositionProfilTravers) {
                levePositionIds.addAll(((PositionProfilTravers)docPosition).getLevePositionIds());
            }
        }

        if (!levePositionIds.isEmpty()) {
            AbstractSIRSRepository<LevePositionProfilTravers> repo = session.getRepositoryForClass(LevePositionProfilTravers.class);
            docIds.addAll(repo.get(levePositionIds.toArray(new String[0])).stream().map(tmpLeve -> tmpLeve.getLeveId()).collect(Collectors.toSet()));
        }

        return session.getPreviews().get(docIds.toArray(new String[0]));
    }

    /**
     * Try to retrieve available space on chosen mobile folder.
     *
     * @param obs source observable value
     * @param oldValue previous selected folder
     * @param newValue currently selected folder
     */
    void updateAvailableSpace(final ObservableValue<? extends Path> obs, Path oldValue, Path newValue) {
        if (newValue == null) {
            uiRemainingSpace.setText("inconnu");
        } else {
            try {
                final long usableSpace = Files.getFileStore(newValue).getUsableSpace();
                outputSize.set(usableSpace);
                uiRemainingSpace.setText(toReadableSize(usableSpace));
            } catch (IOException ex) {
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
    void updateTask(final ObservableValue<? extends Task> obs, final Task oldTask, final Task newTask) {
        if (newTask != null) {
            uiCopyPane.visibleProperty().bind(newTask.runningProperty());
            uiCopyTitle.textProperty().bind(newTask.titleProperty());
            uiCopyMessage.textProperty().bind(newTask.messageProperty());

            uiCopyProgress.progressProperty().bind(newTask.progressProperty());
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
        uiDesktopList.getItems().removeAll(selectedItems);
    }

    @FXML
    void deleteFromMobile(ActionEvent event) {
        ObservableList<SIRSFileReference> selectedItems = uiMobileList.getSelectionModel().getSelectedItems();
        if (selectedItems.isEmpty())
            return;

        uiMobileList.getItems().removeAll(selectedItems);
    }

    @FXML
    void cancelTask(ActionEvent event) {
        final Task tmp = copyTaskProperty.get();
        if (tmp != null)
            tmp.cancel();
    }

    @FXML
    void exportToMobile(ActionEvent event) {
        final Path destination = mobileDocumentDir.get();
        if (destination == null || !Files.isDirectory(destination)) {
            new Alert(Alert.AlertType.WARNING, "Impossible de déterminer le répertoire de sortie.", ButtonType.OK).show();
            return;
        }

        final ArrayList<Path> toCopy = new ArrayList<>();
        long sizeToCopy = 0;
        try {
            for (final SIRSFileReference ref : uiMobileList.getItems()) {
                final String chemin = ref.getChemin();
                if (chemin == null || chemin.isEmpty())
                    continue;
                final Path doc = SIRS.getDocumentAbsolutePath(chemin);
                if (Files.isRegularFile(doc)) {
                    toCopy.add(doc);
                    sizeToCopy += Files.getFileAttributeView(doc, BasicFileAttributeView.class).readAttributes().size();
                }
            }
        } catch (Exception e) {
            SirsCore.LOGGER.log(Level.WARNING, "Impossible to compute file list for copy.", e);
            GeotkFX.newExceptionDialog("Une erreur est survenue pendant l'analyse des fichiers à copier.", e).show();
            return;
        }

        if (sizeToCopy <= 0) {
            new Alert(Alert.AlertType.INFORMATION, "Aucune donnée à copier.", ButtonType.OK).show();
        } else if (outputSize.get() < sizeToCopy) {
            new Alert(Alert.AlertType.ERROR, "Espace insuffisant sur le media de sortie.", ButtonType.OK).show();
        } else {
            final String sourceReadable = toReadableSize(sizeToCopy);
            final String outputName = mobileDocumentDir.get().toString();
            final String outputReadable = uiRemainingSpace.getText();
            ButtonType choice = new Alert(Alert.AlertType.CONFIRMATION,
                    "Vous allez copier " + sourceReadable + " sur " + outputName + " (Espace restant : " + outputReadable + ").\nÊtes-vous sûr ?",
                    ButtonType.NO, ButtonType.YES)
                    .showAndWait().orElse(ButtonType.NO);

            if (ButtonType.YES.equals(choice)) {
                final CopyTask copyTask = new CopyTask(toCopy, destination);
                copyTaskProperty.set(copyTask);
                TaskManager.INSTANCE.submit(copyTask);
            }
        }
    }

    public static String toReadableSize(final long byteNumber) {
        NumberFormat format = NumberFormat.getNumberInstance();
        format.setMaximumFractionDigits(2);
        if (byteNumber < 0) {
            return "inconnu";
        } else if (byteNumber < 1e3) {
            return "" + byteNumber + " octets";
        } else if (byteNumber < 1e6) {
            return "" + format.format(byteNumber / 1e3) + " ko";
        } else if (byteNumber < 1e9) {
            return "" + format.format(byteNumber / 1e6) + " Mo";
        } else if (byteNumber < 1e12) {
            return "" + format.format(byteNumber / 1e9) + " Go";
        } else {
            return "" + (byteNumber / 1e12) + " To";
        }
    }

    /**
     * Cell to display label of input element.
     */
    private static class TextCell extends ListCell {

        final SirsStringConverter strConverter = new SirsStringConverter();

        @Override
        protected void updateItem(Object item, boolean empty) {
            super.updateItem(item, empty);
            if (item == null || isEmpty()) {
                setText(null);
            } else {
                setText(strConverter.toString(item));
            }
        }
    }

    /**
     * Give a proper title for a given class name.
     */
    private static class ClassNameConverter extends StringConverter<Class> {

        static final String ALL_DOCS = "Tous";

        @Override
        public String toString(Class object) {
            if (object == null)
                return "";
            else if (SIRSFileReference.class.equals(object))
                return ALL_DOCS;
            else
                try {
                    LabelMapper mapper = LabelMapper.get(object);
                    return mapper.mapClassName();
                } catch (MissingResourceException e) {
                    return object.getSimpleName();
                }
        }

        @Override
        public Class fromString(String string) {
            if (ALL_DOCS.equalsIgnoreCase(string))
                return SIRSReference.class;
            else
                return null;
        }
    }

    /**
     * A task which role is to copy all files under application root path to the
     * given directory. If target directory does not exists, it will be created.
     * If a conflict is detected dduring the operation, user is asked what to do
     * (replace, ignore, cancel).
     */
    private static class CopyTask extends Task<Boolean> {

        private final List<Path> toCopy;
        private final Path destination;

        CopyTask(final List<Path> toCopy, final Path destination) {
            ArgumentChecks.ensureNonNull("Files to copy", toCopy);
            ArgumentChecks.ensureNonNull("Destination", destination);
            if (toCopy.isEmpty()) {
                throw new IllegalArgumentException("No file to copy");
            }

            if (Files.isRegularFile(destination)) {
                throw new IllegalArgumentException("Destination path is not a directory !");
            }
            this.toCopy = toCopy;
            this.destination = destination;
        }

        @Override
        protected Boolean call() throws Exception {
            updateTitle("Copie vers " + destination.toString());

            boolean replaceAll = false;
            boolean ignoreAll = false;

            Path srcRoot = SIRS.getDocumentRootPath();
            final Thread currentThread = Thread.currentThread();
            for (final Path p : toCopy) {
                if (currentThread.isInterrupted() || isCancelled()) {
                    return false;
                }
                Path target = destination.resolve(srcRoot.relativize(p));

                updateProgress(0, toCopy.size());
                updateMessage("Copie de " + p.toString() + " vers " + target.toString());

                if (!Files.isDirectory(target.getParent())) {
                    Files.createDirectories(target.getParent());
                }

                try {
                    if (replaceAll) {
                        Files.copy(p, target, StandardCopyOption.REPLACE_EXISTING);
                    } else {
                        Files.copy(p, target);
                    }
                } catch (FileAlreadyExistsException e) {
                    if (ignoreAll) {
                        continue;
                    }

                    /*
                     * If we cannot copy file because it already exists, we ask
                     * user if we must replace or ignore file, or just stop here.
                     * We also propose user to repeat the same operation for all
                     * future conflicts.
                     */
                    ButtonType replace = new ButtonType("Remplacer");
                    ButtonType ignore = new ButtonType("Ignorer");
                    final StringBuilder strBuilder = new StringBuilder("Impossible de copier \n")
                            .append('\t').append(p.toString()).append('\n')
                            .append("vers")
                            .append('\t').append(target.toString()).append('\n')
                            .append("Le fichier existe déjà. Voulez-vous le remplacer ?");

                    BasicFileAttributes srcAttr = Files.getFileAttributeView(p, BasicFileAttributeView.class).readAttributes();
                    BasicFileAttributes dstAttr = Files.getFileAttributeView(target, BasicFileAttributeView.class).readAttributes();

                    final Label header = new Label("Impossible de copier un fichier car il existe déjà dans le dossier destination.\nVoulez-vous le remplacer ?");
                    // source file information
                    final GridPane srcInfo = new GridPane();
                    srcInfo.add(new Label(p.toString(), new ImageView(SIRS.ICON_FILE_BLACK)), 0, 0, 2, 1);
                    srcInfo.add(new Label("Taille du fichier : "), 0, 1);
                    srcInfo.add(new Label(toReadableSize(srcAttr.size())), 1, 1);
                    srcInfo.add(new Label("Dernière modification : "), 0, 2);
                    srcInfo.add(new Label(Timestamp.from(srcAttr.lastModifiedTime().toInstant()).toLocalDateTime().toString()), 1, 2);

                    // destination file information
                    final GridPane dstInfo = new GridPane();
                    dstInfo.add(new Label(target.toString(), new ImageView(SIRS.ICON_FILE_BLACK)), 0, 0, 2, 1);
                    dstInfo.add(new Label("Taille du fichier : "), 0, 1);
                    dstInfo.add(new Label(toReadableSize(dstAttr.size())), 1, 1);
                    dstInfo.add(new Label("Dernière modification : "), 0, 2);
                    dstInfo.add(new Label(Timestamp.from(dstAttr.lastModifiedTime().toInstant()).toLocalDateTime().toString()), 1, 2);

                    final CheckBox repeat = new CheckBox("Appliquer ce choix pour les futurs conflits");

                    final BorderPane msgDisplay = new BorderPane(new Label("vers"), header, dstInfo, repeat, srcInfo);

                    final Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Un conflit a été détecté", ButtonType.CANCEL, ignore, replace);
                    alert.getDialogPane().setContent(new VBox(10, msgDisplay, repeat));

                    final Task<ButtonType> askUser = new TaskManager.MockTask(() -> alert.showAndWait().orElse(ButtonType.CANCEL));
                    Platform.runLater(askUser);
                    final ButtonType result = askUser.get();
                    if (ButtonType.CANCEL.equals(result)) {
                        throw e;
                    } else if (replace.equals(result)) {
                        Files.copy(p, target, StandardCopyOption.REPLACE_EXISTING);
                        if (repeat.isSelected()) {
                            replaceAll = true;
                        }
                    } else {
                        if (repeat.isSelected()) {
                            ignoreAll = true;
                        }
                    }
                }
            }
            return true;
        }
    }
}
