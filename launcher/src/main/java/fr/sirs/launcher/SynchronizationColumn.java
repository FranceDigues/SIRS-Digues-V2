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
package fr.sirs.launcher;

import fr.sirs.core.SirsCore;
import fr.sirs.core.SirsDBInfo;
import fr.sirs.core.component.DatabaseRegistry;
import fr.sirs.maj.ModuleChecker;
import fr.sirs.util.property.SirsPreferences;
import java.awt.Color;
import java.io.IOException;
import java.util.logging.Level;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.Border;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.apache.sis.util.ArgumentChecks;
import org.geotoolkit.font.FontAwesomeIcons;
import org.geotoolkit.font.IconBuilder;
import org.geotoolkit.gui.javafx.util.TaskManager;
import org.geotoolkit.internal.GeotkFX;

/**
 *
 * @author Alexis Manin (Geomatys)
 */
public class SynchronizationColumn extends TableColumn<String, Task> {

    private static final Image ICON_SYNCHRO_STOPPED = SwingFXUtils.toFXImage(IconBuilder.createImage(
            FontAwesomeIcons.ICON_EXCHANGE, 16, new Color(130, 0, 0)), null);

    private static final Image ICON_SYNCHRO_RUNNING = SwingFXUtils.toFXImage(IconBuilder.createImage(
            FontAwesomeIcons.ICON_EXCHANGE, 16, Color.GREEN), null);

    private static final Tooltip PAUSE_SYNCHRO = new Tooltip("Passer en mode hors-ligne.");
    private static final Tooltip RESUME_SYNCHRO = new Tooltip("Reprendre la synchronisation automatique.");

    private final DatabaseRegistry dbRegistry;

    public SynchronizationColumn(final DatabaseRegistry registry) {
        super();
        setSortable(false);
        setResizable(false);
        setPrefWidth(24);
        setMinWidth(24);
        setMaxWidth(24);

        ArgumentChecks.ensureNonNull("Database registry", registry);
        dbRegistry = registry;

        setCellValueFactory((CellDataFeatures<String, Task> param) -> {

            // récupération du nom local de la base
            final String dbName = param.getValue();
            if (dbName == null || dbName.isEmpty()) {
                return null;
            } else {

                final String dbUrl = SirsPreferences.INSTANCE.getProperty(SirsPreferences.PROPERTIES.COUCHDB_LOCAL_ADDR) + dbName;

                try {
                    if (dbRegistry.getSynchronizationTasks(dbUrl).count() > 0) {
                        return new SimpleObjectProperty<>(new StopSync(dbUrl));
                    } else {
                        final String remoteName = dbRegistry.getInfo(dbUrl).orElse(new SirsDBInfo()).getRemoteDatabase();
                        if (remoteName != null) {
                            return new SimpleObjectProperty<>(new ResumeSync(dbUrl, remoteName));
                        }
                    }
                } catch (IOException ex) {
                    SirsCore.LOGGER.log(Level.WARNING, null, ex);
                }
            }
            return null;
        });

        setCellFactory((TableColumn<String, Task> param) -> new SynCell());
    }

    private final class SynCell extends TableCell<String, Task> {

        protected final Button button;

        private SynCell() {
            button = new Button();
            button.setBackground(Background.EMPTY);
            button.setBorder(Border.EMPTY);
            button.setPadding(Insets.EMPTY);

            button.setOnAction(evt -> {
                final Alert ask = new Alert(Alert.AlertType.CONFIRMATION,
                        "Êtes-vous sûr de vouloir reprendre/arrêter la synchronisation automatique?",
                        ButtonType.YES, ButtonType.NO);
                ask.setResizable(true);
                final ButtonType choice = ask.showAndWait().orElse(ButtonType.NO);
                if (ButtonType.YES.equals(choice)) {
                    updateState();
                }
            });
        }

