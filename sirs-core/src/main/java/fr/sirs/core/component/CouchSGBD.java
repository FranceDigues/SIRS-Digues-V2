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
package fr.sirs.core.component;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Mapping minimal de l'url racine de CouchDB pour récupérer la version.
 * 
 * Exemple en CouchDB 1 : 
 * <pre>
 * {"couchdb":"Welcome","uuid":"1298997918f91278e88a77ddc380590c","version":"1.6.0","vendor":{"name":"Ubuntu","version":"15.10"}}
 * </pre>
 * 
 * Exemple en CouchDB 2 :
 * <pre>
 * {"couchdb":"Welcome","version":"2.1.1","features":["scheduler"],"vendor":{"name":"The Apache Software Foundation"}}
 * </pre>
 * 
 * @author Samuel Andrés (Geomatys)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CouchSGBD {

    private String version;

    public String getVersion() {
        return version;
    }
}
