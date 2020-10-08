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
import fr.sirs.StructBeanSupplier;
import fr.sirs.core.component.AbstractSIRSRepository;
import fr.sirs.core.model.ArbreVegetation;
import fr.sirs.core.model.Element;
import fr.sirs.core.model.GeometryType;
import fr.sirs.core.model.InvasiveVegetation;
import fr.sirs.core.model.PeuplementVegetation;
import fr.sirs.core.model.TraitementZoneVegetation;
import fr.sirs.core.model.ZoneVegetation;
import fr.sirs.plugin.vegetation.PluginVegetation;
import static fr.sirs.plugin.vegetation.PluginVegetation.DEFAULT_INVASIVE_VEGETATION_TYPE;
import static fr.sirs.plugin.vegetation.PluginVegetation.DEFAULT_PEUPLEMENT_VEGETATION_TYPE;
import static fr.sirs.plugin.vegetation.PluginVegetation.paramTraitement;
import java.util.ArrayList;
import java.util.function.Consumer;
import javafx.beans.property.ObjectProperty;
import javafx.scene.control.TableColumn;
import javafx.stage.Modality;

/**
 *
 * @author Samuel Andrés (Geomatys)
 */
public class ZoneVegetationPojoTable extends ListenPropertyPojoTable<String> {

    public ZoneVegetationPojoTable(String title, final ObjectProperty<? extends Element> container) {
        super(ZoneVegetation.class, title, container);
        setDeletor(new Consumer<Element>() {

            @Override
            public void accept(Element pojo) {
                if(pojo instanceof ZoneVegetation) ((AbstractSIRSRepository) Injector.getSession().getRepositoryForClass(pojo.getClass())).remove(pojo);
            }
        });
    }

    @Override
    protected StructBeanSupplier getStructBeanSupplier(){
        return new StructBeanSupplier(pojoClass, "documentId", () -> new ArrayList(uiTable.getSelectionModel().getSelectedItems()));
    }

    @Override
    protected ZoneVegetation createPojo() {

        final ZoneVegetation zone;

        final ChoiceStage stage = new ChoiceStage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.showAndWait();

        final Class<? extends ZoneVegetation> retrievedClass = stage.getRetrievedElement().get();
        if(retrievedClass!=null){
            //Création de la zone
            final AbstractSIRSRepository zoneVegetationRepo = Injector.getSession().getRepositoryForClass(retrievedClass);
            zone = (ZoneVegetation) zoneVegetationRepo.create();
            zone.setForeignParentId(getPropertyReference());
            zoneVegetationRepo.add(zone);
            getAllValues().add(zone);

            //Création du traitement associé
            zone.setTraitement(Injector.getSession().getElementCreator().createElement(TraitementZoneVegetation.class));

            // S'il s'agit d'une zone d'invasive ou de peuplement, il faut affecter le type par défaut et effectuer le paramétrage éventuel

            if(retrievedClass==PeuplementVegetation.class){
                ((PeuplementVegetation) zone).setTypeVegetationId(DEFAULT_PEUPLEMENT_VEGETATION_TYPE);
                paramTraitement(PeuplementVegetation.class, (PeuplementVegetation) zone, DEFAULT_PEUPLEMENT_VEGETATION_TYPE);
            }
            else if(retrievedClass==InvasiveVegetation.class){
                ((InvasiveVegetation) zone).setTypeVegetationId(DEFAULT_INVASIVE_VEGETATION_TYPE);
                paramTraitement(InvasiveVegetation.class, (InvasiveVegetation) zone, DEFAULT_INVASIVE_VEGETATION_TYPE);
            }
            else if(retrievedClass==ArbreVegetation.class){
                zone.setGeometryType(GeometryType.PONCTUAL);
            }

        }
        else {
            zone = null;
        }
        return zone;
    }

    @Override
    protected void elementEdited(TableColumn.CellEditEvent<Element, Object> event){
        final Element obj = event.getRowValue();
        if(obj != null){
            ((AbstractSIRSRepository) Injector.getSession().getRepositoryForClass(obj.getClass())).update(obj);
        }
    }


    /**
     * Window allowing to define the type of ZoneVegetation at creation stage.
     */
    private static class ChoiceStage extends PojoTableComboBoxChoiceStage<Class<? extends ZoneVegetation>, Class<? extends ZoneVegetation>> {

        private ChoiceStage(){
            super();
            setTitle("Choix du type de zone");
            comboBox.setItems(PluginVegetation.zoneVegetationClasses());
            retrievedElement.bind(comboBox.getSelectionModel().selectedItemProperty());
        }
    }
}
