/**
 * This file is part of SIRS-Digues 2.
 *
 * Copyright (C) 2016, FRANCE-DIGUES,
 *
 * SIRS-Digues 2 is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * SIRS-Digues 2 is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * SIRS-Digues 2. If not, see <http://www.gnu.org/licenses/>
 */
package fr.sirs.plugin.document;

import fr.sirs.Injector;
import fr.sirs.core.SirsDBInfo;
import fr.sirs.core.TronconUtils;
import fr.sirs.core.component.AbstractPositionableRepository;
import fr.sirs.core.component.DigueRepository;
import fr.sirs.core.component.SirsDBInfoRepository;
import fr.sirs.core.component.SystemeEndiguementRepository;
import fr.sirs.core.component.TronconDigueRepository;
import fr.sirs.core.model.Digue;
import fr.sirs.core.model.Objet;
import fr.sirs.core.model.SystemeEndiguement;
import fr.sirs.core.model.TronconDigue;
import fr.sirs.plugin.document.ui.DatabaseVersionPane;
import fr.sirs.plugin.document.ui.DocumentsPane;
import static fr.sirs.plugin.document.ui.DocumentsPane.*;
import fr.sirs.util.StreamingIterable;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.util.logging.Logging;
import org.geotoolkit.nio.IOUtilities;
import org.geotoolkit.util.collection.CloseableIterator;

/**
 * Utility class managing the properties file adding different properties to the filesystem objects.
 *
 * @author Guilhem Legal (Geomatys)
 */
public class PropertiesFileUtilities {

    private static final Logger LOGGER = Logging.getLogger("fr.sirs");

    /**
     * Extract a property in the sirs.properties file coupled to the specified file.
     *
     * @param f A file, can be a folder corresponding to a SE, DG or TR. Or a simple file.
     * @param property Name of the property.
     * @return
     */
    public static String getProperty(final File f, final String property) {
        final Properties prop = getSirsProperties(f, true);
        return prop.getProperty(f.getName() + "_" + property, "");
    }

    /**
     * Set a property in the sirs.properties file coupled to the specified file.
     *
     * @param f A file, can be a folder corresponding to a SE, DG or TR. Or a simple file.
     * @param property Name of the property.
     * @param value The value to set.
     */
    public static void setProperty(final File f, final String property, final String value) {
        final Properties prop   = getSirsProperties(f, true);
        prop.put(f.getName() + "_" + property, value);
        storeSirsProperties(prop, f, true);
    }

    /**
     * Remove a property in the sirs.properties file coupled to the specified file.
     * @param f A file, can be a folder correspounding to a SE, DG or TR. Or a simple file.
     * @param property Name of the property.
     */
    public static void removeProperty(final File f, final String property) {
        final Properties prop   = getSirsProperties(f, true);
        prop.remove(f.getName() + "_" + property);
        storeSirsProperties(prop, f, true);
    }

    /**
     * Extract a property in the sirs.properties file coupled to the specified file.
     *
     * @param f A file, can be a folder correspounding to a SE, DG or TR. Or a simple file.
     * @param property Name of the property.
     * @return
     */
    public static Boolean getBooleanProperty(final File f, final String property) {
        final Properties prop = getSirsProperties(f, true);
        return Boolean.parseBoolean(prop.getProperty(f.getName() + "_" + property, "false"));
    }

    /**
     * Set a property in the sirs.properties file coupled to the specified file.
     *
     * @param f A file, can be a folder correspounding to a SE, DG or TR. Or a simple file.
     * @param property Name of the property.
     * @param value The value to set.
     */
    public static void setBooleanProperty(final File f, final String property, boolean value) {
        final Properties prop   = getSirsProperties(f, true);
        prop.put(f.getName() + "_" + property, Boolean.toString(value));

        storeSirsProperties(prop, f, true);
    }

