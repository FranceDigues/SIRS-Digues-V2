package fr.sirs.importer.theme.document;

import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.Row;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import fr.sirs.core.component.DocumentRepository;
import fr.sirs.core.model.BorneDigue;
import fr.sirs.core.model.Document;
import fr.sirs.core.model.ProfilLong;
import fr.sirs.core.model.SystemeReperage;
import fr.sirs.core.model.TronconDigue;
import fr.sirs.importer.AccessDbImporterException;
import fr.sirs.importer.BorneDigueImporter;
import fr.sirs.importer.DbImporter;
import fr.sirs.importer.SystemeReperageImporter;
import fr.sirs.importer.TronconGestionDigueImporter;
import fr.sirs.importer.theme.document.related.profilLong.ProfilLongImporter;
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
class DocumentProfilLongImporter extends GenericDocumentImporter {

    private final ProfilLongImporter profilLongImporter;
    
    DocumentProfilLongImporter(final Database accessDatabase, 
            final CouchDbConnector couchDbConnector, 
            final DocumentRepository documentRepository, 
            final BorneDigueImporter borneDigueImporter, 
            final SystemeReperageImporter systemeReperageImporter,
            final TronconGestionDigueImporter tronconGestionDigueImporter,
            final ProfilLongImporter profilLongImporter) {
        super(accessDatabase, couchDbConnector, documentRepository, 
                borneDigueImporter, systemeReperageImporter, 
                tronconGestionDigueImporter);
        this.profilLongImporter = profilLongImporter;
    }
    
    private enum DocumentProfilLongColumns {
        ID_DOC,
//        id_nom_element, // Redondant avec ID_DOC
//        ID_SOUS_GROUPE_DONNEES, // Redondant avec le type de données
//        LIBELLE_TYPE_DOCUMENT, // Redondant avec le type de document
//        DECALAGE_DEFAUT, // Affichage
//        DECALAGE, // Affichage
//        LIBELLE_SYSTEME_REP, // Redondant avec l'importaton des SR
//        NOM_BORNE_DEBUT, // Redondant avec l'importation des bornes
//        NOM_BORNE_FIN, // Redondant avec l'importation des bornes
//        NOM_PROFIL_EN_TRAVERS, 
//        LIBELLE_MARCHE,
//        INTITULE_ARTICLE,
//        TITRE_RAPPORT_ETUDE,
//        ID_TYPE_RAPPORT_ETUDE,
//        TE16_AUTEUR_RAPPORT,
//        DATE_RAPPORT,
        ID_TRONCON_GESTION,
//        ID_TYPE_DOCUMENT,
//        ID_DOSSIER,
//        DATE_DEBUT_VAL,
//        DATE_FIN_VAL,
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
//        REFERENCE_PAPIER,
//        REFERENCE_NUMERIQUE,
//        REFERENCE_CALQUE,
        DATE_DOCUMENT,
        NOM,
//        TM_AUTEUR_RAPPORT,
//        ID_MARCHE,
//        ID_INTERV_CREATEUR,
//        ID_ORG_CREATEUR,
//        ID_ARTICLE_JOURNAL,
//        ID_PROFIL_EN_TRAVERS,
//        ID_TYPE_DOCUMENT_A_GRANDE_ECHELLE,
//        ID_CONVENTION,
//        ID_RAPPORT_ETUDE,
//        ID_AUTO
    }

    @Override
    public List<String> getUsedColumns() {
        final List<String> columns = new ArrayList<>();
        for (DocumentProfilLongColumns c : DocumentProfilLongColumns.values()) {
            columns.add(c.toString());
        }
        return columns;
    }

    @Override
    public String getTableName() {
        return DbImporter.TableName.SYS_EVT_PROFIL_EN_LONG.toString();
    }

    @Override
    protected void preCompute() throws IOException {
        
        documents = new HashMap<>();
        
        final Iterator<Row> it = this.accessDatabase.getTable(getTableName()).iterator();
        while (it.hasNext()){
            final Row row = it.next();
            final Document document = new Document();
            documents.put(row.getInt(DocumentProfilLongColumns.ID_DOC.toString()), document);
        }
    }

