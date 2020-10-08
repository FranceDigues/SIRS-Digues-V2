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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.geotoolkit.data.bean.BeanFeature;
import org.geotoolkit.display2d.GO2Utilities;
import org.geotoolkit.display2d.container.ContextContainer2D;
import org.geotoolkit.feature.Feature;
import org.geotoolkit.filter.identity.DefaultFeatureId;
import org.geotoolkit.gui.javafx.render2d.AbstractNavigationHandler;
import org.geotoolkit.gui.javafx.render2d.FXMap;
import org.geotoolkit.gui.javafx.render2d.FXPanMouseListen;
import org.geotoolkit.gui.javafx.render2d.edition.EditionHelper;
import org.geotoolkit.gui.javafx.render2d.navigation.FXPanHandler;
import org.geotoolkit.map.FeatureMapLayer;
import org.geotoolkit.map.MapContext;
import org.geotoolkit.map.MapLayer;
import org.opengis.filter.identity.Identifier;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class TronconMergeHandler extends AbstractNavigationHandler {

    private final MouseListen mouseInputListener = new MouseListen();

    //edition variables
    private FeatureMapLayer tronconLayer = null;
    private EditionHelper helper;

    private Stage dialog;
    private final FXTronconMerge editPane;
    private final Session session;

    // overriden variable by init();
    protected String layerName;
    protected String typeName;
    protected boolean maleGender;

    /** List of layers deactivated on tool install. They will be activated back at uninstallation. */
    private List<MapLayer> toActivateBack;

    protected void init() {
        this.layerName = CorePlugin.TRONCON_LAYER_NAME;
        this.typeName = "tronçon";
        this.maleGender = true;
    }

    public TronconMergeHandler(final FXMap map) {
        super();
        init();

        session = Injector.getSession();
        editPane = new FXTronconMerge(map, typeName, maleGender);

        editPane.getTroncons().addListener(this::tronconChanged);
    }

    private void tronconChanged(ListChangeListener.Change c){
        if(tronconLayer!=null){
            final Set<Identifier> ids = new HashSet<>();
            for(TronconDigue td : editPane.getTroncons()){
                ids.add(new DefaultFeatureId(td.getDocumentId()));
            }
            tronconLayer.setSelectionFilter(GO2Utilities.FILTER_FACTORY.id(ids));
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void install(final FXMap component) {
        super.install(component);
        component.addEventHandler(MouseEvent.ANY, mouseInputListener);
        component.addEventHandler(ScrollEvent.ANY, mouseInputListener);
        map.setCursor(Cursor.CROSSHAIR);

        //recuperation du layer de troncon
        tronconLayer = null;
        final ContextContainer2D cc = (ContextContainer2D) map.getCanvas().getContainer();
        final MapContext context = cc.getContext();
        toActivateBack = new ArrayList<>();
        for(MapLayer layer : context.layers()){
            if(layer.getName().equalsIgnoreCase(layerName)){
                tronconLayer = (FeatureMapLayer) layer;
                layer.setSelectable(true);
            } else if (layer.isSelectable()) {
                toActivateBack.add(layer);
                layer.setSelectable(false);
            }
        }

        helper = new EditionHelper(map, tronconLayer);
        helper.setMousePointerSize(6);

        installDialog();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean uninstall(final FXMap component) {
        final Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Confirmer la fin du mode \"fusion de tronçons\".",
                        ButtonType.YES,ButtonType.NO);
        alert.setResizable(true);
        if(tronconLayer == null || tronconLayer.getSelectionFilter() == null || editPane.getTroncons().isEmpty() ||
                ButtonType.YES.equals(alert.showAndWait().get())){
            super.uninstall(component);
            if (toActivateBack != null) {
                for (final MapLayer layer : toActivateBack) {
                    layer.setSelectable(true);
                }
            }
            component.removeEventHandler(MouseEvent.ANY, mouseInputListener);
            component.removeEventHandler(ScrollEvent.ANY, mouseInputListener);

            uninstallDialog();
            return true;
        }

        return false;
    }

    private void installDialog() {
        dialog = new Stage();

        final Button finishBtn = new Button("Terminer");
        final Button cancelBtn = new Button("Annuler");
        cancelBtn.setCancelButton(true);

        final ButtonBar babar = new ButtonBar();
        babar.getButtons().addAll(cancelBtn, finishBtn);

        final BorderPane dialogContent = new BorderPane();
        dialogContent.setCenter(editPane);
        dialogContent.setBottom(babar);

        dialog.getIcons().add(SIRS.ICON);
        dialog.setTitle("Fusion de " + typeName + "s");
        dialog.setResizable(true);
        dialog.initModality(Modality.NONE);
        dialog.initOwner(map.getScene().getWindow());
        dialog.setScene(new Scene(dialogContent));

        finishBtn.setOnAction((ActionEvent e)-> {

            editPane.processMerge();

            dialog.hide();
            if (tronconLayer != null) {
                tronconLayer.setSelectionFilter(null);
            }
            map.setHandler(new FXPanHandler(false));
        });

        cancelBtn.setOnAction((ActionEvent e)-> {
            dialog.hide();
            if (tronconLayer != null) {
                tronconLayer.setSelectionFilter(null);
            }
            map.setHandler(new FXPanHandler(false));
        });

        dialog.show();
    }

    private void uninstallDialog() {
        if (dialog != null) {
            dialog.close();
            dialog = null;
        }
    }

    private class MouseListen extends FXPanMouseListen {

        private final ContextMenu popup = new ContextMenu();

        public MouseListen() {
            super(TronconMergeHandler.this);
            popup.setAutoHide(true);
        }

        @Override
        public void mouseClicked(final MouseEvent e) {
            if(tronconLayer==null) return;

            mousebutton = e.getButton();

            if(mousebutton == MouseButton.PRIMARY){
                //selection d'un troncon
                final Feature feature = helper.grabFeature(e.getX(), e.getY(), false);
                if(feature !=null){
                    Object bean = feature.getUserData().get(BeanFeature.KEY_BEAN);
                    if (bean instanceof TronconDigue && session.editionAuthorized((TronconDigue)bean)) {
                        //on recupere le troncon complet, celui ci n'est qu'une mise a plat
                        bean = session.getRepositoryForClass(TronconDigue.class).get(((TronconDigue) bean).getDocumentId());
                        if(!editPane.getTroncons().contains(bean)) {
                            editPane.getTroncons().add((TronconDigue)bean);
                        }
                    }
                }
            }
        }
    }

}
