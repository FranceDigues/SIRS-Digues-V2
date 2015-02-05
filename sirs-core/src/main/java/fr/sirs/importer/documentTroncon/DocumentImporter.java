package fr.sirs.importer.documentTroncon;

import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.Row;
import fr.sirs.core.SirsCore;
import fr.sirs.core.model.ArticleJournal;
import fr.sirs.core.model.Convention;
import fr.sirs.core.model.DocumentGrandeEchelle;
import fr.sirs.core.model.DocumentTroncon;
import fr.sirs.core.model.Marche;
import fr.sirs.core.model.ProfilLong;
import fr.sirs.core.model.ProfilTravers;
import fr.sirs.core.model.RapportEtude;
import fr.sirs.core.model.RefTypeDocument;
import fr.sirs.importer.AccessDbImporterException;
import fr.sirs.importer.BorneDigueImporter;
import fr.sirs.importer.DbImporter;
import fr.sirs.importer.DocumentsUpdatable;
import fr.sirs.importer.IntervenantImporter;
import fr.sirs.importer.OrganismeImporter;
import fr.sirs.importer.SystemeReperageImporter;
import fr.sirs.importer.documentTroncon.document.DocumentManager;
import fr.sirs.importer.evenementHydraulique.EvenementHydrauliqueImporter;
import fr.sirs.importer.documentTroncon.document.GenericDocumentRelatedImporter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.ektorp.CouchDbConnector;

/**
 *
 * @author Samuel Andrés (Geomatys)
 */
public class DocumentImporter extends GenericDocumentImporter implements DocumentsUpdatable {
    
    private final DocumentManager documentManager;
    private final List<GenericDocumentRelatedImporter> documentRelatedImporters;
    
    private final TypeDocumentImporter typeDocumentImporter;
    
    private final SysEvtConventionImporter sysEvtConventionImporter;
    private final SysEvtProfilEnLongImporter sysEvtProfilLongImporter;
    private final SysEvtProfilEnTraversImporter sysEvtProfilTraversImporter;
    private final SysEvtRapportEtudesImporter sysEvtRapportEtudeImporter;
    private final SysEvtJournalImporter sysEvtJournalImporter;
    private final SysEvtMarcheImporter sysEvtMarcheImporter;
    private final SysEvtDocumentAGrandeEchelleImporter sysEvtDocumentAGrandeEchelleImporter;
    
    private final List<GenericDocumentImporter> documentImporters = new ArrayList<>();
    
    public DocumentImporter(final Database accessDatabase, 
            final CouchDbConnector couchDbConnector, 
            final BorneDigueImporter borneDigueImporter,
            final IntervenantImporter intervenantImporter,
            final OrganismeImporter organismeImporter,
            final SystemeReperageImporter systemeReperageImporter,
            final EvenementHydrauliqueImporter evenementHydrauliqueImporter){
        super(accessDatabase, couchDbConnector, 
                borneDigueImporter, systemeReperageImporter);
        this.typeDocumentImporter = new TypeDocumentImporter(accessDatabase, 
                couchDbConnector);
        documentManager = new DocumentManager(accessDatabase, couchDbConnector, 
                organismeImporter, intervenantImporter, 
                evenementHydrauliqueImporter, typeDocumentImporter, this);
        
        documentRelatedImporters = documentManager.getDocumentRelatedImporters();
        
        sysEvtConventionImporter = new SysEvtConventionImporter(
                accessDatabase, couchDbConnector, 
                borneDigueImporter, systemeReperageImporter, 
                documentManager.getConventionImporter());
        documentImporters.add(sysEvtConventionImporter);
        sysEvtProfilTraversImporter = new SysEvtProfilEnTraversImporter(
                accessDatabase, couchDbConnector, 
                borneDigueImporter, systemeReperageImporter, 
                documentManager.getProfilEnTraversImporter());
        documentImporters.add(sysEvtProfilTraversImporter);
        sysEvtProfilLongImporter = new SysEvtProfilEnLongImporter(
                accessDatabase, couchDbConnector, 
                borneDigueImporter, systemeReperageImporter, 
                documentManager.getProfilEnLongImporter());
        documentImporters.add(sysEvtProfilLongImporter);
        sysEvtRapportEtudeImporter = new SysEvtRapportEtudesImporter(
                accessDatabase, couchDbConnector, 
                borneDigueImporter, systemeReperageImporter, 
                documentManager.getRapportEtudeImporter());
        documentImporters.add(sysEvtRapportEtudeImporter);
        sysEvtJournalImporter = new SysEvtJournalImporter(accessDatabase, 
                couchDbConnector, borneDigueImporter, 
                systemeReperageImporter, documentManager.getJournalArticleImporter());
        documentImporters.add(sysEvtJournalImporter);
        sysEvtMarcheImporter = new SysEvtMarcheImporter(accessDatabase, 
                couchDbConnector, borneDigueImporter, 
                systemeReperageImporter, documentManager.getMarcheImporter());
        documentImporters.add(sysEvtMarcheImporter);
        sysEvtDocumentAGrandeEchelleImporter = new SysEvtDocumentAGrandeEchelleImporter(
                accessDatabase, couchDbConnector, borneDigueImporter, 
                systemeReperageImporter, documentManager.getDocumentAGrandeEchelleImporter());
        documentImporters.add(sysEvtDocumentAGrandeEchelleImporter);
    }
    
