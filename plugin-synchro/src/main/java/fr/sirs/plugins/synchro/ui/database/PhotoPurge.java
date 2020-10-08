package fr.sirs.plugins.synchro.ui.database;

import fr.sirs.SIRS;
import fr.sirs.Session;
import fr.sirs.core.model.AbstractPhoto;
import fr.sirs.plugins.synchro.attachment.AttachmentUtilities;
import fr.sirs.plugins.synchro.common.PhotoFinder;
import fr.sirs.plugins.synchro.concurrent.AsyncPool;
import fr.sirs.ui.Growl;
import java.time.LocalDate;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Stream;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.apache.sis.util.ArgumentChecks;
import org.ektorp.CouchDbConnector;
import org.geotoolkit.gui.javafx.util.TaskManager;

/**
 *
 * @author Alexis Manin (Geomatys)
 */
public class PhotoPurge extends StackPane {

    private final Session session;

    private final AsyncPool pool;

    @FXML
    private Button uiCancel;

    @FXML
    private Button uiCompact;

    @FXML
    private VBox uiProgressPane;

    @FXML
    private Label uiProgressLabel;

    @FXML
    private VBox uiForm;

    @FXML
    private RadioButton uiDateTrigger;

    @FXML
    private HBox uiDateBox;

    @FXML
    private DatePicker uiDate;

    @FXML
    private Label uiNb;

    @FXML
    private Label uiSize;

    public PhotoPurge(final AsyncPool pool, final Session session) {
        ArgumentChecks.ensureNonNull("Asynchronous executor", pool);
        ArgumentChecks.ensureNonNull("Session", session);
        SIRS.loadFXML(this);
        this.pool = pool;
        this.session = session;

        uiForm.disableProperty().bind(uiProgressPane.visibleProperty());
        uiDate.disableProperty().bind(uiDateTrigger.selectedProperty().not());

        uiDateBox.managedProperty().bind(uiDateBox.visibleProperty());
        uiCompact.setOnAction(evt -> Platform.runLater(() -> askForCompaction(false)));
    }

    @FXML
    void estimate(ActionEvent event) {
        final Stream<AbstractPhoto> photos = getPhotographs();

        final CouchDbConnector connector = session.getConnector();
        final Task<Map.Entry<Long, Long>> t = AttachmentUtilities.estimateSize(connector, photos);

        t.setOnFailed(ResultTaskUtilities.failedEstimation(t));
        t.setOnSucceeded(ResultTaskUtilities.succedEstimation(t, uiNb, uiSize));

        uiProgressPane.visibleProperty().bind(t.runningProperty());

        uiCancel.setOnAction(evt -> t.cancel(true));

        TaskManager.INSTANCE.submit(t);
    }

    @FXML
    void purgePhotos(ActionEvent event) {

        final Alert ask = new Alert(Alert.AlertType.CONFIRMATION,
                "Attention, en cliquant sur 'Purger', vous supprimerez toutes les photographies encore présentes dans la base de données",
                ButtonType.YES, ButtonType.NO);
        ask.setResizable(true);
        final ButtonType choice = ask.showAndWait().orElse(ButtonType.NO);

        if (ButtonType.YES.equals(choice)) {
            final CouchDbConnector connector = session.getConnector();
            // Note : incremented in handleResult method.
            final LongProperty count = new SimpleLongProperty(0);
            final Function<AbstractPhoto, LongProperty> processor = photo -> {
                AttachmentUtilities.delete(connector, photo);
                return count;
            };

            final Task<Void> delete = pool.prepare(processor)
                    .setTarget(getPhotographs())
                    .setWhenComplete(this::handleResult)
                    .build();

            uiProgressPane.visibleProperty().bind(delete.runningProperty());
            uiProgressLabel.textProperty().bind(Bindings.createStringBinding(() -> "Images supprimées : " + count.get(), count));

            uiCancel.setOnAction(evt -> delete.cancel(true));

            delete.setOnSucceeded(evt -> Platform.runLater(() -> askForCompaction(true)));

            TaskManager.INSTANCE.submit(delete);
        }
    }

    /**
     * Displays an alert to the user to query for a database compaction (see {@link CouchDbConnector#compact()
     * }.). If user confirms, we launch a compaction asynchronously.
     */
    private void askForCompaction(final boolean afterDeletion) {
        final String message;
        if (afterDeletion) {
            message =
                "Les photographies ont été supprimées de la base de données. "
                + "Voulez-vous compacter cette dernière ?"
                + System.lineSeparator()
                + "Note : Cette opération supprimera d'éventuels relicats de documents "
                + "supprimés de la base de données, afin d'en optimiser le stockage.";
        } else {
            message = "Êtes vous sûr de vouloire compacter la base de données?"
                + System.lineSeparator()
                + "Note : Cette opération supprimera d'éventuels relicats de documents "
                + "supprimés de la base de données, afin d'en optimiser le stockage.";
        }

        final Alert ask = new Alert(Alert.AlertType.CONFIRMATION,
                message,
                ButtonType.YES, ButtonType.NO);
        ask.setResizable(true);
        final ButtonType choice = ask.showAndWait().orElse(ButtonType.NO);
        if (ButtonType.YES.equals(choice)) {
            TaskManager.INSTANCE.submit("Nettoyage de la base de données", () -> session.getConnector().compact());
        }
    }

    private void handleResult(final LongProperty count, final Throwable error) {
        if (error == null) {
            Platform.runLater(() -> count.set(count.get() + 1));
        } else if (error instanceof Error) {
            throw (Error) error;
        } else {
            SIRS.LOGGER.log(Level.WARNING, "Cannot delete an image", error);
            final String msg = String.format("Une image ne peut être supprimée de la base de données.%nCause : %s", error.getLocalizedMessage() == null? "Inconnue" : error.getLocalizedMessage());
            Platform.runLater(() -> new Growl(Growl.Type.ERROR, msg).showAndFade());
        }
    }

    private Stream<AbstractPhoto> getPhotographs() {
        Stream<AbstractPhoto> distantPhotos = new PhotoFinder(session).get()
                .flatMap(photoTronconWrapper -> photoTronconWrapper.getPhotosStream());

        if (uiDateTrigger.isSelected()) {
            final LocalDate since = uiDate.getValue().minusDays(1);
            distantPhotos = distantPhotos.filter(photo -> photo.getDate() != null && since.isBefore(photo.getDate()));
        }

        final CouchDbConnector connector = session.getConnector();
        return distantPhotos
                // Find photographs uploaded in database.
                .filter(photo -> AttachmentUtilities.isAvailable(connector, photo));
    }
}