        @Override
        protected void updateItem(Task item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setGraphic(null);

            } else if (item instanceof StopSync) {
                button.setGraphic(new ImageView(ICON_SYNCHRO_RUNNING));
                button.setTooltip(PAUSE_SYNCHRO);
                button.setVisible(true);
                setGraphic(button);

            } else if (item instanceof ResumeSync) {
                button.setGraphic(new ImageView(ICON_SYNCHRO_STOPPED));
                button.setTooltip(RESUME_SYNCHRO);
                button.setVisible(true);
                setGraphic(button);

            } else {
                setGraphic(null);
            }
        }

        private void updateState() {
            final Task t = getItem();
            t.setOnFailed(evt -> Platform.runLater(() ->
                GeotkFX.newExceptionDialog("Mise à jour de la synchronisation impossible.", t.getException()).show()
            ));

            t.setOnSucceeded(evt -> Platform.runLater(() ->
                   updateItem((Task)t.getValue(), false)
            ));

            // Block user actions until process is finished.
            final Stage checkStage = new Stage(StageStyle.TRANSPARENT);
            checkStage.setWidth(451);
            checkStage.initModality(Modality.WINDOW_MODAL);
            checkStage.initOwner(getScene().getWindow());
            checkStage.titleProperty().bind(t.titleProperty());
            checkStage.setScene(new Scene(new FXLoadingPane(t)));
            checkStage.show();
            t.runningProperty().addListener((obs, oldValue, newValue) -> {
                if (!newValue) {
                    Platform.runLater(() -> checkStage.close());
                }
            });

            TaskManager.INSTANCE.submit(t);
        }
    }

    /**
     * A simple action to stop all replication tasks running on a given
     * database. returns a callable allowing to restart synchronization between
     * input database and itss remote, specified into {@link SirsDBInfo}.
     */
    private class StopSync extends Task<ResumeSync> {

        private final String localDb;

        public StopSync(final String localDb) {
            this.localDb = localDb;
            updateTitle("Arrêt de la synchronisation");
        }

        @Override
        public ResumeSync call() throws Exception {
            dbRegistry.cancelAllSynchronizations(localDb);
            final String remoteDatabase = dbRegistry.getInfo(localDb).orElse(new SirsDBInfo()).getRemoteDatabase();
            if (remoteDatabase != null) {
                return new ResumeSync(localDb, remoteDatabase);
            } else {
                return null;
            }
        }
    }

    /**
     * A simple action to start synchronisation between two given databases.
     * Returns a callable to put main database offline.
     */
    private class ResumeSync extends Task<StopSync> {

        private final String localDb;
        private final String remoteDb;

        /**
         *
         * @param localDb base de données locale à synchroniser
         * @param remoteDb base de données à laquelle synchroniser la base locale
         */
        public ResumeSync(final String localDb, final String remoteDb) {
            this.localDb = localDb;
            this.remoteDb = remoteDb;
            updateTitle("Reprise de la synchronisation");
        }

        @Override
        public StopSync call() throws Exception {
            final ChangeListener<String> msgListener = (obs, oldMsg, newMsg) -> updateMessage(newMsg);
            // First, we ensure that local database is up to date with installed modules.
            final ModuleChecker modCheck = new ModuleChecker(dbRegistry, localDb);
            Platform.runLater(() -> modCheck.messageProperty().addListener(msgListener));

            modCheck.run();
            // Local database is up to date.
            if (modCheck.get()) {
                // Now, we must ensure that local AND distant database are based on same module versions.
                final DatabaseRegistry remoteRegistry = new DatabaseRegistry(remoteDb);
                final ModuleChecker modCheck2 = new ModuleChecker(remoteRegistry, remoteDb);
                Platform.runLater(() -> modCheck2.messageProperty().addListener(msgListener));
                modCheck2.run();
                if (modCheck2.get()) {
                    dbRegistry.synchronizeSirsDatabases(remoteDb, localDb, true);
                    return new StopSync(localDb);
                }
            }

            throw new IllegalStateException("Impossible de synchroniser les bases de données, car les versions de "
                    + "certains modules installés ne sont pas compatibles avec les versions utilisées par la base distante.");
        }
    }
}
