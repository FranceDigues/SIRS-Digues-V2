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

import fr.sirs.FXEditMode;
import fr.sirs.Injector;
import fr.sirs.SIRS;
import static fr.sirs.SIRS.createFXPaneForElement;
import fr.sirs.Session;
import fr.sirs.core.component.AbstractSIRSRepository;
import fr.sirs.core.model.AvecDateMaj;
import fr.sirs.core.model.AvecGeometrie;
import fr.sirs.core.model.Element;
import fr.sirs.core.model.LabelMapper;
import fr.sirs.core.model.Positionable;
import static fr.sirs.core.model.Role.ADMIN;
import fr.sirs.map.FXMapTab;
import fr.sirs.ui.Growl;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.function.Predicate;
import java.util.logging.Level;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.geotoolkit.internal.GeotkFX;

/**
 *
 * @author Samuel Andrés (Geomatys)
 * @param <T>
 */
public class FXElementContainerPane<T extends Element> extends AbstractFXElementPane<T> {

    private final Session session = Injector.getSession();
    protected FXElementPane specificThemePane;

    @FXML private Label uiHeaderLabel;
    @FXML private Label uiDateMajLabel;
    @FXML private TextField uiDesignation;
    @FXML private Label date_maj;
    @FXML private FXEditMode uiMode;
    @FXML private Button uiShowOnMapButton;

    /**
     * Keep a reference of the document at element initialization. We need it,
     * because if the element parent is switched, we have to update both
     * old and new parent at save.
     */
    private Element originCouchDbDocument;

    public FXElementContainerPane(final T element, final Predicate<Element> newlyCreated) {
        super();
        SIRS.loadFXML(this);
        setFocusTraversable(true);

        uiShowOnMapButton.managedProperty().bind(uiShowOnMapButton.visibleProperty());
        uiDateMajLabel.managedProperty().bind(uiDateMajLabel.visibleProperty());
        date_maj.managedProperty().bind(date_maj.visibleProperty());

        uiMode.setSaveAction(this::save);
        disableFieldsProperty().bind(uiMode.editionState().not());

        uiDesignation.disableProperty().bind(disableFieldsProperty());

        elementProperty.addListener(this::initPane);

        // When requesting edition, focus on designation field.
        uiMode.editionState().addListener((obs, oldValue, newValue) -> {
            if (newValue)
                uiDesignation.requestFocus();
        });

        if (element != null) {
            uiMode.validProperty().bind(element.validProperty());
            uiMode.authorIDProperty().bind(element.authorProperty());
            uiMode.requireEditionForElement(element, newlyCreated);
            setElement((T) element);
            if (uiMode.editionState().get())
                uiDesignation.requestFocus();
        }

        focusedProperty().addListener((obs, oldValue, newValue) -> {
            if (Boolean.TRUE.equals(newValue))
                uiDesignation.requestFocus();
        });
    }

    public void setShowOnMapButton(final boolean isShown){
        uiShowOnMapButton.setVisible(isShown);
    }

    @FXML
    void save() {
        try {
            preSave();

            Element elementDocument = elementProperty.get().getCouchDBDocument();
            if (elementDocument == null) {
                final Alert alert = new Alert(Alert.AlertType.INFORMATION, "Un élément ne peut être sauvegardé sans document valide.", ButtonType.OK);
                alert.setResizable(true);
                alert.show();
                return;
            }

            final AbstractSIRSRepository repo = session.getRepositoryForClass(elementDocument.getClass());
            if (originCouchDbDocument == null) {
                originCouchDbDocument = elementDocument;
            } else if (!originCouchDbDocument.equals(elementDocument)) {
                repo.update(originCouchDbDocument);
                originCouchDbDocument = elementDocument;
            }

            repo.update(originCouchDbDocument);

            final Growl growlInfo = new Growl(Growl.Type.INFO, "Enregistrement effectué.");
            growlInfo.showAndFade();
        } catch (Exception e) {
            final Growl growlError = new Growl(Growl.Type.ERROR, "Erreur survenue pendant l'enregistrement.");
            growlError.showAndFade();
            GeotkFX.newExceptionDialog("L'élément ne peut être sauvegardé.", e).show();
            SIRS.LOGGER.log(Level.WARNING, e.getMessage(), e);
        }
    }

