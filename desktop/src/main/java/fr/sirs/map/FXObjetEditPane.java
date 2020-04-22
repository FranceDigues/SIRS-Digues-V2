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

import com.vividsolutions.jts.geom.Point;
import fr.sirs.SIRS;
import fr.sirs.core.model.BorneDigue;
import fr.sirs.core.model.Objet;
import fr.sirs.core.model.SystemeReperage;
import fr.sirs.core.model.TronconDigue;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import org.geotoolkit.gui.javafx.render2d.FXMap;

/**
 *
 * @author Johann Sorel (Geomatys)
 * @author Matthieu Bastianelli (Geomatys)
 * @param <T>
 */
public class FXObjetEditPane <T extends Objet> extends FXAbstractEditOnTronconPane<T> {

//    private final AbstractSIRSRepository<T> repo;
//
//    private final String defaultEmptyLibelle = "Aucun tronçon sélectionné";


//    public FXAbstractEditOnTronconPane(FXMap map, final String typeName, final Class clazz, final boolean createNameColumn, final boolean createDeleteColumn) {
//        this(map, clazz, createNameColumn, createDeleteColumn);
//        setTypeNameLabel(typeName);
//    }

    /**
     *
     * @param map
     * @param typeName
     * @param clazz
     */
    public FXObjetEditPane(FXMap map,final String typeName, final Class clazz) {
        super(map, typeName, clazz, true, false);
//        SIRS.loadFXML(this);
//        setTypeNameLabel(typeName);
    }

//    final void setTypeNameLabel(final String name){
//        typeNameLabel.setText(StringUtilities.firstToUpper(name)+ " :");
//    }

    /**
     * Set the input Tablecolumn to the {@link #uiObjetTable}
     * and set it sortable if needed.
     * @param toAddColumn
     * @param toSort
     */
//    final void addColumToTable(final TableColumn toAddColumn, final boolean toSort) {
//            uiObjetTable.getColumns().add(toAddColumn);
//            if (toSort)
//                toAddColumn.setSortable(true);
//    }

//    public void reset(){
//        mode.set(ObjetEditMode.PICK_TRONCON);
//        tronconProperty().set(null);
//    }

//    public ReadOnlyObjectProperty<ObjetEditMode> modeProperty(){
//        return mode;
//    }

//    public ObjectProperty<TronconDigue> tronconProperty(){
//        return tronconProp;
//    }
//
//    public TronconDigue getTronconProperty(){
//        return tronconProp.get();
//    }
//
//    ObjetEditMode getMode() {
//        return mode.get();
//    }

//    public ObservableList<T> objetProperties(){
//        return uiObjetTable.getSelectionModel().getSelectedItems();
//    }

//    public void save() {
//        save(tronconProp.getValue());
//    }

//    private void save(final TronconDigue td) {
//        final boolean mustSaveTd = saveTD.get();
//
//        if (mustSaveTd) {
//            saveTD.set(false);
//
//            TaskManager.INSTANCE.submit("Sauvegarde...", () -> {
//                if (td != null && mustSaveTd) {
//                    ((AbstractSIRSRepository) session.getRepositoryForClass(td.getClass())).update(td);
//                }
//            });
//        }
//    }
//
//    private void startPickTroncon(ActionEvent evt){
//        mode.set(ObjetEditMode.PICK_TRONCON);
//    }

    /*
     * OBJET UTILITIES
     */

//    /**
//     * Constuit un composant graphique listant les éléments du tronçon.
//     *
//     * @param toExclude Liste des identifiants des éléments à exclure de la liste.
//     * @return A list view of all bornes bound to currently selected troncon, or
//     * null if no troncon is selected.
//     */
//    ListView<T> buildObjetList(final Set<String> toExclude) {
//
//        // Construction du composant graphique.
//        final ListView<T> elementsView = new ListView<>();
//        elementsView.setItems(getObjectListFromTroncon(toExclude));
//        elementsView.setCellFactory(TextFieldListCell.forListView(new SirsStringConverter()));
//        elementsView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
//
//        return elementsView;
//    }

    /**
     * Retourne la liste des éléments T de la base associés au tronçon sélectionné.
     *
     * @param toExclude
     * @return
     */
//    private ObservableList<T> getObjectListFromTroncon(final Set<String> toExclude) {
//
//        final TronconDigue troncon = tronconProperty().get();
//        if (troncon == null) return null;
//
////        // Construction de la liste définitive des éléments à afficher.
//        final List<T> elements =repo.getAll().stream()
////                    .map(elt -> (T) elt)
//                    .filter(elt -> (troncon.getId().equals( ((Objet) elt).getForeignParentId())))
//                    .collect(Collectors.toList());
//
//        if (toExclude != null && !toExclude.isEmpty()) {
//            elements.removeIf(elt -> toExclude.contains(elt.getId()));
//        }
//        return FXCollections.observableArrayList(elements);
//    }

