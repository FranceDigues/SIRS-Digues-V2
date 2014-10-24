/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.sym.util.importer.structure;

import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.Row;
import fr.sym.util.importer.AccessDbImporterException;
import fr.sym.util.importer.GenericImporter;
import fr.sym.util.importer.TronconGestionDigueImporter;
import fr.symadrem.sirs.core.model.Desordre;
import fr.symadrem.sirs.core.model.TronconDigue;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Samuel Andrés (Geomatys)
 */
class DesordreImporter extends GenericImporter {

    private Map<Integer, Desordre> desordres = null;
    private Map<Integer, List<Desordre>> desordresByTronconId = null;
    private TronconGestionDigueImporter tronconGestionDigueImporter;

    private DesordreImporter(Database accessDatabase) {
        super(accessDatabase);
    }

    DesordreImporter(final Database accessDatabase, final TronconGestionDigueImporter tronconGestionDigueImporter) {
        this(accessDatabase);
        this.tronconGestionDigueImporter = tronconGestionDigueImporter;
    }

    @Override
    public String getTableName() {
        return "SYS_EVT_DESORDRE";
    }
    
    
    private enum DesordreColumns {
            ID_DESORDRE,
//            id_nom_element,
//            ID_SOUS_GROUPE_DONNEES,
//            LIBELLE_SOUS_GROUPE_DONNEES,
//            ID_TYPE_DESORDRE,
//            LIBELLE_TYPE_DESORDRE,
//            DECALAGE_DEFAUT,
//            DECALAGE,
//            LIBELLE_SOURCE,
//            LIBELLE_TYPE_COTE,
//            LIBELLE_SYSTEME_REP,
//            NOM_BORNE_DEBUT,
//            NOM_BORNE_FIN,
//            DISPARU_OUI_NON,
//            DEJA_OBSERVE_OUI_NON,
//            LIBELLE_TYPE_POSITION,
//            ID_TYPE_COTE,
//            ID_TYPE_POSITION,
            ID_TRONCON_GESTION,
//            ID_SOURCE,
//            DATE_DEBUT_VAL,
//            DATE_FIN_VAL,
//            PR_DEBUT_CALCULE,
//            PR_FIN_CALCULE,
//            ID_SYSTEME_REP,
//            ID_BORNEREF_DEBUT,
//            AMONT_AVAL_DEBUT,
            DIST_BORNEREF_DEBUT,
//            ID_BORNEREF_FIN,
//            AMONT_AVAL_FIN,
            DIST_BORNEREF_FIN,
//            LIEU_DIT_DESORDRE,
//            DESCRIPTION_DESORDRE,
//            ID_AUTO
     
     //Empty fields
//     ID_PRESTATION,
//     LIBELLE_PRESTATION,
//     X_DEBUT,
//     Y_DEBUT,
//     X_FIN,
//     Y_FIN,
//     COMMENTAIRE,
    };
    
