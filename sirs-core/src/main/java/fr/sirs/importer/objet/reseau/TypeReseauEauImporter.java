package fr.sirs.importer.objet.reseau;

import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.Row;
import static fr.sirs.core.model.ElementCreator.createAnonymValidElement;
import fr.sirs.core.model.RefReseauHydroCielOuvert;
import fr.sirs.importer.DbImporter;
import static fr.sirs.importer.DbImporter.TableName.*;
import fr.sirs.importer.GenericTypeReferenceImporter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import org.ektorp.CouchDbConnector;

/**
 *
 * @author Samuel Andrés (Geomatys)
 */
class TypeReseauEauImporter extends GenericTypeReferenceImporter<RefReseauHydroCielOuvert> {
    
    TypeReseauEauImporter(final Database accessDatabase, 
            final CouchDbConnector couchDbConnector) {
        super(accessDatabase, couchDbConnector);
    }

    private enum Columns {
        ID_TYPE_RESEAU_EAU,
        LIBELLE_TYPE_RESEAU_EAU,
        ABREGE_TYPE_RESEAU_EAU,
        DATE_DERNIERE_MAJ
    };
    
    @Override
    protected List<String> getUsedColumns() {
        final List<String> columns = new ArrayList<>();
        for (Columns c : Columns.values()) {
            columns.add(c.toString());
        }
        return columns;
    }

    @Override
    public String getTableName() {
        return TYPE_RESEAU_EAU.toString();
    }

    @Override
    protected void compute() throws IOException {
        types = new HashMap<>();
        
        final Iterator<Row> it = accessDatabase.getTable(getTableName()).iterator();
        while (it.hasNext()) {
            final Row row = it.next();
            final RefReseauHydroCielOuvert typeReseau = createAnonymValidElement(RefReseauHydroCielOuvert.class);
            
            typeReseau.setId(typeReseau.getClass().getSimpleName()+":"+row.getInt(String.valueOf(Columns.ID_TYPE_RESEAU_EAU.toString())));
            typeReseau.setLibelle(row.getString(Columns.LIBELLE_TYPE_RESEAU_EAU.toString()));
            typeReseau.setAbrege(row.getString(Columns.ABREGE_TYPE_RESEAU_EAU.toString()));
            if (row.getDate(Columns.DATE_DERNIERE_MAJ.toString()) != null) {
                typeReseau.setDateMaj(DbImporter.parseLocalDateTime(row.getDate(Columns.DATE_DERNIERE_MAJ.toString()), dateTimeFormatter));
            }
            typeReseau.setDesignation(String.valueOf(row.getInt(String.valueOf(Columns.ID_TYPE_RESEAU_EAU.toString()))));
            
            types.put(row.getInt(String.valueOf(Columns.ID_TYPE_RESEAU_EAU.toString())), typeReseau);
        }
        couchDbConnector.executeBulk(types.values());
    }
}
