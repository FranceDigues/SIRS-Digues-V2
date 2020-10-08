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
package fr.sirs.plugin.vegetation;

import fr.sirs.Injector;
import fr.sirs.SIRS;
import fr.sirs.Session;
import fr.sirs.core.component.ParcelleVegetationRepository;
import fr.sirs.core.component.Previews;
import fr.sirs.core.model.Element;
import fr.sirs.core.model.ParcelleVegetation;
import fr.sirs.core.model.PlanVegetation;
import fr.sirs.core.model.Preview;
import fr.sirs.core.model.TraitementParcelleVegetation;
import fr.sirs.core.model.TronconDigue;
import fr.sirs.theme.ui.PojoTable;
import fr.sirs.util.SirsStringConverter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.cell.ComboBoxListCell;
import javafx.scene.layout.BorderPane;
import org.geotoolkit.gui.javafx.util.ComboBoxCompletion;

/**
 *
 * @author Samuel Andrés (Geomatys)
 * @author Johann Sorel (Geomatys)
 */
public class FXTraitementsPane extends SplitPane {

    private final Session session = Injector.getSession();
    private final Previews previews = session.getPreviews();
    private final ParcelleVegetationRepository parcellesRepo = VegetationSession.INSTANCE.getParcelleRepo();

    @FXML ComboBox ui_linear;
    @FXML ListView<ParcelleVegetation> ui_parcelles;
    @FXML SplitPane uiSplitPane;
    private Node rightPane = new BorderPane();


    public FXTraitementsPane() {
        final ResourceBundle bundle = null;
        SIRS.loadFXML(this, bundle);

        final List<Preview> tronconsPreviews = previews.getByClass(TronconDigue.class);
        final List troncons = new ArrayList(tronconsPreviews);// Pour ne pas modifier la liste de "Previews" avec l'ajout de la ligne suivante.
        troncons.add(0,"-Tous les tronçons-");
        ui_linear.setConverter(new SirsStringConverter());
        ui_linear.setEditable(true);
        ui_linear.setItems(FXCollections.observableArrayList(troncons));
        ui_linear.getSelectionModel().selectedItemProperty().addListener(this::tronconChanged);
        ComboBoxCompletion.autocomplete(ui_linear);

        ui_parcelles.setCellFactory(ComboBoxListCell.forListView(new SirsStringConverter()));
        ui_parcelles.getSelectionModel().selectedItemProperty().addListener(this::parcelleChanged);

        uiSplitPane.getItems().add(rightPane);

        //list to plan change
        VegetationSession.INSTANCE.planProperty().addListener((ObservableValue<? extends PlanVegetation> observable, PlanVegetation oldValue, PlanVegetation newValue) -> planChanged());
        planChanged();
    }

    private void planChanged(){
        tronconChanged(null, null, ui_linear.getValue());
    }

    private void tronconChanged(ObservableValue<? extends Object> observable, Object oldValue, Object newValue){
        final PlanVegetation plan = VegetationSession.INSTANCE.planProperty().get();

        List<ParcelleVegetation> parcelles;
        if(plan==null){
            parcelles = Collections.EMPTY_LIST;
        }else if(newValue instanceof Preview){
            parcelles = parcellesRepo.getByLinearId(((Preview)newValue).getElementId());

            //on enleve les parcelles qui ne font pas partie de ce plan.
            for(int i=parcelles.size()-1;i>=0;i--){
                if(!parcelles.get(i).getPlanId().equals(plan.getDocumentId())){
                    parcelles.remove(i);
                }
            }

        }else{
            parcelles = parcellesRepo.getByPlan(plan);
        }
        ui_parcelles.setItems(FXCollections.observableList(parcelles));
    }

    private void parcelleChanged(ObservableValue<? extends ParcelleVegetation> observable, ParcelleVegetation oldValue, ParcelleVegetation newValue){
        uiSplitPane.getItems().remove(rightPane);

        if(newValue==null){
            rightPane = new BorderPane();
        }else{
            final PojoTable traitementsTable = new PojoTable(TraitementParcelleVegetation.class, null, (ObjectProperty<? extends Element>) null);
            traitementsTable.setParentElement(newValue);
            traitementsTable.setTableItems(()-> (ObservableList) newValue.getTraitements());
            rightPane = traitementsTable;
        }
        uiSplitPane.getItems().add(rightPane);

    }

}
