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
package fr.sirs.plugin.reglementaire.ui;

import fr.sirs.Injector;
import fr.sirs.SIRS;
import fr.sirs.core.component.EtapeObligationReglementaireRepository;
import fr.sirs.core.model.Element;
import fr.sirs.core.model.EtapeObligationReglementaire;
import fr.sirs.core.model.PlanificationObligationReglementaire;
import fr.sirs.theme.ui.PojoTable;
import fr.sirs.util.FXFreeTab;
import fr.sirs.util.SimpleFXEditMode;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Separator;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import org.geotoolkit.filter.DefaultPropertyIsNull;
import org.geotoolkit.filter.DefaultPropertyName;
import org.geotoolkit.filter.binarylogic.DefaultAnd;
import org.geotoolkit.gui.javafx.util.ButtonTableCell;
import org.geotoolkit.internal.GeotkFX;
import org.opengis.filter.Filter;

/**
 *
 */
public class EtapesPojoTable extends PojoTable {

    private final CheckBox uiHideRealizedCheckBox = new CheckBox("Masquer les étapes réalisées");

    public EtapesPojoTable(final TabPane tabPane, final ObjectProperty<? extends Element> container) {
        super(EtapeObligationReglementaire.class, "Etapes d'obligations réglementaires", container, false);
        // Ajout de la colonne de duplication
        getColumns().add(2, new DuplicateEtapeColumn());

        final Button uiPlanificationBtn = new Button(null, new ImageView(SIRS.ICON_CLOCK_WHITE));
        uiPlanificationBtn.getStyleClass().add(BUTTON_STYLE);
        uiPlanificationBtn.setTooltip(new Tooltip("Planification automatique"));
        uiPlanificationBtn.setOnMouseClicked(event -> showPlanificationTable(tabPane, null));
        searchEditionToolbar.getChildren().add(1, uiPlanificationBtn);

        if (getFilterUI() instanceof VBox) {
            final VBox vbox = (VBox) getFilterUI();
            vbox.getChildren().add(vbox.getChildren().size() - 1, uiHideRealizedCheckBox);
            uiHideRealizedCheckBox.setStyle("-fx-text-fill: #FFF");
        }
        
        applyPreferences();
        listenPreferences();
    }

    @Override
    public Filter getFilter() {
        if (uiHideRealizedCheckBox == null || !uiHideRealizedCheckBox.isSelected()) {
            return super.getFilter();
        }

        final Filter filterNotRealized = new DefaultPropertyIsNull(new DefaultPropertyName("dateRealisation"));
        final Filter filter = super.getFilter();
        if (filter == null) {
            return filterNotRealized;
        }

        return new DefaultAnd(filterNotRealized, filter);
    }

    @Override
    public void resetFilter(final VBox filterContent) {
        super.resetFilter(filterContent);

        uiHideRealizedCheckBox.setSelected(false);
    }

    /**
     //     * Ouvre un onglet sur la table des planifications.
     //     *
     //     * @param tabPane Le conteneur d'onglet dans lequel ajouter ce nouvel onglet.
     //     */
    private void showPlanificationTable(final TabPane tabPane, final ObjectProperty<? extends Element> container) {
        final FXFreeTab planTab = new FXFreeTab("Planification");
        // Gestion du bouton consultation / édition pour la pojo table
        final Separator separator = new Separator();
        separator.setVisible(false);
        final SimpleFXEditMode editMode = new SimpleFXEditMode();
        final HBox topPane = new HBox(separator, editMode);
        HBox.setHgrow(separator, Priority.ALWAYS);
        final PojoTable pojoTable = new PojoTable(Injector.getSession().getRepositoryForClass(
                PlanificationObligationReglementaire.class), "Planification(s) programmée(s)", container);
        pojoTable.editableProperty().bind(editMode.editionState());
        planTab.setContent(new BorderPane(pojoTable, topPane, null, null, null));
        tabPane.getTabs().add(planTab);
        tabPane.getSelectionModel().select(planTab);
    }

    public static class DuplicateEtapeColumn extends TableColumn {

        public DuplicateEtapeColumn() {
            super("Dupliquer");
            setSortable(false);
            setResizable(false);
            setPrefWidth(24);
            setMinWidth(24);
            setMaxWidth(24);
            setGraphic(new ImageView(GeotkFX.ICON_DUPLICATE));

            setCellValueFactory(new Callback<TableColumn.CellDataFeatures, ObservableValue>() {

                @Override
                public ObservableValue call(TableColumn.CellDataFeatures param) {
                    return new SimpleObjectProperty(param.getValue());
                }
            });

            setCellFactory(new Callback<TableColumn, TableCell>() {

                @Override
                public TableCell call(TableColumn param) {
                    return new ButtonTableCell(
                            false, new ImageView(GeotkFX.ICON_DUPLICATE),
                            (Object t) -> t!=null, (Object t) -> {
                        if (t instanceof EtapeObligationReglementaire) {
                            final EtapeObligationReglementaire initialEtape = (EtapeObligationReglementaire)t;
                            final EtapeObligationReglementaire newEtape = initialEtape.copy();
                            final EtapeObligationReglementaireRepository eorr = Injector.getBean(EtapeObligationReglementaireRepository.class);
                            eorr.add(newEtape);
                        }
                        return t;
                    });
                }
            });
        }
    }
}
