package fr.sirs.plugins.synchro.ui.database;

import fr.sirs.SIRS;
import fr.sirs.Session;
import fr.sirs.core.model.AbstractPhoto;
import fr.sirs.core.model.Preview;
import fr.sirs.core.model.SIRSFileReference;
import fr.sirs.plugins.synchro.ui.mount.DocumentExportPane;
import fr.sirs.plugins.synchro.attachment.AttachmentUtilities;
import fr.sirs.plugins.synchro.common.DocumentUtilities;
import fr.sirs.plugins.synchro.common.PhotoFinder;
import fr.sirs.plugins.synchro.common.TaskProvider;
import fr.sirs.plugins.synchro.concurrent.AsyncPool;
import fr.sirs.ui.Growl;
import fr.sirs.util.SirsStringConverter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.StackPane;
import org.ektorp.CouchDbConnector;
import org.geotoolkit.gui.javafx.util.TaskManager;

/**
 *
 * @author Alexis Manin (Geomatys)
 */
public class PhotoExport extends StackPane implements TaskProvider {

    @FXML
    private Button uiExportBtn;

    @FXML
    private Label uiRemainingSpace;

    @FXML
    private ChoiceBox<Integer> uiPhotoChoice;

    @FXML
    private Spinner<Integer> uiPhotoSpinner;

    @FXML
    private ProgressIndicator uiProgress;

    final IntegerBinding photoNumber;
    private final Session session;
    private final ObservableList<Preview> troncons;
    private final LocalDate dateFilter;

    private final AsyncPool pool;

    private final LongProperty estimatedSize = new SimpleLongProperty(-1);

    private final ObjectProperty<Task<Long>> estimateTaskProperty = new SimpleObjectProperty<>();

    private final ReadOnlyObjectWrapper<Task> exportTaskProperty = new ReadOnlyObjectWrapper<>();

