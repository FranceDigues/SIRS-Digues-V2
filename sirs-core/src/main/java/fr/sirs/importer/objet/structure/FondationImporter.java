package fr.sirs.importer.objet.structure;

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
import fr.sirs.core.model.Fondation;
import fr.sirs.core.model.RefCote;
import fr.sirs.core.model.RefFonction;
import fr.sirs.core.model.RefMateriau;
import fr.sirs.core.model.RefNature;
import fr.sirs.core.model.RefPosition;
import fr.sirs.core.model.RefSource;
import fr.sirs.core.model.SystemeReperage;
import fr.sirs.core.model.TronconDigue;
import fr.sirs.importer.OrganismeImporter;
import fr.sirs.importer.objet.GenericStructureImporter;
import fr.sirs.importer.objet.TypeCoteImporter;
import fr.sirs.importer.objet.TypeFonctionImporter;
import fr.sirs.importer.objet.TypeMateriauImporter;
import fr.sirs.importer.objet.TypeNatureImporter;
import fr.sirs.importer.objet.TypePositionImporter;
import fr.sirs.importer.objet.TypeSourceImporter;
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
class FondationImporter extends GenericStructureImporter<Fondation> {

    private Map<Integer, Fondation> fondations = null;
    private Map<Integer, List<Fondation>> fondationsByTronconId = null;

