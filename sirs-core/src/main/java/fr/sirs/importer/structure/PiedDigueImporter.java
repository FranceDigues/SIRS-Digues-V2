package fr.sirs.importer.structure;

import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.Row;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import fr.sirs.importer.AccessDbImporterException;
import fr.sirs.importer.BorneDigueImporter;
import fr.sirs.importer.DbImporter;
import fr.sirs.importer.SystemeReperageImporter;
import fr.sirs.importer.TronconGestionDigueImporter;
import fr.sirs.core.model.BorneDigue;
import fr.sirs.core.model.PiedDigue;
import fr.sirs.core.model.RefCote;
import fr.sirs.core.model.RefMateriau;
import fr.sirs.core.model.RefPosition;
import fr.sirs.core.model.RefSource;
import fr.sirs.core.model.SystemeReperage;
import fr.sirs.core.model.TronconDigue;
import fr.sirs.importer.OrganismeImporter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ektorp.CouchDbConnector;
import org.geotoolkit.geometry.jts.JTS;
import org.geotoolkit.referencing.CRS;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

/**
 *
 * @author Samuel Andrés (Geomatys)
 */
class PiedDigueImporter extends GenericStructureImporter {

    private Map<Integer, PiedDigue> piedsDigue = null;
    private Map<Integer, List<PiedDigue>> piedsDigueByTronconId = null;

    PiedDigueImporter(final Database accessDatabase, 
            final CouchDbConnector couchDbConnector,
            final TronconGestionDigueImporter tronconGestionDigueImporter, 
            final SystemeReperageImporter systemeReperageImporter, 
            final BorneDigueImporter borneDigueImporter, 
            final OrganismeImporter organismeImporter,
            final TypeSourceImporter typeSourceImporter,
            final TypePositionImporter typePositionImporter,
            final TypeCoteImporter typeCoteImporter,
            final TypeMateriauImporter typeMateriauImporter, 
            final TypeNatureImporter typeNatureImporter) {
        super(accessDatabase, couchDbConnector, tronconGestionDigueImporter, 
                systemeReperageImporter, borneDigueImporter, organismeImporter,
                typeSourceImporter, typeCoteImporter, typePositionImporter, 
                typeMateriauImporter, typeNatureImporter);
    }

    private enum PiedDigueColumns {

        ID_ELEMENT_STRUCTURE,
        //        id_nom_element, // Inutile
        //        ID_SOUS_GROUPE_DONNEES, // Redondant
        //        LIBELLE_TYPE_ELEMENT_STRUCTURE, //Redondant
        //        DECALAGE_DEFAUT, // Affichage
        //        DECALAGE, //Affichage
        //        LIBELLE_SOURCE, // Dans le TypeSourceImporter
        //        LIBELLE_TYPE_COTE, // Dans le TypeCoteImporter
        //        LIBELLE_SYSTEME_REP, //Dans le SystemeReperageImporter
        //        NOM_BORNE_DEBUT, // Dans le BorneImporter
        //        NOM_BORNE_FIN, // Dans le BorneImporter
        //        LIBELLE_TYPE_MATERIAU, // Redondant avec l'importation des matériaux
        //        LIBELLE_TYPE_NATURE,
        //        LIBELLE_TYPE_FONCTION,
        //        ID_TYPE_ELEMENT_STRUCTURE, //Dans le TypeElementStructureImporter
                ID_TYPE_COTE,
                ID_SOURCE,
        ID_TRONCON_GESTION,
                DATE_DEBUT_VAL,
        PR_DEBUT_CALCULE,
        PR_FIN_CALCULE,
                ID_SYSTEME_REP,
                ID_BORNEREF_DEBUT,
                AMONT_AVAL_DEBUT,
        DIST_BORNEREF_DEBUT,
                ID_BORNEREF_FIN,
                AMONT_AVAL_FIN,
        DIST_BORNEREF_FIN,
        COMMENTAIRE,
//        N_COUCHE, // À ignorer (probablement une valeur par défaut parasite)
        ID_TYPE_MATERIAU,
//        ID_TYPE_NATURE,
//        ID_TYPE_FONCTION,
//        ID_AUTO

