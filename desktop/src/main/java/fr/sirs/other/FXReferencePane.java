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
package fr.sirs.other;

import fr.sirs.Injector;
import fr.sirs.ReferenceChecker;
import static fr.sirs.SIRS.BUNDLE_KEY_CLASS;
import static fr.sirs.SIRS.ICON_CHECK_CIRCLE;
import static fr.sirs.SIRS.ICON_EXCLAMATION_CIRCLE;
import fr.sirs.Session;
import fr.sirs.core.model.Element;
import fr.sirs.core.model.ReferenceType;
import fr.sirs.theme.ui.PojoTable;
import fr.sirs.theme.ui.pojotable.DeleteColumn;
import fr.sirs.util.FXFreeTab;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.function.Function;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.util.Callback;
import org.geotoolkit.gui.javafx.util.ButtonTableCell;

/**
 *
 * @author Samuel Andrés (Geomatys)
 * @param <T>
 */
public class FXReferencePane<T extends ReferenceType> extends BorderPane {

    private final ReferencePojoTable references;
    private final Session session = Injector.getSession();
    private final ReferenceChecker referenceChecker;

    public FXReferencePane(final Class<T> type) {
        final ResourceBundle bundle = ResourceBundle.getBundle(type.getName(), Locale.getDefault(), Thread.currentThread().getContextClassLoader());
        referenceChecker = session.getReferenceChecker();
        references = new ReferencePojoTable(type, bundle.getString(BUNDLE_KEY_CLASS)+" (type : "+type.getSimpleName()+")", (ObjectProperty<? extends Element>) null);
        references.editableProperty().set(false);
        references.fichableProperty().set(false);
        references.detaillableProperty().set(false);
        references.searchableProperty().set(false);
        references.commentAndPhotoProperty().set(false);
        this.setCenter(references);
    }


    private class ReferencePojoTable extends PojoTable{

        private final List<? extends ReferenceType> serverInstanceNotLocal;
        private final List<ReferenceType> localInstancesNotOnTheServer;

        public ReferencePojoTable(Class<T> pojoClass, String title, final ObjectProperty<? extends Element> container) {
            super(pojoClass, title, container);

            serverInstanceNotLocal = referenceChecker.getServerInstancesNotLocal().get(pojoClass);
            localInstancesNotOnTheServer = referenceChecker.getLocalInstancesNotOnTheServer().get(pojoClass);

            getColumns().replaceAll(t ->  t instanceof DeleteColumn ? new StateColumn() : t);

            getTable().setRowFactory((TableView<Element> param) -> {
                    return new ReferenceTableRow();
                });

            TableColumn<Element, String> idCol = new TableColumn<>("Identifiant");
            idCol.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<Element, String>, ObservableValue<String>>() {

                @Override
                public ObservableValue<String> call(TableColumn.CellDataFeatures<Element, String> param) {
                    return new SimpleStringProperty(param.getValue().getId());
                }
            });
            getColumns().add(1, idCol);


            setTableItems(() -> {

                final ObservableList allItems = FXCollections.observableArrayList(repo.getAll());
                if(serverInstanceNotLocal!=null && !serverInstanceNotLocal.isEmpty()){
                    final List<Element> newServerInstances = new ArrayList<>();
                    for(final Object asObject : serverInstanceNotLocal){
                        if(asObject instanceof Element){
                            newServerInstances.add((Element) asObject);
                        }
                    }
                    allItems.addAll(newServerInstances);
                }

                return allItems;
            });
        }

        private class ReferenceTableRow extends TableRow<Element>{

            @Override
            protected void updateItem(Element item, boolean empty) {
                super.updateItem(item, empty);

                if(item!=null){
                    // La mise à jour des références nouvelles et incohérentes est automatique.
//                    if(incoherentReferences!=null
//                            && incoherentReferences.get(item)!=null){
//                        getStyleClass().add("incoherentReferenceRow");
//
//                    } else if(serverInstanceNotLocal!=null
//                            && serverInstanceNotLocal.contains(item)){
//                        getStyleClass().add("newReferenceRow");
//                    } else
                        if(localInstancesNotOnTheServer!=null
                            && localInstancesNotOnTheServer.contains(item)){
                        getStyleClass().add("deprecatedReferenceRow");
                    }
                    else{
                        getStyleClass().removeAll("incoherentReferenceRow", "newReferenceRow", "deprecatedReferenceRow");
                    }
                }
            }
        }

        private class StateButtonTableCell extends ButtonTableCell<Element, ReferenceType>{

            private final Node defaultGraphic;

            public StateButtonTableCell(Node graphic) {
                super(true, graphic, (ReferenceType t) -> true, new Function<ReferenceType, ReferenceType>() {
                    @Override
                    public ReferenceType apply(ReferenceType t) {

//                        if (localInstancesNotOnTheServer != null
//                                && localInstancesNotOnTheServer.contains(t)) {
                            final Tab tab = new FXFreeTab("Analyse de la base");
                            tab.setContent(new FXReferenceAnalysePane(t));
                            Injector.getSession().getFrame().addTab(tab);
//                        }
//                        else{
//                            new Alert(Alert.AlertType.INFORMATION, "Cette référence est à jour.", ButtonType.OK).showAndWait();
//                        }
                        return t;
                    }
                });
                defaultGraphic = graphic;
            }

            @Override
            protected void updateItem(ReferenceType item, boolean empty) {
                super.updateItem(item, empty);

                if(item!=null){
                    // La mise à jour des références incohérentes et nouvelles est automatique.
//                    if(incoherentReferences!=null
//                            && incoherentReferences.get(item)!=null){
//                        button.setGraphic(new ImageView(ICON_EXCLAMATION_TRIANGLE));
//                        button.setText("Incohérente");
//                        decorate(true);
//                    }
//                    else if(serverInstanceNotLocal!=null
//                            && serverInstanceNotLocal.contains(item)){
//                        button.setGraphic(new ImageView(ICON_DOWNLOAD));
//                        button.setText("Nouvelle");
//                        decorate(false);
//                    }
//                    else
                        if(localInstancesNotOnTheServer!=null
                            && localInstancesNotOnTheServer.contains(item)){
                        button.setGraphic(new ImageView(ICON_EXCLAMATION_CIRCLE));
                        button.setText("Dépréciée");
                    }
                    else{
                        button.setGraphic(defaultGraphic);
                        button.setText("À jour");
                    }
                }
            }
        }



    private class StateColumn extends TableColumn<Element, ReferenceType>{

        public StateColumn() {
            super("État");
            setEditable(false);
            setSortable(false);
            setResizable(true);
            setPrefWidth(70);

            setCellValueFactory(new Callback<TableColumn.CellDataFeatures<Element, ReferenceType>, ObservableValue<ReferenceType>>() {
                @Override
                public ObservableValue<ReferenceType> call(TableColumn.CellDataFeatures<Element, ReferenceType> param) {
                    return new SimpleObjectProperty<>((ReferenceType)param.getValue());
                }
            });

            setCellFactory(new Callback<TableColumn<Element, ReferenceType>, TableCell<Element, ReferenceType>>() {

                @Override
                public TableCell<Element, ReferenceType> call(TableColumn<Element,ReferenceType> param) {

                    return new StateButtonTableCell(new ImageView(ICON_CHECK_CIRCLE));
                }
            });
        }
    }
    }
}
