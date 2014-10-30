package fr.sym.digue;

import com.vividsolutions.jts.geom.Geometry;
import fr.sym.Session;
import fr.sym.Symadrem;
import fr.sym.theme.AbstractPojoTable;
import fr.sym.theme.AbstractTronconTheme;
import fr.sym.theme.DefaultTronconPojoTable;
import fr.symadrem.sirs.core.model.Digue;
import fr.symadrem.sirs.core.model.Element;
import fr.symadrem.sirs.core.model.TronconDigue;
import java.time.LocalDateTime;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.web.HTMLEditor;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import jidefx.scene.control.field.LocalDateTimeField;
import org.geotoolkit.gui.javafx.util.FXDateField;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Samuel Andrés (Geomatys)
 */
public class DigueController extends BorderPane {
    
//    private TreeView uiTree;
    private final ObjectProperty<Digue> digueProperty = new SimpleObjectProperty<>();
    private ObservableList<TronconDigue> troncons;
    
    @Autowired
    private Session session;

    @FXML private BorderPane borderPaneTables;
    @FXML private TextField libelle;
    @FXML private Label id;
    @FXML private Label mode;
    @FXML private FXDateField date_maj;
    @FXML private WebView commentaire;
//    @FXML private TableView<TronconDigue> tronconsTable;
    @FXML private ToggleButton editionButton;
    @FXML private Button saveButton;
    @FXML private Button addTroncon;
    @FXML private Button deleteTroncon;

    private final TronconPojoTable table = new TronconPojoTable();

    public DigueController() {
        Symadrem.loadFXML(this);
        Injector.injectDependencies(this);
        
        digueProperty.addListener((ObservableValue<? extends Digue> observable, Digue oldValue, Digue newValue) -> {
            initFields();
        });
    }
    
    public ObjectProperty<Digue> tronconProperty(){
        return digueProperty;
    }
    
    public Digue getDigue(){
        return digueProperty.get();
    }
    
    public void setDigue(Digue digue){
        this.digueProperty.set(digue);
//            table.tronconPropoerty().bindBidirectional(uiTronconChoice.valueProperty());
        initFields();
    }

    @FXML
    public void enableFields(ActionEvent event) {
        
        if (this.editionButton.isSelected()) {
            this.editionButton.setText("Passer en consultation");
            this.mode.setText("Mode saisie");
            this.mode.setTextFill(Color.RED);
            this.addTroncon.setGraphic(new ImageView("fr/sym/images/add-icon.png"));
            this.deleteTroncon.setGraphic(new ImageView("fr/sym/images/delete-icon.png"));
            this.saveButton.setDisable(false);
        } else {
            this.editionButton.setText("Passer en saisie");
            this.mode.setText("Mode consultation");
            this.mode.setTextFill(Color.WHITE);
            this.addTroncon.setGraphic(new ImageView("fr/sym/images/add-icon-inactif.png"));
            this.deleteTroncon.setGraphic(new ImageView("fr/sym/images/delete-icon-inactif.png"));
            this.saveButton.setDisable(true);
        }
    }
    
    @FXML
    private void save(ActionEvent event){
        this.session.update(this.digueProperty.get());
        this.session.update(this.troncons);
        
        // Set the fields no longer editable.-----------------------------------
        this.editionButton.setSelected(false);
        this.enableFields(event);
    }
    
    @FXML
    private void addTroncon(){
        
        if (editionButton.isSelected()){
            
            final Stage dialog = new Stage();
            final Label label = new Label("Libellé : ");
            final TextField libelleInput = new TextField();
            final Button ok = new Button("Ok");
            ok.setOnAction((ActionEvent event1) -> {
                TronconDigue tronconDigue = new TronconDigue();
                tronconDigue.nomProperty().bindBidirectional(libelleInput.textProperty());
                tronconDigue.setDigueId(digueProperty.get().getId());
                this.loadTronconUI(tronconDigue);
                this.session.add(tronconDigue);
                dialog.hide();
            });

            final VBox vBox = new VBox();
            vBox.setPadding(new Insets(20));
            vBox.setAlignment(Pos.CENTER);
            vBox.getChildren().add(label);
            vBox.getChildren().add(libelleInput);
            vBox.getChildren().add(ok);

            final Scene dialogScene = new Scene(vBox);
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(editionButton.getScene().getWindow());
            dialog.setScene(dialogScene);
            dialog.setTitle("Nouveau tronçon de digue.");
            dialog.show();
        }
    }
    
