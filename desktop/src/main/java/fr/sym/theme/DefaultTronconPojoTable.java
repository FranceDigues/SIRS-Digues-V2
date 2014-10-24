
package fr.sym.theme;

import fr.symadrem.sirs.core.model.Element;
import fr.symadrem.sirs.core.model.TronconDigue;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class DefaultTronconPojoTable extends AbstractPojoTable{
    
    private final ObjectProperty<TronconDigue> troncon = new SimpleObjectProperty<>();
    private final AbstractTronconTheme.ThemeGroup group;

    public DefaultTronconPojoTable(AbstractTronconTheme.ThemeGroup group) {
        super(group.getDataClass());
        this.group = group;
        
        final ChangeListener listener = (ChangeListener) (ObservableValue observable, Object oldValue, Object newValue) -> {
            updateTable();
        };
        
        troncon.addListener(listener);
    }
    
    public ObjectProperty<TronconDigue> tronconPropoerty(){
        return troncon;
    }
        
    private void updateTable(){
        final TronconDigue trc = troncon.get();
        if(trc==null || group==null){
            uiTable.setItems(FXCollections.emptyObservableList());
        }else{
            uiTable.setItems(group.getExtractor().apply(trc));
        }
    }

    @Override
    protected void deletePojo(Element pojo) {
        group.getDeletor().delete(troncon.get(), pojo);
    }

    @Override
    protected void editPojo(Element pojo) {
        
    }
    
}
