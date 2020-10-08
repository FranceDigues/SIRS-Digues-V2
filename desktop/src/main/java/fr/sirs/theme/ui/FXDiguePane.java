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

import fr.sirs.Injector;
import fr.sirs.Session;
import fr.sirs.core.component.TronconDigueRepository;
import fr.sirs.core.model.Digue;
import fr.sirs.core.model.Element;
import fr.sirs.core.model.TronconDigue;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Samuel Andrés (Geomatys)
 */
public class FXDiguePane extends FXDiguePaneStub {

    @Autowired private Session session;

    @FXML private VBox centerContent;

    private final TronconPojoTable table;


    protected FXDiguePane() {
        super();
        Injector.injectDependencies(this);
        table = new TronconPojoTable(elementProperty());
        table.editableProperty().set(false);
        table.parentElementProperty().bind(elementProperty);
        centerContent.getChildren().add(table);
    }

    public FXDiguePane(final Digue digue){
        this();
        this.elementProperty().set(digue);
    }

    /**
     *
     * @param observable
     * @param oldValue
     * @param newValue
     */
    @Override
    public void initFields(ObservableValue<? extends Digue> observable, Digue oldValue, Digue newValue) {
        super.initFields(observable, oldValue, newValue);
        if (newValue != null) {
            table.setTableItems(()->FXCollections.observableArrayList(((TronconDigueRepository) session.getRepositoryForClass(TronconDigue.class)).getByDigue(newValue)));
        }
    }

    private class TronconPojoTable extends PojoTable {

        public TronconPojoTable(final ObjectProperty<? extends Element> container) {
            super(TronconDigue.class, "Tronçons de la digue", container);
            createNewProperty.set(false);
            fichableProperty.set(false);
            uiAdd.setVisible(false);
            uiFicheMode.setVisible(false);
            uiDelete.setVisible(false);
            setDeletor(input -> {
                if (input instanceof TronconDigue) {
                    ((TronconDigue)input).setDigueId(null);
                    session.getRepositoryForClass((Class)input.getClass()).update(input);
                }
            });
        }

        @Override
        protected TronconDigue createPojo() {
            throw new UnsupportedOperationException("Vous ne devez pas créer de tronçon à partir d'ici !");
        }
    }
}