    FondationImporter(final Database accessDatabase,
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
    
    private enum FondationColumns {
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
        ID_TYPE_MATERIAU,
        ID_TYPE_NATURE,
        ID_TYPE_FONCTION,
        EPAISSEUR,
//        TALUS_INTERCEPTE_CRETE,
//        ID_TYPE_NATURE_HAUT,
//        ID_TYPE_MATERIAU_HAUT,
//        ID_TYPE_MATERIAU_BAS,
//        ID_TYPE_NATURE_BAS,
//        LONG_RAMP_HAUT,
//        LONG_RAMP_BAS,
//        PENTE_INTERIEURE,
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
     * @return A map containing all Crete instances accessibles from the
     * internal database identifier.
     * @throws IOException
     * @throws AccessDbImporterException
     */
    @Override
    public Map<Integer, Fondation> getStructures() throws IOException, AccessDbImporterException {
        if (this.fondations == null) {
            compute();
        }
        return fondations;
    }

    /**
     *
     * @return A map containing all Crete instances accessibles from the
     * internal database <em>TronconDigue</em> identifier.
     * @throws IOException
     * @throws AccessDbImporterException
     */
    @Override
    public Map<Integer, List<Fondation>> getStructuresByTronconId() throws IOException, AccessDbImporterException {
        if (this.fondationsByTronconId == null) {
            compute();
        }
        return this.fondationsByTronconId;
    }

    @Override
    public String getTableName() {
        return DbImporter.TableName.SYS_EVT_FONDATION.toString();
    }

    @Override
    protected void compute() throws IOException, AccessDbImporterException {

        this.fondations = new HashMap<>();
        this.fondationsByTronconId = new HashMap<>();
        
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
            final Fondation fondation = new Fondation();
            
            if(row.getInt(FondationColumns.ID_TYPE_COTE.toString())!=null){
                fondation.setCoteId(typesCote.get(row.getInt(FondationColumns.ID_TYPE_COTE.toString())).getId());
            }
            
            if(row.getInt(FondationColumns.ID_SOURCE.toString())!=null){
                fondation.setSourceId(typesSource.get(row.getInt(FondationColumns.ID_SOURCE.toString())).getId());
            }
            
            final TronconDigue troncon = troncons.get(row.getInt(FondationColumns.ID_TRONCON_GESTION.toString()));
            if (troncon.getId() != null) {
                fondation.setTroncon(troncon.getId());
            } else {
                throw new AccessDbImporterException("Le tronçon "
                        + troncons.get(row.getInt(FondationColumns.ID_TRONCON_GESTION.toString())) + " n'a pas encore d'identifiant CouchDb !");
            }
            
            if (row.getDate(FondationColumns.DATE_DEBUT_VAL.toString()) != null) {
                fondation.setDate_debut(LocalDateTime.parse(row.getDate(FondationColumns.DATE_DEBUT_VAL.toString()).toString(), dateTimeFormatter));
            }
            
            if (row.getDate(FondationColumns.DATE_FIN_VAL.toString()) != null) {
                fondation.setDate_fin(LocalDateTime.parse(row.getDate(FondationColumns.DATE_FIN_VAL.toString()).toString(), dateTimeFormatter));
            }
            
            if (row.getDouble(FondationColumns.PR_DEBUT_CALCULE.toString()) != null) {
                fondation.setPR_debut(row.getDouble(FondationColumns.PR_DEBUT_CALCULE.toString()).floatValue());
            }
            
            if (row.getDouble(FondationColumns.PR_FIN_CALCULE.toString()) != null) {
                fondation.setPR_fin(row.getDouble(FondationColumns.PR_FIN_CALCULE.toString()).floatValue());
            }
            
            GeometryFactory geometryFactory = new GeometryFactory();
            final MathTransform lambertToRGF;
            try {
                lambertToRGF = CRS.findMathTransform(CRS.decode("EPSG:27563"), CRS.decode("EPSG:2154"), true);

                try {

                    if (row.getDouble(FondationColumns.X_DEBUT.toString()) != null && row.getDouble(FondationColumns.Y_DEBUT.toString()) != null) {
                        fondation.setPositionDebut((Point) JTS.transform(geometryFactory.createPoint(new Coordinate(
                                row.getDouble(FondationColumns.X_DEBUT.toString()),
                                row.getDouble(FondationColumns.Y_DEBUT.toString()))), lambertToRGF));
                    }
                } catch (MismatchedDimensionException | TransformException ex) {
                    Logger.getLogger(FondationImporter.class.getName()).log(Level.SEVERE, null, ex);
                }

                try {

                    if (row.getDouble(FondationColumns.X_FIN.toString()) != null && row.getDouble(FondationColumns.Y_FIN.toString()) != null) {
                        fondation.setPositionFin((Point) JTS.transform(geometryFactory.createPoint(new Coordinate(
                                row.getDouble(FondationColumns.X_FIN.toString()),
                                row.getDouble(FondationColumns.Y_FIN.toString()))), lambertToRGF));
                    }
                } catch (MismatchedDimensionException | TransformException ex) {
                    Logger.getLogger(FondationImporter.class.getName()).log(Level.SEVERE, null, ex);
                }
            } catch (FactoryException ex) {
                Logger.getLogger(FondationImporter.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            if (row.getInt(FondationColumns.ID_SYSTEME_REP.toString()) != null) {
                fondation.setSystemeRepId(systemesReperage.get(row.getInt(FondationColumns.ID_SYSTEME_REP.toString())).getId());
            }
            
            if (row.getDouble(FondationColumns.ID_BORNEREF_DEBUT.toString()) != null) {
                fondation.setBorneDebutId(bornes.get((int) row.getDouble(FondationColumns.ID_BORNEREF_DEBUT.toString()).doubleValue()).getId());
            }
            
            fondation.setBorne_debut_aval(row.getBoolean(FondationColumns.AMONT_AVAL_DEBUT.toString()));
            
            if (row.getDouble(FondationColumns.DIST_BORNEREF_DEBUT.toString()) != null) {
                fondation.setBorne_debut_distance(row.getDouble(FondationColumns.DIST_BORNEREF_DEBUT.toString()).floatValue());
            }
            
            if (row.getDouble(FondationColumns.ID_BORNEREF_FIN.toString()) != null) {
                fondation.setBorneFinId(bornes.get((int) row.getDouble(FondationColumns.ID_BORNEREF_FIN.toString()).doubleValue()).getId());
            }
            
            fondation.setBorne_fin_aval(row.getBoolean(FondationColumns.AMONT_AVAL_FIN.toString()));
            
            if (row.getDouble(FondationColumns.DIST_BORNEREF_FIN.toString()) != null) {
                fondation.setBorne_fin_distance(row.getDouble(FondationColumns.DIST_BORNEREF_FIN.toString()).floatValue());
            }
            
            fondation.setCommentaire(row.getString(FondationColumns.COMMENTAIRE.toString()));

            fondation.setNum_couche(row.getInt(FondationColumns.N_COUCHE.toString()));
            
            if(row.getInt(FondationColumns.ID_TYPE_MATERIAU.toString())!=null){
                fondation.setMateriauId(typesMateriau.get(row.getInt(FondationColumns.ID_TYPE_MATERIAU.toString())).getId());
            }
            
            if(row.getInt(FondationColumns.ID_TYPE_NATURE.toString())!=null){
                fondation.setNatureId(typesNature.get(row.getInt(FondationColumns.ID_TYPE_NATURE.toString())).getId());
            }
            
            if(row.getInt(FondationColumns.ID_TYPE_FONCTION.toString())!=null){
                fondation.setFonctionId(typesFonction.get(row.getInt(FondationColumns.ID_TYPE_FONCTION.toString())).getId());
            }
            
            if (row.getDouble(FondationColumns.EPAISSEUR.toString()) != null) {
                fondation.setEpaisseur(row.getDouble(FondationColumns.EPAISSEUR.toString()).floatValue());
            }
            
            if(row.getInt(FondationColumns.ID_TYPE_POSITION.toString())!=null){
                fondation.setPosition_structure(typesPosition.get(row.getInt(FondationColumns.ID_TYPE_POSITION.toString())).getId());
            }
            

            // Don't set the old ID, but save it into the dedicated map in order to keep the reference.
            //tronconDigue.setId(String.valueOf(row.getString(TronconDigueColumns.ID.toString())));
            fondations.put(row.getInt(FondationColumns.ID_ELEMENT_STRUCTURE.toString()), fondation);

            // Set the list ByTronconId
            List<Fondation> listByTronconId = fondationsByTronconId.get(row.getInt(FondationColumns.ID_TRONCON_GESTION.toString()));
            if (listByTronconId == null) {
                listByTronconId = new ArrayList<>();
                fondationsByTronconId.put(row.getInt(FondationColumns.ID_TRONCON_GESTION.toString()), listByTronconId);
            }
            listByTronconId.add(fondation);
        }
    }

    @Override
    public List<String> getUsedColumns() {
        final List<String> columns = new ArrayList<>();
        for (FondationColumns c : FondationColumns.values()) {
            columns.add(c.toString());
        }
        return columns;
    }
}
