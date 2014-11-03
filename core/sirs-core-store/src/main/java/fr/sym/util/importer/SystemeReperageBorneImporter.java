/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.sym.util.importer;

import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.Row;
import fr.symadrem.sirs.core.component.SystemeReperageRepository;
import fr.symadrem.sirs.core.model.BorneDigue;
import fr.symadrem.sirs.core.model.SystemeReperage;
import fr.symadrem.sirs.core.model.SystemeReperageBorne;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.ektorp.CouchDbConnector;

/**
 *
 * @author Samuel Andrés (Geomatys)
 */
public class SystemeReperageBorneImporter extends GenericImporter {

    private Map<Integer, List<SystemeReperageBorne>> byBorneId = null;
    private Map<Integer, List<SystemeReperageBorne>> bySystemeReperageId = null;
    private SystemeReperageImporter systemeReperageImporter;
    private BorneDigueImporter borneDigueImporter;
    private SystemeReperageRepository repo;

    private SystemeReperageBorneImporter(final Database accessDatabase,
            final CouchDbConnector couchDbConnector, SystemeReperageRepository repo) {
        super(accessDatabase, couchDbConnector);
        this.repo = repo;
    }

    SystemeReperageBorneImporter(final Database accessDatabase,
            final CouchDbConnector couchDbConnector, 
            final SystemeReperageImporter systemeReperageImporter,
            final BorneDigueImporter borneDigueImporter,
            SystemeReperageRepository repo) {
        this(accessDatabase, couchDbConnector,repo);
        this.systemeReperageImporter = systemeReperageImporter;
        this.borneDigueImporter = borneDigueImporter;
    }

    private enum SystemeReperageBorneColumns {
        ID_BORNE,
        ID_SYSTEME_REP,
        VALEUR_PR,
        DATE_DERNIERE_MAJ
    };

    /**
     *
     * @return A map containing all SystemeReperageBorne instances accessibles from
     * the internal database <em>BorneDigue</em> identifier.
     * @throws IOException
     * @throws fr.sym.util.importer.AccessDbImporterException
     */
    public Map<Integer, List<SystemeReperageBorne>> getByBorneId() throws IOException, AccessDbImporterException {
        if (byBorneId == null) compute();
        return byBorneId;
    }

    /**
     *
     * @return A map containing all SystemeReperageBorne instances accessibles from
     * the internal database <em>SystemeReperage</em> identifier.
     * @throws IOException
     * @throws fr.sym.util.importer.AccessDbImporterException
     */
    public Map<Integer, List<SystemeReperageBorne>> getBySystemeReperageId() throws IOException, AccessDbImporterException {
        if (bySystemeReperageId == null) compute();
        return bySystemeReperageId;
    }

    @Override
    public List<String> getUsedColumns() {
        final List<String> columns = new ArrayList<>();
        for (SystemeReperageBorneColumns c : SystemeReperageBorneColumns.values()) {
            columns.add(c.toString());
        }
        return columns;
    }

    @Override
    public String getTableName() {
        return DbImporter.TableName.BORNE_PAR_SYSTEME_REP.toString();
    }

    @Override
    protected void compute() throws IOException, AccessDbImporterException {
        byBorneId = new HashMap<>();
        bySystemeReperageId = new HashMap<>();
        final Iterator<Row> it = this.accessDatabase.getTable(getTableName()).iterator();
        
        final Map<Object,SystemeReperage> srs = new HashMap<>();
        
        while (it.hasNext()) {
            final Row row = it.next();
            final SystemeReperageBorne systemeReperageBorne = new SystemeReperageBorne();

            if (row.getDate(SystemeReperageBorneColumns.DATE_DERNIERE_MAJ.toString()) != null) {
                systemeReperageBorne.setDateMaj(LocalDateTime.parse(row.getDate(SystemeReperageBorneColumns.DATE_DERNIERE_MAJ.toString()).toString(), dateTimeFormatter));
            }
            
            if (row.getDouble(SystemeReperageBorneColumns.VALEUR_PR.toString())!=null){
//                System.out.println(row.getDouble(SystemeReperageBorneColumns.VALEUR_PR.toString()));
                systemeReperageBorne.setValeurPR(row.getDouble(SystemeReperageBorneColumns.VALEUR_PR.toString()).floatValue());
            }
            
            final BorneDigue borne = borneDigueImporter.getBorneDigue().get(row.getInt(SystemeReperageBorneColumns.ID_BORNE.toString()));
            systemeReperageBorne.setBorneId(borne.getId());
            final Integer srid = row.getInt(SystemeReperageBorneColumns.ID_SYSTEME_REP.toString());
            SystemeReperage systemeReperage = srs.get(srid);
            if(systemeReperage==null){
                systemeReperage = systemeReperageImporter.getSystemeRepLineaire().get(srid);
                srs.put(srid,systemeReperage);
            }
            systemeReperage.systemereperageborneId.add(systemeReperageBorne);
            
            if(byBorneId.get(row.getInt(SystemeReperageBorneColumns.ID_BORNE.toString()))==null) byBorneId.put(row.getInt(SystemeReperageBorneColumns.ID_BORNE.toString()), new ArrayList<>());
            byBorneId.get(row.getInt(SystemeReperageBorneColumns.ID_BORNE.toString())).add(systemeReperageBorne);
            
            if(bySystemeReperageId.get(row.getInt(SystemeReperageBorneColumns.ID_SYSTEME_REP.toString()))==null) bySystemeReperageId.put(row.getInt(SystemeReperageBorneColumns.ID_SYSTEME_REP.toString()), new ArrayList<>());
            bySystemeReperageId.get(row.getInt(SystemeReperageBorneColumns.ID_SYSTEME_REP.toString())).add(systemeReperageBorne);
        }
        
        //sauvegarde des systemes de reperages
        for(SystemeReperage sr : srs.values()){
            repo.update(sr);
        }
        
                
    }
}
