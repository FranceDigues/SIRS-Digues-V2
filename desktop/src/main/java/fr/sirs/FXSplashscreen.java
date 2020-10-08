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
package fr.sirs;

import fr.sirs.core.SirsDBInfo;
import fr.sirs.core.component.DatabaseRegistry;
import fr.sirs.util.property.SirsPreferences;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import org.geotoolkit.gui.javafx.util.TaskManager;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class FXSplashscreen {

    @FXML public GridPane uiLoadingPane;
    @FXML public Label uiProgressLabel;
    @FXML public ProgressBar uiProgressBar;
    @FXML public Button uiCancel;

    @FXML public GridPane uiLoginPane;
    @FXML public TextField uiLogin;
    @FXML public PasswordField uiPassword;
    @FXML public Button uiConnexion;
    @FXML public Label uiLogInfo;
    @FXML public Label uiRemoteDb;
    @FXML public Label uiSynchroState;

    @FXML
    void closeApp(ActionEvent event) {
        System.exit(0);
    }

    void analyzeDatabase(final DatabaseRegistry registry, final String databaseName) {
        final Tooltip remoteTip = new Tooltip();
        remoteTip.textProperty().bind(uiRemoteDb.textProperty());
        uiRemoteDb.setTooltip(remoteTip);

        final TaskManager.MockTask<SirsDBInfo> remoteInfoTask = new TaskManager.MockTask<>("Recherche d'informations sur une base distante…", 
                () -> registry.getInfo(databaseName).orElse(null));
        
        remoteInfoTask.setOnSucceeded(evt -> Platform.runLater(() -> {
            final SirsDBInfo dbInfo = remoteInfoTask.getValue();
            if (dbInfo == null || dbInfo.getRemoteDatabase() == null || dbInfo.getRemoteDatabase().isEmpty()) {
                uiRemoteDb.setText("Aucune");
            } else {
                uiRemoteDb.setText(DatabaseRegistry.cleanDatabaseName(dbInfo.getRemoteDatabase()));
            }
        }));

        final Runnable onCancelOrFail = () -> {
            uiRemoteDb.setText("Impossible de récupérer l'information");
            uiRemoteDb.setTextFill(Color.RED);
        };

        remoteInfoTask.setOnCancelled(evt -> Platform.runLater(onCancelOrFail));
        remoteInfoTask.setOnFailed(evt -> {
            SIRS.LOGGER.log(Level.WARNING, "Cannot get database information", remoteInfoTask.getException());
            Platform.runLater(onCancelOrFail);
        });

        TaskManager.INSTANCE.submit(remoteInfoTask);

        TaskManager.MockTask<Set<String>> remoteSynchroTask = new TaskManager.MockTask<>("", () -> 
                registry.getSynchronizationTasks(SirsPreferences.INSTANCE.getProperty(SirsPreferences.PROPERTIES.COUCHDB_LOCAL_ADDR) + databaseName)
                .map(status -> {
                    if (status.getSourceDatabaseName().equals(databaseName)) {
                        return DatabaseRegistry.cleanDatabaseName(status.getTargetDatabaseName());
                    } else {
                        return DatabaseRegistry.cleanDatabaseName(status.getSourceDatabaseName());
                    }
                })
                .collect(Collectors.toSet()));

        remoteSynchroTask.setOnSucceeded(evt -> {
            final Set<String> synchronisations = remoteSynchroTask.getValue();
            if (synchronisations.isEmpty()) {
                uiSynchroState.setText("Aucune synchronisation en cours");
            } else {
                final StringBuilder tipBuilder = new StringBuilder("synchronisation avec :");
                for (final String dbName : synchronisations) {
                    tipBuilder.append(System.lineSeparator()).append(dbName);
                }
                uiSynchroState.setText(String.valueOf(synchronisations.size()).concat(synchronisations.size() == 1 ? " synchronisation en cours" : " synchronisations en cours"));
                uiSynchroState.setTooltip(new Tooltip(tipBuilder.toString()));
            }
        });

        TaskManager.INSTANCE.submit(remoteSynchroTask);
    }
}