    @FXML
    private void deleteTroncon(){
        
//        if (editionButton.isSelected()){
//
//            final TronconDigue tronconDigue = this.tronconsTable.getSelectionModel().getSelectedItem();
//            final Stage dialog = new Stage();
//            final Label name = new Label(tronconDigue.getNom());
//            final Label id = new Label(tronconDigue.getId());
//            final Label confirmationMsg = new Label("Voulez-vous vraiment supprimer ce tronçon ?");
//            final Button annuler = new Button("Annuler");
//            annuler.setOnAction((ActionEvent event1) -> {
//                dialog.hide();
//            });
//            final Button ok = new Button("Ok");
//            ok.setOnAction((ActionEvent event1) -> {
//                this.session.delete(tronconDigue);
//                this.discardTronconUI(tronconDigue);
//                dialog.hide();
//            });
//            
//            final HBox hBox = new HBox();
//            hBox.setAlignment(Pos.CENTER);
//            hBox.getChildren().add(annuler);
//            hBox.getChildren().add(ok);
//
//            final VBox vBox = new VBox();
//            vBox.setPadding(new Insets(20));
//            vBox.setAlignment(Pos.CENTER);
//            vBox.getChildren().add(name);
//            vBox.getChildren().add(id);
//            vBox.getChildren().add(confirmationMsg);
//            vBox.getChildren().add(hBox);
//
//            final Scene dialogScene = new Scene(vBox);
//            dialog.initModality(Modality.APPLICATION_MODAL);
//            dialog.initOwner(editionButton.getScene().getWindow());
//            dialog.setScene(dialogScene);
//            dialog.setTitle("Suppression d'un tronçon de digue.");
//            dialog.show();
//        }
    }
    
    private void loadTroncons(){
        
        final List<TronconDigue> items = session.getTronconDigueByDigue(digueProperty.get());
        this.troncons = FXCollections.observableArrayList();
        items.stream().forEach((item) -> {
            this.troncons.add(item);
        });
        this.table.updateTable();
    }
    
    /**
     * Update the TreeView and TableView with the new section.
     * @param tronconDigue 
     */
    private void loadTronconUI(final TronconDigue tronconDigue){
//        ((TreeItem) this.uiTree.getSelectionModel().getSelectedItem()).getChildren().add(new TreeItem(tronconDigue));
        this.troncons.add(tronconDigue);
    }
    
    /**
     * Remove the section from the TreeView and the tableView.
     * @param tronconDigue 
     */
    private void discardTronconUI(final TronconDigue tronconDigue){
//        for (final Object treeItem : ((TreeItem) this.uiTree.getSelectionModel().getSelectedItem()).getChildren()){
//            if(tronconDigue.equals(((TreeItem) treeItem).getValue())){
//                ((TreeItem) this.uiTree.getSelectionModel().getSelectedItem()).getChildren().remove(treeItem);
//                this.troncons.remove(tronconDigue);
//                break;
//            }
//        }
    }

    /**
     * 
     * @param uiTree 
     */
    public void initFields() {
        borderPaneTables.setCenter(table);
        
        // Binding levee's name.------------------------------------------------
        this.libelle.textProperty().bindBidirectional(digueProperty.get().libelleProperty());
        this.libelle.editableProperty().bindBidirectional(this.editionButton.selectedProperty());
        
        // Display levee's id.--------------------------------------------------
        this.id.setText(this.digueProperty.get().getId());
        
        // Display levee's update date.-----------------------------------------
        this.date_maj.setValue(this.digueProperty.get().getDateMaj());
        this.date_maj.setDisable(true);
        this.date_maj.valueProperty().bindBidirectional(this.digueProperty.get().dateMajProperty());

        // Binding levee's comment.---------------------------------------------
        this.commentaire.getEngine().loadContent(digueProperty.get().getCommentaire());
        this.commentaire.setOnMouseClicked(new OpenHtmlEditorEventHandler());
        
        this.loadTroncons();
        
        // Disable the save button.---------------------------------------------
        this.saveButton.setDisable(true);
    }

    // FocusTransverse ?
    /**
     * Defines the customized table cell for displaying id of each levee's section.
     */
    private class CustomizedIdTableCell extends TableCell<TronconDigue, String> {
        