    /**
     * Return true if the specified file correspound to a a SE, DG or TR folder.
     *
     * @param f A file.
     * @return
     */
    public static Boolean getIsModelFolder(final File f) {
        return getIsModelFolder(f, SE) || getIsModelFolder(f, TR) || getIsModelFolder(f, DG);
    }

    /**
     * Return true if the specified file correspound to a a specific specified model (SE, DG or TR).
     * @param f A file.
     * @param model SE, DG or TR.
     * @return
     */
    public static Boolean getIsModelFolder(final File f, final String model) {
        final Properties prop = getSirsProperties(f, true);
        return Boolean.parseBoolean(prop.getProperty(f.getName() + "_" + model, "false"));
    }

    /**
     * Set the specific specified model (SE, DG or TR) for a folder.
     *
     * @param f A model folder.
     * @param model SE, DG or TR.
     * @param libelle The name that will be displayed in UI.
     */
    private static void setIsModelFolder(final File f, final String model, final String libelle) {
        final Properties prop   = getSirsProperties(f, true);
        prop.put(f.getName() + "_" + model, "true");
        prop.put(f.getName() + "_" + LIBELLE, libelle);

       storeSirsProperties(prop, f, true);
    }

    /**
     * Remove all properties coupled to the specified file.
     *
     * @param f A file.
     */
    public static void removeProperties(final File f) {
        final Properties prop   = getSirsProperties(f, true);

        Set<Entry<Object,Object>> properties = new HashSet<>(prop.entrySet());
        for (Entry<Object,Object> entry : properties) {
            if (((String)entry.getKey()).startsWith(f.getName())) {
                prop.remove(entry.getKey());
            }
        }
        //save cleaned properties file
        storeSirsProperties(prop, f, true);
    }

    /**
     * Store the updated properties to the sirs file.
     *
     * @param prop the updated properties. (will replace the previous one in the file).
     * @param f The file adding properties (not the sirs file).
     * @param parent {@code true} if the file f is not the root directory.
     */
    private static void storeSirsProperties(final Properties prop, final File f, boolean parent) {
        final File sirsPropFile;
        try {
            sirsPropFile = getSirsPropertiesFile(f, parent);
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "Error while accessing sirs properties file.", ex);
            return;
        }

