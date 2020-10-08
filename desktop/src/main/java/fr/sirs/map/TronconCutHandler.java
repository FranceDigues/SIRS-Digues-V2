
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

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import fr.sirs.CorePlugin;
import fr.sirs.Injector;
import fr.sirs.SIRS;
import fr.sirs.Session;
import fr.sirs.core.LinearReferencingUtilities;
import fr.sirs.core.SirsCore;
import fr.sirs.core.TronconUtils;
import fr.sirs.core.model.AvecBornesTemporelles;
import fr.sirs.core.model.TronconDigue;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.logging.Level;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.sis.referencing.CRS;
import org.ektorp.DocumentOperationResult;
import org.geotoolkit.data.bean.BeanFeature;
import org.geotoolkit.display2d.GO2Utilities;
import org.geotoolkit.display2d.container.ContextContainer2D;
import org.geotoolkit.display2d.container.fx.FXFeature;
import org.geotoolkit.display2d.container.fx.FXMapContainerPane;
import org.geotoolkit.feature.Feature;
import org.geotoolkit.feature.FeatureTypeBuilder;
import org.geotoolkit.feature.FeatureUtilities;
import org.geotoolkit.feature.type.FeatureType;
import org.geotoolkit.geometry.jts.JTS;
import org.geotoolkit.gui.javafx.render2d.AbstractNavigationHandler;
import org.geotoolkit.gui.javafx.render2d.FXMap;
import org.geotoolkit.gui.javafx.render2d.FXPanMouseListen;
import org.geotoolkit.gui.javafx.render2d.edition.EditionHelper;
import org.geotoolkit.gui.javafx.util.FXUtilities;
import org.geotoolkit.gui.javafx.util.TaskManager;
import org.geotoolkit.internal.GeotkFX;
import org.geotoolkit.map.FeatureMapLayer;
import org.geotoolkit.map.MapContext;
import org.geotoolkit.map.MapLayer;
import org.geotoolkit.style.MutableStyleFactory;
import org.geotoolkit.util.StringUtilities;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.style.LineSymbolizer;
import org.opengis.util.FactoryException;

/**
 * Outil cartographique permettant le découpage de tronçons.
 *
 * @author Johann Sorel (Geomatys)
 * @author Alexis Manin (Geomatys)
 */
public class TronconCutHandler extends AbstractNavigationHandler {

    private static final MutableStyleFactory SF = GO2Utilities.STYLE_FACTORY;
    private final MouseListen mouseInputListener = new MouseListen();
    private final FXMapContainerPane geomlayer= new FXMapContainerPane();

    //edition variables
    private FeatureMapLayer tronconLayer = null;
    private EditionHelper helper;

    private Stage dialog;
    private final FXTronconCut editPane;
    private final FeatureType featureType;
    private final Session session;

    // overriden variable by init();
    protected String layerName;
    protected String typeName;
    protected boolean maleGender;

    /** List of layers deactivated on tool install. They will be activated back at uninstallation. */
    private List<MapLayer> toActivateBack;

    protected void init() {
        this.layerName  = CorePlugin.TRONCON_LAYER_NAME;
        this.typeName   = "tronçon";
        this.maleGender = true;
    }

    public TronconCutHandler(final FXMap map) {
        super();
        init();
        session = Injector.getSession();
        geomlayer.setMap2D(map);
        editPane = new FXTronconCut(typeName);

        //recalcule l'affichage lorsque le tronçon cible change.
        editPane.tronconProperty().addListener(new ChangeListener<TronconDigue>() {
            @Override
            public void changed(ObservableValue<? extends TronconDigue> observable, TronconDigue oldValue, TronconDigue newValue) {
                if (dialog == null) return;
                if (newValue != null) {
                    dialog.show();
                } else {
                    dialog.hide();
                }
            }
        });

        //recalcule de l'affichage sur changement d'un segment
        editPane.getSegments().addListener(this::segmentChanged);

        final FeatureTypeBuilder ftb = new FeatureTypeBuilder();
        ftb.setName("name");
        ftb.add("geom", LineString.class,session.getProjection());
        ftb.setDefaultGeometry("geom");
        featureType = ftb.buildFeatureType();
    }