        private Button button;
        
        @Override
        protected void updateItem(String item, boolean empty) {
            
            super.updateItem(item, empty);
            
            if(item != null && !empty) {
                button = new Button();
                button.setText("ID");
                setGraphic(button);
                button.setBackground(new Background(new BackgroundFill(Color.ALICEBLUE, new CornerRadii(20), Insets.EMPTY)));
                button.setBorder(new Border(new BorderStroke(Color.ROYALBLUE, BorderStrokeStyle.SOLID, new CornerRadii(20), BorderWidths.DEFAULT)));
                button.setOnAction((ActionEvent event) -> {
                    final TronconDigue troncon = (TronconDigue) ((TableRow) CustomizedIdTableCell.this.getParent()).getItem();
                    final Stage dialog = new Stage();
                    final Label nom = new Label(troncon.getNom());
                    final Label id = new Label(troncon.getId());
                    final Button ok = new Button("Ok");
                    ok.setOnAction((ActionEvent event1) -> {
                        dialog.hide();
                    });

                    final VBox vBox = new VBox();
                    vBox.setAlignment(Pos.CENTER);
                    vBox.setPadding(new Insets(20));
                    vBox.getChildren().add(nom);
                    vBox.getChildren().add(id);
                    vBox.getChildren().add(ok);

                    final Scene dialogScene = new Scene(vBox);
                    dialog.initModality(Modality.APPLICATION_MODAL);
                    dialog.initOwner(button.getScene().getWindow());
                    dialog.setScene(dialogScene);
                    dialog.setTitle("Identifiant de tronçon de digue.");
                    dialog.show();
                });
            }
        }
    }
    
    /**
     * Defines the customized table cell for displaying geometry of each levee's section.
     */
    private class CustomizedGeometryTableCell extends TableCell<TronconDigue, Geometry> {
        
        private Button button = new Button();
                
        @Override
        protected void updateItem(Geometry item, boolean empty) {
            
            super.updateItem(item, empty);
            
            if(item != null) {
                button.setText(item.getGeometryType());
                setGraphic(button);
                button.setBackground(new Background(new BackgroundFill(Color.LIGHTPINK, new CornerRadii(20), Insets.EMPTY)));
                button.setBorder(new Border(new BorderStroke(Color.DARKMAGENTA, BorderStrokeStyle.SOLID, new CornerRadii(20), BorderWidths.DEFAULT)));
                button.setOnAction((ActionEvent event) -> {
                    final TronconDigue troncon = (TronconDigue) ((TableRow) CustomizedGeometryTableCell.this.getParent()).getItem();
                    final Stage dialog = new Stage();
                    final Label nom = new Label(troncon.getNom());
                    final Label wkt = new Label(troncon.getGeometry().toText());
                    final Button ok = new Button("Ok");
                    ok.setOnAction((ActionEvent event1) -> {
                        dialog.hide();
                    });

                    final VBox vBox = new VBox();
                    vBox.setAlignment(Pos.CENTER);
                    vBox.setPadding(new Insets(20));
                    vBox.getChildren().add(nom);
                    vBox.getChildren().add(wkt);
                    vBox.getChildren().add(ok);

                    final Scene dialogScene = new Scene(vBox);
                    dialog.initModality(Modality.APPLICATION_MODAL);
                    dialog.initOwner(button.getScene().getWindow());
                    dialog.setScene(dialogScene);
                    dialog.setTitle("Géométrie de tronçon de digue.");
                    dialog.show();
                });
            }
        }
    }
    
    /**
     * Defines the OpenHtmlEditorEventHandler for editing comment field.
     */
    private class OpenHtmlEditorEventHandler implements EventHandler<MouseEvent> {