    public PhotoExport(final Session session, final AsyncPool pool, final ObservableList<Preview> troncons, final LocalDate dateFilter) {
        SIRS.loadFXML(this);
        this.session = session;
        this.pool = pool;
        this.troncons = troncons;
        this.dateFilter = dateFilter;

        // Prepare UI to choose the number of photographs we want to export.
        ObservableList<Integer> photoList = FXCollections.observableArrayList();
        photoList.addAll(0, 1, -1, Integer.MAX_VALUE);
        uiPhotoChoice.setItems(photoList);
        uiPhotoChoice.setConverter(new DocumentExportPane.PhotoNumberConverter());
        uiPhotoChoice.getSelectionModel().select(0);

        uiPhotoChoice.valueProperty().addListener((ObservableValue<? extends Integer> observable, Integer oldValue, Integer newValue) -> {
            if (newValue < 0) {
                uiPhotoSpinner.setVisible(true);
            } else {
                uiPhotoSpinner.setVisible(false);
            }
        });
        uiPhotoSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, Integer.MAX_VALUE));

        photoNumber = Bindings.createIntegerBinding(
                () -> {
                    Integer choice = uiPhotoChoice.getValue();
                    if (choice == null || choice < 0) {
                        choice = uiPhotoSpinner.getValue();
                    }

                    return choice == null? 0 : choice;
                },
                uiPhotoChoice.valueProperty(), uiPhotoSpinner.valueProperty()
        );

        // Activate export button only if at least one photo should be send.
        uiExportBtn.disableProperty().bind(photoNumber.lessThan(1).or(Bindings.isEmpty(troncons)));
        uiRemainingSpace.textProperty().bind(Bindings.createStringBinding(() -> {
            final long size = estimatedSize.get();
            return size < 0? "inconnu" : SIRS.toReadableSize(size);
        }, estimatedSize));
        troncons.addListener(this::sourceChanged);
        photoNumber.addListener(this::sourceChanged);

        estimateTaskProperty.addListener(this::estimateTaskUpdated);
        exportTaskProperty.addListener(this::exportTaskUpdated);
        disableProperty().bind(uiProgress.visibleProperty());
    }

    @FXML
    void exportToMobile(ActionEvent event) {
        if (troncons.isEmpty() || photoNumber.get() < 1) {
            return;
        }

        final CouchDbConnector connector = session.getConnector();

        final UnaryOperator<AbstractPhoto> upload = ref -> {
            try {
                AttachmentUtilities.upload(connector, ref);
            } catch (Exception e) {
                throw new LocalDistantView.ParameterizedException(ref, e.getLocalizedMessage(), e);
            }
            return ref;
        };

        final Task<Void> uploader = pool.prepare(upload)
                .setTarget(find())
                .setWhenComplete(this::handleUploadResult)
                .build();
        exportTaskProperty.set(uploader);
    }


    private void handleUploadResult(final SIRSFileReference ref, final Throwable error) {
        if (error == null) {
            // TODO : something to do ?
        } else if (error instanceof Error) {
            throw (Error) error;
        } else {
            Throwable search = error;
            while (search != null && !(search instanceof LocalDistantView.ParameterizedException)) {
                search = search.getCause();
            }

            final String msg;
            if (search instanceof LocalDistantView.ParameterizedException) {
                SIRS.LOGGER.log(Level.WARNING, search, () -> "Cannot upload photo for " + ref);
                msg = String.format("Impossible d'envoyer la photo en base.%nPhoto : %s%nCause : %s",
                        new SirsStringConverter().toString(ref), search.getLocalizedMessage());
            } else {
                SIRS.LOGGER.log(Level.WARNING, error, () -> "Cannot upload a photograph");
                msg = String.format("Impossible d'envoyer une photo vers la base de données.%nCause : %s", error.getLocalizedMessage());
            }

            SIRS.fxRun(false, () -> new Growl(Growl.Type.ERROR, msg).showAndFade());
        }
    }

    private void sourceChanged(final Observable obs) {
        final Task<Long> sizeComputer = new TaskManager.MockTask<>("Estimation", ()
                -> find()
                        .mapToLong(PhotoExport::size)
                        .sum()
        );

        estimateTaskProperty.set(sizeComputer);
    }

    private static long size(final AbstractPhoto photo) {
        final Path file = SIRS.getDocumentAbsolutePath(photo);
        try {
            return Files.size(file);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private Stream<AbstractPhoto> find() {
        final int limit = photoNumber.get();
        if (limit < 1)
            return Stream.empty();

        // Defensive copy. It also ensure that we've only distinct identifiers.
        final Set<String> tdIds = troncons.stream()
                .map(Preview::getElementId)
                .collect(Collectors.toSet());

        UnaryOperator<Stream<AbstractPhoto>> preProcessor = stream
                -> stream
                        .filter(DocumentUtilities::isFileAvailable)
                        .sorted(new DocumentExportPane.PhotoDateComparator());

        if (limit < Integer.MAX_VALUE) {
            final UnaryOperator<Stream<AbstractPhoto>> firstPreprocessor = preProcessor;
            preProcessor = stream -> firstPreprocessor.apply(stream).limit(limit);
        }

        final Stream<AbstractPhoto> photos = new PhotoFinder(session)
                .setTronconIds(tdIds)
                .setDocDateFilter(dateFilter)
                .setPreProcessor(preProcessor)
                .get()
                .flatMap(photoTronconWrapper -> photoTronconWrapper.getPhotosStream());

        return photos
                .filter(DocumentUtilities::isFileAvailable);
    }

    private void estimateTaskUpdated(final ObservableValue obs, final Task<Long> oldValue, final Task<Long> newValue) {
        if (oldValue != null) {
            oldValue.cancel(true);
            uiRemainingSpace.getGraphic().visibleProperty().unbind();
            uiRemainingSpace.getGraphic().setVisible(false);
        }

        if (newValue != null) {
            newValue.setOnSucceeded(evt -> Platform.runLater(() -> estimatedSize.set(newValue.getValue())));
            newValue.setOnCancelled(evt -> Platform.runLater(() -> estimatedSize.set(-1)));
            newValue.setOnFailed(evt -> {
                SIRS.LOGGER.log(Level.WARNING, "Cannot estimate size for photographs to export.", newValue.getException());
                Platform.runLater(() -> {
                    estimatedSize.set(-1);
                    new Growl(Growl.Type.ERROR, "Impossible d'estimer la taille des photos pour envoi")
                            .showAndFade();
                });
            });
            uiRemainingSpace.getGraphic().visibleProperty().bind(newValue.runningProperty());

            TaskManager.INSTANCE.submit(newValue);
        }
    }


    private void exportTaskUpdated(final ObservableValue obs, final Task oldValue, final Task newValue) {
        if (oldValue != null) {
            oldValue.cancel(true);
            uiProgress.visibleProperty().unbind();
            uiProgress.setVisible(false);
        }

        if (newValue != null) {
            // We cancel size estimation, it's not needed anymore
            estimateTaskProperty.set(null);
            newValue.setOnSucceeded(evt -> Platform.runLater(() -> new Growl(Growl.Type.INFO, "Images téléversées avec succès").showAndFade()));
            newValue.setOnFailed(evt -> {
                SIRS.LOGGER.log(Level.WARNING, "Cannot upload photographs.", newValue.getException());
                Platform.runLater(()
                        -> new Growl(Growl.Type.ERROR, "Impossible d'envoyer les images en base de données")
                                .showAndFade());
            });

            uiProgress.visibleProperty().bind(newValue.runningProperty());
            TaskManager.INSTANCE.submit(newValue);
        }
    }

    @Override
    public ObservableValue<Task> taskProperty() {
        return exportTaskProperty.getReadOnlyProperty();
    }

    @Override
    public Task<Void> getTask() {
        return exportTaskProperty.get();
    }
}