    /**
     * We check operations to perform. If all segments are marked "CONSERVER",
     * we ask user if he really want to end cut, because there's nothing to do
     * in this case. Otherwise, we submit a new task to process the cut.
     */
    private void processCut() {
        final ObservableList<FXTronconCut.Segment> segments = editPane.getSegments();
        int i = 0;
        int nbSegments = segments.size();
        for (; i < nbSegments; i++) {
            if (!FXTronconCut.SegmentType.CONSERVER.equals(segments.get(i).typeProp.get())) {
                break;
            }
        }
        final String prefix = maleGender ? "du " : "de la ";
        if (i >= nbSegments) {
            final Alert choice = new Alert(Alert.AlertType.INFORMATION,
                    "Êtes-vous sûr ? Tous les morceaux " + prefix + typeName + " sont marqués \"à conserver\". Aucune opération ne sera effectuée.",
                    ButtonType.NO, ButtonType.YES);
            choice.setResizable(true);
            final Optional result = choice.showAndWait();
            if (ButtonType.YES.equals(result.get())) {
                // empty our handler, to allow new operation.
                editPane.tronconProperty().set(null);
            }
        } else {
            // finish button is bound to troncon property state to avoid null value here.
            final TronconDigue troncon = editPane.tronconProperty().get();
            final String s = maleGender ? "le ":  "la ";
            final Alert confirmCut = new Alert(Alert.AlertType.CONFIRMATION, "Voulez-vous vraiment découper " + s + typeName + " ? Si oui, vos modifications seront enregistrées.", ButtonType.YES, ButtonType.NO);
            confirmCut.setResizable(true);
            confirmCut.showAndWait();
            final ButtonType result = confirmCut.getResult();
            if(result==ButtonType.YES){
                final Task submitted = TaskManager.INSTANCE.submit(new CutTask(
                        troncon,
                        // defensive copies
                        FXCollections.observableArrayList(editPane.getCutpoints()),
                        FXCollections.observableArrayList(segments),
                        typeName
                ));

                submitted.setOnFailed(event -> {
                    // Warn user that something has gone wrong.
                    final Dialog d;
                    if (submitted.getException() != null) {
                        d = GeotkFX.newExceptionDialog(null, submitted.getException());
                    } else {
                        d = new Alert(Alert.AlertType.ERROR, "Cause : erreur inconnue.", ButtonType.OK);
                        d.setResizable(true);
                    }
                    d.setHeaderText("Le découpage de " + troncon.getLibelle() + " n'a pas pu être mené à son terme.");
                    d.show();
                });

                submitted.setOnSucceeded(event -> {
                    final Alert alert = new Alert(Alert.AlertType.INFORMATION,
                            "Le découpage " + prefix + typeName + " \"" + troncon.getLibelle() + "\" s'est terminé avec succcès.",
                            ButtonType.OK);
                    alert.setResizable(true);
                    alert.showAndWait();
                    try {
                        Injector.getSession().getFrame().getMapTab().getMap().setTemporalRange(LocalDate.now(), map);
                    } catch (Exception ex) {
                        SirsCore.LOGGER.log(Level.WARNING, "Map temporal range cannot be updated.", ex);
                    }
                });
            }
            // empty our handler, to allow new operation.
            editPane.tronconProperty().set(null);
        }
    }

    private void segmentChanged(ListChangeListener.Change c){
        geomlayer.getChildren().clear();

        final TronconDigue troncon = editPane.tronconProperty().get();
        if(troncon==null) return;

        final FXTronconCut.Segment[] segments = editPane.getSegments().toArray(new FXTronconCut.Segment[0]);
        for (FXTronconCut.Segment segment : segments) {
            final Feature feature = FeatureUtilities.defaultFeature(featureType, "id-0");
            feature.getProperty("geom").setValue(segment.geometryProp.get());

            final LineSymbolizer ls = SF.lineSymbolizer(SF.stroke(FXUtilities.toSwingColor(segment.colorProp.get()), 4), null);
            final FXFeature fxf = new FXFeature(geomlayer.getContext(), feature);
            fxf.setSymbolizers(ls);
            geomlayer.getChildren().add(fxf);
        }
    }

    private void installDialog() {
        dialog = new Stage();
        dialog.getIcons().add(SIRS.ICON);
        dialog.setTitle("Découpage de " + typeName);
        dialog.setResizable(true);
        dialog.initModality(Modality.NONE);
        dialog.initOwner(map.getScene().getWindow());

        final Button finishBtn = new Button("Terminer");
        final Button cancelBtn = new Button("Annuler");
        cancelBtn.setCancelButton(true);

        final ButtonBar babar = new ButtonBar();
        babar.setPadding(new Insets(5, 5, 5, 5));
        babar.getButtons().addAll(cancelBtn, finishBtn);

        final BorderPane dialogContent = new BorderPane();
        dialogContent.setCenter(editPane);
        dialogContent.setBottom(babar);

        cancelBtn.setOnAction((ActionEvent e) -> {
            editPane.tronconProperty().set(null);
        });

        finishBtn.setOnAction((ActionEvent e) -> this.processCut());
        finishBtn.disableProperty().bind(editPane.tronconProperty().isNull());
        dialog.setScene(new Scene(dialogContent));
    }