    public DocumentManager getDocumentManager() {return this.documentManager;}

    @Override
    public void update() throws IOException, AccessDbImporterException {
        for(final GenericDocumentRelatedImporter related : documentRelatedImporters){
            related.update();
        }
        if(documentTroncons==null) compute();
    }
    
    private enum Columns {
        ID_DOC,
        ID_TRONCON_GESTION,
        ID_TYPE_DOCUMENT,
////        ID_DOSSIER, // Pas dans le nouveau modèle
////        REFERENCE_PAPIER, // Pas dans le nouveau modèle
////        REFERENCE_NUMERIQUE, // Pas dans le nouveau modèle
////        REFERENCE_CALQUE, // Pas dans le nouveau modèle
////        DATE_DOCUMENT,
////        DATE_DEBUT_VAL, // Pas dans le nouveau modèle
////        DATE_FIN_VAL, // Pas dans le nouveau modèle
//        PR_DEBUT_CALCULE,
//        PR_FIN_CALCULE,
//        X_DEBUT,
//        Y_DEBUT,
//        X_FIN,
//        Y_FIN,
//        ID_SYSTEME_REP,
//        ID_BORNEREF_DEBUT,
//        AMONT_AVAL_DEBUT,
//        DIST_BORNEREF_DEBUT,
//        ID_BORNEREF_FIN,
//        AMONT_AVAL_FIN,
//        DIST_BORNEREF_FIN,
//        COMMENTAIRE, 
////        NOM,
////        ID_MARCHE,
////        ID_INTERV_CREATEUR,
////        ID_ORG_CREATEUR,
//        ID_ARTICLE_JOURNAL,
//        ID_PROFIL_EN_TRAVERS,
////        ID_PROFIL_EN_LONG, // Utilisation interdite ! C'est ID_DOC qui est utilisé par les profils en long !
////        ID_TYPE_DOCUMENT_A_GRANDE_ECHELLE,
//        ID_CONVENTION,
////        DATE_DERNIERE_MAJ,
////        AUTEUR_RAPPORT,
//        ID_RAPPORT_ETUDE
    }

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
        return DbImporter.TableName.DOCUMENT.toString();
    }

    private Map<Integer, DocumentTroncon> properDocumentTroncons = new HashMap<>();
    
    @Override
    protected void preCompute() throws IOException, AccessDbImporterException {
        
        documentTroncons = new HashMap<>();
        documentTronconByTronconId = new HashMap<>();
        
        // Begin feeding the map (accessible by doc id) by objects form SYS_EVT tables.
        for (final GenericDocumentImporter gdi : documentImporters){
            final Map<Integer, DocumentTroncon> byDocId = gdi.getPrecomputedDocuments();
            if(byDocId!=null){
                for (final Integer key : byDocId.keySet()){
                    // If the key used in one SYS_EVT table has ever been used into another one, throws an exception.
                    if(documentTroncons.get(key)!=null){
                        throw new AccessDbImporterException(byDocId.get(key).getClass().getCanonicalName()+" : This structure ID is ever used ("+key+") by "+documentTroncons.get(key).getClass().getCanonicalName());
                    }
                    else {
                        documentTroncons.put(key, byDocId.get(key));
                    }
                }
            }
            
            final Map<Integer, List<DocumentTroncon>> byTronconId = gdi.getPrecomputedDocumentsByTronconId();
            if(byTronconId!=null){
                for(final Integer key : byTronconId.keySet()){
                    
                    if(byTronconId.get(key)!=null){
                        if(documentTronconByTronconId.get(key)==null)
                            documentTronconByTronconId.put(key, new ArrayList());

                        documentTronconByTronconId.get(key).addAll(byTronconId.get(key));
                    }
                }
            }
        }
        
        
        // Then, precomputing documents that would not be into one SYS_EVT table
        final Iterator<Row> it = this.accessDatabase.getTable(getTableName()).iterator();
        while (it.hasNext()){
            final Row row = it.next();
            final Integer rowId = row.getInt(Columns.ID_DOC.toString());
            
            // If the document does not ever exists, add it.
            if(documentTroncons.get(rowId)==null){
                final DocumentTroncon documentTroncon = new DocumentTroncon();
                documentTroncons.put(rowId, documentTroncon);
                properDocumentTroncons.put(rowId, documentTroncon); // Memorize it for computation purpose.
                
                final Integer troncon = row.getInt(Columns.ID_TRONCON_GESTION.toString());
                if(troncon!=null){
                    if(documentTronconByTronconId.get(troncon)==null)
                        documentTronconByTronconId.put(troncon, new ArrayList());
                    
                    documentTronconByTronconId.get(troncon).add(documentTroncon);
                }
            }
        }
    }

    @Override
    protected void compute() throws IOException, AccessDbImporterException {
        
        
        
        // Begin computing documents from SYS_EVT tables.
        for (final GenericDocumentImporter gdi : documentImporters){
            gdi.getDocuments();
        }
        
        
        
        final Iterator<Row> it = this.accessDatabase.getTable(getTableName()).iterator();
        while(it.hasNext()){
            final Row row = it.next();
            final Integer rowId = row.getInt(Columns.ID_DOC.toString());
            final DocumentTroncon docTroncon = documentTroncons.get(row.getInt(Columns.ID_DOC.toString()));
            
            // Compute only if docTroncon is one proper value of this table (does not exist into one SYS_EVT table)
            if(properDocumentTroncons.get(rowId)!=null){
                importRow(row, docTroncon);
            }
        }
    }
    
    

    @Override
    void importRow(Row row, DocumentTroncon docTroncon) throws IOException, AccessDbImporterException {
        
        final Map<Integer, Class> classesDocument = typeDocumentImporter.getClasseDocument();
        final Map<Integer, RefTypeDocument> typesDocument = typeDocumentImporter.getTypeDocument();
        
        final Class classeDocument = classesDocument.get(row.getInt(Columns.ID_TYPE_DOCUMENT.toString()));

            if (classeDocument != null) {
                docTroncon.setTypeDocumentId(classeDocument.getCanonicalName());
                
                if (classeDocument.equals(Convention.class)) {
                    sysEvtConventionImporter.importRow(row, docTroncon);
                } 
                else if (classeDocument.equals(DocumentGrandeEchelle.class)){
                    sysEvtDocumentAGrandeEchelleImporter.importRow(row, docTroncon);
                }
                else if(classeDocument.equals(ArticleJournal.class)){
                    sysEvtJournalImporter.importRow(row, docTroncon);
                }
                else if(classeDocument.equals(Marche.class)){
                    sysEvtMarcheImporter.importRow(row, docTroncon);
                }
                else if(classeDocument.equals(ProfilLong.class)){
                    sysEvtProfilLongImporter.importRow(row, docTroncon);
                }
                else if(classeDocument.equals(ProfilTravers.class)){
                    sysEvtProfilTraversImporter.importRow(row, docTroncon);
                }
                else if(classeDocument.equals(RapportEtude.class)){
                    sysEvtRapportEtudeImporter.importRow(row, docTroncon);
                }
                else {
                    SirsCore.LOGGER.log(Level.FINE, "Type de document non pris en charge : ID = " + row.getInt(Columns.ID_TYPE_DOCUMENT.toString()));
                }
            } else {
                SirsCore.LOGGER.log(Level.FINE, "Type de document inconnu !");
            }
                
            docTroncon.setTypeDocumentId(typesDocument.get(row.getInt(Columns.ID_TYPE_DOCUMENT.toString())).getId());
    }
}