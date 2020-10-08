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
package fr.sirs;

import fr.sirs.core.SirsCore;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import static java.util.logging.Level.WARNING;
import javafx.concurrent.Task;
import org.apache.sis.util.ArgumentChecks;

/**
 * Tâche de téléchargement du fichier de requêtes préprogrammées et d'enregistrement en local.
 *
 * @author Samuel Andrés (Geomatys)
 */
public class QueryChecker extends Task<Void> {

    private String preprogrammedQueryFilePath;

    private QueryChecker(){}

    public QueryChecker(final String preprogrammedQueryFilePath){
        this.preprogrammedQueryFilePath = preprogrammedQueryFilePath;
        updateTitle("Téléchargement des requêtes préprogrammées");
    }

    @Override
    protected Void call() throws Exception {
        updateMessage("Téléchargement des requêtes préprogrammées.");
        try{
            retriveFileFromURL(new URL(preprogrammedQueryFilePath));
        } catch (IOException ex) {
            SIRS.LOGGER.log(WARNING, "Impossible de récupérer les requêtes préprogrammées", ex);
        }
        return null;
    }

    /**
     * Return the file content located at the URL.
     *
     * @param url
     * @return null if the input URL is null
     * @throws IOException
     */
    private static void retriveFileFromURL(final URL url) throws IOException {
        ArgumentChecks.ensureNonNull("URL du fichier des requêtes préprogrammées", url);

        final URLConnection connection = url.openConnection();
        try{
            try (final InputStreamReader inputStreamReader = new InputStreamReader(connection.getInputStream(), StandardCharsets.ISO_8859_1)) {

                final File localFile = SirsCore.PREPROGRAMMED_QUERIES_PATH.toFile();
                
                if(localFile.exists()){
                    if(localFile.isDirectory()){
                        SIRS.LOGGER.warning("un dossier porte le nom attendu du fichier local des requêtes préprogrammées : les requêtes préprogrammées ne seront pas téléchargées");
                        return;
                    }
                    if(!localFile.delete()){
                        SIRS.LOGGER.warning("impossible de supprimer le fichier local des requêtes préprogrammées : les requêtes préprogrammées ne seront pas mises à jour");
                        return;
                    }
                }
                else {
                    SIRS.LOGGER.info("le fichier local des requêtes préprogrammées n'existe pas encore");
                }
                
                try(final OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(localFile), StandardCharsets.ISO_8859_1)){
                    int r;
                    while (true) {
                        r = inputStreamReader.read();
                        if (r != -1) {
                            outputStreamWriter.write(r);
                        } else {
                            break;
                        }
                    }
                    outputStreamWriter.flush();
                }
            }
        } catch (NullPointerException ex){
            SIRS.LOGGER.log(Level.WARNING, ex.getMessage());
        }
    }
}
