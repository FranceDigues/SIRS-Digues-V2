package fr.sirs.importer.objet.geometry;

import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.Row;
import fr.sirs.core.model.RefLargeurFrancBord;
import fr.sirs.importer.DbImporter;
import fr.sirs.importer.GenericTypeImporter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import org.ektorp.CouchDbConnector;

/**
 *
 * @author Samuel Andrés (Geomatys)
 */
class TypeLargeurFrancBordImporter extends GenericTypeImporter<RefLargeurFrancBord> {
    
    TypeLargeurFrancBordImporter(final Database accessDatabase, 
            final CouchDbConnector couchDbConnector) {
        super(accessDatabase, couchDbConnector);
    }

    private enum Columns {
        ID_TYPE_LARGEUR_FB,
        ABREGE_TYPE_LARGEUR_FB,
        LIBELLE_TYPE_LARGEUR_FB,
        DATE_DERNIERE_MAJ
    };
    
    @Override
    public List<String> getUsedColumns() {
        final List<String> columns = new ArrayList<>();
        for (Columns c : Columns.values()) {
            columns.add(c.toString());
        }
        return columns;
    }

    @Override
    public String getTableName() {
        return DbImporter.TableName.TYPE_LARGEUR_FRANC_BORD.toString();
    }

    @Override
    protected void compute() throws IOException {
        types = new HashMap<>();
        
        final Iterator<Row> it = accessDatabase.getTable(getTableName()).iterator();
        while (it.hasNext()) {
            final Row row = it.next();
            final RefLargeurFrancBord typeLargeur = new RefLargeurFrancBord();
            
            typeLargeur.setLibelle(row.getString(Columns.LIBELLE_TYPE_LARGEUR_FB.toString()));
            typeLargeur.setAbrege(row.getString(Columns.ABREGE_TYPE_LARGEUR_FB.toString()));
            if (row.getDate(Columns.DATE_DERNIERE_MAJ.toString()) != null) {
                typeLargeur.setDateMaj(LocalDateTime.parse(row.getDate(Columns.DATE_DERNIERE_MAJ.toString()).toString(), dateTimeFormatter));
            }
            types.put(row.getInt(String.valueOf(Columns.ID_TYPE_LARGEUR_FB.toString())), typeLargeur);
        }
        couchDbConnector.executeBulk(types.values());
    }
}