    private void uninstallDialog() {
        if (dialog != null) {
            dialog.close();
            dialog = null;
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void install(final FXMap component) {
        super.install(component);

        installDialog();

        component.addEventHandler(MouseEvent.ANY, mouseInputListener);
        component.addEventHandler(ScrollEvent.ANY, mouseInputListener);
        map.setCursor(Cursor.CROSSHAIR);
        map.addDecoration(0,geomlayer);

        //recuperation du layer de troncon
        tronconLayer = null;

        // On instancie une nouvelle liste pour les couches à désactiver provisoirement (le temps de l'activation de l'outil)
        toActivateBack = new ArrayList<>();

        final ContextContainer2D cc = (ContextContainer2D) map.getCanvas().getContainer();
        final MapContext context = cc.getContext();
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
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean uninstall(final FXMap component) {
        if (editPane.tronconProperty().get() != null) {
            final Alert confirmUninstall = new Alert(
                    Alert.AlertType.CONFIRMATION,
                    "Confirmer la fin du mode édition.",
                    ButtonType.YES, ButtonType.NO);
            confirmUninstall.setResizable(true);
            if (ButtonType.YES.equals(confirmUninstall.showAndWait().get())) {
                editPane.tronconProperty().set(null);
            }
        }
        if (editPane.tronconProperty().get() == null) {
            super.uninstall(component);
            if (toActivateBack != null) {
                for (final MapLayer layer : toActivateBack) {
                    layer.setSelectable(true);
                }
            }
            component.removeEventHandler(MouseEvent.ANY, mouseInputListener);
            component.removeEventHandler(ScrollEvent.ANY, mouseInputListener);
            component.removeDecoration(geomlayer);

            uninstallDialog();

            return true;
        }

        return false;
    }

    private class MouseListen extends FXPanMouseListen {

        private final ContextMenu popup = new ContextMenu();
        private double startX;
        private double startY;

        public MouseListen() {
            super(TronconCutHandler.this);
            popup.setAutoHide(true);
        }

        @Override
        public void mouseClicked(final MouseEvent e) {
            if(tronconLayer==null) return;

            startX = getMouseX(e);
            startY = getMouseY(e);
            mousebutton = e.getButton();

            final TronconDigue troncon = editPane.tronconProperty().get();
            if (troncon==null || !editPane.isCutMode()) {
                //actions en l'absence de troncon
                if(mousebutton == MouseButton.PRIMARY) {
                    //selection d'un troncon
                    final Feature feature = helper.grabFeature(e.getX(), e.getY(), false);
                    if(feature !=null){
                        Object bean = feature.getUserData().get(BeanFeature.KEY_BEAN);
                        if(bean instanceof TronconDigue && session.editionAuthorized((TronconDigue)bean)) {
                            //on recupere le troncon complet, celui ci n'est qu'une mise a plat
                            bean = session.getRepositoryForClass(TronconDigue.class).get(((TronconDigue)bean).getDocumentId());
                            editPane.tronconProperty().set((TronconDigue)bean);
                        }
                    }
                }
            } else if (editPane.isCutMode()) {
                try{
                    //ajout d'un point de coupe
                    Point pt = helper.toJTS(startX, startY);
                    //to data crs
                    final MathTransform ptToData = CRS.findOperation(map.getCanvas().getObjectiveCRS2D(), session.getProjection(), null).getMathTransform();
                    pt = (Point) JTS.transform(pt,ptToData);

                    final LineString linear = LinearReferencingUtilities.asLineString(troncon.getGeometry());
                    final LinearReferencingUtilities.SegmentInfo[] segments = LinearReferencingUtilities.buildSegments(linear);
                    final LinearReferencingUtilities.ProjectedPoint proj = LinearReferencingUtilities.projectReference(segments, pt);

                    //si le point de coupe est avant le debut ou apres la fin on ne le fait pas
                    if(proj.distanceAlongLinear<=0 || proj.distanceAlongLinear>=linear.getLength()){
                        return;
                    }

                    final FXTronconCut.CutPoint cutPoint = new FXTronconCut.CutPoint();
                    cutPoint.distance.set(proj.distanceAlongLinear);
                    if (!editPane.getCutpoints().contains(cutPoint)) {
                        final List<FXTronconCut.CutPoint> cuts = new ArrayList<>(editPane.getCutpoints());
                        cuts.add(cutPoint);
                        Collections.sort(cuts);
                        editPane.getCutpoints().setAll(cuts);
                    }

                } catch(TransformException | FactoryException ex) {
                    SIRS.LOGGER.log(Level.WARNING, ex.getMessage(), ex);
                }
            } else {
                super.handle(e);
            }

            if (editPane.tronconProperty().get() != null) dialog.show();
        }
    }

    /**
     * The task cuts and updates {@link TronconDigue} elements.
     * Note : Cancel the task is not allowed, because database updates are
     * performed all along the process.
     *
     * TODO : put all database updates at the end of the process ?
     */
    private static class CutTask extends Task<Boolean> {
        private final TronconDigue toCut;
        private final ObservableList<FXTronconCut.CutPoint> cutpoints;
        private final ObservableList<FXTronconCut.Segment> segments;
        private final String typeName;

        public CutTask(final TronconDigue toCut,
                final ObservableList<FXTronconCut.CutPoint> cutpoints,
                final ObservableList<FXTronconCut.Segment> segments, final String typeName) {
            this.toCut = toCut;
            this.cutpoints = cutpoints;
            this.segments = segments;
            this.typeName = typeName;
        }

        @Override
        protected Boolean call() throws Exception {
            updateTitle("Découpage de " + typeName);

            if (toCut == null || cutpoints.isEmpty() || segments.isEmpty()) {
                return false;
            }

            updateMessage(StringUtilities.firstToUpper(typeName) + " " + toCut.getLibelle());

            final Session session = Injector.getSession();
            
            // Agrégat des sections conservées
            TronconDigue aggregate = null;
            
            // Liste des tronçons archivés
            final List<TronconDigue> archived = new ArrayList<>();
            
            // Date d'archivage
            final LocalDate yesterday = LocalDate.now().minusDays(1);
            // Prédicat d'archivage : on force la modification de la date d'archivage pour tout ce qui a une date d'archivage non nulle mais ultérieure
            final Predicate<AvecBornesTemporelles> archiveIf = new AvecBornesTemporelles.ArchivePredicate(yesterday);
            
            // Parcours des segments issus du découpage
            for (int i = 0, n = segments.size(); i < n; i++) {

                updateProgress(i, n);

                // récupération des informations du segment courant
                final FXTronconCut.Segment segment = segments.get(i);
                final String segmentName = segment.nameProperty.get()==null || segment.nameProperty.get().isEmpty() ? toCut.getLibelle() + "[" + i + "]" : segment.nameProperty.get();
                
                // création d'un tronçon à partir du segment
                final TronconDigue cut = TronconUtils.cutTroncon(toCut, segment.geometryProp.get(), segmentName, session);

                final FXTronconCut.SegmentType type = segment.typeProp.get();
                if (null != type) switch (type) {
                    case CONSERVER:
                        //on aggrege le morceau
                        if (aggregate == null) {
                            aggregate = cut;
                        } else {
                            aggregate = TronconUtils.mergeTroncon(aggregate, cut, session);

                            //on sauvegarde les modifications
                            session.getRepositoryForClass(TronconDigue.class).update(aggregate);
                            session.getRepositoryForClass(TronconDigue.class).remove(cut);
                        }
                        break;

                    case ARCHIVER:
                        //on marque comme terminé le troncon et ses structures
                        // TODO : check failures ?
                        archived.add(cut);// on garde le tronçon archivé pour examiner l'archivage de ses bornes
                        final List<DocumentOperationResult> result = TronconUtils.archiveSectionWithTemporalObjects(cut, session, yesterday, archiveIf);
                        for (final DocumentOperationResult failure : result) {
                            SirsCore.LOGGER.log(Level.WARNING, "Update failed : ".concat(failure.getError()));
                        }
                        break;
                    //rien a faire
                    case SECTIONNER:
                        break;
                    default:
                        throw new IllegalArgumentException("Type de coupe inconnue : " + type);
                }
            }

            //on archive l'ancien troncon et les objets dessus
            updateMessage("Archivage du tronçon découpé "+toCut.getLibelle());

            // TODO : check failures ?
            List<DocumentOperationResult> result = TronconUtils.archiveSectionWithTemporalObjects(toCut, session, yesterday, archiveIf);
            for (final DocumentOperationResult failure : result) {
                SirsCore.LOGGER.log(Level.WARNING, "Update failed : ".concat(failure.getError()));
            }
            
            updateMessage("Archivage des bornes… "+toCut.getLibelle());
            result = new ArrayList<>();
            for(final TronconDigue cut : archived){
                result.addAll(TronconUtils.archiveBornes(cut.getBorneIds(), session, yesterday, archiveIf));
            }
            result.addAll(TronconUtils.archiveBornes(toCut.getBorneIds(), session, yesterday, archiveIf));

            return true;
        }
    }

}
