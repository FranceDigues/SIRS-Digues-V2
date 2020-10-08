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

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;
import fr.sirs.CorePlugin;
import fr.sirs.core.model.BorneDigue;
import fr.sirs.core.model.SystemeReperage;
import fr.sirs.core.model.SystemeReperageBorne;
import fr.sirs.core.model.TronconDigue;
import fr.sirs.core.TronconUtils;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import org.geotoolkit.data.bean.BeanFeature;
import org.geotoolkit.display2d.GO2Utilities;
import org.geotoolkit.feature.Feature;
import org.geotoolkit.filter.identity.DefaultFeatureId;
import org.geotoolkit.geometry.jts.JTS;
import org.geotoolkit.gui.javafx.render2d.FXMap;
import org.opengis.filter.Id;
import org.opengis.filter.identity.Identifier;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class BorneEditHandler extends AbstractOnTronconEditHandler {

    private static final String BORNE_LAYER_NAME = CorePlugin.BORNE_LAYER_NAME;

    public BorneEditHandler(final FXMap map) {
        this(map, "troncon");
    }

    public BorneEditHandler(final FXMap map, final String typeName) {
        super(map, BorneDigue.class, new FXSystemeReperagePane(map, typeName), false);

        editPane.objetProperties().addListener(new ListChangeListener<SystemeReperageBorne>() {
            @Override
            public void onChanged(ListChangeListener.Change<? extends SystemeReperageBorne> c) {
                if (objetLayer == null) {
                    return;
                }

                final ObservableList<SystemeReperageBorne> lst = editPane.objetProperties();
                final Set<Identifier> ids = new HashSet<>();
                for (SystemeReperageBorne borne : lst) {
                    ids.add(new DefaultFeatureId(borne.getBorneId()));
                }

                final Id filter = GO2Utilities.FILTER_FACTORY.id(ids);
                objetLayer.setSelectionFilter(filter);

                if (ids.size() == 1) {
                    //borne edition mode
                    final String borneId = lst.get(0).getBorneId();
                    editedObjet = session.getRepositoryForClass(BorneDigue.class).get(borneId);
                    updateGeometry();
                } else {
                    editedObjet = null;
                    updateGeometry();
                }

            }
        });

        mouseInputListener = new MouseListenForBorne();

    }

    @Override
    String getObjetLayerName() {
        return BORNE_LAYER_NAME;
    }

    private class MouseListenForBorne extends AbstractSIRSEditMouseListen<BorneDigue> {

        public MouseListenForBorne() {
            super(BorneEditHandler.this);
        }

        @Override
        public void mouseClicked(final MouseEvent e) {
            if (tronconLayer == null) {
                return;
            }

            startX = getMouseX(e);
            startY = getMouseY(e);
            mousebutton = e.getButton();

            final EditModeObjet mode = editPane.getMode();
            final FXSystemeReperagePane srbEditPane = (FXSystemeReperagePane) editPane; //Cast possible as the editPane is initialized in the current constructor as FXSystemeReperagePane

            if (EditModeObjet.PICK_TRONCON.equals(mode)) {
                if (mousebutton == MouseButton.PRIMARY) {
                    //selection d'un troncon
                    final Feature feature = helperTroncon.grabFeature(e.getX(), e.getY(), false);
                    if (feature != null) {
                        Object bean = feature.getUserData().get(BeanFeature.KEY_BEAN);
                        if (bean instanceof TronconDigue && session.editionAuthorized((TronconDigue) bean)) {
                            bean = session.getRepositoryForClass(TronconDigue.class).get(((TronconDigue) bean).getDocumentId());
                            srbEditPane.tronconProperty().set((TronconDigue) bean);
                        }
                    }
                }
            } else if (EditModeObjet.EDIT_OBJET.equals(mode)) {
                final SystemeReperage sr = srbEditPane.systemeReperageProperty().get();

                if (editedObjet == null || editGeometry.selectedNode[0] < 0) {
                    //selection d'une borne
                    final Feature feature = helperObjet.grabFeature(e.getX(), e.getY(), false);
                    if (feature != null) {
                        final Object bean = feature.getUserData().get(BeanFeature.KEY_BEAN);
                        if (bean instanceof BorneDigue && session.editionAuthorized((BorneDigue) bean)) {
                            final BorneDigue candidate = (BorneDigue) bean;
                            final String candidateId = candidate.getDocumentId();

                            //on vérifie que la borne fait bien partie du SR sélectionné
                            final List<SystemeReperageBorne> srbs = sr.getSystemeReperageBornes();
                            for (SystemeReperageBorne srb : srbs) {
                                if (srb.getBorneId().equals(candidateId)) {
                                    srbEditPane.selectSRB(srb);
                                    break;
                                }
                            }
                        }
                    }
                }

            } else if (EditModeObjet.CREATE_OBJET.equals(mode)) {

                final Coordinate coord = helperObjet.toCoord(startX, startY);
                final Point point = GO2Utilities.JTS_FACTORY.createPoint(coord);
                JTS.setCRS(point, session.getProjection());
                //les events vont induire le repaint de la carte
                srbEditPane.createObjet(point);
            }

        }

        @Override
        public void mousePressed(final MouseEvent e) {
            super.mousePressed(e);

            initialX = getMouseX(e);
            initialY = getMouseY(e);
            startX = getMouseX(e);
            startY = getMouseY(e);

            if (editGeometry.geometry.get() != null) {
                helperObjet.grabGeometryNode(startX, startY, editGeometry);
            }
        }

        @Override
        public void mouseDragged(MouseEvent me) {
            //do not use getX/getY to calculate difference
            //JavaFX Bug : https://javafx-jira.kenai.com/browse/RT-34608

            //calcul du deplacement
            startX = getMouseX(me);
            startY = getMouseY(me);

            if (editedObjet != null && editGeometry.selectedNode[0] >= 0) {
                //deplacement d'une borne
                editGeometry.moveSelectedNode(helperObjet.toCoord(startX, startY));
                updateGeometry();
            } else {
                super.mouseDragged(me);
            }
        }

        @Override
        public void mouseReleased(MouseEvent me) {
            mouseDragged(me);

            if (editedObjet != null && editGeometry.selectedNode[0] >= 0) {
                //On demande à l'utilisateur s'il souhaite sauvegarder en base de données les modifications apportées.
                final Alert alert = new Alert(Alert.AlertType.WARNING, "Confirmer le déplacement de la borne?", ButtonType.OK, ButtonType.CANCEL);
                alert.setResizable(true);
                Optional<ButtonType> clickedButton = alert.showAndWait();

                if (clickedButton.get() == ButtonType.OK) {

                    ((BorneDigue) editedObjet).setGeometry((Point) editGeometry.geometry.get());
                    session.getRepositoryForClass(objetClass).update(editedObjet);
                    ((FXSystemeReperagePane) editPane).selectSRB(null); //Cast possible as the editPane is initialized in the current constructor as FXSystemeReperagePane

                    //les event vont induire le repaint de la carte
                    final TronconDigue troncon = editPane.getTronconFromProperty();
                    if (troncon != null) {
                        //on recalcule les geometries des positionables du troncon.
                        TronconUtils.updatePositionableGeometry(troncon, session);
                    }
                } else {
                    startX = initialX;
                    startY = initialY;

                    // La position initiale de la borne est rétablie.
                    editGeometry.moveSelectedNode(helperObjet.toCoord(startX, startY));
                    updateGeometry();

                    // Probablement pas nécessaire de sauvegarder le non changement de la borne
                    // mais permet d'actualiser la carte
                    //-> TODO : appliquer un refresh de la map uniquement.
                    ((BorneDigue) editedObjet).setGeometry((Point) editGeometry.geometry.get());
                    session.getRepositoryForClass(objetClass).update(editedObjet);
                }
            } else {
                super.mouseReleased(me);
            }
        }
    }

}