        if (sirsPropFile != null && sirsPropFile.exists()) {
            try (final FileWriter writer = new FileWriter(sirsPropFile)) {
                prop.store(writer, "");
            } catch (IOException ex) {
                LOGGER.log(Level.WARNING, "Error while writing sirs properties file.", ex);
            }
        }
    }

    /**
     * Get or create a sirs.properties file next to the specified one (or in the directory if parent is set to false)
     *
     * @param f A file.
     * @param parent {@code true} if the file f is not the root directory.
     *
     * @return A sirs.properties file.
     * @throws IOException
     */
    private static File getSirsPropertiesFile(final File f, final boolean parent) throws IOException {
        final File parentFile;
        if (parent) {
            parentFile = f.getParentFile();
        } else {
            parentFile = f;
        }
        if (parentFile != null) {
            final File sirsPropFile = new File(parentFile, "sirs.properties");
            if (!sirsPropFile.exists()) {
                sirsPropFile.createNewFile();
            }
            return sirsPropFile;
        }
        return null;
    }

    /**
     * Return the Properties associated with all the files next to the one specified (or in the directory if parent is set to false).
     *
     * @param f A file.
     * @param parent {@code true} if the file f is not the root directory.
     * @return
     */
    private static Properties getSirsProperties(final File f, final boolean parent) {
        final Properties prop = new Properties();
        File sirsPropFile = null;
        try {
            sirsPropFile = getSirsPropertiesFile(f, parent);
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "Error while loading/creating sirs properties file.", ex);
        }

        if (sirsPropFile != null) {
            try (final FileReader reader = new FileReader(sirsPropFile)) {
                prop.load(reader);
            } catch (IOException ex) {
                LOGGER.log(Level.WARNING, "Error while reading sirs properties file.", ex);
            }
        }

        return prop;
    }

    /**
     * Return a label for a file size (if it is a directory the all the added size of its children).
     *
     * @param f A file.
     * @return
     */
    public static String getStringSizeFile(final File f) {
        final long size        = getFileSize(f);
        final DecimalFormat df = new DecimalFormat("0.0");
        final float sizeKb     = 1024.0f;
        final float sizeMo     = sizeKb * sizeKb;
        final float sizeGo     = sizeMo * sizeKb;
        final float sizeTerra  = sizeGo * sizeKb;

        if (size < sizeKb) {
            return df.format(size)          + " o";
        } else if (size < sizeMo) {
            return df.format(size / sizeKb) + " Ko";
        } else if (size < sizeGo) {
            return df.format(size / sizeMo) + " Mo";
        } else if (size < sizeTerra) {
            return df.format(size / sizeGo) + " Go";
        }
        return "";
    }

    /**
     * Return the size of a file (if it is a directory the all the added size of its children).
     * @param f
     * @return
     */
    private static long getFileSize(final File f) {
        if (f.isDirectory()) {
            long result = 0;
            for (File child : f.listFiles()) {
                result += getFileSize(child);
            }
            return result;
        } else {
            return f.length();
        }
    }

    public static File getOrCreateSE(final File rootDirectory, SystemeEndiguement sd){
        final File sdDir = new File(rootDirectory, sd.getId());
        if (!sdDir.exists()) {
            sdDir.mkdir();
        }
        String name = sd.getLibelle();
        if (name == null) {
            name = "null";
        }
        setIsModelFolder(sdDir, SE, name);
        final File docDir = new File(sdDir, DocumentsPane.DOCUMENT_FOLDER);
        if (!docDir.exists()) {
            docDir.mkdir();
        }
        return sdDir;
    }

    public static File getOrCreateDG(final File rootDirectory, Digue digue){
        final File digueDir = new File(rootDirectory, digue.getId());
        if (!digueDir.exists()) {
            digueDir.mkdir();
        }
        String name = digue.getLibelle();
        if (name == null) {
            name = "null";
        }
        setIsModelFolder(digueDir, DG, name);
        final File docDir = new File(digueDir, DocumentsPane.DOCUMENT_FOLDER);
        if (!docDir.exists()) {
            docDir.mkdir();
        }
        return digueDir;
    }

    public static File getOrCreateTR(final File rootDirectory, TronconDigue tr){
        final File trDir = new File(rootDirectory, tr.getId());
        if (!trDir.exists()) {
            trDir.mkdir();
        }
        String name = tr.getLibelle();
        if (name == null) {
            name = "null";
        }
        setIsModelFolder(trDir, TR, name);
        final File docDir = new File(trDir, DocumentsPane.DOCUMENT_FOLDER);
        if (!docDir.exists()) {
            docDir.mkdir();
        }
        return trDir;
    }

    public static File getOrCreateUnclassif(final File rootDirectory){
        final File unclassifiedDir = new File(rootDirectory, UNCLASSIFIED);
        if (!unclassifiedDir.exists()) {
            unclassifiedDir.mkdir();
        }

        final File docDir = new File(unclassifiedDir, DocumentsPane.DOCUMENT_FOLDER);
        if (!docDir.exists()) {
            docDir.mkdir();
        }
        return unclassifiedDir;
    }

    public static String getExistingDatabaseIdentifier(final File rootDirectory) {
        final Properties prop = getSirsProperties(rootDirectory, false);
        return (String) prop.get("database_identifier");
    }

    public static void updateDatabaseIdentifier(final File rootDirectory) {
        final String key = getDatabaseIdentifier();
        final Properties prop = getSirsProperties(rootDirectory, false);
        prop.put("database_identifier", key);

        storeSirsProperties(prop, rootDirectory, false);
    }

    public static void backupDirectories(final File saveDir, final Collection<File> files) {
        for (File f : files) {
            backupDirectory(saveDir, f);
        }
    }

    public static void backupDirectory(final File saveDir, final File f) {

        // extract properties
        final Map<Object, Object> extracted  = new HashMap<>();
        final Properties prop                = getSirsProperties(f, true);
        Set<Entry<Object,Object>> properties = new HashSet<>(prop.entrySet());
        for (Entry<Object,Object> entry : properties) {
            if (((String)entry.getKey()).startsWith(f.getName())) {
                extracted.put(entry.getKey(), entry.getValue());
                prop.remove(entry.getKey());
            }
        }

        //save cleaned properties file
        storeSirsProperties(prop, f, true);


        final File newDir = new File(saveDir, f.getName());
        try {
            // we copy only the "dossier d'ouvrage" directory
            if (!newDir.exists()) {
                newDir.mkdir();
            }

            final File doFile    = new File(f, DOCUMENT_FOLDER);
            final File newDoFile = new File(newDir, DOCUMENT_FOLDER);

            Files.copy(doFile.toPath(), newDoFile.toPath());
            IOUtilities.deleteRecursively(f.toPath());

            // save new properties
            final Properties newProp = getSirsProperties(newDir, true);
            newProp.putAll(extracted);

            storeSirsProperties(newProp, newDir, true);

        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "Error while moving destroyed obj to backup folder", ex);
        }
    }

    public  static Set<File> listModel(final File rootDirectory, final String model) {
        Set<File> modelList = new HashSet<>();
        listModel(rootDirectory, modelList, model, true);
        return modelList;
    }

    public  static Set<File> listModel(final File rootDirectory, final String model, final boolean deep) {
        Set<File> modelList = new HashSet<>();
        listModel(rootDirectory, modelList, model, deep);
        return modelList;
    }

    private static void listModel(final File rootDirectory, Set<File> modelList, String model, final boolean deep) {
        for (File f : rootDirectory.listFiles()) {
            if (f.isDirectory()) {
                if (getIsModelFolder(f, model)) {
                    modelList.add(f);
                } else if (deep){
                    listModel(f, modelList, model, deep);
                }
            }
        }
    }

    public static File findFile(final File rootDirectory, File file) {
        for (File f : rootDirectory.listFiles()) {
            if (f.getName().equals(file.getName()) && !f.getPath().equals(file.getPath())) {
                return f;
            } else if (f.isDirectory()) {
                File child = findFile(f, file);
                if (child != null) {
                    return child;
                }
            }
        }
        return null;
    }

    public static boolean verifyDatabaseVersion(final File rootDirectory) {
        final String key         = getDatabaseIdentifier();
        final String existingKey = getExistingDatabaseIdentifier(rootDirectory);
        if (existingKey == null) {
            return true;
        } else if (!existingKey.equals(key)) {
            return showBadVersionDialog(existingKey, key);
        }
        return true;
    }

    private static boolean showBadVersionDialog(final String existingKey, final String dbKey) {
        final Dialog dialog    = new Alert(Alert.AlertType.ERROR);
        final DialogPane pane  = new DialogPane();
        final DatabaseVersionPane ipane = new DatabaseVersionPane(existingKey, dbKey);
        pane.setContent(ipane);
        pane.getButtonTypes().addAll(ButtonType.YES, ButtonType.NO);
        dialog.setDialogPane(pane);
        dialog.setResizable(true);
        dialog.setTitle("Version de la base differente");
        dialog.setContentText("Le système de fichier que vous tenter d'ouvrir correspond a une autre base de données.\n Voulez vous l'ouvrir quand même?");
        final Optional opt = dialog.showAndWait();
        return opt.isPresent() && ButtonType.YES.equals(opt.get());
    }

    public static String getDatabaseIdentifier() {
        final SirsDBInfoRepository DBrepo = Injector.getBean(SirsDBInfoRepository.class);
        final Optional<SirsDBInfo> info = DBrepo.get();
        if (info.isPresent()) {
            final SirsDBInfo dbInfo = info.get();
            return dbInfo.getUuid() + "|" + dbInfo.getEpsgCode() + "|" + dbInfo.getVersion()  + "|" + dbInfo.getRemoteDatabase();
        }
        return null;
    }

    public static  List<Objet> getElements(Collection<TronconDigue> troncons, final NumberRange dateRange) {
        final ArrayList<Objet> elements = new ArrayList<>();
        final Collection<AbstractPositionableRepository<Objet>> repos = (Collection) Injector.getSession().getRepositoriesForClass(Objet.class);

        for (TronconDigue troncon : troncons) {
            if (troncon == null) {
                continue;
            }

            for (final AbstractPositionableRepository<Objet> repo : repos) {
                StreamingIterable<Objet> tmpElements = repo.getByLinearIdStreaming(troncon.getId());
                try (final CloseableIterator<Objet> it = tmpElements.iterator()) {
                    while (it.hasNext()) {
                        Objet next = it.next();
                        if (dateRange != null) {
                            //on vérifie la date
                            final LocalDate objDateDebut = next.getDate_debut();
                            final LocalDate objDateFin = next.getDate_fin();
                            final long debut = objDateDebut == null ? 0 : objDateDebut.atTime(0, 0, 0).toInstant(ZoneOffset.UTC).toEpochMilli();
                            final long fin = objDateFin == null ? Long.MAX_VALUE : objDateFin.atTime(23, 59, 59).toInstant(ZoneOffset.UTC).toEpochMilli();
                            final NumberRange objDateRange = NumberRange.create(debut, true, fin, true);
                            if (!dateRange.intersectsAny(objDateRange)) {
                                continue;
                            }
                        }

                        elements.add(next);
                    }
                }
            }
        }
        return elements;
    }

    public static void showErrorDialog(final String errorMsg) {
        final Dialog dialog    = new Alert(Alert.AlertType.ERROR);
        final DialogPane pane  = new DialogPane();
        pane.getButtonTypes().addAll(ButtonType.OK);
        dialog.setDialogPane(pane);
        dialog.setResizable(true);
        dialog.setTitle("Erreur");
        dialog.setContentText(errorMsg);
        dialog.showAndWait();
    }

    public static void showConfirmDialog(final String errorMsg) {
        final Dialog dialog    = new Alert(Alert.AlertType.CONFIRMATION);
        final DialogPane pane  = new DialogPane();
        pane.getButtonTypes().addAll(ButtonType.OK);
        dialog.setDialogPane(pane);
        dialog.setResizable(true);
        dialog.setTitle("Succés");
        dialog.setContentText(errorMsg);
        dialog.showAndWait();
    }

    public static void updateFileSystem(final File rootDirectory) {

        final File saveDir = new File(rootDirectory, SAVE_FOLDER);
        if (!saveDir.exists()) {
            saveDir.mkdir();
        }

        final File unclassifiedDir = getOrCreateUnclassif(rootDirectory);

        final SystemeEndiguementRepository SErepo = Injector.getBean(SystemeEndiguementRepository.class);
        final DigueRepository Drepo = Injector.getBean(DigueRepository.class);
        final TronconDigueRepository TRrepo = Injector.getBean(TronconDigueRepository.class);

        /**
         * On recupere tous les elements.
         */
        final List<SystemeEndiguement> ses    = SErepo.getAll();
        final Set<Digue> digues               = new HashSet<>(Drepo.getAll());
        final Set<TronconDigue> troncons      = new HashSet<>(TRrepo.getAllLight());
        final Set<Digue> diguesFound          = new HashSet<>();
        final Set<TronconDigue> tronconsFound = new HashSet<>();
        final Set<File> seFiles               = listModel(rootDirectory, SE);
        final Set<File> digueMoved            = new HashSet<>();
        final Set<File> tronMoved             = new HashSet<>();

        for (SystemeEndiguement se : ses) {
            final File seDir = getOrCreateSE(rootDirectory, se);
            seFiles.remove(seDir);

            final Set<File> digueFiles = listModel(seDir, DG);
            final List<Digue> diguesForSE = Drepo.getBySystemeEndiguement(se);
            for (Digue digue : digues) {
                if (!diguesForSE.contains(digue)) continue;
                diguesFound.add(digue);

                final File digueDir = getOrCreateDG(seDir, digue);
                digueFiles.remove(digueDir);

                final Set<File> trFiles = listModel(digueDir, TR);

                final List<TronconDigue> tronconForDigue = TRrepo.getByDigue(digue);
                for (final TronconDigue td : troncons) {
                    if (!tronconForDigue.contains(td)) continue;
                    tronconsFound.add(td);

                    final File trDir = getOrCreateTR(digueDir, td);
                    trFiles.remove(trDir);
                }

                // on place les tronçon disparus dans les fichiers deplacé
                tronMoved.addAll(trFiles);
            }

            // on place les digues disparues dans les fichiers deplacé
            digueMoved.addAll(digueFiles);
        }
        digues.removeAll(diguesFound);

        // on recupere les repertoire des digues / tronçons dans les SE detruits
        for (File seFile : seFiles) {
            digueMoved.addAll(listModel(seFile, DG));
            tronMoved.addAll(listModel(seFile, TR));
        }

        /**
         * On place toute les digues et troncons non trouvé dans un group a part.
         */
        final Set<File> digueFiles = listModel(unclassifiedDir, DG);

        for (final Digue digue : digues) {
            final File digueDir = getOrCreateDG(unclassifiedDir, digue);
            digueFiles.remove(digueDir);

            final Set<File> trFiles = listModel(digueDir, TR);

            for (final TronconDigue td : troncons) {
                if (td.getDigueId()==null || !td.getDigueId().equals(digue.getDocumentId())) continue;
                tronconsFound.add(td);

                final File trDir = getOrCreateTR(digueDir, td);
                trFiles.remove(trDir);
            }

            // on place les tronçon disparus dans les fichiers deplacé
            tronMoved.addAll(trFiles);
        }

        // on place les digues disparues dans les fichiers deplacé
        digueMoved.addAll(digueFiles);

        // on recupere les repertoire tronçons dans les digues detruites
        for (File digueFile : digueFiles) {
            tronMoved.addAll(listModel(digueFile, TR));
        }

        troncons.removeAll(tronconsFound);

        final Set<File> trFiles = listModel(unclassifiedDir, TR, false);

        for(final TronconDigue td : troncons){
            final File trDir = getOrCreateTR(unclassifiedDir, td);
            trFiles.remove(trDir);
        }

        // on place les tronçon disparus dans les fichiers deplacé
        tronMoved.addAll(trFiles);

        /**
         * On restore les fichier deplacé dans leur nouvel emplacement.
         */
        final Set<File> tronMovedFound = new HashSet<>();
        for (File movedFile : tronMoved) {
            final File newFile = findFile(rootDirectory, movedFile);
            if (newFile != null) {
                backupDirectory(newFile.getParentFile(), movedFile);
                tronMovedFound.add(movedFile);
            }
        }
        tronMoved.removeAll(tronMovedFound);

        final Set<File> digueMovedFound = new HashSet<>();
        for (File movedFile : digueMoved) {
            final File newFile = findFile(rootDirectory, movedFile);
            if (newFile != null) {
                backupDirectory(newFile.getParentFile(), movedFile);
                digueMovedFound.add(movedFile);
            }
        }
        digueMoved.removeAll(digueMovedFound);

        /**
         * On place les fichiers deplacé non relocaliser dans le backup.
         */
        backupDirectories(saveDir, tronMoved);
        backupDirectories(saveDir, digueMoved);
        backupDirectories(saveDir, seFiles);
    }
}
