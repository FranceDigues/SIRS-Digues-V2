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
package fr.sirs.importer.v2.mapper;

import fr.sirs.core.model.ContactOrganisme;
import java.beans.IntrospectionException;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 *
 * @author Alexis Manin (Geomatys)
 */
@Component
public class ContactOrganismeMapperSpi extends GenericMapperSpi<ContactOrganisme> {

    private final HashMap<String, String> bindings;
    public ContactOrganismeMapperSpi() throws IntrospectionException {
        super(ContactOrganisme.class);

        bindings = new HashMap<>(1);
        bindings.put("ID_INTERVENANT", "contactId");
    }

    @Override
    public Map<String, String> getBindings() {
        return bindings;
    }
}
