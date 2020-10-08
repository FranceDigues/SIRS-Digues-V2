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
package fr.sirs.theme.ui;

import fr.sirs.SIRS;
import fr.sirs.core.model.SQLQueries;
import fr.sirs.core.model.SQLQuery;
import fr.sirs.core.model.report.TableSectionRapport;
import fr.sirs.query.FXSearchPane;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/**
 *
 * @author Alexis Manin (Geomatys)
 */
public class FXTableSectionRapportPane extends AbstractFXElementPane<TableSectionRapport> {

    @FXML private TextField uiTitle;
    @FXML private Label uiQueryTitle;

    private final ObjectProperty<SQLQuery> queryProperty = new SimpleObjectProperty<>();

    public FXTableSectionRapportPane() {
        super();
        SIRS.loadFXML(this);

        elementProperty.addListener(this::elementChanged);
        queryProperty.addListener(this::queryChanged);

        disableProperty().bind(disableFieldsProperty());
    }

    public FXTableSectionRapportPane(final TableSectionRapport rapport) {
        this();
        setElement(rapport);
    }

    private void elementChanged(ObservableValue<? extends TableSectionRapport> obs, TableSectionRapport oldValue, TableSectionRapport newValue) {
        if (oldValue != null) {
            uiTitle.textProperty().unbindBidirectional(oldValue.libelleProperty());
        }
        if (newValue != null) {
            uiTitle.textProperty().bindBidirectional(newValue.libelleProperty());
            try {
                queryProperty.set(SQLQueries.findQuery(newValue.getRequeteId()).orElse(null));
            } catch (Exception e) {
                SIRS.LOGGER.log(Level.WARNING, "Cannot load query", e);
                queryProperty.set(null);
            }
        } else {
            uiTitle.setText(null);
            queryProperty.set(null);
        }
    }

    private void queryChanged(ObservableValue<? extends SQLQuery> obs, SQLQuery oldValue, SQLQuery newValue) {
        if (newValue == null)
            uiQueryTitle.setText("N/A");
        else
            uiQueryTitle.setText(newValue.getLibelle());
    }

    @Override
    public void preSave() throws Exception {
        if (queryProperty.get() != null) {
            elementProperty.get().setRequeteId(queryProperty.get().getId() == null? queryProperty.get().getLibelle() : queryProperty.get().getId());
        } else {
            elementProperty.get().setRequeteId(null);
        }
    }

    @FXML
    private void chooseQuery(ActionEvent event) throws IOException {
        final List<SQLQuery> queries = SQLQueries.dbQueries();
        queries.addAll(SQLQueries.preprogrammedQueries());
        queries.sort(SQLQueries.QUERY_COMPARATOR);

        final Optional<SQLQuery> query = FXSearchPane.chooseSQLQuery(queries);
        if (query.isPresent()){
            queryProperty.set(query.get());
        }
    }

    @FXML
    private void deleteQuery(ActionEvent event) {
        queryProperty.set(null);
    }
}