        // Empty fields
//     LIBELLE_TYPE_NATURE_HAUT,
//     LIBELLE_TYPE_MATERIAU_HAUT, // Redondant avec l'importation des matériaux
//     LIBELLE_TYPE_NATURE_BAS,
//     LIBELLE_TYPE_MATERIAU_BAS, // Redondant avec l'importation des matériaux
//     LIBELLE_TYPE_OUVRAGE_PARTICULIER,
//     LIBELLE_TYPE_POSITION, // Dans le TypePositionImporter
//     RAISON_SOCIALE_ORG_PROPRIO,
//     RAISON_SOCIALE_ORG_GESTION,
//     INTERV_PROPRIO,
//     INTERV_GARDIEN,
//     LIBELLE_TYPE_COMPOSITION,
//     LIBELLE_TYPE_VEGETATION,
             DATE_FIN_VAL,
             X_DEBUT,
             Y_DEBUT,
             X_FIN,
             Y_FIN,
        //     EPAISSEUR, // N'existe pas dans le modèle des pieds de digue
        //     TALUS_INTERCEPTE_CRETE,
        //     ID_TYPE_NATURE_HAUT,
        //     ID_TYPE_MATERIAU_HAUT, // Pas dans le nouveau modèle
        //     ID_TYPE_MATERIAU_BAS, // Pas dans le nouveau modèle
        //     ID_TYPE_NATURE_BAS,
        //     LONG_RAMP_HAUT,
        //     LONG_RAMP_BAS,
        //     PENTE_INTERIEURE,
        //     ID_TYPE_OUVRAGE_PARTICULIER,
             ID_TYPE_POSITION,
        //     ID_ORG_PROPRIO,
        //     ID_ORG_GESTION,
        //     ID_INTERV_PROPRIO,
        //     ID_INTERV_GARDIEN,
        //     DATE_DEBUT_ORGPROPRIO,
        //     DATE_FIN_ORGPROPRIO,
        //     DATE_DEBUT_GESTION,
        //     DATE_FIN_GESTION,
        //     DATE_DEBUT_INTERVPROPRIO,
        //     DATE_FIN_INTERVPROPRIO,
        //     ID_TYPE_COMPOSITION,
        //     DISTANCE_TRONCON,
        //     LONGUEUR,
        //     DATE_DEBUT_GARDIEN,
        //     DATE_FIN_GARDIEN,
        //     LONGUEUR_PERPENDICULAIRE,
        //     LONGUEUR_PARALLELE,
        //     COTE_AXE,
        //     ID_TYPE_VEGETATION,
        //     HAUTEUR,
        //     DIAMETRE,
        //     DENSITE,
        //     EPAISSEUR_Y11,
        //     EPAISSEUR_Y12,
        //     EPAISSEUR_Y21,
        //     EPAISSEUR_Y22,
    };

    /**
     *
     * @return A map containing all TronconDigue instances accessibles from the
     * internal database identifier.
     * @throws IOException
     * @throws AccessDbImporterException
     */
    public Map<Integer, PiedDigue> getPiedsDigue() throws IOException, AccessDbImporterException {
        if (this.piedsDigue == null) {
            compute();
        }
        return piedsDigue;
    }

    /**
     *
     * @return A map containing all TronconDigue instances accessibles from the
     * internal database <em>TronconDigue</em> identifier.
     * @throws IOException
     * @throws AccessDbImporterException
     */
    public Map<Integer, List<PiedDigue>> getPiedsDigueByTronconId() throws IOException, AccessDbImporterException {
        if (this.piedsDigueByTronconId == null) {
            compute();
        }
        return this.piedsDigueByTronconId;
    }

    @Override
    public List<String> getUsedColumns() {
        final List<String> columns = new ArrayList<>();
        for (PiedDigueColumns c : PiedDigueColumns.values()) {
            columns.add(c.toString());
        }
        return columns;
    }

    @Override
    public String getTableName() {
        return DbImporter.TableName.SYS_EVT_PIED_DE_DIGUE.toString();
    }

