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
package fr.sirs.query;

import fr.sirs.SIRS;
import fr.sirs.core.model.SQLQuery;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

/**
 *
 * @author Johann Sorel (Geomatys)
 * @author Alexis Manin (Geomatys)
 */
public class FXQueryPane extends VBox {

    @FXML private TextArea uiDesc;
    @FXML private TextArea uiSql;
    @FXML private TextField uiLibelle;

    private final ObjectProperty<SQLQuery> sqlQueryProperty = new SimpleObjectProperty<>();
    private final BooleanProperty modifiableProperty = new SimpleBooleanProperty(true);
    
    public FXQueryPane() {
        this(null);
    }
    
    public FXQueryPane(SQLQuery query) {
        SIRS.loadFXML(this);
        setSQLQuery(query);
        
        uiLibelle.editableProperty().bind(modifiableProperty());
        
        /*
        Pour permettre la lecture des longues descriptions d'un coup d'œil
        on branche et on synchronyse un tooltip sur la description.
        */
        uiDesc.tooltipProperty().bind(new ObjectBinding<Tooltip>() {
            
            {bind(uiDesc.textProperty());}
            
            @Override
            protected Tooltip computeValue() {
                return new Tooltip(uiDesc.getText());
            }
        });
        uiDesc.editableProperty().bind(modifiableProperty());
        
        /*
        Pour permettre la lecture des longues requêtes d'un coup d'œil
        on branche et on synchronyse un tooltip sur le texte de la requête.
        */
        uiSql.tooltipProperty().bind(new ObjectBinding<Tooltip>() {
            
            {bind(uiSql.textProperty());}
            
            @Override
            protected Tooltip computeValue() {
                return new Tooltip(uiSql.getText());
            }
        });
        uiSql.editableProperty().bind(modifiableProperty());
    }
    
    public SQLQuery getSQLQuery() {
        return sqlQueryProperty.get();
    }

    public final BooleanProperty modifiableProperty(){return modifiableProperty;}
    
    public void setSQLQuery(final SQLQuery newValue) {
        
        final SQLQuery oldValue = sqlQueryProperty.get();
        if (oldValue != null) {
            uiLibelle.textProperty().unbindBidirectional(oldValue.libelleProperty());
            uiDesc.textProperty().unbindBidirectional(oldValue.descriptionProperty());
            uiSql.textProperty().unbindBidirectional(oldValue.sqlProperty());
        }
        
        sqlQueryProperty.set(newValue);
        if (newValue != null) {
            uiLibelle.textProperty().bindBidirectional(newValue.libelleProperty());
            uiDesc.textProperty().bindBidirectional(newValue.descriptionProperty());
            uiSql.textProperty().bindBidirectional(newValue.sqlProperty());
        }
    }
}
