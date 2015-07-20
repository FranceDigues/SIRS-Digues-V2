
package fr.sirs.plugin.document;

import fr.sirs.core.model.Digue;
import fr.sirs.core.model.SystemeEndiguement;
import fr.sirs.core.model.TronconDigue;
import fr.sirs.plugin.document.ui.DocumentsPane;
import static fr.sirs.plugin.document.ui.DocumentsPane.UNCLASSIFIED;
import static fr.sirs.plugin.document.ui.DocumentsPane.DOCUMENT_FOLDER;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sis.util.logging.Logging;
import org.geotoolkit.util.FileUtilities;

/**
 *
 * @author guilhem
 */
public class PropertiesFileUtilities {
    
    private static final Logger LOGGER = Logging.getLogger(PropertiesFileUtilities.class);
    
    public static String getInventoryNumber(final File f) {
        final Properties prop = getSirsProperties(f);
        return prop.getProperty(f.getName() + "_inventory_number", "");
    }
    
    public static void setInventoryNumber(final File f, String value) {
        final Properties prop   = getSirsProperties(f);
        prop.put(f.getName() + "_inventory_number", value);
        
        storeSirsProperties(prop, f);
    }
    
    public static void removeInventoryNumber(final File f) {
        final Properties prop   = getSirsProperties(f);
        prop.remove(f.getName() + "_inventory_number");
        
        storeSirsProperties(prop, f);
    }
    
    public static String getClassPlace(final File f) {
        final Properties prop = getSirsProperties(f);
        return prop.getProperty(f.getName() + "_class_place", "");
    }
    
    public static void setClassPlace(final File f, String value) {
        final Properties prop   = getSirsProperties(f);
        prop.put(f.getName() + "_class_place", value);
        
        storeSirsProperties(prop, f);
    }
    
    public static void removeClassPlace(final File f) {
        final Properties prop   = getSirsProperties(f);
        prop.remove(f.getName() + "_class_place");
        
        storeSirsProperties(prop, f);
    }
    
    public static Boolean getDOIntegrated(final File f) {
        final Properties prop = getSirsProperties(f);
        return Boolean.parseBoolean(prop.getProperty(f.getName() + "_do_integrated", "false"));
    }
    
    public static void setDOIntegrated(final File f, boolean value) {
        final Properties prop   = getSirsProperties(f);
        prop.put(f.getName() + "_do_integrated", Boolean.toString(value));
        
        storeSirsProperties(prop, f);
    }
    
    public static Boolean getIsModelFolder(final File f) {
        return getIsSe(f) || getIsDg(f) || getIsTr(f);
    }
    
    public static String getLibelle(final File f) {
        final Properties prop = getSirsProperties(f);
        return prop.getProperty(f.getName() + "_libelle", "null");
    }
    
    public static Boolean getIsSe(final File f) {
        final Properties prop = getSirsProperties(f);
        return Boolean.parseBoolean(prop.getProperty(f.getName() + "_se", "false"));
    }
    
    public static void setIsSe(final File f, boolean value, final String libelle) {
        final Properties prop   = getSirsProperties(f);
        prop.put(f.getName() + "_se", Boolean.toString(value));
        prop.put(f.getName() + "_libelle", libelle);
        
       storeSirsProperties(prop, f);
    }
    
    public static Boolean getIsTr(final File f) {
        final Properties prop = getSirsProperties(f);
        return Boolean.parseBoolean(prop.getProperty(f.getName() + "_tr", "false"));
    }
    
    public static void setIsTr(final File f, boolean value, final String libelle) {
        final Properties prop   = getSirsProperties(f);
        prop.put(f.getName() + "_tr", Boolean.toString(value));
        prop.put(f.getName() + "_libelle", libelle);
        
        storeSirsProperties(prop, f);
    }
    
    public static Boolean getIsDg(final File f) {
        final Properties prop = getSirsProperties(f);
        return Boolean.parseBoolean(prop.getProperty(f.getName() + "_dg", "false"));
    }
    
    public static void setIsDg(final File f, boolean value, final String libelle) {
        final Properties prop   = getSirsProperties(f);
        prop.put(f.getName() + "_dg", Boolean.toString(value));
        prop.put(f.getName() + "_libelle", libelle);
        
        storeSirsProperties(prop, f);
    }
    
    public static void removeDOIntegrated(final File f) {
        final Properties prop   = getSirsProperties(f);
        prop.remove(f.getName() + "_do_integrated");
        
        storeSirsProperties(prop, f);
    }
    