    @Override
    protected void compute() throws IOException, AccessDbImporterException {

        this.piedsDigue = new HashMap<>();
        this.piedsDigueByTronconId = new HashMap<>();
        
        final Map<Integer, BorneDigue> bornes = borneDigueImporter.getBorneDigue();
        final Map<Integer, SystemeReperage> systemesReperage = systemeReperageImporter.getSystemeRepLineaire();
        final Map<Integer, TronconDigue> troncons = tronconGestionDigueImporter.getTronconsDigues();
        final Map<Integer, RefSource> typesSource = typeSourceImporter.getTypeSource();
        final Map<Integer, RefPosition> typesPosition = typePositionImporter.getTypePosition();
        final Map<Integer, RefCote> typesCote = typeCoteImporter.getTypeCote();
        final Map<Integer, RefMateriau> typesMateriau = typeMateriauImporter.getTypeMateriau();

        final Iterator<Row> it = this.accessDatabase.getTable(getTableName()).iterator();
        while (it.hasNext()) {
            final Row row = it.next();
            final PiedDigue piedDigue = new PiedDigue();
            
            if(row.getInt(PiedDigueColumns.ID_TYPE_COTE.toString())!=null){
                piedDigue.setCoteId(typesCote.get(row.getInt(PiedDigueColumns.ID_TYPE_COTE.toString())).getId());
            }
            
            if(row.getInt(PiedDigueColumns.ID_SOURCE.toString())!=null){
                piedDigue.setSourceId(typesSource.get(row.getInt(PiedDigueColumns.ID_SOURCE.toString())).getId());
            }
            
            final TronconDigue troncon = troncons.get(row.getInt(PiedDigueColumns.ID_TRONCON_GESTION.toString()));
            if (troncon.getId() != null) {
                piedDigue.setTroncon(troncon.getId());
            } else {
                throw new AccessDbImporterException("Le tronçon "
                        + troncons.get(row.getInt(PiedDigueColumns.ID_TRONCON_GESTION.toString())) + " n'a pas encore d'identifiant CouchDb !");
            }
            
            if (row.getDate(PiedDigueColumns.DATE_DEBUT_VAL.toString()) != null) {
                piedDigue.setDate_debut(LocalDateTime.parse(row.getDate(PiedDigueColumns.DATE_DEBUT_VAL.toString()).toString(), dateTimeFormatter));
            }
            
            if (row.getDouble(PiedDigueColumns.PR_DEBUT_CALCULE.toString()) != null) {
                piedDigue.setPR_debut(row.getDouble(PiedDigueColumns.PR_DEBUT_CALCULE.toString()).floatValue());
            }
            
            if (row.getDouble(PiedDigueColumns.PR_FIN_CALCULE.toString()) != null) {
                piedDigue.setPR_fin(row.getDouble(PiedDigueColumns.PR_FIN_CALCULE.toString()).floatValue());
            }
            
            if(row.getInt(PiedDigueColumns.ID_SYSTEME_REP.toString())!=null){
                piedDigue.setSystemeRepId(systemesReperage.get(row.getInt(PiedDigueColumns.ID_SYSTEME_REP.toString())).getId());
            }
            
            if (row.getDouble(PiedDigueColumns.ID_BORNEREF_DEBUT.toString()) != null) {
                piedDigue.setBorneDebutId(bornes.get((int) row.getDouble(PiedDigueColumns.ID_BORNEREF_DEBUT.toString()).doubleValue()).getId());
            }
            
            piedDigue.setBorne_debut_aval(row.getBoolean(PiedDigueColumns.AMONT_AVAL_DEBUT.toString())); 
            
            if (row.getDouble(PiedDigueColumns.DIST_BORNEREF_DEBUT.toString()) != null) {
                piedDigue.setBorne_debut_distance(row.getDouble(PiedDigueColumns.DIST_BORNEREF_DEBUT.toString()).floatValue());
            }
            
            if (row.getDouble(PiedDigueColumns.ID_BORNEREF_FIN.toString()) != null) {
                BorneDigue b = bornes.get((int) row.getDouble(PiedDigueColumns.ID_BORNEREF_FIN.toString()).doubleValue());
                if (b!=null) piedDigue.setBorneFinId(b.getId());
            }
            
            piedDigue.setBorne_fin_aval(row.getBoolean(PiedDigueColumns.AMONT_AVAL_FIN.toString()));
            
            if (row.getDouble(PiedDigueColumns.DIST_BORNEREF_FIN.toString()) != null) {
                piedDigue.setBorne_fin_distance(row.getDouble(PiedDigueColumns.DIST_BORNEREF_FIN.toString()).floatValue());
            }
            
            piedDigue.setCommentaire(row.getString(PiedDigueColumns.COMMENTAIRE.toString()));
            
            if(row.getInt(PiedDigueColumns.ID_TYPE_MATERIAU.toString())!=null){
                piedDigue.setMateriauId(typesMateriau.get(row.getInt(PiedDigueColumns.ID_TYPE_MATERIAU.toString())).getId());
            }

            if (row.getDate(PiedDigueColumns.DATE_FIN_VAL.toString()) != null) {
                piedDigue.setDate_fin(LocalDateTime.parse(row.getDate(PiedDigueColumns.DATE_FIN_VAL.toString()).toString(), dateTimeFormatter));
            }
            
            GeometryFactory geometryFactory = new GeometryFactory();
            final MathTransform lambertToRGF;
            try {
                lambertToRGF = CRS.findMathTransform(CRS.decode("EPSG:27563"), CRS.decode("EPSG:2154"), true);

                try {

                    if (row.getDouble(PiedDigueColumns.X_DEBUT.toString()) != null && row.getDouble(PiedDigueColumns.Y_DEBUT.toString()) != null) {
                        piedDigue.setPositionDebut((Point) JTS.transform(geometryFactory.createPoint(new Coordinate(
                                row.getDouble(PiedDigueColumns.X_DEBUT.toString()),
                                row.getDouble(PiedDigueColumns.Y_DEBUT.toString()))), lambertToRGF));
                    }
                } catch (MismatchedDimensionException | TransformException ex) {
                    Logger.getLogger(PiedDigueImporter.class.getName()).log(Level.SEVERE, null, ex);
                }

                try {

                    if (row.getDouble(PiedDigueColumns.X_FIN.toString()) != null && row.getDouble(PiedDigueColumns.Y_FIN.toString()) != null) {
                        piedDigue.setPositionFin((Point) JTS.transform(geometryFactory.createPoint(new Coordinate(
                                row.getDouble(PiedDigueColumns.X_FIN.toString()),
                                row.getDouble(PiedDigueColumns.Y_FIN.toString()))), lambertToRGF));
                    }
                } catch (MismatchedDimensionException | TransformException ex) {
                    Logger.getLogger(PiedDigueImporter.class.getName()).log(Level.SEVERE, null, ex);
                }
            } catch (FactoryException ex) {
                Logger.getLogger(PiedDigueImporter.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            if(row.getInt(PiedDigueColumns.ID_TYPE_POSITION.toString())!=null){
                piedDigue.setPosition_structure(typesPosition.get(row.getInt(PiedDigueColumns.ID_TYPE_POSITION.toString())).getId());
            }
            
            
            

            // Don't set the old ID, but save it into the dedicated map in order to keep the reference.
            //tronconDigue.setId(String.valueOf(row.getString(TronconDigueColumns.ID.toString())));
            piedsDigue.put(row.getInt(PiedDigueColumns.ID_ELEMENT_STRUCTURE.toString()), piedDigue);

            // Set the list ByTronconId
            List<PiedDigue> listByTronconId = piedsDigueByTronconId.get(row.getInt(PiedDigueColumns.ID_TRONCON_GESTION.toString()));
            if (listByTronconId == null) {
                listByTronconId = new ArrayList<>();
                piedsDigueByTronconId.put(row.getInt(PiedDigueColumns.ID_TRONCON_GESTION.toString()), listByTronconId);
            }
            listByTronconId.add(piedDigue);
            piedsDigueByTronconId.put(row.getInt(PiedDigueColumns.ID_TRONCON_GESTION.toString()), listByTronconId);
        }
    }
}