        @Override
        public void handle(MouseEvent event) {
            
            if(editionButton.isSelected()){
                final Stage dialog = new Stage();
                dialog.initModality(Modality.APPLICATION_MODAL);
                dialog.initOwner(editionButton.getScene().getWindow());
                
                final VBox vbox = new VBox();

                final HTMLEditor htmlEditor = new HTMLEditor();
                htmlEditor.setHtmlText(digueProperty.get().getCommentaire());
                
                final HBox hbox = new HBox();
                hbox.setPadding(new Insets(20));
                hbox.setAlignment(Pos.CENTER);
                
                final Button valider = new Button("Valider");
                valider.setOnAction((ActionEvent event1) -> {
                    digueProperty.get().setCommentaire(htmlEditor.getHtmlText());
                    commentaire.getEngine().loadContent(htmlEditor.getHtmlText());
                    dialog.hide();
                });
                
                final Button annuler = new Button("Annuler");
                annuler.setOnAction((ActionEvent event1) -> {
                    dialog.hide();
                });
                
                hbox.getChildren().add(valider);
                hbox.getChildren().add(annuler);
                
                vbox.getChildren().add(htmlEditor);
                vbox.getChildren().add(hbox);
                
                final Scene dialogScene = new Scene(vbox);
                dialog.setScene(dialogScene);
                dialog.show();
            }
        }
    }

    public static class CustomizedFXLocalDateTimeCell<S> extends TableCell<S, Object> {

        private final CustomizedFXDateField field = new CustomizedFXDateField();
        private boolean secondAction;

        public CustomizedFXLocalDateTimeCell() {
            setGraphic(field);
            setAlignment(Pos.CENTER);
            setContentDisplay(ContentDisplay.CENTER);

            this.secondAction = false;

            field.setOnAction((ActionEvent event) -> {
                if (secondAction) {
                    commitEdit(field.getValue());
                } 
                secondAction = !secondAction;
            });
        }

        @Override
        public void startEdit() {
            LocalDateTime time = (LocalDateTime) getItem();
            if (time == null) {
                time = LocalDateTime.now();
            }
            field.setValue(time);
            super.startEdit();
            setText(null);
            setGraphic(field);
            field.requestFocus();
        }

        @Override
        protected void updateItem(Object item, boolean empty) {
            super.updateItem(item, empty);
            setText(null);
            setGraphic(null);
            if (item != null) {
                setText(((LocalDateTime) item).toString());
            }
        }
    }

    public static class CustomizedFXDateField extends LocalDateTimeField {

        private boolean getValueFirstCall = true;

        public CustomizedFXDateField() {

            super("yyyy-MM-dd HH:mm:ss");

            this.setAutoAdvance(false);
            this.setAutoReformat(false);
            this.setAutoSelectAll(true);
            this.setPopupButtonVisible(true);

        }

        /**
         * Overriden to support null values.
         *
         * @return
         */
        @Override
        public LocalDateTime getValue() {
            String text = getText();
            LocalDateTime value = super.getValue();

            if ("-- ::".equals(text)) {
                if (value == null && getValueFirstCall) {
                    super.setValue(LocalDateTime.now());
                } else {
                    super.setValue(null);
                }
            }

            getValueFirstCall = false;

            return super.getValue();
        }

        @Override
        public void replaceText(int start, int end, String text) {
            if (text.length() >= 1) {
                // Change behavior, avoid erasing all text when replacing a full group
                String existingText = getText();
                int newEnd = Math.max(0, Math.min(end, existingText.length()));
                String deletedText = existingText.substring(start, newEnd);
                if (deletedText.length() > text.length()) {
                    end = start + text.length();
                }
            }
            super.replaceText(start, end, text);
        }
    }
    
    
    
    
    private class TronconPojoTable extends AbstractPojoTable {
    
            public TronconPojoTable() {
            super(TronconDigue.class);

            final ChangeListener listener = (ChangeListener) (ObservableValue observable, Object oldValue, Object newValue) -> {
                updateTable();
            };

            digueProperty.addListener(listener);
        }

        private void updateTable() {
            final Digue dig = digueProperty.get();
            if (dig == null || troncons == null) {
                uiTable.setItems(FXCollections.emptyObservableList());
            } else {
            //JavaFX bug : sortable is not possible on filtered list
                // http://stackoverflow.com/questions/17958337/javafx-tableview-with-filteredlist-jdk-8-does-not-sort-by-column
                // https://javafx-jira.kenai.com/browse/RT-32091
                final SortedList sortedList = new SortedList(troncons);
                uiTable.setItems(sortedList);
                sortedList.comparatorProperty().bind(uiTable.comparatorProperty());
            }
        }
    
        @Override
        protected void deletePojo(Element pojo) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        protected void editPojo(Element pojo) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        protected void elementEdited(TableColumn.CellEditEvent<Element, Object> event) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
        
}
}