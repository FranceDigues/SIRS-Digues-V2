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
import fr.sirs.SIRS;
import fr.sirs.core.component.AbstractSIRSRepository;
import fr.sirs.core.model.AbstractObservation;
import fr.sirs.core.model.Desordre;
import fr.sirs.core.model.Observation;
import fr.sirs.core.model.RefCategorieDesordre;
import fr.sirs.core.model.RefTypeDesordre;
import fr.sirs.core.model.Role;
import fr.sirs.theme.ui.pojotable.PojoTableExternalAddable;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

/**
 *
 * @author Samuel Andrés (Geomatys)
 */
public class FXDesordrePane extends FXDesordrePaneStub {

    public FXDesordrePane(final Desordre desordre){
        super(desordre);

        // Demande SYM-1117 : la sélection d'une catégorie détermine la liste des types de désordres
        // Update available types according to chosen category.
        final AbstractSIRSRepository<RefTypeDesordre> typeRepo = Injector.getSession().getRepositoryForClass(RefTypeDesordre.class);
        ui_categorieDesordreId.getSelectionModel().selectedItemProperty().addListener((ObservableValue observable, Object oldValue, Object newValue) -> {

            final ObservableList<RefTypeDesordre> typeList = SIRS.observableList(typeRepo.getAll());
            if (newValue instanceof RefCategorieDesordre) {
                final FilteredList<RefTypeDesordre> fList = typeList.filtered(type -> type.getCategorieId().equals(((RefCategorieDesordre) newValue).getId()));
                ui_typeDesordreId.setItems(fList);
            } else {
                ui_typeDesordreId.setItems(typeList);
            }
        });

        // Demande SYM-1669 : la sélection d'un type de désordre doit contraintre la catégorie
        ui_typeDesordreId.getSelectionModel().selectedItemProperty().addListener((ObservableValue observable, Object oldValue, Object newValue) -> {

            if(newValue!=null){
                final RefTypeDesordre selectedType = (RefTypeDesordre) newValue;
                final String newCategoryId = selectedType.getCategorieId();
                if(newCategoryId!=null){
                    final RefCategorieDesordre oldCategory = (RefCategorieDesordre) ui_categorieDesordreId.getSelectionModel().getSelectedItem();
                    if(oldCategory==null || (oldCategory!=null && !newCategoryId.equals(oldCategory.getId()))){
                        final ObservableList<RefCategorieDesordre> items = ui_categorieDesordreId.getItems();
                        for(final RefCategorieDesordre category : items){
                            if(newCategoryId.equals(category.getId())){
                                ui_categorieDesordreId.getSelectionModel().select(category);
                                break;
                            }
                        }
                    }
                }
            }
        });

//        /*
// -> removed here to be integrated in fxml-controller.jet
//
//        /!\/!\/!\ HACK /!\/!\/!\ HACK /!\/!\/!\ HACK /!\/!\/!\ HACK /!\/!\/!\
//
//        SYM-1727 : on souhaite que le rôle externe puisse ajouter des observations
//        même dans les désordres dont il n'est pas l'auteur.
//
//        Pour cela, il faut surcharger la création du tableau définie
//        dans le constructeur de la classe-mère.
//        */
//
//        ui_observations.setContent(() -> {
//            observationsTable = new PojoTableExternalAddable(Observation.class, elementProperty());
//            observationsTable.editableProperty().bind(disableFieldsProperty().not());
//            updateObservationsTable(Injector.getSession(), elementProperty.get());
//            return observationsTable;
//        });

    }
}