    @Override
    protected void compute() throws IOException, AccessDbImporterException {
        
        final Map<Integer, TronconDigue> troncons = tronconGestionDigueImporter.getTronconsDigues();
        final Map<Integer, BorneDigue> bornes = borneDigueImporter.getBorneDigue();
        final Map<Integer, SystemeReperage> systemesReperage = systemeReperageImporter.getSystemeRepLineaire();
        final Map<Integer, ProfilLong> profilsLong = profilLongImporter.getProfilLong();
        
        final Iterator<Row> it = this.accessDatabase.getTable(getTableName()).iterator();
        while (it.hasNext()){
            final Row row = it.next();
            final Document document = documents.get(row.getInt(DocumentProfilLongColumns.ID_DOC.toString()));
            
            document.setTronconId(troncons.get(row.getInt(DocumentProfilLongColumns.ID_TRONCON_GESTION.toString())).getId());

            if (row.getDouble(DocumentProfilLongColumns.PR_DEBUT_CALCULE.toString()) != null) {
                document.setPR_debut(row.getDouble(DocumentProfilLongColumns.PR_DEBUT_CALCULE.toString()).floatValue());
            }

            if (row.getDouble(DocumentProfilLongColumns.PR_FIN_CALCULE.toString()) != null) {
                document.setPR_fin(row.getDouble(DocumentProfilLongColumns.PR_FIN_CALCULE.toString()).floatValue());
            }

            GeometryFactory geometryFactory = new GeometryFactory();
            final MathTransform lambertToRGF;
            try {
                lambertToRGF = CRS.findMathTransform(CRS.decode("EPSG:27563"), CRS.decode("EPSG:2154"), true);

                try {

                    if (row.getDouble(DocumentProfilLongColumns.X_DEBUT.toString()) != null && row.getDouble(DocumentProfilLongColumns.Y_DEBUT.toString()) != null) {
                        document.setPositionDebut((Point) JTS.transform(geometryFactory.createPoint(new Coordinate(
                                row.getDouble(DocumentProfilLongColumns.X_DEBUT.toString()),
                                row.getDouble(DocumentProfilLongColumns.Y_DEBUT.toString()))), lambertToRGF));
                    }
                } catch (MismatchedDimensionException | TransformException ex) {
                    Logger.getLogger(DocumentProfilLongImporter.class.getName()).log(Level.SEVERE, null, ex);
                }

                try {

                    if (row.getDouble(DocumentProfilLongColumns.X_FIN.toString()) != null && row.getDouble(DocumentProfilLongColumns.Y_FIN.toString()) != null) {
                        document.setPositionFin((Point) JTS.transform(geometryFactory.createPoint(new Coordinate(
                                row.getDouble(DocumentProfilLongColumns.X_FIN.toString()),
                                row.getDouble(DocumentProfilLongColumns.Y_FIN.toString()))), lambertToRGF));
                    }
                } catch (MismatchedDimensionException | TransformException ex) {
                    Logger.getLogger(DocumentProfilLongImporter.class.getName()).log(Level.SEVERE, null, ex);
                }
            } catch (FactoryException ex) {
                Logger.getLogger(DocumentProfilLongImporter.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            document.setCommentaire(row.getString(DocumentProfilLongColumns.COMMENTAIRE.toString()));
            
            if (row.getDate(DocumentProfilLongColumns.DATE_DOCUMENT.toString()) != null) {
                document.setDate_document(LocalDateTime.parse(row.getDate(DocumentProfilLongColumns.DATE_DOCUMENT.toString()).toString(), dateTimeFormatter));
            }
            
            document.setLibelle(row.getString(DocumentProfilLongColumns.NOM.toString()));
            
            /*
            1- La base du Rhône indique que tous les ID_PROFIL_EN_LONG de la table
            DOCUMENT sont absent de SYS_EVT_PROFIL_EN_LONG.
            2- Elle permet également de se rendre compte que tous les 
            ID_PROFIL_EN_LONG de la table DOCUMENT sont nuls.
            3- Ainsi que du fait que les ID_PROFIL_EN_LONG de la table 
            PROFIL_EN_LONG sont égaux aux ID_DOC des tables DOCUMENT et
            SYS_EVT_PROFIL_EN_LONG
            */
            if (row.getInt(DocumentProfilLongColumns.ID_DOC.toString()) != null) {
                if (profilsLong.get(row.getInt(DocumentProfilLongColumns.ID_DOC.toString())) != null) {
                    document.setProfilLong(profilsLong.get(row.getInt(DocumentProfilLongColumns.ID_DOC.toString())).getId());
                }
            }
            
            if(row.getInt(DocumentProfilLongColumns.ID_SYSTEME_REP.toString())!=null){
                document.setSystemeRepId(systemesReperage.get(row.getInt(DocumentProfilLongColumns.ID_SYSTEME_REP.toString())).getId());
            }
            
            if(row.getDouble(DocumentProfilLongColumns.ID_BORNEREF_DEBUT.toString())!=null){
                document.setBorneDebutId(bornes.get((int) row.getDouble(DocumentProfilLongColumns.ID_BORNEREF_DEBUT.toString()).doubleValue()).getId());
            }
            
            document.setBorne_debut_aval(row.getBoolean(DocumentProfilLongColumns.AMONT_AVAL_DEBUT.toString()));
            
            if (row.getDouble(DocumentProfilLongColumns.DIST_BORNEREF_DEBUT.toString()) != null) {
                document.setBorne_debut_distance(row.getDouble(DocumentProfilLongColumns.DIST_BORNEREF_DEBUT.toString()).floatValue());
            }
            
            if(row.getDouble(DocumentProfilLongColumns.ID_BORNEREF_FIN.toString())!=null){
                document.setBorneFinId(bornes.get((int) row.getDouble(DocumentProfilLongColumns.ID_BORNEREF_FIN.toString()).doubleValue()).getId());
            }
            
            document.setBorne_fin_aval(row.getBoolean(DocumentProfilLongColumns.AMONT_AVAL_FIN.toString()));
            
            if (row.getDouble(DocumentProfilLongColumns.DIST_BORNEREF_FIN.toString()) != null) {
                document.setBorne_fin_distance(row.getDouble(DocumentProfilLongColumns.DIST_BORNEREF_FIN.toString()).floatValue());
            }
            
            documents.put(row.getInt(DocumentProfilLongColumns.ID_DOC.toString()), document);
            
        }
        computed=true;
    }
}
