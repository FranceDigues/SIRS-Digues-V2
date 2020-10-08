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

import fr.sirs.core.model.OuvrageHydrauliqueAssocie;
import fr.sirs.core.model.Positionable;
import fr.sirs.core.model.RefOuvrageHydrauliqueAssocie;
import fr.sirs.util.ConvertPositionableCoordinates;
import fr.sirs.ui.Growl;
import fr.sirs.util.ClosingDaemon;
import java.util.List;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tab;
import org.geotoolkit.gui.javafx.util.TaskManager;
import org.geotoolkit.util.collection.CloseableIterator;

/**
 *
 * @author Samuel Andrés (Geomatys)
 */
public class FXOuvrageAssociePrintPane extends TemporalTronconChoicePrintPane {

    @FXML private Tab uiOuvrageTypeChoice;

    @FXML private CheckBox uiOptionPhoto;
    @FXML private CheckBox uiOptionReseauxFermes;

    private final TypeChoicePojoTable ouvrageTypesTable = new TypeChoicePojoTable(RefOuvrageHydrauliqueAssocie.class, "Types d'ouvrages associés");

    private final ObjectProperty<Task<Boolean>> taskProperty = new SimpleObjectProperty<>();

    @FXML private Button uiPrint;
    @FXML private Button uiCancel;

    @FXML private Label uiCountLabel;
    @FXML private ProgressIndicator uiCountProgress;

    private final InvalidationListener parameterListener;
    private final ObjectProperty<Task> countTask = new SimpleObjectProperty<>();

    public FXOuvrageAssociePrintPane(){
        super(FXOuvrageAssociePrintPane.class);
        ouvrageTypesTable.setTableItems(()-> (ObservableList) SIRS.observableList(Injector.getSession().getRepositoryForClass(RefOuvrageHydrauliqueAssocie.class).getAll()));
        ouvrageTypesTable.commentAndPhotoProperty().set(false);
        uiOuvrageTypeChoice.setContent(ouvrageTypesTable);

        uiPrint.disableProperty().bind(uiCancel.disableProperty().not());
        uiCancel.setDisable(true);
        taskProperty.addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                uiCancel.disableProperty().unbind();
                uiCancel.setDisable(true);
            } else {
                uiCancel.disableProperty().bind(newVal.runningProperty().not());
                newVal.setOnSucceeded(evt -> Platform.runLater(() -> {
                    if (Boolean.TRUE.equals(evt.getSource().getValue())) {
                        new Growl(Growl.Type.INFO, "La fiche a été générée avec succès.").showAndFade();
                    } else {
                        new Growl(Growl.Type.WARNING, "Aucun élément trouvé pour les critères demandés.").showAndFade();
                    }
                    taskProperty.set(null);
                }));
                newVal.setOnFailed(evt -> Platform.runLater(() -> {
                    new Growl(Growl.Type.ERROR, "L'impression a échouée.").showAndFade();
                    taskProperty.set(null);
                }));
            }
        });

        parameterListener = this::updateCount;
        ouvrageTypesTable.getSelectedItems().addListener(parameterListener);
        // TODO : listen PR change on selected items.
        tronconsTable.getSelectedItems().addListener(parameterListener);
        uiOptionArchive.selectedProperty().addListener(parameterListener);
        uiOptionNonArchive.selectedProperty().addListener(parameterListener);
        uiOptionDebut.valueProperty().addListener(parameterListener);
        uiOptionFin.valueProperty().addListener(parameterListener);
        uiOptionDebutArchive.valueProperty().addListener(parameterListener);
        uiOptionFinArchive.valueProperty().addListener(parameterListener);


        uiPrestationPredicater.uiOptionPrestation.selectedProperty().addListener(parameterListener);

        uiCountProgress.setVisible(false);
        updateCount(null);
    }

    @FXML private void cancel() {
        final Task t = taskProperty.get();
        if (t != null)
            t.cancel();
    }

    @FXML
    private void print() {
        final Task<Boolean> printing = new TaskManager.MockTask<>("Génération de fiches détaillées d'ouvrages hydrauliques associés", () -> {

            final List<OuvrageHydrauliqueAssocie> toPrint;
            try (final Stream<OuvrageHydrauliqueAssocie> data = getData()) {
                toPrint = data.collect(Collectors.toList());
            }

            if (!toPrint.isEmpty() && !Thread.currentThread().isInterrupted()) {
                Injector.getSession().getPrintManager().printOuvragesAssocies(toPrint, uiOptionPhoto.isSelected(), uiOptionReseauxFermes.isSelected());
            }

            return !toPrint.isEmpty();
        });
        taskProperty.set(printing);

        TaskManager.INSTANCE.submit(printing);
    }

    private Stream<OuvrageHydrauliqueAssocie> getData() {
            final Predicate userOptions = new TypeOuvragePredicate()
                    .and(OuvrageHydrauliqueAssocie::getValid) // On n'autorise à l'impression uniquement les désordre valides.
                    .and(new TemporalPredicate())
                    .and(new LinearPredicate<>())
                // /!\ It's important that pr filtering is done AFTER linear filtering.
                    .and(new PRPredicate<>())
                    .and(uiPrestationPredicater.getPredicate());

        final CloseableIterator<OuvrageHydrauliqueAssocie> it = Injector.getSession()
                .getRepositoryForClass(OuvrageHydrauliqueAssocie.class)
                .getAllStreaming()
                .iterator();

        final Spliterator<OuvrageHydrauliqueAssocie> split = Spliterators.spliteratorUnknownSize(it, 0);
        final Stream dataStream = StreamSupport.stream(split, false)
                .filter(userOptions)
                .peek(p -> ConvertPositionableCoordinates.COMPUTE_MISSING_COORD.test((Positionable) p));

        dataStream.onClose(() -> it.close());
        ClosingDaemon.watchResource(dataStream, it);

        return dataStream;
    }

    private void updateCount(final Observable changedObs) {
        final Task oldTask = countTask.get();
        if (oldTask != null)
            oldTask.cancel(true);

        final Task t = new TaskManager.MockTask<>("Décompte des éléments à imprimer", () -> {
            try (final Stream data = getData()) {
                return data.count();
            }
        });

        uiCountProgress.visibleProperty().bind(t.runningProperty());
        t.setOnRunning(evt -> uiCountLabel.setText(null));

        t.setOnSucceeded(evt -> uiCountLabel.setText(String.valueOf(t.getValue())));

        t.setOnFailed(evt -> new Growl(Growl.Type.ERROR, "Impossible de déterminer le nombre d'ouvrages à imprimer.").showAndFade());

        countTask.set(t);
        TaskManager.INSTANCE.submit(t);
    }


    @Override
    protected InvalidationListener getParameterListener() {
        return parameterListener;
    }

    private class TypeOuvragePredicate implements Predicate<OuvrageHydrauliqueAssocie> {

        final Set<String> acceptedIds;

        TypeOuvragePredicate() {
            acceptedIds = ouvrageTypesTable.getSelectedItems().stream()
                    .map(e -> e.getId())
                    .collect(Collectors.toSet());
        }

        @Override
        public boolean test(OuvrageHydrauliqueAssocie t) {
            return acceptedIds.isEmpty() || (t.getTypeOuvrageHydroAssocieId()!= null && acceptedIds.contains(t.getTypeOuvrageHydroAssocieId()));
        }
    }
}
