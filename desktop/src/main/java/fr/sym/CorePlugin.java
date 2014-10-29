

package fr.sym;

import java.io.IOException;
import java.sql.SQLException;

import static fr.sym.Session.PROJECTION;
import fr.sym.digue.Injector;
import org.geotoolkit.data.bean.BeanStore;
import fr.sym.theme.ContactsTheme;
import fr.sym.theme.DesordreTheme;
import fr.sym.theme.DocumentsTheme;
import fr.sym.theme.EmpriseCommunaleTheme;
import fr.sym.theme.EvenementsHydrauliquesTheme;
import fr.sym.theme.FrancBordTheme;
import fr.sym.theme.MesureEvenementsTheme;
import fr.sym.theme.PrestationsTheme;
import fr.sym.theme.ProfilsEnTraversTheme;
import fr.sym.theme.ReseauxDeVoirieTheme;
import fr.sym.theme.ReseauxEtOuvragesTheme;
import fr.sym.theme.StructuresTheme;
import fr.symadrem.sirs.core.component.BorneDigueRepository;
import fr.symadrem.sirs.core.component.TronconDigueRepository;
import fr.symadrem.sirs.core.model.BorneDigue;
import fr.symadrem.sirs.core.model.Crete;
import fr.symadrem.sirs.core.model.Fondation;
import fr.symadrem.sirs.core.model.Structure;
import fr.symadrem.sirs.core.model.TronconDigue;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;
import javafx.scene.control.MenuItem;
import javax.measure.unit.NonSI;
import org.apache.sis.storage.DataStoreException;
import org.ektorp.CouchDbConnector;
import org.ektorp.ViewQuery;
import org.geotoolkit.data.FeatureCollection;
import org.geotoolkit.data.FeatureStore;
import org.geotoolkit.data.bean.BeanFeatureSupplier;
import org.geotoolkit.data.query.QueryBuilder;
import org.geotoolkit.display2d.GO2Utilities;
import org.geotoolkit.feature.type.Name;
import org.geotoolkit.map.FeatureMapLayer;
import org.geotoolkit.map.MapBuilder;
import org.geotoolkit.map.MapItem;
import org.geotoolkit.map.MapLayer;
import org.geotoolkit.style.MutableFeatureTypeStyle;
import org.geotoolkit.style.MutableRule;
import org.geotoolkit.style.MutableStyle;
import org.geotoolkit.style.MutableStyleFactory;
import org.geotoolkit.style.RandomStyleBuilder;
import org.geotoolkit.style.StyleConstants;
import static org.geotoolkit.style.StyleConstants.*;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Expression;
import org.opengis.style.Fill;
import org.opengis.style.Graphic;
import org.opengis.style.GraphicalSymbol;
import org.opengis.style.LineSymbolizer;
import org.opengis.style.Mark;
import org.opengis.style.PointSymbolizer;
import org.opengis.style.Stroke;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class CorePlugin extends Plugin{
    
    private static final FilterFactory2 FF = GO2Utilities.FILTER_FACTORY;
    private static final MutableStyleFactory SF = GO2Utilities.STYLE_FACTORY;
    
    public CorePlugin() {
    }

    @Override
    public List<MapItem> getMapItems() {
        final List<MapItem> items = new ArrayList<>();
        
        final TronconDigueRepository repo = getSession().getTronconDigueRepository();
        final BorneDigueRepository bornesRepo = getSession().getBorneDigueRepository();
        
        try{
            
            final CouchDbConnector connector = Injector.getBean(CouchDbConnector.class);
            
            //todo : rendre dynamique
            // Nécessité de l'utilisation d'un dépôt de bornes depuis que les 
            // tronçons ne référencent plus directement les bornes mais leurs ID
            // Du coup il est probable que ceci devienne très lent. Cela pourrait
            // peut-être être amélioré par un map/reduce du côté serveur couchDb
            // mais il faudrait pour cela que les bornes contiennent un id de 
            // navigation vers les tronçons, ce qui n'est pas le cas actuellement.
            final List<BorneDigue> bornes = new ArrayList<>();
            for(TronconDigue td : getSession().getTronconDigueRepository().getAll()){
                // SOLUTION 1 (brutale)
//                td.getBorneIds().stream().forEach((id) -> {
//                    bornes.add(bornesRepo.get(id));
//                });
                
                // SOLUTION 2 (ektorp bulk)
                ViewQuery vq = new ViewQuery()
                      .allDocs()
                      .includeDocs(true)
                      .keys(td.getBorneIds());
                bornes.addAll(connector.queryView(vq, BorneDigue.class));
            }
            
            //troncons
            final BeanStore tronconStore = new BeanStore(
                    new BeanFeatureSupplier(TronconDigue.class, "id", "geometry", null, PROJECTION, ()-> repo.getAll())
            );
            items.addAll(buildLayers(tronconStore,createTronconStyle(),true));
            
            //bornes
            final BeanStore borneStore = new BeanStore(
                    new BeanFeatureSupplier(BorneDigue.class, "id", "positionBorne", null, PROJECTION, ()-> bornes)
            );
            items.addAll(buildLayers(borneStore,createBorneStyle(),true));
            
            //structures
            final BeanStore structStore = new BeanStore(
                    new BeanFeatureSupplier(Crete.class, "id", "geometry", null, PROJECTION, new StructSupplier((Predicate) (Object t) -> t instanceof Crete)),
                    new BeanFeatureSupplier(Fondation.class, "id", "geometry", null, PROJECTION, new StructSupplier((Predicate) (Object t) -> t instanceof Fondation))
            );
                        
            final MapItem structLayer = MapBuilder.createItem();
            structLayer.setName("Structures");
            structLayer.items().addAll(buildLayers(structStore,createStructureStyle(Color.red),true));
            items.add(structLayer);
               
            
        }catch(DataStoreException ex){
            Symadrem.LOGGER.log(Level.WARNING, ex.getMessage(), ex);
        }
        
        return items;
    }

    private class StructSupplier implements BeanStore.FeatureSupplier{

        private final Predicate predicate;
        
        public StructSupplier(Predicate predicate) {
            this.predicate = predicate;
        }
        
        @Override
        public Iterable get() {
            return new SubIterable(predicate);
        }
        
    }
    
    private class SubIterable implements Iterable{

        private final Predicate predicate;

        public SubIterable(Predicate predicate) {
            this.predicate = predicate;
        }
        
        public Iterator iterator() {
            final TronconDigueRepository repo = getSession().getTronconDigueRepository();
            final List<TronconDigue> troncons = repo.getAll();
            final List col = new ArrayList();
            for(TronconDigue td : troncons){
                col.addAll(td.stuctures.filtered(predicate));
            }
            return col.iterator();
        }
        
    }
    
    private List<MapLayer> buildLayers(FeatureStore store, MutableStyle baseStyle, boolean visible) throws DataStoreException{
        final List<MapLayer> layers = new ArrayList<>();
        final org.geotoolkit.data.session.Session symSession = store.createSession(false);
        for(Name name : store.getNames()){
            final FeatureCollection col = symSession.getFeatureCollection(QueryBuilder.all(name));
            final MutableStyle style = (baseStyle==null) ? RandomStyleBuilder.createRandomVectorStyle(col.getFeatureType()) : baseStyle;
            final FeatureMapLayer fml = MapBuilder.createFeatureLayer(col, style);
            fml.setVisible(visible);
            fml.setName(name.getLocalPart());
            layers.add(fml);
        }
        return layers;
    }

    @Override
    public List<MenuItem> getMapActions(Object obj) {
        final List<MenuItem> lst = new ArrayList<>();
        
        if(obj instanceof TronconDigue){
            final TronconDigue candidate = (TronconDigue) obj;
            final String nom = candidate.getNom();
            lst.add(new MenuItem(nom));            
            final String docId = candidate.getDocumentId();
            
        }else if(obj instanceof Structure){
            final Structure candidate = (Structure) obj;
            final String docId = candidate.getDocumentId();
            lst.add(new MenuItem(docId));
            
        }
        
        return lst;
    }
    
    @Override
    public void load() throws SQLException, IOException {
        themes.add(new StructuresTheme());
        themes.add(new FrancBordTheme());
        themes.add(new ReseauxDeVoirieTheme());
        themes.add(new ReseauxEtOuvragesTheme());
        themes.add(new DesordreTheme());
        themes.add(new PrestationsTheme());
        themes.add(new MesureEvenementsTheme());
        themes.add(new EmpriseCommunaleTheme());
        themes.add(new ProfilsEnTraversTheme());
        themes.add(new ContactsTheme());
        themes.add(new EvenementsHydrauliquesTheme());
        themes.add(new DocumentsTheme());
        
    }

    private static MutableStyle createTronconStyle(){
        final Stroke stroke1 = GO2Utilities.STYLE_FACTORY.stroke(SF.literal(Color.DARK_GRAY),FF.literal(4),LITERAL_ONE_FLOAT);
        final LineSymbolizer line1 = GO2Utilities.STYLE_FACTORY.lineSymbolizer("symbol",
                (String)null,DEFAULT_DESCRIPTION,NonSI.PIXEL,stroke1,LITERAL_ONE_FLOAT);
        
        final Stroke stroke2 = GO2Utilities.STYLE_FACTORY.stroke(SF.literal(Color.WHITE),FF.literal(2),LITERAL_ONE_FLOAT);
        final LineSymbolizer line2 = GO2Utilities.STYLE_FACTORY.lineSymbolizer("symbol",
                (String)null,DEFAULT_DESCRIPTION,NonSI.PIXEL,stroke2,LITERAL_ONE_FLOAT);
        
        
        final MutableStyle style = GO2Utilities.STYLE_FACTORY.style(line1,line2);
        return style;
    }
    
    private static MutableStyle createBorneStyle(){
        final Expression size = GO2Utilities.FILTER_FACTORY.literal(7);

        final List<GraphicalSymbol> symbols = new ArrayList<GraphicalSymbol>();
        final Stroke stroke = GO2Utilities.STYLE_FACTORY.stroke(Color.BLACK, 1);
        final Fill fill = GO2Utilities.STYLE_FACTORY.fill(Color.WHITE);
        final Mark mark = GO2Utilities.STYLE_FACTORY.mark(StyleConstants.MARK_CIRCLE, fill, stroke);
        symbols.add(mark);
        final Graphic graphic = GO2Utilities.STYLE_FACTORY.graphic(symbols, LITERAL_ONE_FLOAT, 
                size, LITERAL_ONE_FLOAT, DEFAULT_ANCHOR_POINT, DEFAULT_DISPLACEMENT);

        final PointSymbolizer pointSymbolizer = GO2Utilities.STYLE_FACTORY.pointSymbolizer("symbol",(String)null,DEFAULT_DESCRIPTION,NonSI.PIXEL,graphic);
        
        final MutableRule ruleClose = GO2Utilities.STYLE_FACTORY.rule(pointSymbolizer);
        ruleClose.setMaxScaleDenominator(50000);
        
        final MutableFeatureTypeStyle fts = GO2Utilities.STYLE_FACTORY.featureTypeStyle();
        fts.rules().add(ruleClose);
        final MutableStyle style = GO2Utilities.STYLE_FACTORY.style();
        style.featureTypeStyles().add(fts);
        return style;
    }
    
    private static MutableStyle createStructureStyle(Color col){
        final Expression offset = GO2Utilities.FILTER_FACTORY.literal(6);
        final Expression color = GO2Utilities.STYLE_FACTORY.literal(col);
        final Expression width = GO2Utilities.FILTER_FACTORY.literal(2);
        final Stroke lineStroke = GO2Utilities.STYLE_FACTORY.stroke(color,width,LITERAL_ONE_FLOAT);
        final LineSymbolizer lineSymbolizer = GO2Utilities.STYLE_FACTORY.lineSymbolizer("symbol",
                (String)null,DEFAULT_DESCRIPTION,NonSI.PIXEL,lineStroke,offset);
        
        //the visual element
        final Expression size = GO2Utilities.FILTER_FACTORY.literal(13);

        final List<GraphicalSymbol> symbols = new ArrayList<GraphicalSymbol>();
        final Stroke stroke = GO2Utilities.STYLE_FACTORY.stroke(Color.WHITE, 0);
        final Fill fill = GO2Utilities.STYLE_FACTORY.fill(col);
        final Mark mark = GO2Utilities.STYLE_FACTORY.mark(StyleConstants.MARK_TRIANGLE, fill, stroke);
        symbols.add(mark);
        final Graphic graphic = GO2Utilities.STYLE_FACTORY.graphic(symbols, LITERAL_ONE_FLOAT, 
                size, LITERAL_ONE_FLOAT, DEFAULT_ANCHOR_POINT, DEFAULT_DISPLACEMENT);

        final PointSymbolizer pointSymbolizer = GO2Utilities.STYLE_FACTORY.pointSymbolizer("symbol",(String)null,DEFAULT_DESCRIPTION,NonSI.PIXEL,graphic);
        
        final MutableRule ruleClose = GO2Utilities.STYLE_FACTORY.rule(lineSymbolizer);
        ruleClose.setMaxScaleDenominator(500000);
        final MutableRule ruleFar = GO2Utilities.STYLE_FACTORY.rule(pointSymbolizer);
        ruleFar.setMinScaleDenominator(500000);
        
        final MutableFeatureTypeStyle fts = GO2Utilities.STYLE_FACTORY.featureTypeStyle();
        fts.rules().add(ruleClose);
        fts.rules().add(ruleFar);
        
        final MutableStyle style = GO2Utilities.STYLE_FACTORY.style();
        style.featureTypeStyles().add(fts);
        return style;
    }
    
}