    /**
     *
     * @return A map containing all TronconDigue instances accessibles from the
     * internal database identifier.
     * @throws IOException
     * @throws AccessDbImporterException
     */
    public Map<Integer, Desordre> getDesordres() throws IOException, AccessDbImporterException {

        final Iterator<Row> it = this.accessDatabase.getTable(getTableName()).iterator();

        if (this.desordres == null) {
            this.desordres = new HashMap<>();
            this.desordresByTronconId = new HashMap<>();
            while (it.hasNext()) {
                final Row row = it.next();
                final Desordre desordre = new Desordre();
                
                
                if (row.getDouble(DesordreColumns.DIST_BORNEREF_DEBUT.toString()) != null) {
                    desordre.setBorne_debut_distance(row.getDouble(DesordreColumns.DIST_BORNEREF_DEBUT.toString()).floatValue());
                }
             if (row.getDouble(DesordreColumns.DIST_BORNEREF_FIN.toString()) != null) {
                    desordre.setBorne_fin_distance(row.getDouble(DesordreColumns.DIST_BORNEREF_FIN.toString()).floatValue());
                }
                
                final TronconDigue troncon = tronconGestionDigueImporter.getTronconsDigues().get(row.getInt(DesordreColumns.ID_TRONCON_GESTION.toString()));
                if (troncon.getId() != null) {
                    desordre.setTroncon(troncon.getId());
                } else {
                    throw new AccessDbImporterException("Le tronçon "
                            + tronconGestionDigueImporter.getTronconsDigues().get(row.getInt(DesordreColumns.ID_TRONCON_GESTION.toString())) + " n'a pas encore d'identifiant CouchDb !");
                }

//            tronconDigue.setNom(row.getString(TronconGestionDigueColumns.NOM.toString()));
//            tronconDigue.setCommentaire(row.getString(TronconGestionDigueColumns.COMMENTAIRE.toString()));
//            if (row.getDate(TronconGestionDigueColumns.MAJ.toString()) != null) {
//                tronconDigue.setDateMaj(LocalDateTime.parse(row.getDate(TronconGestionDigueColumns.MAJ.toString()).toString(), dateTimeFormatter));
//            }
//            if (row.getDate(TronconGestionDigueColumns.DEBUT_VAL_TRONCON.toString()) != null) {
//                tronconDigue.setDate_debut(LocalDateTime.parse(row.getDate(TronconGestionDigueColumns.DEBUT_VAL_TRONCON.toString()).toString(), dateTimeFormatter));
//            }
//            if (row.getDate(TronconGestionDigueColumns.FIN_VAL_TRONCON.toString()) != null) {
//                tronconDigue.setDate_fin(LocalDateTime.parse(row.getDate(TronconGestionDigueColumns.FIN_VAL_TRONCON.toString()).toString(), dateTimeFormatter));
//            }
//
                // Don't set the old ID, but save it into the dedicated map in order to keep the reference.
                //tronconDigue.setId(String.valueOf(row.getString(TronconDigueColumns.ID.toString())));
                desordres.put(row.getInt(DesordreColumns.ID_DESORDRE.toString()), desordre);

                // Set the list ByTronconId
                List<Desordre> listByTronconId = desordresByTronconId.get(row.getInt(DesordreColumns.ID_TRONCON_GESTION.toString()));
                if (listByTronconId == null) {
                    listByTronconId = new ArrayList<>();
                    desordresByTronconId.put(row.getInt(DesordreColumns.ID_TRONCON_GESTION.toString()), listByTronconId);
                }
                listByTronconId.add(desordre);
                desordresByTronconId.put(row.getInt(DesordreColumns.ID_TRONCON_GESTION.toString()), listByTronconId);
//
//            // Set the references.
//            tronconDigue.setDigueId(digueIds.get(row.getInt(TronconGestionDigueColumns.DIGUE.toString())).getId());
//            
//            final List<GestionTroncon> gestions = new ArrayList<>();
//            this.getGestionnaires().stream().forEach((gestion) -> {gestions.add(gestion);});
//            tronconDigue.setGestionnaires(gestions);
//            
//            tronconDigue.setTypeRive(typesRive.get(row.getInt(TronconGestionDigueColumns.TYPE_RIVE.toString())).toString());
//
//            // Set the geometry
//            tronconDigue.setGeometry(tronconDigueGeoms.get(row.getInt(TronconGestionDigueColumns.ID.toString())));
//            
//            tronconsDigues.add(tronconDigue);
            }
        }
        return desordres;
    }

    /**
     *
     * @return A map containing all TronconDigue instances accessibles from the
     * internal database <em>TronconDigue</em> identifier.
     * @throws IOException
     * @throws AccessDbImporterException
     */
    public Map<Integer, List<Desordre>> getDesordresByTronconId() throws IOException, AccessDbImporterException {
        if (this.desordresByTronconId == null) {
            this.getDesordres();
        }
        return this.desordresByTronconId;
    }

    @Override
    public List<String> getUsedColumns() {
        final List<String> columns = new ArrayList<>();
        for(DesordreColumns c : DesordreColumns.values())
            columns.add(c.toString());
        return columns;
    }
}