    /**
     * Ajout
     *
     * @param evt
     */
    @Override
    void startAddObjet(ActionEvent evt) {
        throw new UnsupportedOperationException("Unsupported yet.");
//        final TronconDigue troncon = tronconProperty().get();
//        if(troncon==null) return;
//
//        // Do not show bornes already present in selected SR.
//        final Set<String> borneIdsAlreadyInSR = new HashSet<>();
//        for (final SystemeReperageBorne srb : csr.systemeReperageBornes) {
//            borneIdsAlreadyInSR.add(srb.getBorneId());
//        }
//
//        // Construction et affichage du composant graphique de choix des bornes à ajouter.
//        final ListView<BorneDigue> bornesView = buildBorneList(borneIdsAlreadyInSR);
//        final Dialog dialog = new Dialog();
//        final DialogPane pane = new DialogPane();
//        pane.setContent(bornesView);
//        pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
//        dialog.setDialogPane(pane);
//
//        // Récupération des bornes sélectionnées et ajout dans le SR.
//        final Object res = dialog.showAndWait().get();
//        if(ButtonType.OK.equals(res)){
//            final ObservableList<BorneDigue> selectedItems = bornesView.getSelectionModel().getSelectedItems();
//            for(BorneDigue bd : selectedItems){
//                addBorneToSR(bd);
//            }
//        }
    }

//    private void startCreateObjet(ActionEvent evt){
//        if(mode.get().equals(ObjetEditMode.CREATE_OBJET)){
//            //on retourne on mode edition
//            mode.set(ObjetEditMode.EDIT_OBJET);
//        }else{
//            mode.set(ObjetEditMode.CREATE_OBJET);
//        }
//    }

    /**
     * Création d'un élément
     *
     * @param geom
     */
    @Override
    public void createObjet(final Point geom) { //uniquement un point ici, on veut pouvoir éditer un segment!

//        throw new UnsupportedOperationException("Unsupported yet.");
//
//        // Formulaire de renseignement du libellé de la borne.
//        final TextInputDialog dialog = new TextInputDialog("");
//        dialog.getEditor().setPromptText("borne ...");
//        dialog.setTitle("Nouvelle borne");
//        dialog.setGraphic(null);
//        dialog.setHeaderText("Libellé de la nouvelle borne");
//
//        final Optional<String> opt = dialog.showAndWait();
//        if(!opt.isPresent() || opt.get().isEmpty()) return;
//
//        //creation de la borne
//        final String borneLbl = opt.get();
//
//        // On vérifie que le libellé renseigné pour la borne ne fait pas partie des libellés utilisés par le SR élémentaire.
//        if(SirsCore.SR_ELEMENTAIRE_START_BORNE.equals(borneLbl) || SirsCore.SR_ELEMENTAIRE_END_BORNE.equals(borneLbl)){
//            final Alert alert = new Alert(Alert.AlertType.ERROR, "Le libellé de borne \""+borneLbl+"\" est réservé au SR élémentaire.", ButtonType.CLOSE);
//            alert.setResizable(true);
//            alert.showAndWait();
//            return;
//        }
//
//        final BorneDigue borne = session.getRepositoryForClass(BorneDigue.class).create();
//        borne.setLibelle(borneLbl);
//        borne.setGeometry(geom);
//        session.getRepositoryForClass(BorneDigue.class).add(borne);
//        final TronconDigue tr = tronconProp.get();
//        if (tr != null) {
//            tr.getBorneIds().add(borne.getId());
//        }
//
//        // Ajout de la borne au SR.
//        addBorneToSR(borne);
    }

    /**
     * Open a {@link ListView} to allow user to select one or more {@link BorneDigue}
     * to delete.
     *
     * Note : Once suppression is confirmed, we're forced to check all {@link SystemeReperage}
     * defined on the currently edited {@link TronconDigue}, and update them if
     * they use chosen bornes.
     *
     * @param e Event fired when deletion button has been fired.
     */
    @FXML
    @Override
    void deleteObjets(ActionEvent e) {

        throw new UnsupportedOperationException("Unsupported yet.");
//        final ListView<BorneDigue> borneList = buildObjetList(null);
//        if (borneList == null) return;
//
//        final Stage stage = new Stage();
//        stage.setTitle("Sélectionnez les bornes à supprimer");
//        stage.initModality(Modality.WINDOW_MODAL);
//        stage.initOwner(this.getScene().getWindow());
//
//        final Separator blankSpace = new Separator();
//        blankSpace.setVisible(false);
//
//        final Button cancelButton = new Button("Annuler");
//        cancelButton.setCancelButton(true);
//        cancelButton.setOnAction(event -> stage.hide());
//        final Button deleteButton = new Button("Supprimer");
//        deleteButton.disableProperty().bind(borneList.getSelectionModel().selectedItemProperty().isNull());
//
//        final HBox buttonBar = new HBox(10, blankSpace, cancelButton, deleteButton);
//        buttonBar.setPadding(new Insets(5));
//        buttonBar.setAlignment(Pos.CENTER_RIGHT);
//        final VBox content = new VBox(borneList, buttonBar);
//
//        stage.setScene(new Scene(content));
//
//        deleteButton.setOnAction(event -> {
//            final Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION, "Attention, les bornes séléctionnées seront effacées définitivement. Si elles sont utilisées par un système de repérage, cela entrainera le recalcul des positions liées à ce dernier. Continuer ?", ButtonType.NO, ButtonType.YES);
//            confirmation.setResizable(true);
//            final ButtonType userDecision = confirmation.showAndWait().orElse(ButtonType.NO);
//            if (ButtonType.YES.equals(userDecision)) {
//                final BorneDigue[] selectedItems = borneList.getSelectionModel().getSelectedItems().toArray(new BorneDigue[0]);
//                if (checkObjetSuppression(selectedItems)) {
//                    final TaskManager.MockTask deletor = new TaskManager.MockTask("Suppression de bornes", () -> {
//                        InjectorCore.getBean(BorneDigueRepository.class).remove(selectedItems);
//                    });
//
//                    deletor.setOnSucceeded(evt -> Platform.runLater(() -> borneList.getItems().removeAll(selectedItems)));
//                    deletor.setOnFailed(evt -> Platform.runLater(() -> GeotkFX.newExceptionDialog("Une erreur est survenue lors de la suppression des bornes.", deletor.getException()).show()));
//                    content.disableProperty().bind(deletor.runningProperty());
//
//                    TaskManager.INSTANCE.submit(deletor);
//                }
//            }
//        });
//
//        stage.show();
    }