    public static void removeProperties(final File f) {
        final Properties prop   = getSirsProperties(f);
        
        Set<Entry<Object,Object>> properties = new HashSet<>(prop.entrySet());
        for (Entry<Object,Object> entry : properties) {
            if (((String)entry.getKey()).startsWith(f.getName())) {
                prop.remove(entry.getKey());
            }
        }
        
        //save cleaned properties file
        storeSirsProperties(prop, f);
    }
    
    public static void storeSirsProperties(final Properties prop, final File f) {
        storeSirsProperties(prop, f, true);
    }
    
    public static void storeSirsProperties(final Properties prop, final File f, boolean parent) {
        try {
            final File sirsPropFile = getSirsPropertiesFile(f, parent);
            prop.store(new FileWriter(sirsPropFile), "");
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "Erro while accessing sirs properties file.", ex);
        }
    }
    
    public static File getSirsPropertiesFile(final File f) throws IOException {
        return getSirsPropertiesFile(f, true);
    }
    
    public static File getSirsPropertiesFile(final File f, final boolean parent) throws IOException {
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
    
    public static Properties getSirsProperties(final File f) {
        return getSirsProperties(f, true);
    }
    
    public static Properties getSirsProperties(final File f, final boolean parent) {
        final Properties prop = new Properties();
        try {
            final File sirsPropFile = getSirsPropertiesFile(f, parent);
            if (sirsPropFile != null) {
                prop.load(new FileReader(sirsPropFile));
            } 
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "Erro while loading/creating sirs properties file.", ex);
        }
        return prop;
    }
    
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
    
    public static long getFileSize(final File f) {
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
        setIsSe(sdDir, true, name);
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
        setIsDg(digueDir, true, name);
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
        setIsTr(trDir, true, name);
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
        Properties prop = getSirsProperties(rootDirectory, false);
        return (String) prop.get("database_identifier");
    }
    
    public static void setDatabaseIdentifier(final File rootDirectory, final String key) {
        Properties prop = getSirsProperties(rootDirectory, false);
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
        final Properties prop                = getSirsProperties(f);
        Set<Entry<Object,Object>> properties = new HashSet<>(prop.entrySet());
        for (Entry<Object,Object> entry : properties) {
            if (((String)entry.getKey()).startsWith(f.getName())) {
                extracted.put(entry.getKey(), entry.getValue());
                prop.remove(entry.getKey());
            }
        }
        
        //save cleaned properties file
        storeSirsProperties(prop, f);
        
        
        final File newDir = new File(saveDir, f.getName());
        try {
            // we copy only the "dossier d'ouvrage" directory
            if (!newDir.exists()) {
                newDir.mkdir();
            }
            
            final File doFile    = new File(f, DOCUMENT_FOLDER);
            final File newDoFile = new File(newDir, DOCUMENT_FOLDER);
            
            FileUtilities.copy(doFile, newDoFile);
            FileUtilities.deleteDirectory(f);
            
            // save new properties
            final Properties newProp = getSirsProperties(newDir);
            newProp.putAll(extracted);
            
            storeSirsProperties(newProp, newDir);
            
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "Error while moving destroyed obj to backup folder", ex);
        }
    }
    
    public  static Set<File> listSE(final File rootDirectory) {
        Set<File> seList = new HashSet<>();
        listSE(rootDirectory, seList);
        return seList;
    }
    
    private static void listSE(final File rootDirectory, Set<File> seList) {
        for (File f : rootDirectory.listFiles()) {
            if (f.isDirectory()) {
                if (getIsSe(f)) {
                    seList.add(f);
                } else {
                    listSE(f, seList);
                }
            }
        }
    }
    
    public static Set<File> listDigue(final File rootDirectory) {
        Set<File> digueList = new HashSet<>();
        listDigue(rootDirectory, digueList);
        return digueList;
    }
    
    private static void listDigue(final File rootDirectory, Set<File> digueList) {
        for (File f : rootDirectory.listFiles()) {
            if (f.isDirectory()) {
                if (getIsDg(f)) {
                    digueList.add(f);
                } else {
                    listDigue(f, digueList);
                }
            }
        }
    }
    
    public static Set<File> listTroncon(final File digueDirectory) {
        return listTroncon(digueDirectory, true);
    }
    
    public static Set<File> listTroncon(final File digueDirectory, boolean deep) {
        Set<File> trList = new HashSet<>();
        listTroncon(digueDirectory, trList, deep);
        return trList;
    }
    
    private static void listTroncon(final File digueDirectory, Set<File> trList, boolean deep) {
        for (File f : digueDirectory.listFiles()) {
            if (f.isDirectory()) {
                if (getIsTr(f)) {
                    trList.add(f);
                } else if (deep){
                    listTroncon(f, trList, deep);
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
}