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
package fr.sirs;

import static fr.sirs.SIRS.COLOR_INVALID_ICON;
import static fr.sirs.SIRS.ICON_CHECK_CIRCLE;
import static fr.sirs.SIRS.ICON_EXCLAMATION_CIRCLE;
import fr.sirs.core.model.Element;
import java.io.IOException;
import java.util.function.Predicate;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.apache.sis.util.ArgumentChecks;

/**
 * Composant de controle de l'édition des fiches.
 *
 * @author Johann Sorel (Geomatys)
 */
public class FXEditMode extends VBox {

    private static final String VALID_TEXT = "Validé";
    private static final String INVALID_TEXT = "Invalidé";

    @FXML protected HBox uiValidationBox;
    @FXML protected ImageView uiImageValid;
    @FXML protected Label uiLabelValid;
    @FXML protected ToggleButton uiEdit;
    @FXML protected Button uiSave;
    @FXML protected ToggleButton uiConsult;

    private final Session session = Injector.getBean(Session.class);
    private final StringProperty authorIDProperty;
    private final BooleanProperty validProperty;

    private Runnable saveAction;

    /**
     * A binding defining if current user is not allowed to edit current element.
     * True : read-only
     * False : read-write
     */
    private final BooleanBinding editionProhibited;

    public FXEditMode() {
        
        // 1- chargement du FXML associé
        final Class cdtClass = FXEditMode.class;
        final String fxmlpath = "/"+cdtClass.getName().replace('.', '/')+".fxml";
        final FXMLLoader loader = new FXMLLoader(cdtClass.getResource(fxmlpath));
        loader.setController(this);
        loader.setRoot(this);
        //in special environement like osgi or other, we must use the proper class loaders
        //not necessarly the one who loaded the FXMLLoader class
        loader.setClassLoader(cdtClass.getClassLoader());
        try {
            loader.load();
        } catch (IOException ex) {
            throw new IllegalArgumentException(ex.getMessage(), ex);
        }

        // 2- initialisation des propriétés auteur/validité
        authorIDProperty = new SimpleStringProperty();
        validProperty = new SimpleBooleanProperty();
        validProperty.addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
                resetValidUIs(newValue);
            });
        validProperty.set(true);

        // 3- initialisation du contrôle de l'édition/consultation
        
        // on récupère le binding indiquant si l'édition doit être bloquée
        editionProhibited = initEditionProhibition();
        
        /*
        Les changements d'utilisateurs peuvent provoquer la récupération des panneaux 
        en cache. Il faut donc réinitialiser en conséquence l'état d'édition/consultation.
        L'action est susceptible de varier.
        */
        editionProhibited.addListener(editionListener());
        
        // on fige les fonctionnalités de changement de mode si et seulement si l'édition est interdite
        uiEdit.disableProperty().bind(editionProhibited);
        uiConsult.disableProperty().bind(editionProhibited);
        
        // On peut enregistrer si et seulement si le mode édition est sélectionné
        uiSave.disableProperty().bind(uiEdit.selectedProperty().not());

        final ToggleGroup group = new ToggleGroup();
        uiConsult.setToggleGroup(group);
        uiEdit.setToggleGroup(group);
        
         // Par défaut, on est en mode consultation.
         setToConsult();
    }
    
    protected final void setToConsult(){
        uiEdit.setSelected(false);
        uiConsult.setSelected(true);
    }
    
    /**
     * Dans les panneaux d'éléments, on écoute l'invalidation de la propriété
     * d'interdiction de l'édition et on passe en mode consultation uniquement si
     * on n'a plus le droit d'éditer.
     * @return 
     */
    protected InvalidationListener editionListener(){
        return (Observable observable) -> {
            if(editionProhibited.get()){
                setToConsult();
            }};
    }

    /**
     * Détermine la condition sous laquelle le passage en mode édition est interdit.
     *
     * Dans le cas général des fiches, on interdit l'édition des fiches si :
     *  - la session indique que les éléments créés sont dans un état "invalidé" (c'est le cas du rôle "externe").
     *  ET
     *  (
     *  - l'état de l'élément consulté est "validé" (le role "externe" n'a plus la main sur les éléments "validés", qu'il en soit l'auteur ou non).
     *  OU
     *  - l'utilisateur connecté n'est pas l'auteur de l'élément créé (parmi les éléments "invalidés", le role "externe" n'a la main que sur ceux dont il l'utilisateur courant est l'auteur).
     *  )
     *
     * @return  Le binding de contrôle du bouton d'édition.
     */
    protected BooleanBinding initEditionProhibition() {
        
        /*
        on bloque l'édition si l'utilisateur ne crée pas des documents valides 
        d'office c'est à dire s'il n'est ni ADMIN ni USER (i.e. donc s'il est 
        EXTERN ou GUEST).
        */
        return session.createValidDocuments().not().and(
            /*
            Mais pour bloquer l'édition, on veut aussi que le document soit 
            valide ou que l'utilisateur courant n'en soit pas l'auteur
            (i.e. on autorise l'édition des documents invalides par leur auteur
            même s'il ne s'agit pas d'un ADMIN ou d'un USER).
            */
            validProperty.or(
                authorIDProperty.isNotEqualTo(session.userIdBinding())
            )
        );
    }
            
    /**
     * Mise à jour de l'indication de validité.
     *
     * @param valid
     */
    private void resetValidUIs(final boolean valid){
        if(valid){
            uiImageValid.setImage(ICON_CHECK_CIRCLE);
            uiLabelValid.setText(VALID_TEXT);
            uiLabelValid.setTextFill(Color.WHITE);
        } else {
            uiImageValid.setImage(ICON_EXCLAMATION_CIRCLE);
            uiLabelValid.setText(INVALID_TEXT);
            uiLabelValid.setTextFill(Color.valueOf(COLOR_INVALID_ICON));
        }
    }

    // indique l'auteur de l'élément dont l'édition est contrôlée
    public StringProperty authorIDProperty(){return authorIDProperty;}
    
    // indique la validité de l'élément dont l'édition est contrôlée
    public BooleanProperty validProperty(){return validProperty;}


    public void requireEditionForElement(final Element element, final Predicate<Element> editionPredicate){
        ArgumentChecks.ensureNonNull("element", element);
        if(editionPredicate.test(element)){
            if(!editionProhibited.get()){
                uiEdit.setSelected(true);
            }
            else {
                SIRS.LOGGER.fine("édition non autorisée");
            }
        }
    }

    public void setSaveAction(Runnable saveAction) {
        this.saveAction = saveAction;
    }

    public BooleanProperty editionState(){
        return uiEdit.selectedProperty();
    }

    @FXML
    public void save(ActionEvent event) {
        if(saveAction!=null) saveAction.run();
    }
}