    /*
     * TABLE UTILITIES
     */

    @Override
    void updateObjetTable(ObservableValue observable, SystemeReperage oldValue, SystemeReperage newValue) {
        throw new UnsupportedOperationException("Unsupported updateObjetTable yet.");
//        if (oldValue != null) {
//            save(oldValue, null);
//        }
//
//        if (newValue == null) {
//            uiObjetTable.setItems(FXCollections.emptyObservableList());
//        } else {
//            final ObjetEditMode current = mode.get();
//            if (current.equals(ObjetEditMode.CREATE_OBJET) || current.equals(ObjetEditMode.EDIT_OBJET)) {
//                //do nothing
//            } else {
//                mode.set(ObjetEditMode.EDIT_OBJET);
//            }
//
//            // By default, we'll sort bornes from uphill to downhill, but alow user to sort them according to available table columns.
//            final Comparator defaultComparator = defaultSRBComparator.get();
//            final SortedList sortedItems;
//            if (defaultComparator != null) {
//                sortedItems = newValue.getSystemeReperageBornes().sorted(defaultComparator).sorted();
//            } else {
//                sortedItems = newValue.getSystemeReperageBornes().sorted();
//            }
//
//            sortedItems.comparatorProperty().bind(uiObjetTable.comparatorProperty());
//            uiObjetTable.setItems(sortedItems);
//        }
    }


//    /**
//     * Colonne de suppression d'une borne d'un système de repérage.
//     */
//    private class DeleteColumn extends SimpleButtonColumn<Element, Element> {
//
//        public DeleteColumn() {
//            super(GeotkFX.ICON_UNLINK,
//                    (TableColumn.CellDataFeatures<Element, Element> param) -> new SimpleObjectProperty<>(param.getValue()),
//                    (Element t) -> true,
//                    new Function<Element, Element>() {
//
//                        public Element apply(Element srb) {
//                            final Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Confirmer la suppression?",
//                                    ButtonType.NO, ButtonType.YES);
//                            alert.setResizable(true);
//                            final ButtonType res = alert.showAndWait().get();
//                            if (ButtonType.YES == res) {
////                                saveSR.set(systemeReperageProperty().get().getSystemeReperageBornes().remove(srb));
//                            }
//                            return null;
//                        }
//                    },
//                    "Enlever du système de repérage"
//            );
//        }
//    }


//    /**
//     * Colonne d'affichage et de mise à jour du nom d'une borne.
//     */
//    private static class NameColumn extends TableColumn<Element,Element>{
//
//        public NameColumn() {
//            super("Nom");
//            setSortable(false);
//
//            setCellValueFactory(new Callback<TableColumn.CellDataFeatures<Element, Element>, ObservableValue<Element>>() {
//                @Override
//                public ObservableValue<Element> call(TableColumn.CellDataFeatures<Element, Element> param) {
//                    return new SimpleObjectProperty<>(param.getValue());
//                }
//            });
//
//            final SirsStringConverter sirsStringConverter = new SirsStringConverter();
//            setCellFactory((TableColumn<Element, Element> param) -> {
//                final FXTableCell<Element, Element> tableCell = new FXTableCell<Element, Element>() {
//
//                    @Override
//                    protected void updateItem(Element item, boolean empty) {
//                        super.updateItem(item, empty);
//                        if (empty || item == null) {
//                            setText(null);
//                            setGraphic(null);
//                        } else {
//                            setGraphic(new ImageView(ReferenceTableCell.ICON_LINK));
//                            setText(sirsStringConverter.toString(item));
//                        }
//                    }
//
//                };
//                tableCell.setEditable(false);
//                return tableCell;
//            });
//
//            setComparator(new LabelComparator());
//        }
//    }

}