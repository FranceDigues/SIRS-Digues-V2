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

import fr.sirs.core.component.AbstractZoneVegetationRepository;
import fr.sirs.core.model.Element;
import fr.sirs.core.model.ParcelleVegetation;
import fr.sirs.core.model.PlanVegetation;
import fr.sirs.core.model.ZoneVegetation;
import fr.sirs.plugin.vegetation.PluginVegetation;
import fr.sirs.plugin.vegetation.VegetationSession;
import fr.sirs.theme.AbstractTheme;
import fr.sirs.theme.TronconTheme;
import fr.sirs.util.SimpleFXEditMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.beans.property.ObjectProperty;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/**
 *
 * @author Samuel Andrés (Geomatys)
 */
public class FXVegetationTronconThemePane extends FXTronconThemePane {

    public FXVegetationTronconThemePane(TronconTheme.ThemeManager ... groups) {
        super(groups);
    }

    protected class VegetationTronconThemePojoTable extends TronconThemePojoTable<ParcelleVegetation>{

        public VegetationTronconThemePojoTable(TronconTheme.ThemeManager<ParcelleVegetation> group, final ObjectProperty<? extends Element> container) {
            super(group, container);
// On n'a plus de colonne d'alerte dans la nouvelle spec car elle n'a plus de sens du fait du panneau d'exploitation.
//            final TableColumn<ParcelleVegetation, ParcelleVegetation> alertColumn = new AlertTableColumn();
//            getTable().getColumns().add((TableColumn) alertColumn);
        }

        @Override
        protected ParcelleVegetation createPojo(){
            final PlanVegetation planActif = VegetationSession.INSTANCE.planProperty().get();

            if(planActif==null){
                final Alert alert = new Alert(Alert.AlertType.WARNING, "La création de parcelles ne s'effectue que dans le plan actif.\n Si vous voulez créer une parcelle, veuillez s'il vous plaît activer auparavant un plan de gestion.", ButtonType.OK);
                alert.setResizable(true);
                alert.showAndWait();
                return null;
            }


            final int dureePlan = planActif.getAnneeFin()-planActif.getAnneeDebut();
            if(dureePlan<0){
                throw new IllegalStateException("La durée du plan "+planActif+" ("+dureePlan+") ne doit pas être négative");
            }
            else{
                final ParcelleVegetation created =  (ParcelleVegetation) repo.create();

                if(created!=null){
                    // Association au troçon sélectionné
                    created.setForeignParentId(getForeignParentId());

                    // Mode auto par défaut
                    created.setModeAuto(true);

                    // Association au plan actif
                    created.setPlanId(planActif.getId());

                    // Initialisation des planifications pour les années du plan.
                    PluginVegetation.ajustPlanifSize(created, dureePlan);

                    repo.add(created);
                    getAllValues().add(created);
                }
                return created;
            }
        }

        @Override
        protected void deletePojos(Element... pojos) {
            // Avant de supprimer les parcelles, il faut supprimer les zones de végétation qu'elles contiennent !

            final Map<Class, List<ZoneVegetation>> indexedZones = new HashMap<>();
            for(final Element pojo : pojos){

                if(pojo instanceof ParcelleVegetation){
                    // 1-Pour cela il faut commencer par les récupérer
                    final List<? extends ZoneVegetation> zones = AbstractZoneVegetationRepository.getAllZoneVegetationByParcelleId(pojo.getId(), session);

                    // 2-Ensuite on les indexe en fonction de leur classe
                    for(final ZoneVegetation zone : zones){
                        if(indexedZones.get(zone.getClass())==null) indexedZones.put(zone.getClass(), new ArrayList<>());
                        indexedZones.get(zone.getClass()).add(zone);
                    }
                }
            }


            // 3-Une fois qu'on a indexé les zones par classe, on peut les supprimer en masse
            for(final Class zoneClass : indexedZones.keySet()){
                session.getRepositoryForClass(zoneClass).executeBulkDelete(indexedZones.get(zoneClass));
            }


            // On peut ensuite supprimer les parcelles.
            super.deletePojos(pojos);
        }
    }

    @Override
    protected Parent createContent(AbstractTheme.ThemeManager manager) {
        final Separator separator = new Separator();
        separator.setVisible(false);
        final SimpleFXEditMode editMode = new SimpleFXEditMode();
        final HBox topPane = new HBox(separator, editMode);
        HBox.setHgrow(separator, Priority.ALWAYS);

        final VegetationTronconThemePojoTable table = new VegetationTronconThemePojoTable(manager, (ObjectProperty<? extends Element>) null);
        table.setDeletor(manager.getDeletor());
        table.editableProperty().bind(editMode.editionState());
        table.foreignParentProperty().bindBidirectional(linearIdProperty());

        return new BorderPane(table, topPane, null, null, null);
    }

//    /**
//     * Colonne d'alerte lorsque les traitements réalisés sur une parcelle paraissent incohérents avec la planification.
//     * @deprecated : plus utilisé d'après la spec 0.3
//     */
//    @Deprecated
//    private static class AlertTableColumn extends TableColumn<ParcelleVegetation, ParcelleVegetation> {
//
//        /**
//         * @deprecated : plus utilisé d'après la spec 0.3
//         */
//        @Deprecated
//        public AlertTableColumn(){
//            setGraphic(new ImageView(SIRS.ICON_EXCLAMATION_TRIANGLE_BLACK));
//            setPrefWidth(24);
//            setMinWidth(24);
//            setMaxWidth(24);
//            setCellValueFactory((TableColumn.CellDataFeatures<ParcelleVegetation, ParcelleVegetation> param) -> new SimpleObjectProperty(param.getValue()));
//            setCellFactory((TableColumn<ParcelleVegetation, ParcelleVegetation> param) -> new AlertTableCell());
//        }
//    }
//
//
//    /**
//     * Cellule d'alerte lorsque les traitements réalisés sur une parcelle paraissent incohérents avec la planification.
//     * @deprecated : plus utilisé d'après la spec 0.3
//     */
//    @Deprecated
//    private static class AlertTableCell extends TableCell<ParcelleVegetation, ParcelleVegetation>{
//        @Override
//        protected void updateItem(final ParcelleVegetation item, boolean empty){
//            super.updateItem(item, empty);
//
//            if(item!=null && item.getPlanId()!=null){
//
//                final Runnable cellUpdater = () -> {
//                    final ImageView image = isCoherent(item) ? null : new ImageView(SIRS.ICON_EXCLAMATION_TRIANGLE);
//                    Platform.runLater(() -> setGraphic(image));
//                };
//
//                Injector.getSession().getTaskManager().submit("Vérification de la cohérence de traitement de la parcelle "+item.getDesignation(), cellUpdater);
//            }
//            else{
//                setGraphic(null);
//            }
//        }
//    }
}
