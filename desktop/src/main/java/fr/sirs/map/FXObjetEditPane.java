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
package fr.sirs.map;

import fr.sirs.SIRS;
import fr.sirs.core.model.BorneDigue;
import fr.sirs.core.model.Element;
import fr.sirs.core.model.Objet;
import fr.sirs.core.model.Preview;
import fr.sirs.core.model.SystemeReperage;
import fr.sirs.core.model.TronconDigue;
import fr.sirs.theme.ui.PojoTableChoiceStage;
import fr.sirs.theme.ui.pojotable.ChoiceStage;
import fr.sirs.ui.Growl;
import java.util.ArrayList;
import java.util.logging.Level;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.ImageView;
import org.geotoolkit.gui.javafx.render2d.FXMap;
import org.geotoolkit.gui.javafx.util.TaskManager;

/**
 *
 * @author Johann Sorel (Geomatys)
 * @author Matthieu Bastianelli (Geomatys)
 * @param <T>
 */
public class FXObjetEditPane<T extends Objet> extends FXAbstractEditOnTronconPane<T> {

    @FXML ToggleButton uiSelectTroncon;
    @FXML ComboBox<String> uiGeomTypeBox;
    @FXML ToggleButton uiModifyObjet;

    final ObjectProperty<String> geometryTypeProperty;

    final ObjectProperty<T> selectedObjetProperty =new SimpleObjectProperty<>();

    /**
     *
     * @param map
     * @param typeName
     * @param clazz
     */
    public FXObjetEditPane(FXMap map, final String typeName, final Class clazz) {
        super(map, typeName, clazz, true);


        //etat des boutons sélectionné
        final ToggleGroup group = new ToggleGroup();
        uiPickTroncon.setToggleGroup(group);
        uiCreateObjet.setToggleGroup(group);
        uiModifyObjet.setToggleGroup(group);

        mode.addListener((observable, oldValue, newValue) -> {
            switch ((EditModeObjet) newValue) {
                case CREATE_OBJET:
                    group.selectToggle(uiCreateObjet);
                    break;
                case PICK_TRONCON:
                    group.selectToggle(uiPickTroncon);
                    break;
                case EDIT_OBJET:
                    group.selectToggle(uiModifyObjet);
                    break;
                default:
                    group.selectToggle(null);
                    break;
            }
        });

        uiModifyObjet.setOnAction(this::modifyObjet);


        uiGeomTypeBox.setItems(FXCollections.observableArrayList("Linéaire", "Ponctuel"));
        uiGeomTypeBox.getSelectionModel().selectFirst();
        geometryTypeProperty = new SimpleObjectProperty<>(getGeomType());
         uiGeomTypeBox.getSelectionModel().selectedItemProperty().addListener((o, old, n) -> {
            geometryTypeProperty.setValue(getGeomType());
        });
        uiObjetTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);


        uiSelectTroncon.setOnAction(e -> {
            final ObservableList<Preview> tronconPreviews = SIRS.observableList(new ArrayList<>(session.getPreviews().getByClass(TronconDigue.class)));
            final PojoTableChoiceStage<Element> stage = new ChoiceStage(session.getRepositoryForClass(TronconDigue.class), tronconPreviews, null, "Choix du tronçon d'appartenance", "Choisir");
            stage.showAndWait();
            tronconProp.setValue( (TronconDigue) stage.getRetrievedElement().get());
        });

        uiSelectTroncon.setGraphic(new ImageView(SIRS.ICON_ARROW_RIGHT_BLACK));

        tronconProp.addListener(this::updateObjetTable);

        uiObjetTable.getSelectionModel().selectedItemProperty().addListener((obs,old,newV) -> focusOnTabSelection());
    }

    private void focusOnTabSelection() {
        final Preview selected = uiObjetTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            final T tofocusOn = repo.get(selected.getDocId());
            if (tofocusOn != null) {
                final FXMapTab tab = session.getFrame().getMapTab();
                tab.getMap().focusOnElement(tofocusOn);
                tab.show();

                //On passe en mode mofification pour ne pas créer un nouvel élément:
                mode.setValue(EditModeObjet.EDIT_OBJET);
                geometryTypeProperty.setValue(getGeomType());
                selectedObjetProperty.setValue(tofocusOn);
            }
        }
    }

    /**
     * Retourne le type de géométrie à éditer
     * @return
     */
    public final String getGeomType() {
        return uiGeomTypeBox.getSelectionModel().getSelectedItem();
    }

    /**
     * Mode modification d'objets
     *
     * @param evt
     */
    void modifyObjet(ActionEvent evt) {
            mode.setValue(EditModeObjet.EDIT_OBJET);
            geometryTypeProperty.setValue(getGeomType());
    }

    /**
     * Création d'un élément
     *
     */
//    @Override
    public void createObjet() { //uniquement un point ici, on veut pouvoir éditer un segment!

        if (getTronconFromProperty() == null) {
            Growl alert = new Growl(Growl.Type.WARNING, "Pour créer un nouvel élément, veuillez sélectionner un tronçon d'appartenance");
            alert.showAndFade();
            mode.setValue(EditModeObjet.PICK_TRONCON);
        } else {
            geometryTypeProperty.setValue(getGeomType());
            mode.setValue(EditModeObjet.CREATE_OBJET);
        }
    }

    /**
     * Open a {@link ListView} to allow user to select one or more
     * {@link BorneDigue} to delete.
     *
     * Note : Once suppression is confirmed, we're forced to check all
     * {@link SystemeReperage} defined on the currently edited
     * {@link TronconDigue}, and update them if they use chosen bornes.
     *
     * @param e Event fired when deletion button has been fired.
     */
    @FXML
    @Override
    void deleteObjets(ActionEvent e) {
        throw new UnsupportedOperationException("Unsupported deleteObjets() yet.");
    }


    /*
     * TABLE UTILITIES
     */
    /**
     * Met à jour les éléments de la liste à partir du tronçon sélectionné.
     *
     * @param observable
     * @param oldValue
     * @param newValue
     */
    void updateObjetTable(ObservableValue<? extends TronconDigue> observable, TronconDigue oldValue, TronconDigue newValue) {

        if (newValue == null) {
            uiObjetTable.setItems(FXCollections.emptyObservableList());
        } else {
            final EditModeObjet current = getMode();
            if (current.equals(EditModeObjet.CREATE_OBJET) || current.equals(EditModeObjet.EDIT_OBJET)) {
                //do nothing
            } else {
                mode.set(EditModeObjet.EDIT_OBJET);
            }

            final FXAbstractEditOnTronconPane.FindObjetsOnTronconTask task = new FXAbstractEditOnTronconPane.FindObjetsOnTronconTask(null);
            TaskManager.INSTANCE.submit(task);
            task.setOnSucceeded(evt -> uiObjetTable.setItems((ObservableList<Preview>) task.getValue()));
            task.setOnFailed(eh -> SIRS.LOGGER.log(Level.WARNING, "Echec de la tâche de récupération des éléments du tronçon"));


        }
    }

}
