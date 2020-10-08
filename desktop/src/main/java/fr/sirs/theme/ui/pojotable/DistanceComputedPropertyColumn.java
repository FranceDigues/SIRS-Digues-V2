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

import fr.sirs.core.model.Element;
import fr.sirs.core.model.LigneEau;
import fr.sirs.core.model.PointDZ;
import fr.sirs.core.model.PointXYZ;
import fr.sirs.core.model.Positionable;
import fr.sirs.core.model.PrZPointImporter;
import fr.sirs.core.model.ProfilLong;
import fr.sirs.theme.ui.PojoTablePointBindings;
import javafx.beans.property.ObjectProperty;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.Callback;

/**
 *
 * @author Samuel Andrés (Geomatys) [extraction de la PojoTable]
 */
public class DistanceComputedPropertyColumn extends TableColumn<Element, Double>{

        private boolean titleSet = false;

        public DistanceComputedPropertyColumn(Callback cellFactory, ObjectProperty<Element> parentElementProperty, final TableView<Element> uiTable){
            setCellFactory(cellFactory);
            setEditable(false);
            setText("Valeur calculée");
            setCellValueFactory((TableColumn.CellDataFeatures<Element, Double> param) -> {
                if(param.getValue() instanceof PointXYZ){
                    // Cas des XYZ de profils en long et des lignes d'eau avec PR calculé
                    if(parentElementProperty.get() instanceof ProfilLong
                            || parentElementProperty.get() instanceof LigneEau){
                        if(!titleSet){setText("PR calculé");titleSet=true;}
                        return new PojoTablePointBindings.PRXYZBinding((PointXYZ) param.getValue(), (Positionable) parentElementProperty.get()).asObject();
                    }
                    // Cas des XYZ de levés de profils en travers avec distance calculée
                    else {
                        final Element origine = getTableView().getItems().get(0);
                        if(origine instanceof PointXYZ){
                            if(!titleSet){setText("Distance calculée");titleSet=true;}
                            return new PojoTablePointBindings.DXYZBinding((PointXYZ) param.getValue(), (PointXYZ) origine).asObject();
                        }
                        else{
                            // Sinon la colonne ne sert à rien et on la retire dès que possible.
                            if(uiTable.getColumns().contains(this)) {
                                uiTable.getColumns().remove(this);
                            }
                            return null;
                        }
                    }
                }

                // Das des PrZ de profils en long ou lignes d'eau avec PR saisi converti en PR calculé dans le SR par défaut
                else if(param.getValue() instanceof PointDZ
                        && parentElementProperty.get() instanceof PrZPointImporter
                        && parentElementProperty.get() instanceof Positionable){
                    if(!titleSet){setText("PR calculé"); titleSet=true;}
                    return new PojoTablePointBindings.PRZBinding((PointDZ) param.getValue(), (Positionable) parentElementProperty.get()).asObject();
                }
                else {
                    // Sinon la colonne ne sert à rien et on la retire dès que possible.
                    if(uiTable.getColumns().contains(this)){
                        uiTable.getColumns().remove(this);
                    }
                    return null;
                }
            });
        }
    
}
