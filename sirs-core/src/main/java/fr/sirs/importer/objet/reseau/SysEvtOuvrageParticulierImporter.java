package fr.sirs.importer.objet.reseau;

import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.Row;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import fr.sirs.core.model.BorneDigue;
import fr.sirs.core.model.OuvrageParticulier;
import fr.sirs.importer.AccessDbImporterException;
import fr.sirs.importer.BorneDigueImporter;
import fr.sirs.importer.DbImporter;
import fr.sirs.importer.SystemeReperageImporter;
import fr.sirs.core.model.RefCote;
import fr.sirs.core.model.RefPosition;
import fr.sirs.core.model.RefSource;
import fr.sirs.core.model.SystemeReperage;
import static fr.sirs.importer.DbImporter.cleanNullString;
import fr.sirs.importer.objet.TypeCoteImporter;
import fr.sirs.importer.objet.TypePositionImporter;
import fr.sirs.importer.objet.SourceInfoImporter;
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
class SysEvtOuvrageParticulierImporter extends GenericReseauImporter<OuvrageParticulier> {

    SysEvtOuvrageParticulierImporter(final Database accessDatabase,
            final CouchDbConnector couchDbConnector,
            final SystemeReperageImporter systemeReperageImporter,
            final BorneDigueImporter borneDigueImporter,
            final SourceInfoImporter typeSourceImporter,
            final TypeCoteImporter typeCoteImporter,
            final TypePositionImporter typePositionImporter) {
        super(accessDatabase, couchDbConnector,
                systemeReperageImporter, borneDigueImporter,
                typeSourceImporter, typeCoteImporter,
                typePositionImporter, null);
    }

    private enum Columns {

        ID_ELEMENT_RESEAU,
        //        id_nom_element,
        //        ID_SOUS_GROUPE_DONNEES,
        //        LIBELLE_TYPE_ELEMENT_RESEAU,
        //        DECALAGE_DEFAUT,
        //        DECALAGE,
        //        LIBELLE_SOURCE,
        //        LIBELLE_TYPE_COTE,
        //        LIBELLE_SYSTEME_REP,
        //        NOM_BORNE_DEBUT,
        //        NOM_BORNE_FIN,
        //        LIBELLE_ECOULEMENT,
        //        LIBELLE_IMPLANTATION,
        //        LIBELLE_UTILISATION_CONDUITE,
        //        LIBELLE_TYPE_CONDUITE_FERMEE,
        //        LIBELLE_TYPE_OUVR_HYDRAU_ASSOCIE,
        //        LIBELLE_TYPE_RESEAU_COMMUNICATION,
        //        LIBELLE_TYPE_VOIE_SUR_DIGUE,
        //        NOM_OUVRAGE_VOIRIE,
        //        LIBELLE_TYPE_POSITION,
        //        LIBELLE_TYPE_OUVRAGE_VOIRIE,
        //        LIBELLE_TYPE_RESEAU_EAU,
        //        LIBELLE_TYPE_REVETEMENT,
        //        LIBELLE_TYPE_USAGE_VOIE,
        NOM,
        //        ID_TYPE_ELEMENT_RESEAU,
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
        //        N_SECTEUR,
        //        ID_ECOULEMENT,
        //        ID_IMPLANTATION,
        //        ID_UTILISATION_CONDUITE,
        //        ID_TYPE_CONDUITE_FERMEE,
        //        AUTORISE,
        //        ID_TYPE_OUVR_HYDRAU_ASSOCIE,
        //        ID_TYPE_RESEAU_COMMUNICATION,
        //        ID_OUVRAGE_COMM_NRJ,
        //        ID_TYPE_VOIE_SUR_DIGUE,
        //        ID_OUVRAGE_VOIRIE,
        //        ID_TYPE_REVETEMENT,
        //        ID_TYPE_USAGE_VOIE,
        ID_TYPE_POSITION,
//        LARGEUR,
//        ID_TYPE_OUVRAGE_VOIRIE,
//        HAUTEUR,
//        DIAMETRE,
//        ID_TYPE_RESEAU_EAU,
//        ID_TYPE_NATURE,
//        LIBELLE_TYPE_NATURE,
//        ID_TYPE_NATURE_HAUT,
//        LIBELLE_TYPE_NATURE_HAUT,
//        ID_TYPE_NATURE_BAS,
//        LIBELLE_TYPE_NATURE_BAS,
//        ID_TYPE_REVETEMENT_HAUT,
//        LIBELLE_TYPE_REVETEMENT_HAUT,
//        ID_TYPE_REVETEMENT_BAS,
//        LIBELLE_TYPE_REVETEMENT_BAS,
//        ID_AUTO
    };

