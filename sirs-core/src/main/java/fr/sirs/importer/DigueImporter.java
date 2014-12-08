package fr.sirs.importer;

import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.Row;
import fr.sirs.core.model.Digue;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.ektorp.CouchDbConnector;

/**
 *
 * @author Samuel Andrés (Geomatys)
 */
public class DigueImporter extends GenericImporter {

    private Map<Integer, Digue> digues = null;
    
    DigueImporter(final Database accessDatabase,
            final CouchDbConnector couchDbConnector) {
        super(accessDatabase, couchDbConnector);
    }
    
    private enum Columns {
        ID_DIGUE, 
        LIBELLE_DIGUE, 
        COMMENTAIRE_DIGUE, 
        DATE_DERNIERE_MAJ
    };
    
    /**
     * 
     * @return A map containing all Digue instances accessibles from 
     * the internal database identifier.
     * @throws IOException 
     */
    public Map<Integer, Digue> getDigues() throws IOException {
        if(digues==null) compute();
        return digues;
    }
    
    @Override
    protected List<String> getUsedColumns() {
        final List<String> columns = new ArrayList<>();
        for(Columns c : Columns.values())
            columns.add(c.toString());
        return columns;
    }

    @Override
    public String getTableName() {
        return DbImporter.TableName.DIGUE.toString();
    }

    @Override
    protected void compute() throws IOException {
        digues = new HashMap<>();
        
        final Iterator<Row> it = this.accessDatabase.getTable(getTableName()).iterator();
        while (it.hasNext()) {
            final Row row = it.next();
            final Digue digue = new Digue();

            digue.setLibelle(row.getString(Columns.LIBELLE_DIGUE.toString()));
            digue.setCommentaire(row.getString(Columns.COMMENTAIRE_DIGUE.toString()));
            if (row.getDate(Columns.DATE_DERNIERE_MAJ.toString()) != null) {
                digue.setDateMaj(LocalDateTime.parse(row.getDate(Columns.DATE_DERNIERE_MAJ.toString()).toString(), dateTimeFormatter));
            }

            // Don't set the old ID, but save it into the dedicated map in order to keep the reference.
            //digue.setId(String.valueOf(row.getInt(DigueColumns.ID.toString())));
            digues.put(row.getInt(Columns.ID_DIGUE.toString()), digue);
        }
        couchDbConnector.executeBulk(digues.values());
    }
}
