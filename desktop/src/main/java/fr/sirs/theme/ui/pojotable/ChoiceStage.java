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
package fr.sirs.theme.ui.pojotable;

import fr.sirs.core.component.AbstractSIRSRepository;
import fr.sirs.core.model.Element;
import fr.sirs.core.model.Preview;
import fr.sirs.theme.ui.PojoTableComboBoxChoiceStage;
import javafx.beans.binding.ObjectBinding;
import javafx.collections.ObservableList;

/**
 *
 * @author Samuel Andrés (Geomatys) [extraction de la PojoTable]
 */
public class ChoiceStage extends PojoTableComboBoxChoiceStage<Element, Preview> {

    public ChoiceStage(AbstractSIRSRepository repo, ObservableList<Preview> items){
        super();
        setTitle("Choix de l'élément");

        comboBox.setItems(items);
        retrievedElement.bind(new ObjectBinding<Element>() {

            {
                bind(comboBox.getSelectionModel().selectedItemProperty());
            }

            @Override
            protected Element computeValue() {
                if(comboBox.valueProperty()!=null){
                    final Preview preview = comboBox.valueProperty().get();
                    if(preview!=null){
                        return (Element) repo.get(preview.getDocId());
                    }
                }
                return null;
            }
        });
    }
}
