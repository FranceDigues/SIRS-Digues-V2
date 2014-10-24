
package fr.sym.theme;

import fr.sym.Session;
import fr.sym.Symadrem;
import fr.sym.digue.Injector;
import fr.symadrem.sirs.core.model.TronconDigue;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.util.StringConverter;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class TronconThemePane extends BorderPane {

    @FXML
    private BorderPane uiCenter;
    @FXML
    private ChoiceBox<TronconDigue> uiTronconChoice;
    
    private AbstractTronconTheme.ThemeGroup[] groups;
    
    public TronconThemePane(AbstractTronconTheme.ThemeGroup ... groups) {
        Symadrem.loadJRXML(this);
        
        if(groups.length==1){
            final DefaultTronconPojoTable table = new DefaultTronconPojoTable(groups[0]);
            table.tronconPropoerty().bindBidirectional(uiTronconChoice.valueProperty());
            uiCenter.setCenter(table);
        }else{
            final TabPane pane = new TabPane();
            pane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
            for(int i=0;i<groups.length;i++){
                final DefaultTronconPojoTable table = new DefaultTronconPojoTable(groups[i]);
                table.tronconPropoerty().bindBidirectional(uiTronconChoice.valueProperty());
                final Tab tab = new Tab(groups[i].getName());
                tab.setContent(table);
                pane.getTabs().add(tab);
            }
            uiCenter.setCenter(pane);
        }
        
        //chargement de la liste des troncons disponibles
        final Session session = Injector.getBean(Session.class);
        final List<TronconDigue> troncons = session.getTronconDigueRepository().getAll();
        uiTronconChoice.setItems(FXCollections.observableList(troncons));
        uiTronconChoice.setConverter(new StringConverter<TronconDigue>() {
            @Override
            public String toString(TronconDigue object) {
                return object.getNom();
            }
            @Override
            public TronconDigue fromString(String string) {
                throw new UnsupportedOperationException("Not supported.");
            }
        });
        
        if(!troncons.isEmpty()){
            uiTronconChoice.getSelectionModel().select(troncons.get(0));
        }
        
    }
    
    
}