    @FXML
    private void showOnMap() {
        final Element object = elementProperty.get();
        if (object instanceof Positionable || (object instanceof AvecGeometrie && ((AvecGeometrie)object).getGeometry() != null)) {
            final FXMapTab tab = session.getFrame().getMapTab();

            tab.getMap().focusOnElement(object);
            tab.show();
        } else {
            final Alert alert = new Alert(Alert.AlertType.INFORMATION, "L'élément courant n'est pas positionable sur la carte.", ButtonType.OK);
            alert.setResizable(true);
            alert.show();
        }
    }

    protected void initPane(ObservableValue<? extends Element> observable, Element oldValue, Element newValue) {
        // unbind all mono-directional
        date_maj.textProperty().unbind();
        uiDateMajLabel.visibleProperty().unbind();
        uiMode.validProperty().unbind();
        uiMode.authorIDProperty().unbind();

        if (oldValue != null) {
            //unbind all bidirectionnal
            uiDesignation.textProperty().unbindBidirectional(oldValue.designationProperty());
        }

        if (newValue == null) {
            uiDesignation.textProperty().set(null);
            uiHeaderLabel.setText("Aucune information disponible");
            setCenter(new Label("Pas d'éditeur disponible."));
            specificThemePane = null;
        } else {
            try {
                uiHeaderLabel.setText("Informations sur un(e) "+LabelMapper.get(newValue.getClass()).mapClassName());
            } catch (Exception e) {
                SIRS.LOGGER.log(Level.WARNING, "Header label cannot be updated.", e);
                uiHeaderLabel.setText("Informations sur un ouvrage");
            }
            // Keep parent reference, so we can now if it has been switched at save.
            originCouchDbDocument = newValue.getCouchDBDocument();

            // maj
            if (newValue instanceof AvecDateMaj) {
                final ObjectProperty<LocalDate> dateMajProp = ((AvecDateMaj) newValue).dateMajProperty();
                date_maj.textProperty().bind(Bindings.createStringBinding(() -> dateMajProp.get() == null? null : dateMajProp.get().format(DateTimeFormatter.ofPattern("dd/MM/YYYY")), dateMajProp));
                date_maj.setVisible(true);
                uiDateMajLabel.visibleProperty().bind(dateMajProp.isNotNull());
            } else {
                date_maj.setVisible(false);
                uiDateMajLabel.setVisible(false);
            }

            //validation
            uiMode.validProperty().bind(newValue.validProperty());
            uiMode.authorIDProperty().bind(newValue.authorProperty());

            uiDesignation.textProperty().bindBidirectional(newValue.designationProperty());

            // If we previously edited same type of element, we recycle edition panel.
            SIRS.fxRunAndWait(() -> {
                if (specificThemePane != null && oldValue != null && oldValue.getClass().equals(newValue.getClass())) {
                    specificThemePane.setElement(newValue);
                } else {
                    try {
                        specificThemePane = createFXPaneForElement(newValue);
                        specificThemePane.disableFieldsProperty().bind(disableFieldsProperty());
                        if (specificThemePane instanceof FXUtilisateurPane) {
                            ((FXUtilisateurPane) specificThemePane).setAdministrable(ADMIN.equals(session.getRole()));
                        }
                        
                        // If there was a previous FXElementPane, we run the method previous discard.
                        final Node previousPane = getCenter();
                        if(previousPane instanceof FXElementPane){
                            ((FXElementPane) previousPane).preRemove();
                        }
                        
                        // Set the new pane at center place.
                        setCenter((Node) specificThemePane);
                    } catch (Exception ex) {
                        throw new UnsupportedOperationException("Failed to load panel : " + ex.getMessage(), ex);
                    }
                }
            });
        }

        uiShowOnMapButton.setVisible(newValue instanceof Positionable || newValue instanceof AvecGeometrie);
    }

    @Override
    final public void setElement(T element) {
        elementProperty.set(element);
    }

    /**
     * Ici, on se contente d'appeler la méthode {@link FXElementPane#preSave() } du panneau spécifique à l'élément.
     * @throws Exception 
     */
    @Override
    public void preSave() throws Exception {
        if (specificThemePane != null) {
            specificThemePane.preSave();
        }
    }
    

    /**
     * Ici, on se contente d'appeler la méthode {@link FXElementPane#preRemove() } du panneau spécifique à l'élément.
     */
    public void preRemove() {
        if(specificThemePane!=null){
            specificThemePane.preRemove();
        }
    }
}
