package fr.sirs.importer.structure;

import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.Row;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import fr.sirs.core.model.BorneDigue;
import fr.sirs.importer.AccessDbImporterException;
import fr.sirs.importer.BorneDigueImporter;
import fr.sirs.importer.DbImporter;
import fr.sirs.importer.SystemeReperageImporter;
import fr.sirs.importer.TronconGestionDigueImporter;
import fr.sirs.core.model.Crete;
import fr.sirs.core.model.RefCote;
import fr.sirs.core.model.RefFonction;
import fr.sirs.core.model.RefMateriau;
import fr.sirs.core.model.RefNature;
import fr.sirs.core.model.RefPosition;
import fr.sirs.core.model.RefSource;
import fr.sirs.core.model.SystemeReperage;
import fr.sirs.core.model.TalusRisberme;
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
class TalusRisbermeImporter extends GenericStructureImporter<TalusRisberme> {

    private Map<Integer, TalusRisberme> talusRisbermes = null;
    private Map<Integer, List<TalusRisberme>> talusRisbermesByTronconId = null;

    TalusRisbermeImporter(final Database accessDatabase,
            final CouchDbConnector couchDbConnector,
            final TronconGestionDigueImporter tronconGestionDigueImporter,
            final SystemeReperageImporter systemeReperageImporter,
            final BorneDigueImporter borneDigueImporter, 
            final OrganismeImporter organismeImporter,
            final TypeSourceImporter typeSourceImporter,
            final TypePositionImporter typePositionImporter,
            final TypeCoteImporter typeCoteImporter,
            final TypeMateriauImporter typeMateriauImporter,
            final TypeNatureImporter typeNatureImporter,
            final TypeFonctionImporter typeFonctionImporter) {
        super(accessDatabase, couchDbConnector, tronconGestionDigueImporter, 
                systemeReperageImporter, borneDigueImporter, organismeImporter,
                typeSourceImporter, typeCoteImporter, typePositionImporter, 
                typeMateriauImporter, typeNatureImporter, typeFonctionImporter);
    }
    
    private enum TalusRisbermeColumns {
        ID_ELEMENT_STRUCTURE,
//        id_nom_element, // Redondant avec ID_ELEMENT_STRUCTURE
//        ID_SOUS_GROUPE_DONNEES, // Redondant avec le type de données
//        LIBELLE_TYPE_ELEMENT_STRUCTURE, // Redondant avec le type de données
//        DECALAGE_DEFAUT, // Affichage
//        DECALAGE, // Affichage
//        LIBELLE_SOURCE, // Redondant avec l'importation des sources
//        LIBELLE_TYPE_COTE, // Redondant avec l'importation des cotés
//        LIBELLE_SYSTEME_REP, // Redondant avec l'importation des SR
//        NOM_BORNE_DEBUT, // Redondant avec l'importation des bornes
//        NOM_BORNE_FIN, // Redondant avec l'importation des bornes
//        LIBELLE_TYPE_MATERIAU, // Redondant avec l'importation des matériaux
//        LIBELLE_TYPE_NATURE, // Redondant avec l'importation des natures
//        LIBELLE_TYPE_FONCTION, // Redondant avec l'importation des fonctions
//        LIBELLE_TYPE_NATURE_HAUT, // Redondant avec l'importation des natures
//        LIBELLE_TYPE_MATERIAU_HAUT, // Redondant avec l'importation des matériaux
//        LIBELLE_TYPE_NATURE_BAS, // Redondant avec l'importation des natures
//        LIBELLE_TYPE_MATERIAU_BAS, // Redondant avec l'importation des matériaux
//        LIBELLE_TYPE_OUVRAGE_PARTICULIER,
//        LIBELLE_TYPE_POSITION, // Redondant avec l'importation des positions
//        RAISON_SOCIALE_ORG_PROPRIO, // Redondant avec l'importation des organismes
//        RAISON_SOCIALE_ORG_GESTION, // Redondant avec l'importation des organismes
//        INTERV_PROPRIO,
//        INTERV_GARDIEN,
//        LIBELLE_TYPE_COMPOSITION,
//        LIBELLE_TYPE_VEGETATION,
//        ID_TYPE_ELEMENT_STRUCTURE, // Redondant avec le type de données
        ID_TYPE_COTE,
        ID_SOURCE,
        ID_TRONCON_GESTION,
        DATE_DEBUT_VAL,
        DATE_FIN_VAL,
        PR_DEBUT_CALCULE,
        PR_FIN_CALCULE,
        X_DEBUT,
        Y_DEBUT,
        X_FIN,
        Y_FIN,
        ID_SYSTEME_REP,
        ID_BORNEREF_DEBUT,
        AMONT_AVAL_DEBUT,
        DIST_BORNEREF_DEBUT,
        ID_BORNEREF_FIN,
        AMONT_AVAL_FIN,
        DIST_BORNEREF_FIN,
        COMMENTAIRE,
        N_COUCHE,
//        ID_TYPE_MATERIAU, // Pas dans le nouveau modèle
//        ID_TYPE_NATURE, // Pas dans le nouveau modèle
        ID_TYPE_FONCTION,
        EPAISSEUR,
//        TALUS_INTERCEPTE_CRETE,
        ID_TYPE_NATURE_HAUT,
        ID_TYPE_MATERIAU_HAUT,
        ID_TYPE_MATERIAU_BAS,
        ID_TYPE_NATURE_BAS,
        LONG_RAMP_HAUT,
        LONG_RAMP_BAS,
        PENTE_INTERIEURE,
//        ID_TYPE_OUVRAGE_PARTICULIER,
        ID_TYPE_POSITION,
//        ID_ORG_PROPRIO,
//        ID_ORG_GESTION,
//        ID_INTERV_PROPRIO,
//        ID_INTERV_GARDIEN,
//        DATE_DEBUT_ORGPROPRIO,
//        DATE_FIN_ORGPROPRIO,
//        DATE_DEBUT_GESTION,
//        DATE_FIN_GESTION,
//        DATE_DEBUT_INTERVPROPRIO,
//        DATE_FIN_INTERVPROPRIO,
//        ID_TYPE_COMPOSITION,
//        DISTANCE_TRONCON,
//        LONGUEUR,
//        DATE_DEBUT_GARDIEN,
//        DATE_FIN_GARDIEN,
//        LONGUEUR_PERPENDICULAIRE,
//        LONGUEUR_PARALLELE,
//        COTE_AXE,
//        ID_TYPE_VEGETATION,
//        HAUTEUR,
//        DIAMETRE,
//        DENSITE,
//        EPAISSEUR_Y11,
//        EPAISSEUR_Y12,
//        EPAISSEUR_Y21,
//        EPAISSEUR_Y22,
//        ID_AUTO
    };

