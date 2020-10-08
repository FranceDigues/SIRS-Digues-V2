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
import fr.sirs.core.model.Element;
import fr.sirs.core.model.Objet;
import fr.sirs.core.model.PositionConvention;
import fr.sirs.core.model.TronconDigue;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;

/**
 *
 * @author Samuel Andrés (Geomaty)
 */
public class PositionConventionPojoTable extends ListenPropertyPojoTable<String>{

    public PositionConventionPojoTable(String title, final ObjectProperty<? extends Element> container) {
        super(PositionConvention.class, title, container);
    }

    @Override
    protected PositionConvention createPojo() {
        final Alert choice = new Alert(Alert.AlertType.NONE,
                "Le positionnement de convention est associable à un objet.\n"
                        + "L'association à un objet est facultative et peut être réalisée ultérieurement.\n"
                        + "Voulez-vous associer un objet maintenant ?\n",
                ButtonType.YES, ButtonType.NO);
        choice.setResizable(true);
        final Optional<ButtonType> result = choice.showAndWait();

        final PositionConvention position;
        if(result.isPresent()&&result.get()==ButtonType.YES){

            final ChoiceStage stage = new ChoiceStage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
            final Element retrievedElement = stage.getRetrievedElement().get();

            final TronconDigue troncon;
            if(retrievedElement instanceof Objet && ((Objet) retrievedElement).getLinearId()!=null){
                troncon = Injector.getSession().getRepositoryForClass(TronconDigue.class).get(((Objet) retrievedElement).getLinearId());
            }
            else {
                troncon = Injector.getSession().getRepositoryForClass(TronconDigue.class).getOne();
            }

            position = (PositionConvention) super.createPojo(troncon);


            if(retrievedElement instanceof Objet){
                final Objet retrievedObjet = (Objet) retrievedElement;
                position.setSystemeRepId(retrievedObjet.getSystemeRepId());
                position.setPrDebut(retrievedObjet.getPrDebut());
                position.setPrFin(retrievedObjet.getPrFin());
//                final LineString positionGeometry = LinearReferencingUtilities.buildSubGeometry(retrievedObjet, position,
//                        Injector.getSession().getRepositoryForClass(BorneDigue.class),
//                        Injector.getSession().getRepositoryForClass(SystemeReperage.class));
                position.setPositionDebut(retrievedObjet.getPositionDebut());
                position.setPositionFin(retrievedObjet.getPositionFin());
                position.setGeometry(retrievedObjet.getGeometry());
                position.setLinearId(retrievedObjet.getLinearId());
                position.setObjetId(retrievedElement.getId());
            }


            try {
                ((Property<String>) propertyMethodToListen.invoke(position)).setValue(propertyReference);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                Logger.getLogger(PositionDocumentPojoTable.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            final TronconDigue premierTroncon = Injector.getSession().getRepositoryForClass(TronconDigue.class).getOne();
            position = (PositionConvention) super.createPojo(premierTroncon);

            try {
                ((Property<String>) propertyMethodToListen.invoke(position)).setValue(propertyReference);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                Logger.getLogger(PositionDocumentPojoTable.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        session.getRepositoryForClass(PositionConvention.class).update(position);
        return position;
    }

    private static class ChoiceStage extends PojoTableChoiceStage<Element> {

        private final Button ui_add;
        private final Button ui_cancel;

        private ChoiceStage(){
            super();
            setTitle("Choix de l'élément");
            final FXPositionConventionChoicePane positionConventionChoicePane = new FXPositionConventionChoicePane();
            ui_add = new Button("Ajouter");
            ui_cancel = new Button("Annuler");

            final HBox hBox = new HBox(20., ui_add, ui_cancel);
            hBox.setAlignment(Pos.CENTER);
            hBox.setPadding(new Insets(10));

            setScene(new Scene(new VBox(positionConventionChoicePane, hBox)));
            retrievedElement.bind(positionConventionChoicePane.selectedObjetProperty());

            ui_add.setOnAction((ActionEvent event) -> hide());
            ui_cancel.setOnAction((ActionEvent event) -> {
                retrievedElement.unbind();
                retrievedElement.set(null);
                hide();
                    });
        }

    }
}
