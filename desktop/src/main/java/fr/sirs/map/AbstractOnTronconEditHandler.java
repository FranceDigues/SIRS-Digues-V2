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

import fr.sirs.CorePlugin;
import fr.sirs.Injector;
import fr.sirs.SIRS;
import fr.sirs.Session;
import fr.sirs.core.model.TronconDigue;
import fr.sirs.core.TronconUtils;
import fr.sirs.core.model.AvecSettableGeometrie;
import fr.sirs.core.model.Element;
import fr.sirs.core.model.LabelMapper;
import fr.sirs.core.model.Objet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.apache.sis.util.ArgumentChecks;
import org.geotoolkit.display2d.GO2Utilities;
import org.geotoolkit.display2d.container.ContextContainer2D;
import org.geotoolkit.filter.identity.DefaultFeatureId;
import org.geotoolkit.gui.javafx.render2d.AbstractNavigationHandler;
import org.geotoolkit.gui.javafx.render2d.FXMap;
import org.geotoolkit.gui.javafx.render2d.FXPanMouseListen;
import org.geotoolkit.gui.javafx.render2d.edition.EditionHelper;
import org.geotoolkit.gui.javafx.util.TaskManager;
import org.geotoolkit.map.FeatureMapLayer;
import org.geotoolkit.map.MapContext;
import org.geotoolkit.map.MapLayer;
import org.opengis.filter.identity.Identifier;

/**
 *
 *
 * instantiateMouseEditListener : boolean indiquant si l'on souhaite
 * que ce constructeur instantie {@link #mouseInputListener} with a default
 * value. ATTENTION : l'instantiation par défaut de ce constructeur est une
 * instance de {@link SIRSEditMouseListen} qui nécessite que la classe éditée
 * {@link #objetClass} implémente l'interface {@link AvecSettableGeometrie}.
 * Une exception {@link IllegalStateException} est soulevée si ce n'est pas
 * le cas.
 *
 * @author Johann Sorel (Geomatys)
 * @author Matthieu Bastianelli (Geomatys)
 *
 * @param <T>
 */
public abstract class AbstractOnTronconEditHandler<T extends Element> extends AbstractSIRSEditHandler {

    final Session session;

    //edition variables
    FeatureMapLayer tronconLayer = null;
    EditionHelper helperTroncon;

    AbstractSIRSEditMouseListen mouseInputListener;

    //Panneaux d'édition
    final Stage dialog = new Stage();
    FXAbstractEditOnTronconPane editPane;

    // overriden variable by init();
    protected String TRONCON_LAYER_NAME = CorePlugin.TRONCON_LAYER_NAME;

    /**
     * List of layers deactivated on tool install. They will be activated back
     * at uninstallation.
     */
    List<MapLayer> toActivateBack;

    /**
     * Same as
     * {@link #ObjetEditHandler(org.geotoolkit.gui.javafx.render2d.FXMap, java.lang.String, java.lang.Class, fr.sirs.map.FXObjetEditPane)}
     * but with a default Pane of edition {@link FXAbstractEditOnTronconPane}
     * and default type name (tronçon).
     *
     * @param map
     * @param clazz
     * @param : instantiateMouseEditListener same as {@link #AbstractOnTronconEditHandler(org.geotoolkit.gui.javafx.render2d.FXMap, java.lang.Class, fr.sirs.map.FXAbstractEditOnTronconPane, boolean)}
     * parameter with same warning.
     */
    public AbstractOnTronconEditHandler(final FXMap map, final Class<T> clazz, final boolean instantiateMouseEditListener) {
        this(map, clazz, new FXObjetEditPane(map, "troncon", clazz), true);
    }

