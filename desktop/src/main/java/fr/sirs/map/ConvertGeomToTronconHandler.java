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
import com.vividsolutions.jts.geom.Geometry;
import fr.sirs.Injector;
import fr.sirs.SIRS;
import fr.sirs.Session;
import fr.sirs.core.LinearReferencingUtilities;
import fr.sirs.core.TronconUtils;
import fr.sirs.core.model.Digue;
import fr.sirs.core.model.TronconDigue;
import static fr.sirs.map.TronconEditHandler.showTronconDialog;
import java.awt.geom.Rectangle2D;
import java.util.logging.Level;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import org.apache.sis.referencing.CRS;
import org.geotoolkit.display.VisitFilter;
import org.geotoolkit.display2d.GraphicVisitor;
import org.geotoolkit.display2d.canvas.AbstractGraphicVisitor;
import org.geotoolkit.display2d.canvas.RenderingContext2D;
import org.geotoolkit.display2d.primitive.ProjectedCoverage;
import org.geotoolkit.display2d.primitive.ProjectedFeature;
import org.geotoolkit.display2d.primitive.SearchAreaJ2D;
import org.geotoolkit.feature.Feature;
import org.geotoolkit.geometry.jts.JTS;
import org.geotoolkit.gui.javafx.render2d.AbstractNavigationHandler;
import org.geotoolkit.gui.javafx.render2d.FXMap;
import org.geotoolkit.gui.javafx.render2d.FXPanMouseListen;
import org.geotoolkit.gui.javafx.render2d.shape.FXGeometryLayer;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class ConvertGeomToTronconHandler extends AbstractNavigationHandler {

    private static final int CROSS_SIZE = 5;

    private final MouseListen mouseInputListener = new MouseListen();
    private final FXGeometryLayer geomlayer= new FXGeometryLayer(){
        @Override
        protected Node createVerticeNode(Coordinate c, boolean selected){
            final Line h = new Line(c.x-CROSS_SIZE, c.y, c.x+CROSS_SIZE, c.y);
            final Line v = new Line(c.x, c.y-CROSS_SIZE, c.x, c.y+CROSS_SIZE);
            h.setStroke(Color.RED);
            v.setStroke(Color.RED);
            return new Group(h,v);
        }
    };
    private final double zoomFactor = 2;

     // overriden variable by init();
    protected String typeName;
    protected Class typeClass;
    protected boolean maleGender;
    protected Class parentClass;
    protected boolean showRive;
    protected String parentLabel;

    protected void init() {
        this.typeName    = "tronçon";
        this.typeClass   = TronconDigue.class;
        this.maleGender  = true;
        this.parentClass = Digue.class;
        this.showRive = true;
        this.parentLabel = "à la digue";
    }

    public ConvertGeomToTronconHandler(final FXMap map) {
        super();
        init();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void install(final FXMap map) {
        super.install(map);
        map.addEventHandler(MouseEvent.ANY, mouseInputListener);
        map.addEventHandler(ScrollEvent.ANY, mouseInputListener);
        map.setCursor(Cursor.CROSSHAIR);
        map.addDecoration(0,geomlayer);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean uninstall(final FXMap component) {
        super.uninstall(component);
        component.removeEventHandler(MouseEvent.ANY, mouseInputListener);
        component.removeEventHandler(ScrollEvent.ANY, mouseInputListener);
        component.removeDecoration(geomlayer);
        return true;
    }

    private class MouseListen extends FXPanMouseListen {

        private final ContextMenu popup = new ContextMenu();

        public MouseListen() {
            super(ConvertGeomToTronconHandler.this);
            popup.setAutoHide(true);
        }

        @Override
        public void mousePressed(final MouseEvent e) {
            final GraphicVisitor visitor = new AbstractGraphicVisitor() {
                private boolean stopRequest = false;
                
                @Override
                public boolean isStopRequested(){
                    return stopRequest;
                }
                
                @Override
                public void endVisit(){
                    stopRequest = true;
                }

                @Override
                public void visit(ProjectedFeature feature, RenderingContext2D context, SearchAreaJ2D area) {
                    if (stopRequest) stopRequest = false;
                    final Feature f = feature.getCandidate();
                    Geometry geom = (Geometry) f.getDefaultGeometryProperty().getValue();

                    if (geom != null) {
                        geom = LinearReferencingUtilities.asLineString(geom);
                    }

                    if(geom !=null) {
                        final Session session = Injector.getBean(Session.class);
//                        final TronconDigue troncon = showTronconDialog(typeName, typeClass, maleGender, Digue.class, showRive, parentLabel);
                        final TronconDigue troncon = showTronconDialog(typeName, typeClass, maleGender, parentClass, showRive, parentLabel);
                        if (troncon == null) {this.endVisit(); return;}
                        try{
                            //convertion from data crs to base crs
                            geom = JTS.transform(geom, CRS.findOperation(
                                    f.getDefaultGeometryProperty().getType().getCoordinateReferenceSystem(),
                                    session.getProjection(), null).getMathTransform());
                            JTS.setCRS(geom, session.getProjection());
                            troncon.setGeometry(geom);

                            //save troncon
                            session.getRepositoryForClass(typeClass).add(troncon);
                            TronconUtils.updateSRElementaire(troncon,session);

                            map.getCanvas().repaint();
                        }catch(TransformException | FactoryException ex){
                            SIRS.LOGGER.log(Level.WARNING, ex.getMessage(),ex);
                        }
                    }
                    this.endVisit();
                }

                @Override
                public void visit(ProjectedCoverage coverage, RenderingContext2D context, SearchAreaJ2D area) {}
            };

            final Rectangle2D rect = new Rectangle2D.Double(getMouseX(e)-3, getMouseY(e)-3, 6, 6);
            map.getCanvas().getGraphicsIn(rect, visitor, VisitFilter.INTERSECTS);
        }
    }

}
