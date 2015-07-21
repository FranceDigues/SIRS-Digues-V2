package fr.sirs.importer.documentTroncon.document;

import com.healthmarketscience.jackcess.Database;
import fr.sirs.core.model.RefTypeDocument;
import fr.sirs.importer.GenericImporter;
import java.util.Map;
import org.ektorp.CouchDbConnector;

/**
 *
 * @author Samuel Andrés (Geomatys)
 */
public abstract class TypeDocumentImporter extends GenericImporter {
    
    protected Map<Integer, Class> classesDocument = null;
    protected Map<Integer, RefTypeDocument> typesDocument = null;
    
    public TypeDocumentImporter(final Database accessDatabase,
            final CouchDbConnector couchDbConnector) {
        super(accessDatabase, couchDbConnector);
    }
}
