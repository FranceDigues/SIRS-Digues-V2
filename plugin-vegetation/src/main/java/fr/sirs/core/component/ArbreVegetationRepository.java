

package fr.sirs.core.component;


import fr.sirs.core.InjectorCore;
import fr.sirs.core.SessionCore;
import static fr.sirs.core.component.ArbreVegetationRepository.BY_PARCELLE_ID;

import org.ektorp.CouchDbConnector;
import org.ektorp.support.View;
import org.ektorp.support.Views;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Component;
import fr.sirs.core.model.ArbreVegetation;
import java.util.List;
import org.apache.sis.util.ArgumentChecks;

/**
 * Outil g�rant les �changes avec la bdd CouchDB pour tous les objets ArbreVegetation.
 * 
 * @author Olivier Nouguier (Geomatys)
 * @author Alexis Manin     (Geomatys)
 */
@Views({
@View(name=BY_PARCELLE_ID, map="function(doc) {if(doc['@class']=='fr.sirs.core.model.ArbreVegetation') {emit(doc.parcelleId, doc._id)}}"),
@View(name="all", map="function(doc) {if(doc['@class']=='fr.sirs.core.model.ArbreVegetation') {emit(doc._id, doc._id)}}")
})
@Component("fr.sirs.core.component.ArbreVegetationRepository")
public class ArbreVegetationRepository extends AbstractSIRSRepository<ArbreVegetation> {
    
    public static final String BY_PARCELLE_ID = "byParcelleId";
        
    @Autowired
    private ArbreVegetationRepository ( CouchDbConnector db) {
       super(ArbreVegetation.class, db);
       initStandardDesignDocument();
   }
    
    @Override
    public ArbreVegetation create() {
        return InjectorCore.getBean(SessionCore.class).getElementCreator().createElement(ArbreVegetation.class);
    }
    
    public List<ArbreVegetation> getByParcelleId(final String parcelleId) {
        ArgumentChecks.ensureNonNull("Parcelle", parcelleId);
        return this.queryView(BY_PARCELLE_ID, parcelleId);
    }

    public List<ArbreVegetation> getByParcelleIds(final String ... parcelleIds) {
        ArgumentChecks.ensureNonNull("Parcelles", parcelleIds);
        return this.queryView(BY_PARCELLE_ID, (Object[])parcelleIds);
    }

}

