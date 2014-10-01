package fr.sym.digue;

import fr.sym.Session;
import fr.sym.Symadrem;
import fr.symadrem.sirs.core.component.DigueRepository;
import fr.symadrem.sirs.core.model.Digue;
import fr.symadrem.sirs.core.model.TronconDigue;
import java.io.IOException;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 *
 * @author Samuel Andrés (Geomatys)
 */


public class FXDigueTryController {
    
    public Parent root;
    public Digue digue;

    @FXML
    private TextField libelleDigueTextField;

    @FXML
    private TextArea commentaireDigueTextField;

    @FXML
    private TableView<TronconDigue> tronconsTable;

    @FXML
    private ToggleButton editionButton;
    
    @Autowired
    private Session session;
    

    public void init(Digue digue) {

        // Set the levee for the controller.------------------------------------
        this.digue = digue;
        
        // Binding levee's name.------------------------------------------------
        this.libelleDigueTextField.textProperty().bindBidirectional(digue.libelle);
        this.libelleDigueTextField.setEditable(true);

        // Binding levee's comment.---------------------------------------------
        this.commentaireDigueTextField.textProperty().bindBidirectional(digue.commentaire);
        this.commentaireDigueTextField.setWrapText(true);
        this.commentaireDigueTextField.setEditable(true);

        // Configuring table for levee's sections.------------------------------
        final TableColumn colName = this.tronconsTable.getColumns().get(1);
        colName.setCellValueFactory(new PropertyValueFactory<>("libelle"));
        colName.setEditable(true);

        /*colName.setCellFactory(TextFieldTableCell.forTableColumn());
         colName.setOnEditCommit(
         new EventHandler<TableColumn.CellEditEvent<Troncon, String>>() {
        
         @Override
         public void handle(TableColumn.CellEditEvent<Troncon, String> event) {
         ((Troncon) event.getTableView().getItems().get(
         event.getTablePosition().getRow())).setName(event.getNewValue());
         }
         }
         );*/
        /* 
         final TableColumn<FieldValue, Field> valueColumn = new TableColumn<>("Value");*/
        colName.setCellFactory(new Callback<TableColumn<TronconDigue, String>, CustomizedTableCell>() {

            @Override
            public CustomizedTableCell call(TableColumn<TronconDigue, String> param) {
                return new CustomizedTableCell();
            }
        });

        /*final TableColumn colJojo = this.tronconsTable.getColumns().get(0);
         colJojo.setCellValueFactory(new PropertyValueFactory<>("jojo"));
         colJojo.setEditable(true);
         StringConverter<Troncon.jojoenum> sc = new StringConverter<Troncon.jojoenum>() {
            
         @Override
         public String toString(Troncon.jojoenum object) {
            
         String result;
         switch(object){
         case oui: result = "je vaux oui"; break;
         case non: result = "je vaux non"; break;
         case bof:
         default: result = "je vaux bof";
         }
         return result;
            
         }

         @Override
         public Troncon.jojoenum fromString(String string) {
                
         return Troncon.jojoenum.bof;
         }
         };
         colJojo.setCellFactory(TextFieldTableCell.forTableColumn(sc));
         colJojo.setOnEditCommit(new EventHandler<TableColumn.CellEditEvent<Troncon, Troncon.jojoenum>>(){

         @Override
         public void handle(TableColumn.CellEditEvent<Troncon, Troncon.jojoenum> event) {
         ((Troncon) event.getTableView().getItems().get(
         event.getTablePosition().getRow())).setJojo(Troncon.jojoenum.non);  
         }
         }
        
         );*/
        // Binding levee's section.---------------------------------------------
        final List<TronconDigue> troncons = session.getTronconGestionDigueTrysByDigueTry(this.digue);
        final ObservableList<TronconDigue> tronconsObservables = FXCollections.observableArrayList();
        troncons.stream().forEach((troncon) -> {
            tronconsObservables.add(troncon);
        });
        this.tronconsTable.setItems(tronconsObservables);
        this.tronconsTable.setEditable(true);

        /*
        PropertyValueFactory<TronconDigue, String> pvf = new PropertyValueFactory<>("libelle");
        TableColumn.CellDataFeatures<TronconDigue, String> cdf = 
                new TableColumn.CellDataFeatures<TronconDigue, String>(tronconsTable, colName, null);*/
    }

    // FocusTransverse ?
    class CustomizedTableCell extends TableCell<TronconDigue, String> {

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            final Button button = new Button();
            button.setText((String) item);
            setGraphic(button);
            button.setBackground(new Background(new BackgroundFill(Color.ALICEBLUE, new CornerRadii(20), Insets.EMPTY)));
            button.setBorder(new Border(new BorderStroke(Color.ROYALBLUE, BorderStrokeStyle.SOLID, new CornerRadii(20), BorderWidths.DEFAULT)));
            button.setOnAction((ActionEvent event) -> {
                final TronconDigue troncon = (TronconDigue) ((TableRow) this.getParent()).getItem();
                
                final Stage dialog = new Stage();
                
                final Label label = new Label(troncon.getLibelle());
                final TextField editableLabel = new TextField(troncon.getLibelle());
                troncon.libelle.bindBidirectional(editableLabel.textProperty());
                ((Button)event.getSource()).textProperty().bindBidirectional(editableLabel.textProperty());

                final Button ok = new Button("Ok");
                ok.setOnAction((ActionEvent event1) -> {
                    dialog.hide();
                });
                final Button cancel = new Button("Cancel");
                cancel.setOnAction((ActionEvent event1) -> {
                    button.setText(label.getText());
                    dialog.hide();
                });
                final VBox popUpVBox = new VBox();
                popUpVBox.getChildren().add(label);
                popUpVBox.getChildren().add(editableLabel);
                popUpVBox.getChildren().add(ok);
                popUpVBox.getChildren().add(cancel);

                final Scene dialogScene = new Scene(popUpVBox, 300, 200);

                dialog.initModality(Modality.APPLICATION_MODAL);
                dialog.initOwner(root.getScene().getWindow());
                dialog.setScene(dialogScene);
                dialog.show();
            });
                
        }
    }

    @FXML
    public void change(ActionEvent event) {
        System.out.println(digue.libelle);
    }

    @FXML
    public void enableFields(ActionEvent event) {
        if (this.editionButton.isSelected()) {
            this.libelleDigueTextField.setEditable(false);
            this.commentaireDigueTextField.setEditable(false);
            this.tronconsTable.setEditable(false);
        } else {
            this.libelleDigueTextField.setEditable(true);
            this.commentaireDigueTextField.setEditable(true);
            this.tronconsTable.setEditable(true);
        }
    }

    public static FXDigueTryController create(Digue digue) {

        final FXMLLoader loader = new FXMLLoader(Symadrem.class.getResource("/fr/sym/digue/digueTryDisplay.fxml"));
        final Parent root;

        try {
            root = loader.load();
            
            
        } catch (IOException ex) {
            throw new IllegalArgumentException(ex.getMessage(), ex);
        }

        final FXDigueTryController controller = loader.getController();

        Injector.injectDependencies(controller);
    
        
        controller.root = root;
        controller.init(digue);
        return controller;
    }

}
