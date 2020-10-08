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
package fr.sirs.core.model.report;

import com.sun.javafx.PlatformUtil;
import fr.sirs.core.SirsCore;
import fr.sirs.core.model.Element;
import fr.sirs.core.model.ElementCreator;
import fr.sirs.core.model.SIRSFileReference;
import fr.sirs.util.odt.ODTUtils;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * A section composed of an unique external file (can be an image, a PDF or
 * another ODT document).
 *
 * Note : {@link #requeteIdProperty() } is not used here, because section generation
 * is independent from any element.
 *
 * @author Alexis Manin (Geomatys)
 */
@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ExterneSectionRapport extends AbstractSectionRapport implements SIRSFileReference {
    /**
     * Path to external document to reference.
     */
    private final StringProperty  chemin = new SimpleStringProperty();
    public StringProperty cheminProperty() {
       return chemin;
    }

    @Override
    public String getChemin() {
        return chemin.get();
    }

    @Override
    public void setChemin(String ref) {
        chemin.set(ref);
    }

    @Override
    public Element copy() {
        final ExterneSectionRapport rapport = ElementCreator.createAnonymValidElement(ExterneSectionRapport.class);
        super.copy(rapport);

        rapport.setChemin(getChemin());

        return rapport;
    }

    @Override
    public boolean removeChild(Element toRemove) {
        return false;
    }

    @Override
    public boolean addChild(Element toAdd) {
        return false;
    }

    @Override
    public Element getChildById(String toSearch) {
        if (toSearch != null && toSearch.equals(getId()))
            return this;
        return null;
    }

    /**
     * Pour les sections de type "document externe", l'impression consiste en l'inclusion du document indiqué dans le 
     * document principal.
     * 
     * @param context
     * @throws Exception 
     */
    @Override
    protected void printSection(PrintContext context) throws Exception {
        
        /*
        A- Détermination du chemin vers le document externe
        ==================================================*/
        
        /* First, we'll analyze input path to determine where target document is
         * located. First, we will use the same strategy as other documents
         * refered by SIRS : We'll concatenate local path with defined root one.
         * If we're not succcessful, we'll try to interpret local path as an
         * absolute one.
         */
        final String strPath = getChemin();
        if (strPath == null || strPath.isEmpty()) {
            return;
        }

        Path path;
        Exception suppressed = null;
        try {
            path = SirsCore.getDocumentAbsolutePath(this);
            if (!Files.isRegularFile(path))
                path = null;
        } catch (Exception e) {
            path = null;
            suppressed = e;
        }

        if (path == null) {
            // Replace separators from another OS.
            if (PlatformUtil.isWindows()) {
                path = Paths.get(strPath.replaceAll("/+", "\\\\"));
            } else {
                path = Paths.get(strPath.replaceAll("\\\\+", File.separator));
            }
        }

        if (!Files.isReadable(path)) {
            final IllegalStateException ex = new IllegalStateException("No readable file can be found using path "+ strPath);
            if (suppressed != null) {
                ex.addSuppressed(suppressed);
            }
            throw ex;
        }

        /*
        B- inclusion du document externe dans le document principal
        ==========================================================*/
        
        ODTUtils.append(context.target, path);
    }
}
