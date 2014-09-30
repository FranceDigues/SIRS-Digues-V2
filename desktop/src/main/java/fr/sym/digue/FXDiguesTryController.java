

package fr.sym.digue;

import fr.sym.*;
import fr.sym.digue.dto.Dam;
import fr.sym.digue.dto.DamSystem;
import fr.sym.digue.dto.Section;
import fr.sym.util.WrapTreeItem;
import fr.symadrem.sirs.core.model.Digue;
import fr.symadrem.sirs.core.model.Troncon;
import java.io.IOException;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;

public class FXDiguesTryController {

    public Parent root;
     
    @FXML
    private BorderPane uiRight;
    
    @FXML
    private TreeView uiTree;

    private void init(){
        
        final TreeItem root = new TreeItem("root");
        
        //build the tree
        /*for(DamSystem ds : Session.getInstance().getDamSystems()){
            root.getChildren().add(new WrapTreeItem(ds));
        }*/
        
        for(Digue ds : Session.getInstance().getDigues()){
            root.getChildren().add(new WrapTreeItem(ds));
        }
        
        uiTree.setRoot(root);
        uiTree.setShowRoot(false);
        uiTree.setCellFactory((Object param) -> new TC());
        
        uiTree.getSelectionModel().getSelectedIndices().addListener(new ListChangeListener() {

            @Override
            public void onChanged(ListChangeListener.Change c) {
                Object obj = uiTree.getSelectionModel().getSelectedItem();
                if(obj instanceof TreeItem){
                    obj = ((TreeItem)obj).getValue();
                }
                
                if(obj instanceof Digue){
                    final FXDigueTryController ctrl = FXDigueTryController.create((Digue)obj);
                    uiRight.setCenter(ctrl.root);
                }else if(obj instanceof Troncon){
                    final FXTronconGestionDigueTryController ctrl = FXTronconGestionDigueTryController.create((Troncon)obj);
                    uiRight.setCenter(ctrl.root);
                }
                
            }
        });
        
    }
    
    @FXML
    void openSearchPopup(ActionEvent event) {
        System.out.println("TODO");
    }
    
    
    public static FXDiguesTryController create() {
        final FXMLLoader loader = new FXMLLoader(Symadrem.class.getResource("/fr/sym/digue/diguesTryDisplay.fxml"));
        final Parent root;
        try {
            root = loader.load();
        } catch (IOException ex) {
            throw new IllegalArgumentException(ex.getMessage(), ex);
        }
        final FXDiguesTryController controller = loader.getController();
        controller.root = root;
        controller.init();
        return controller;
    }
    
    public static class TC extends TreeCell{

        @Override
        protected void updateItem(Object obj, boolean empty) {
            super.updateItem(obj, empty);
            
            if(obj instanceof TreeItem){
                obj = ((TreeItem)obj).getValue();
            }
            
            if(obj instanceof Digue){
                this.setText(((Digue)obj).getLabel()+" ("+getTreeItem().getChildren().size()+") ");
            }
            else if(obj instanceof Troncon){
                this.setText(((Troncon)obj).getName()+" ("+getTreeItem().getChildren().size()+") ");
            }
            else if(obj instanceof DamSystem){
                setText( ((DamSystem)obj).getName().getValue()+" ("+getTreeItem().getChildren().size()+")");
            }else if(obj instanceof Dam){
                setText( ((Dam)obj).getName().getValue()+" ("+getTreeItem().getChildren().size()+")");
            }else if(obj instanceof Section){
                setText( ((Section)obj).getName().getValue()+" ("+getTreeItem().getChildren().size()+")");
            }else if(obj instanceof Theme){
                setText( ((Theme)obj).getName());
            }else{
                setText(null);
            }
        }
        
    }
    
}
