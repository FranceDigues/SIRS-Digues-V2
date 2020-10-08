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
import fr.sirs.core.component.ObligationReglementaireRepository;
import fr.sirs.core.model.Element;
import fr.sirs.core.model.ObligationReglementaire;
import fr.sirs.theme.ui.PojoTable;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.image.ImageView;
import javafx.util.Callback;
import org.geotoolkit.gui.javafx.util.ButtonTableCell;
import org.geotoolkit.internal.GeotkFX;

/**
 * Table présentant les obligations réglementaires.
 *
 * @author Cédric Briançon (Geomatys)
 */
public final class ObligationsPojoTable extends PojoTable {

    private static final String PLANIF_PROP_NAME = "Planification";

    /**
     * Création de la table présentant les obligations réglementaires.
     */
    public ObligationsPojoTable(final ObjectProperty<? extends Element> container) {
        super(ObligationReglementaire.class, "Liste des obligations réglementaires", container);

        // Ajout de la colonne de duplication
        getColumns().add(2, new DuplicateObligationColumn());

        final ObservableList<TableColumn<Element, ?>> cols = getColumns();
        for (final TableColumn col : cols) {
            if (PLANIF_PROP_NAME.equals(col.getText())) {
                cols.remove(col);
                break;
            }
        }
    }

    public static class DuplicateObligationColumn extends TableColumn {

        public DuplicateObligationColumn() {
            super("Dupliquer");
            setSortable(false);
            setResizable(false);
            setPrefWidth(24);
            setMinWidth(24);
            setMaxWidth(24);
            setGraphic(new ImageView(GeotkFX.ICON_DUPLICATE));

            setCellValueFactory(new Callback<CellDataFeatures, ObservableValue>() {

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
                            (Object t) -> t != null, (Object t) -> {
                        if (t instanceof ObligationReglementaire) {
                            final ObligationReglementaire initialObligation = (ObligationReglementaire) t;
                            final ObligationReglementaire newObligation = initialObligation.copy();
                            final ObligationReglementaireRepository orr = Injector.getBean(ObligationReglementaireRepository.class);
                            orr.add(newObligation);
                        }
                        return t;
                    });
                }
            });
        }
    }
}
