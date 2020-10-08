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
package fr.sirs.map;

import fr.sirs.Injector;
import fr.sirs.SIRS;
import fr.sirs.Session;
import org.geotoolkit.gui.javafx.util.TaskManager;
import fr.sirs.core.model.TronconDigue;
import fr.sirs.core.TronconUtils;
import fr.sirs.core.component.AbstractSIRSRepository;
import fr.sirs.core.model.AvecBornesTemporelles;
import fr.sirs.ui.Growl;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.apache.sis.util.ArgumentChecks;
import org.geotoolkit.gui.javafx.render2d.FXMap;
import org.geotoolkit.gui.javafx.util.FXDeleteTableColumn;
import org.geotoolkit.gui.javafx.util.FXMoveDownTableColumn;
import org.geotoolkit.gui.javafx.util.FXMoveUpTableColumn;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class FXTronconMerge extends VBox {

    @FXML private TableView uiTable;
    @FXML private TextField uiLinearName;
    @FXML private Label infoLabel;
    @FXML private Label nameLabel;

    private final ObservableList<TronconDigue> troncons = FXCollections.observableArrayList();
    private final FXMap map;
    private final String typeName;
    private final boolean maleGender;

    public FXTronconMerge(final FXMap map, final String typeName, final boolean maleGender) {
        SIRS.loadFXML(this);

        this.typeName = typeName;
        this.maleGender = maleGender;

        if (maleGender) {
            nameLabel.setText("Nom du " + typeName + " résultant de la fusion :");
        } else {
            nameLabel.setText("Nom de la " + typeName + " résultante de la fusion :");
        }
        infoLabel.setText("Les " + typeName + "s avant fusion seront archivés.");
        final TableColumn<TronconDigue,String> col = new TableColumn<>("Nom");
        col.setEditable(false);
        col.setCellValueFactory((TableColumn.CellDataFeatures<TronconDigue, String> param) -> param.getValue().libelleProperty());

        uiTable.setItems(troncons);
        uiTable.getColumns().add(new FXMoveUpTableColumn());
        uiTable.getColumns().add(new FXMoveDownTableColumn());
        uiTable.getColumns().add(col);
        uiTable.getColumns().add(new FXDeleteTableColumn(false));
        this.map = map;
    }

    public ObservableList<TronconDigue> getTroncons() {
        return troncons;
    }

    public void processMerge() {
        final Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Voulez-vous vraiment fusionner les " + typeName + "s ? Si oui, vos modifications seront enregistrées.", ButtonType.YES, ButtonType.NO);
        confirm.setResizable(true);
        confirm.showAndWait();
        final ButtonType result = confirm.getResult();
        if(result==ButtonType.YES) {
            final MergeTask mergeTask = new MergeTask(uiLinearName.getText(), typeName, maleGender, troncons);
            mergeTask.setOnSucceeded(evt -> Platform.runLater(()
                    -> new Growl(Growl.Type.INFO, "Fusion terminée avec succès").showAndFade()
            ));
            mergeTask.setOnFailed(evt -> Platform.runLater(()
                    -> new Growl(Growl.Type.ERROR, "La fusion a échouée").showAndFade()
            ));
            TaskManager.INSTANCE.submit(mergeTask);
        }
    }

    /**
     * A task whose job is to perform fusion of {@link TronconDigue} selected via merge tool.
     */
    private static class MergeTask extends Task<Boolean> {

        final String mergeName;
        final String typeName;
        final boolean maleGender;
        final List<TronconDigue> troncons;

        MergeTask(String outputName, String typeName, final boolean maleGender, List<TronconDigue> toMerge) {
            ArgumentChecks.ensureNonNull("List of data to merge", toMerge);
            if (toMerge.isEmpty()) {
                throw new IllegalArgumentException("No data to merge");
            }
            // defensive copy
            troncons = new ArrayList<>(toMerge);

            if (outputName == null || (outputName = outputName.trim()).isEmpty()) {
                mergeName = toMerge.stream()
                        .map(TronconDigue::getLibelle)
                        .collect(Collectors.joining(" + "));
            } else {
                mergeName = outputName;
            }

            this.typeName = typeName;
            this.maleGender = maleGender;
        }

        @Override
        protected Boolean call() throws Exception {
            if (troncons.size() <= 1) {
                return false;
            }

            updateTitle("Fusion de " + typeName + "s");
            updateProgress(0, troncons.size());

            final Session session = Injector.getSession();

            final TronconDigue merge = TronconUtils.copyTronconAndRelative(troncons.get(0), session);
            final AbstractSIRSRepository<TronconDigue> tronconRepo = (AbstractSIRSRepository<TronconDigue>) session.getRepositoryForClass(merge.getClass());
            try {
                for (int i = 1, n = troncons.size(); i < n; i++) {
                    if (Thread.currentThread().isInterrupted()) throw new InterruptedException("La fusion de " + typeName + " a été interrompue.");

                    final TronconDigue current = troncons.get(i);
                    updateProgress(i, troncons.size());
                    if (maleGender) {
                        updateMessage("Ajout du " + typeName + " "+current.getLibelle());
                    } else {
                        updateMessage("Ajout de la " + typeName + " "+current.getLibelle());
                    }

                    TronconUtils.mergeTroncon(merge, current, session);
                }

                merge.setLibelle(mergeName);
                session.executeBulk(Collections.singleton(merge));

            } catch (Exception e) {
                /* An exception has been thrown. We remove the resulting troncon from
                 * database, as it is not complete.
                 */
                try {
                    tronconRepo.remove(merge);
                } catch (Exception suppressed) {
                    e.addSuppressed(suppressed);
                }
                throw e;
            }

            // Merge succeeded, we must now archive old ones.
            final LocalDate archiveDate = LocalDate.now().minusDays(1);
            final Iterator<TronconDigue> it = troncons.iterator();
            while (it.hasNext()) {
                final TronconDigue next = it.next();
                final Predicate<AvecBornesTemporelles> archiveIf = new AvecBornesTemporelles.ArchivePredicate(archiveDate);
                TronconUtils.archiveSectionWithTemporalObjects(next, session, archiveDate, archiveIf);
                TronconUtils.archiveBornes(next.getBorneIds(), session, archiveDate, archiveIf);
            }

            return true;
        }
    }
}
