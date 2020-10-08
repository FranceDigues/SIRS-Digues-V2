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
package fr.sirs.plugin.dependance.map;

import com.vividsolutions.jts.geom.Geometry;
import fr.sirs.core.model.DesordreDependance;
import fr.sirs.map.AbstractSIRSEditHandler;
import fr.sirs.map.SIRSEditMouseListen;
import fr.sirs.plugin.dependance.PluginDependance;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import org.geotoolkit.gui.javafx.render2d.FXMap;

/**
 * Contrôle les actions possibles pour le bouton d'édition et de modification de dépendances
 * sur la carte.
 *
 * Note : appelé dans l'application Sirs depuis la fiche du {@link DesordreDependance}.
 * Pourrait être regroupé avec {@link DesordreCreateHandler}.
 *
 * @author Cédric Briançon (Geomatys)
 * @author Johann Sorel (Geomatys)
 */
public class DesordreEditHandler extends AbstractSIRSEditHandler {
//    private final MouseListen mouseInputListener = new MouseListen();
//    private final FXGeometryLayer geomLayer = new FXGeometryLayer();

    private SIRSEditMouseListen mouseInputListener;

    /**
     * Couches présentant les dépendances sur la carte.
     */
//    private FeatureMapLayer objetLayer;

    /**
     * La dépendance en cours.
     */
//    private DesordreDependance editedObjet;

    /**
     * Outil d'aide pour éditer une {@linkplain #editGeometry géométrie} existante.
     */
//    private EditionHelper objetHelper;

    /**
     * Géométrie en cours d'édition.
     */
//    private final EditionHelper.EditionGeometry editGeometry = new EditionHelper.EditionGeometry();

    /**
     * Coordonnées de la {@linkplain #editGeometry géométrie}.
     */
//    private final List<Coordinate> coords = new ArrayList<>();

    /**
     * Vrai si une dépendance vient d'être créée.
     */
//    private boolean newCreatedObjet = false;

    /**
     * Définit le type de géométries à dessiner, pour les dépendances de types "ouvrages de voirie" ou "autres"
     * pour lesquelles plusieurs choix sont possibles.
     */
//    private Class newGeomType = Point.class;

    /**
     * Vrai si la {@linkplain #coords liste des coordonnées} de la {@linkplain #editGeometry géométrie}
     * vient d'être créée.
     */
//    private boolean justCreated = false;

    public DesordreEditHandler() {
//        super(DesordreDependance.class,  new FXGeometryLayer());
        super(DesordreDependance.class);
//        mouseInputListener = new MouseListen();
//        mouseInputListener = new SIRSEditMouseListen(this);
    }

    boolean newCreatedObjet = true; //TODO : replace using EditionModes

    public DesordreEditHandler(final DesordreDependance dependance) {
        this();
        this.editedObjet = dependance;

        if (dependance.getGeometry() != null) {
            editGeometry.geometry.set((Geometry)dependance.getGeometry().clone());
            geomLayer.getGeometries().setAll(editGeometry.geometry.get());
            newCreatedObjet = false;
        }

        mouseInputListener = new SIRSEditMouseListen(this, true);
    }

    @Override
    public SIRSEditMouseListen getMouseInputListener() {
        return mouseInputListener;
    }



    /**
     * {@inheritDoc}
     */
    @Override
    public void install(FXMap component) {
//        mouseInputListener = new SIRSEditMouseListen(this);
        super.install(component);
//        component.addEventHandler(MouseEvent.ANY, mouseInputListener);
//        component.addEventHandler(ScrollEvent.ANY, mouseInputListener);
//        map.setCursor(Cursor.CROSSHAIR);
//        map.addDecoration(0, geomLayer);

        objetLayer = PluginDependance.getDesordreLayer();

        //Debug point to check the binding
//        mouseInputListener = new SIRSEditMouseListen(this);
//        mouseInputListener.setNewCreatedObjet(newCreatedObjet);


    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean uninstall(FXMap component) {
        if (editGeometry.geometry.get() == null) {
            super.uninstall(component);
            return true;
        }

        final Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Confirmer la fin du mode édition ? Les modifications non sauvegardées seront perdues.",
                ButtonType.YES,ButtonType.NO);
        if (ButtonType.YES.equals(alert.showAndWait().get())) {
            super.uninstall(component);
            return true;
        }

        return false;
    }

}