    /**
     *
     * @return A map containing all TalusRisberme instances accessibles from the
     * internal database identifier.
     * @throws IOException
     * @throws AccessDbImporterException
     */
    @Override
    public Map<Integer, TalusRisberme> getStructures() throws IOException, AccessDbImporterException {
        if (this.talusRisbermes == null) {
            compute();
        }
        return talusRisbermes;
    }

    /**
     *
     * @return A map containing all TalusRisberme instances accessibles from the
     * internal database <em>TronconDigue</em> identifier.
     * @throws IOException
     * @throws AccessDbImporterException
     */
    @Override
    public Map<Integer, List<TalusRisberme>> getStructuresByTronconId() throws IOException, AccessDbImporterException {
        if (this.talusRisbermesByTronconId == null) {
            compute();
        }
        return this.talusRisbermesByTronconId;
    }

    @Override
    public String getTableName() {
        return DbImporter.TableName.SYS_EVT_TALUS_RISBERME.toString();
    }

    @Override
    protected void compute() throws IOException, AccessDbImporterException {

        this.talusRisbermes = new HashMap<>();
        this.talusRisbermesByTronconId = new HashMap<>();
        
        final Map<Integer, BorneDigue> bornes = borneDigueImporter.getBorneDigue();
        final Map<Integer, SystemeReperage> systemesReperage = systemeReperageImporter.getSystemeRepLineaire();
        final Map<Integer, TronconDigue> troncons = tronconGestionDigueImporter.getTronconsDigues();
        final Map<Integer, RefSource> typesSource = typeSourceImporter.getTypeSource();
        final Map<Integer, RefPosition> typesPosition = typePositionImporter.getTypePosition();
        final Map<Integer, RefCote> typesCote = typeCoteImporter.getTypeCote();
        final Map<Integer, RefMateriau> typesMateriau = typeMateriauImporter.getTypeMateriau();
        final Map<Integer, RefNature> typesNature = typeNatureImporter.getTypeNature();
        final Map<Integer, RefFonction> typesFonction = typeFonctionImporter.getTypeFonction();
        
        final Iterator<Row> it = this.accessDatabase.getTable(getTableName()).iterator();
        while (it.hasNext()) {
            final Row row = it.next();
            final TalusRisberme talusRisberme = new TalusRisberme();
            
            if(row.getInt(TalusRisbermeColumns.ID_TYPE_COTE.toString())!=null){
                talusRisberme.setCoteId(typesCote.get(row.getInt(TalusRisbermeColumns.ID_TYPE_COTE.toString())).getId());
            }
            
            if(row.getInt(TalusRisbermeColumns.ID_SOURCE.toString())!=null){
                talusRisberme.setSourceId(typesSource.get(row.getInt(TalusRisbermeColumns.ID_SOURCE.toString())).getId());
            }
            
            final TronconDigue troncon = troncons.get(row.getInt(TalusRisbermeColumns.ID_TRONCON_GESTION.toString()));
            if (troncon.getId() != null) {
                talusRisberme.setTroncon(troncon.getId());
            } else {
                throw new AccessDbImporterException("Le tronçon "
                        + troncons.get(row.getInt(TalusRisbermeColumns.ID_TRONCON_GESTION.toString())) + " n'a pas encore d'identifiant CouchDb !");
            }
            
            if (row.getDate(TalusRisbermeColumns.DATE_DEBUT_VAL.toString()) != null) {
                talusRisberme.setDate_debut(LocalDateTime.parse(row.getDate(TalusRisbermeColumns.DATE_DEBUT_VAL.toString()).toString(), dateTimeFormatter));
            }
            
            if (row.getDate(TalusRisbermeColumns.DATE_FIN_VAL.toString()) != null) {
                talusRisberme.setDate_fin(LocalDateTime.parse(row.getDate(TalusRisbermeColumns.DATE_FIN_VAL.toString()).toString(), dateTimeFormatter));
            }
            
            if (row.getDouble(TalusRisbermeColumns.PR_DEBUT_CALCULE.toString()) != null) {
                talusRisberme.setPR_debut(row.getDouble(TalusRisbermeColumns.PR_DEBUT_CALCULE.toString()).floatValue());
            }
            
            if (row.getDouble(TalusRisbermeColumns.PR_FIN_CALCULE.toString()) != null) {
                talusRisberme.setPR_fin(row.getDouble(TalusRisbermeColumns.PR_FIN_CALCULE.toString()).floatValue());
            }
            
            GeometryFactory geometryFactory = new GeometryFactory();
            final MathTransform lambertToRGF;
            try {
                lambertToRGF = CRS.findMathTransform(CRS.decode("EPSG:27563"), CRS.decode("EPSG:2154"), true);

                try {

                    if (row.getDouble(TalusRisbermeColumns.X_DEBUT.toString()) != null && row.getDouble(TalusRisbermeColumns.Y_DEBUT.toString()) != null) {
                        talusRisberme.setPositionDebut((Point) JTS.transform(geometryFactory.createPoint(new Coordinate(
                                row.getDouble(TalusRisbermeColumns.X_DEBUT.toString()),
                                row.getDouble(TalusRisbermeColumns.Y_DEBUT.toString()))), lambertToRGF));
                    }
                } catch (MismatchedDimensionException | TransformException ex) {
                    Logger.getLogger(TalusRisbermeImporter.class.getName()).log(Level.SEVERE, null, ex);
                }

                try {

                    if (row.getDouble(TalusRisbermeColumns.X_FIN.toString()) != null && row.getDouble(TalusRisbermeColumns.Y_FIN.toString()) != null) {
                        talusRisberme.setPositionFin((Point) JTS.transform(geometryFactory.createPoint(new Coordinate(
                                row.getDouble(TalusRisbermeColumns.X_FIN.toString()),
                                row.getDouble(TalusRisbermeColumns.Y_FIN.toString()))), lambertToRGF));
                    }
                } catch (MismatchedDimensionException | TransformException ex) {
                    Logger.getLogger(TalusRisbermeImporter.class.getName()).log(Level.SEVERE, null, ex);
                }
            } catch (FactoryException ex) {
                Logger.getLogger(TalusRisbermeImporter.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            if (row.getInt(TalusRisbermeColumns.ID_SYSTEME_REP.toString()) != null) {
                talusRisberme.setSystemeRepId(systemesReperage.get(row.getInt(TalusRisbermeColumns.ID_SYSTEME_REP.toString())).getId());
            }
            
            if (row.getDouble(TalusRisbermeColumns.ID_BORNEREF_DEBUT.toString()) != null) {
                talusRisberme.setBorneDebutId(bornes.get((int) row.getDouble(TalusRisbermeColumns.ID_BORNEREF_DEBUT.toString()).doubleValue()).getId());
            }
            
            talusRisberme.setBorne_debut_aval(row.getBoolean(TalusRisbermeColumns.AMONT_AVAL_DEBUT.toString()));
            
            if (row.getDouble(TalusRisbermeColumns.DIST_BORNEREF_DEBUT.toString()) != null) {
                talusRisberme.setBorne_debut_distance(row.getDouble(TalusRisbermeColumns.DIST_BORNEREF_DEBUT.toString()).floatValue());
            }
            
            if (row.getDouble(TalusRisbermeColumns.ID_BORNEREF_FIN.toString()) != null) {
                talusRisberme.setBorneFinId(bornes.get((int) row.getDouble(TalusRisbermeColumns.ID_BORNEREF_FIN.toString()).doubleValue()).getId());
            }
            
            talusRisberme.setBorne_fin_aval(row.getBoolean(TalusRisbermeColumns.AMONT_AVAL_FIN.toString()));
            
            if (row.getDouble(TalusRisbermeColumns.DIST_BORNEREF_FIN.toString()) != null) {
                talusRisberme.setBorne_fin_distance(row.getDouble(TalusRisbermeColumns.DIST_BORNEREF_FIN.toString()).floatValue());
            }
            
            talusRisberme.setCommentaire(row.getString(TalusRisbermeColumns.COMMENTAIRE.toString()));

            talusRisberme.setNum_couche(row.getInt(TalusRisbermeColumns.N_COUCHE.toString()));
            
            if(row.getInt(TalusRisbermeColumns.ID_TYPE_FONCTION.toString())!=null){
                talusRisberme.setFonctionHautId(typesFonction.get(row.getInt(TalusRisbermeColumns.ID_TYPE_FONCTION.toString())).getId());
                talusRisberme.setFonctionBasId(typesFonction.get(row.getInt(TalusRisbermeColumns.ID_TYPE_FONCTION.toString())).getId());
            }
            
            if (row.getDouble(TalusRisbermeColumns.EPAISSEUR.toString()) != null) {
                talusRisberme.setEpaisseur_sommet(row.getDouble(TalusRisbermeColumns.EPAISSEUR.toString()).floatValue());
            }
            
            if(row.getInt(TalusRisbermeColumns.ID_TYPE_NATURE_HAUT.toString())!=null){
                talusRisberme.setNatureHautId(typesNature.get(row.getInt(TalusRisbermeColumns.ID_TYPE_NATURE_HAUT.toString())).getId());
            }
            
            if(row.getInt(TalusRisbermeColumns.ID_TYPE_MATERIAU_HAUT.toString())!=null){
                talusRisberme.setMateriauHautId(typesMateriau.get(row.getInt(TalusRisbermeColumns.ID_TYPE_MATERIAU_HAUT.toString())).getId());
            }
            
            if(row.getInt(TalusRisbermeColumns.ID_TYPE_MATERIAU_BAS.toString())!=null){
                talusRisberme.setMateriauBasId(typesMateriau.get(row.getInt(TalusRisbermeColumns.ID_TYPE_MATERIAU_BAS.toString())).getId());
            }
            
            if(row.getInt(TalusRisbermeColumns.ID_TYPE_NATURE_BAS.toString())!=null){
                talusRisberme.setNatureBasId(typesNature.get(row.getInt(TalusRisbermeColumns.ID_TYPE_NATURE_BAS.toString())).getId());
            }
            
            if (row.getDouble(TalusRisbermeColumns.LONG_RAMP_HAUT.toString()) != null) {
                talusRisberme.setLongueur_rampart_haut(row.getDouble(TalusRisbermeColumns.LONG_RAMP_HAUT.toString()).floatValue());
            }
            
            if (row.getDouble(TalusRisbermeColumns.LONG_RAMP_BAS.toString()) != null) {
                talusRisberme.setLongueur_rampart_bas(row.getDouble(TalusRisbermeColumns.LONG_RAMP_BAS.toString()).floatValue());
            }
            
            if (row.getDouble(TalusRisbermeColumns.PENTE_INTERIEURE.toString()) != null) {
                talusRisberme.setPente_interieur(row.getDouble(TalusRisbermeColumns.PENTE_INTERIEURE.toString()).floatValue());
            }
            
            if(row.getInt(TalusRisbermeColumns.ID_TYPE_POSITION.toString())!=null){
                talusRisberme.setPosition_structure(typesPosition.get(row.getInt(TalusRisbermeColumns.ID_TYPE_POSITION.toString())).getId());
            }
            

            // Don't set the old ID, but save it into the dedicated map in order to keep the reference.
            talusRisbermes.put(row.getInt(TalusRisbermeColumns.ID_ELEMENT_STRUCTURE.toString()), talusRisberme);

            // Set the list ByTronconId
            List<TalusRisberme> listByTronconId = talusRisbermesByTronconId.get(row.getInt(TalusRisbermeColumns.ID_TRONCON_GESTION.toString()));
            if (listByTronconId == null) {
                listByTronconId = new ArrayList<>();
                talusRisbermesByTronconId.put(row.getInt(TalusRisbermeColumns.ID_TRONCON_GESTION.toString()), listByTronconId);
            }
            listByTronconId.add(talusRisberme);
        }
    }

    @Override
    public List<String> getUsedColumns() {
        final List<String> columns = new ArrayList<>();
        for (TalusRisbermeColumns c : TalusRisbermeColumns.values()) {
            columns.add(c.toString());
        }
        return columns;
    }
}
