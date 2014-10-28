
package fr.sym.theme;

import fr.sym.Session;
import fr.sym.digue.Injector;
import fr.symadrem.sirs.core.model.Element;
import fr.symadrem.sirs.core.model.TronconDigue;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.TableColumn;

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
            //JavaFX bug : sortable is not possible on filtered list
            // http://stackoverflow.com/questions/17958337/javafx-tableview-with-filteredlist-jdk-8-does-not-sort-by-column
            // https://javafx-jira.kenai.com/browse/RT-32091
            final SortedList sortedList = new SortedList(group.getExtractor().apply(trc));
            uiTable.setItems(sortedList);
            sortedList.comparatorProperty().bind(uiTable.comparatorProperty());
        }
    }

    @Override
    protected void deletePojo(Element pojo) {
        final TronconDigue trc = troncon.get();
        if(trc==null) return;
        group.getDeletor().delete(trc, pojo);
        //sauvegarde des modifications du troncon
        final Session session = Injector.getBean(Session.class);
        session.getTronconDigueRepository().update(trc);
    }

    @Override
    protected void editPojo(Element pojo) {
        
    }

    @Override
    protected void elementEdited(TableColumn.CellEditEvent<Element, Object> event) {
        final TronconDigue obj = troncon.get();
        final Session session = Injector.getBean(Session.class);
        session.getTronconDigueRepository().update(obj);
    }
    
}