    /**
     * {@link AbstractNavigationHandler} permettant l'édition d'{@link Objet} du
     * SIRS depuis une carte de l'application.
     *
     * @param map : carte à partir de laquelle on permet l'édition.
     * @param clazz : classe éditée.
     * @param editPane : panneau d'édition associé.
     * @param instantiateMouseEditListener : boolean indiquant si l'on souhaite
     * que ce constructeur instantie {@link #mouseInputListener} with a default
     * value. ATTENTION : l'instantiation par défaut de ce constructeur est une
     * instance de {@link SIRSEditMouseListen} qui nécessite que la classe éditée
     * {@link #objetClass} implémente l'interface {@link AvecSettableGeometrie}.
     * Une exception {@link IllegalStateException} est soulevée si ce n'est pas
     * le cas.
     */
    public AbstractOnTronconEditHandler(final FXMap map, final Class<T> clazz, final FXAbstractEditOnTronconPane editPane, final boolean instantiateMouseEditListener) {
        super(clazz);
        ArgumentChecks.ensureNonNull("Panneau d'édition", editPane);

        this.map = map;

        session = Injector.getSession();
        dialog.getIcons().add(SIRS.ICON);
        this.editPane = editPane;

        // Prepare footer to set an "exit" button
        final Button exitButton = new Button("Fermer");
        exitButton.setCancelButton(true);
        exitButton.setOnAction(event -> dialog.hide());
        final Separator sep = new Separator();
        sep.setVisible(false);
        final HBox footer = new HBox(sep, exitButton);
        footer.setPadding(new Insets(0, 10, 10, 0));
        HBox.setHgrow(sep, Priority.ALWAYS);
        final BorderPane bp = new BorderPane(editPane, null, null, footer, null);

        dialog.setResizable(true);
        dialog.initModality(Modality.NONE);
        dialog.initOwner(map.getScene().getWindow());
        dialog.setScene(new Scene(bp));

        //on ecoute la selection du troncon et des bornes pour les mettre en surbrillant
        editPane.tronconProperty().addListener(new ChangeListener<TronconDigue>() {
            @Override
            public void changed(ObservableValue<? extends TronconDigue> observable, TronconDigue oldValue, TronconDigue newValue) {
                if (tronconLayer == null) {
                    return;
                }

//                borne = null;
                updateGeometry();
                if (objetLayer != null) {
                    objetLayer.setSelectionFilter(null);
                }

                if (newValue == null) {
                    tronconLayer.setSelectionFilter(null);
                } else {
                    final Identifier id = new DefaultFeatureId(newValue.getDocumentId());
                    tronconLayer.setSelectionFilter(GO2Utilities.FILTER_FACTORY.id(Collections.singleton(id)));
                }
            }
        });

        dialog.setOnCloseRequest(eh -> {
            dialog.hide();
        });

        //fin de l'edition
        dialog.setOnHiding((WindowEvent event) -> {
            final TronconDigue troncon = editPane.getTronconFromProperty();
            if (troncon != null) {
                //on recupère la derniere version, la maj des sr entraine la maj des troncons
                final TronconDigue currentTroncon = session.getRepositoryForClass(TronconDigue.class).get(troncon.getDocumentId());
                //on recalcule les geometries des positionables du troncon.
                TronconUtils.updateSRElementaireIfExists(currentTroncon, session);
//                TronconUtils.updatePositionableGeometry(currentTroncon, session); //Coûteux
            }
            editPane.save();
            editPane.reset();
            if (mouseInputListener instanceof AbstractSIRSEditMouseListen) {
                ((AbstractSIRSEditMouseListen) mouseInputListener).reset();
            }
            uninstall(map);
        });

        editPane.tronconProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                dialog.show();
            } else {
                dialog.hide();
            }
        });

        editPane.getModeProperty().bindBidirectional(modeProperty);

        dialog.show();

        if (instantiateMouseEditListener) {
            if(!AvecSettableGeometrie.class.isAssignableFrom(objetClass) ) {
                throw new IllegalStateException("Can't initialize SIRSEditMouseListen if edited object's class doesn't implement AvecSettableGeometrie interface.");
            }
            mouseInputListener = new SIRSEditMouseListen(this, false);
        }
    }

    String getObjetLayerName() {
        final LabelMapper mapper = LabelMapper.get(objetClass);
        return mapper.mapClassName();
    }

    @Override
    protected FXPanMouseListen getMouseInputListener() {
        return mouseInputListener;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void install(final FXMap component) {
        super.install(component);

        //recuperation du layer de troncon
        tronconLayer = null;

        //on passe en mode sélection de troncon
        editPane.reset();

        final ContextContainer2D cc = (ContextContainer2D) map.getCanvas().getContainer();
        final MapContext context = cc.getContext();
        toActivateBack = new ArrayList<>();
        for (MapLayer layer : context.layers()) {
            if (layer.getName().equalsIgnoreCase(TRONCON_LAYER_NAME)) {
                tronconLayer = (FeatureMapLayer) layer;
                layer.setSelectable(true);
            } else if (layer.getName().equalsIgnoreCase(getObjetLayerName())) {
                objetLayer = (FeatureMapLayer) layer;
                layer.setSelectable(true);
            } else if (layer.isSelectable()) {
                toActivateBack.add(layer);
                layer.setSelectable(false);
            }
        }

        helperTroncon = new EditionHelper(map, tronconLayer);
        helperTroncon.setMousePointerSize(6);

        helperObjet = new EditionHelper(map, objetLayer);
        helperObjet.setMousePointerSize(6);

    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean uninstall(final FXMap component) {

            super.uninstall(component);
            if (toActivateBack != null) {
                for (final MapLayer layer : toActivateBack) {
                    layer.setSelectable(true);
                }
            }

            //déselection borne et troncon
            if (tronconLayer != null) {
                tronconLayer.setSelectionFilter(null);
            }
            if (objetLayer != null) {
                objetLayer.setSelectionFilter(null);
            }

            dialog.close();
            return true;
//        }

    }

}