    @Override
    public String getTableName() {
        return DbImporter.TableName.SYS_EVT_OUVRAGE_PARTICULIER.toString();
    }

    @Override
    protected void compute() throws IOException, AccessDbImporterException {

        this.structures = new HashMap<>();
        this.structuresByTronconId = new HashMap<>();

        final Iterator<Row> it = this.accessDatabase.getTable(getTableName()).iterator();
        while (it.hasNext()) {
            final Row row = it.next();
            final OuvrageParticulier ouvrage = importRow(row);

            // Don't set the old ID, but save it into the dedicated map in order to keep the reference.
            structures.put(row.getInt(Columns.ID_ELEMENT_RESEAU.toString()), ouvrage);

            // Set the list ByTronconId
            List<OuvrageParticulier> listByTronconId = structuresByTronconId.get(row.getInt(Columns.ID_TRONCON_GESTION.toString()));
            if (listByTronconId == null) {
                listByTronconId = new ArrayList<>();
                structuresByTronconId.put(row.getInt(Columns.ID_TRONCON_GESTION.toString()), listByTronconId);
            }
            listByTronconId.add(ouvrage);
        }
    }

    @Override
    public OuvrageParticulier importRow(Row row) throws IOException, AccessDbImporterException {

        final Map<Integer, BorneDigue> bornes = borneDigueImporter.getBorneDigue();
        final Map<Integer, SystemeReperage> systemesReperage = systemeReperageImporter.getSystemeRepLineaire();

        final Map<Integer, RefSource> typesSource = sourceInfoImporter.getTypeReferences();
        final Map<Integer, RefCote> typesCote = typeCoteImporter.getTypeReferences();
        final Map<Integer, RefPosition> typesPosition = typePositionImporter.getTypeReferences();

        final OuvrageParticulier ouvrage = new OuvrageParticulier();

        ouvrage.setLibelle(cleanNullString(row.getString(Columns.NOM.toString())));

        if (row.getInt(Columns.ID_TYPE_COTE.toString()) != null) {
            ouvrage.setCoteId(typesCote.get(row.getInt(Columns.ID_TYPE_COTE.toString())).getId());
        }

        if (row.getInt(Columns.ID_SOURCE.toString()) != null) {
            ouvrage.setSourceId(typesSource.get(row.getInt(Columns.ID_SOURCE.toString())).getId());
        }

        if (row.getDate(Columns.DATE_DEBUT_VAL.toString()) != null) {
            ouvrage.setDate_debut(LocalDateTime.parse(row.getDate(Columns.DATE_DEBUT_VAL.toString()).toString(), dateTimeFormatter));
        }

        if (row.getDate(Columns.DATE_FIN_VAL.toString()) != null) {
            ouvrage.setDate_fin(LocalDateTime.parse(row.getDate(Columns.DATE_FIN_VAL.toString()).toString(), dateTimeFormatter));
        }

        if (row.getDouble(Columns.PR_DEBUT_CALCULE.toString()) != null) {
            ouvrage.setPR_debut(row.getDouble(Columns.PR_DEBUT_CALCULE.toString()).floatValue());
        }

        if (row.getDouble(Columns.PR_FIN_CALCULE.toString()) != null) {
            ouvrage.setPR_fin(row.getDouble(Columns.PR_FIN_CALCULE.toString()).floatValue());
        }

        GeometryFactory geometryFactory = new GeometryFactory();
        final MathTransform lambertToRGF;
        try {
            lambertToRGF = CRS.findMathTransform(CRS.decode("EPSG:27563"), getOutputCrs(), true);

            try {

                if (row.getDouble(Columns.X_DEBUT.toString()) != null && row.getDouble(Columns.Y_DEBUT.toString()) != null) {
                    ouvrage.setPositionDebut((Point) JTS.transform(geometryFactory.createPoint(new Coordinate(
                            row.getDouble(Columns.X_DEBUT.toString()),
                            row.getDouble(Columns.Y_DEBUT.toString()))), lambertToRGF));
                }
            } catch (MismatchedDimensionException | TransformException ex) {
                Logger.getLogger(SysEvtOuvrageParticulierImporter.class.getName()).log(Level.SEVERE, null, ex);
            }

            try {

                if (row.getDouble(Columns.X_FIN.toString()) != null && row.getDouble(Columns.Y_FIN.toString()) != null) {
                    ouvrage.setPositionFin((Point) JTS.transform(geometryFactory.createPoint(new Coordinate(
                            row.getDouble(Columns.X_FIN.toString()),
                            row.getDouble(Columns.Y_FIN.toString()))), lambertToRGF));
                }
            } catch (MismatchedDimensionException | TransformException ex) {
                Logger.getLogger(SysEvtOuvrageParticulierImporter.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (FactoryException ex) {
            Logger.getLogger(SysEvtOuvrageParticulierImporter.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (row.getInt(Columns.ID_SYSTEME_REP.toString()) != null) {
            ouvrage.setSystemeRepId(systemesReperage.get(row.getInt(Columns.ID_SYSTEME_REP.toString())).getId());
        }

        if (row.getDouble(Columns.ID_BORNEREF_DEBUT.toString()) != null) {
            ouvrage.setBorneDebutId(bornes.get((int) row.getDouble(Columns.ID_BORNEREF_DEBUT.toString()).doubleValue()).getId());
        }

        ouvrage.setBorne_debut_aval(row.getBoolean(Columns.AMONT_AVAL_DEBUT.toString()));

        if (row.getDouble(Columns.DIST_BORNEREF_DEBUT.toString()) != null) {
            ouvrage.setBorne_debut_distance(row.getDouble(Columns.DIST_BORNEREF_DEBUT.toString()).floatValue());
        }

        if (row.getDouble(Columns.ID_BORNEREF_FIN.toString()) != null) {
            if (bornes.get((int) row.getDouble(Columns.ID_BORNEREF_FIN.toString()).doubleValue()) != null) {
                ouvrage.setBorneFinId(bornes.get((int) row.getDouble(Columns.ID_BORNEREF_FIN.toString()).doubleValue()).getId());
            }
        }

        ouvrage.setBorne_fin_aval(row.getBoolean(Columns.AMONT_AVAL_FIN.toString()));

        if (row.getDouble(Columns.DIST_BORNEREF_FIN.toString()) != null) {
            ouvrage.setBorne_fin_distance(row.getDouble(Columns.DIST_BORNEREF_FIN.toString()).floatValue());
        }

        ouvrage.setCommentaire(row.getString(Columns.COMMENTAIRE.toString()));

        if (row.getInt(Columns.ID_TYPE_POSITION.toString()) != null) {
            ouvrage.setPositionId(typesPosition.get(row.getInt(Columns.ID_TYPE_POSITION.toString())).getId());
        }

        ouvrage.setDesignation(String.valueOf(row.getInt(Columns.ID_ELEMENT_RESEAU.toString())));
        ouvrage.setValid(true);
        
        return ouvrage;
    }

    @Override
    protected List<String> getUsedColumns() {
        final List<String> columns = new ArrayList<>();
        for (Columns c : Columns.values()) {
            columns.add(c.toString());
        }
        return columns;
    }
}
