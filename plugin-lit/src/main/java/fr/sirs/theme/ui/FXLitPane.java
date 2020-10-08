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

import static fr.sirs.CorePlugin.initTronconDigue;
import fr.sirs.Injector;
import fr.sirs.Session;
import fr.sirs.core.component.TronconLitRepository;
import fr.sirs.core.model.Element;
import fr.sirs.core.model.Lit;
import fr.sirs.core.model.TronconLit;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Olivier Nouguier (Geomatys)
 * @author Alexis Manin (Geomatys)
 */
public class FXLitPane extends FXLitPaneStub {
    @Autowired private Session session;

    @FXML protected VBox contentBox;

    private final TronconLitPojoTable table;

    /**
     * Constructor. Initialize part of the UI which will not require update when
     * element edited change.
     */
    protected FXLitPane() {
        super();
        Injector.injectDependencies(this);
        this.table = new TronconLitPojoTable(elementProperty());
        table.parentElementProperty().bind(elementProperty);
        contentBox.getChildren().add(table);
    }

    public FXLitPane(final Lit lit){
        this();
        this.elementProperty().set(lit);
    }

    /**
     * Initialize fields at element setting.
     * @param observableElement
     * @param oldElement
     * @param newElement
     */
    @Override
    protected void initFields(ObservableValue<? extends Lit > observableElement, Lit oldElement, Lit newElement) {
        super.initFields(observableElement, oldElement, newElement);
        if(newElement!=null){
            table.setTableItems(()->FXCollections.observableArrayList(
                ((TronconLitRepository) session.getRepositoryForClass(TronconLit.class)).getByLit(newElement)));
        }
    }

    private class TronconLitPojoTable extends PojoTable {

        public TronconLitPojoTable(final ObjectProperty<? extends Element> container) {
            super(TronconLit.class, "Tronçon du lit", container);
            editableProperty().unbind();
            editableProperty().set(false);
        }

        @Override
        protected TronconLit createPojo() {
            TronconLit result = (TronconLit) super.createPojo();
            if (elementProperty().get() != null) {
                ((TronconLit) result).setLitId(elementProperty().get().getId());
            }
            initTronconDigue(result, Injector.getSession());
            return result;
        }
    }
}
