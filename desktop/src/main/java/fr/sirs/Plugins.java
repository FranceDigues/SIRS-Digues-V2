
package fr.sirs;

import fr.sirs.core.SirsCore;
import fr.sirs.theme.Theme;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import javax.imageio.spi.ServiceRegistry;

/**
 * Classe utilitaire de chargement des plugins.
 * 
 * @author Johann Sorel (Geomatys)
 */
public class Plugins {
    
    private static List<Plugin> REGISTERED_PLUGINS;
    private static List<Theme> THEMES;
    
    private static final Pattern JAR_PATTERN = Pattern.compile("(?i).*(\\.jar)$");
    
    /**
     * Récupérer la liste des plugins.
     * 
     * @return Tableau de plugin, jamais nul.
     */
    public static Plugin[] getPlugins() {
        if (REGISTERED_PLUGINS == null) {
            //creation de la liste des plugins disponibles.
            final Iterator<Plugin> ite = ServiceRegistry.lookupProviders(Plugin.class);
            final List<Plugin> candidates = new ArrayList<>();
            while(ite.hasNext()){
                candidates.add(ite.next());
            }
            REGISTERED_PLUGINS = Collections.unmodifiableList(candidates);
        }
        return REGISTERED_PLUGINS.toArray(new Plugin[0]);
    }
    
    /**
     * Récupérer la liste des thèmes.
     * 
     * @return Tableau de thème, jamais nul.
     */
    public static synchronized Theme[] getThemes() {
        if(THEMES==null){
            THEMES = new ArrayList<>();
            for(Plugin plugin : getPlugins()){
                THEMES.addAll(plugin.getThemes());
            }
        }
        return THEMES.toArray(new Theme[0]);
    }
    
    public static void loadPlugins() throws IOException, IllegalStateException {
        if (Files.isDirectory(SirsCore.PLUGINS_PATH)) {
            // TODO : Keep list of jars in a static variable to perform scan only once ?
            URL[] libs = Files.walk(SirsCore.PLUGINS_PATH).filter(Plugins::isJar).map(Plugins::toURL).toArray(URL[]::new);
            if (libs.length > 0) {
                ClassLoader parentLoader = Thread.currentThread().getContextClassLoader();
                final URLClassLoader newLoader = new URLClassLoader(libs, parentLoader);
                Thread.currentThread().setContextClassLoader(newLoader);
            }
        }
    }
    
    public static boolean isJar(final Path input) {
        return Files.isRegularFile(input) && JAR_PATTERN.matcher(input.getFileName().toString()).matches();
    }
    
    public static URL toURL(final Path input) {
        try {
            return input.toUri().toURL();
        } catch (MalformedURLException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
