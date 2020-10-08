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
package fr.sirs.util.odt;

import fr.sirs.core.SirsCore;
import fr.sirs.core.model.Element;
import java.beans.PropertyDescriptor;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import org.apache.sis.util.ArgumentChecks;
import org.opengis.feature.Feature;

/**
 * Convert a property from an object to a string. Created in order to print object
 * properties into ODT document.
 * @author Alexis Manin (Geomatys)
 */
public abstract class PropertyPrinter {

    protected final Set<String> properties;

    protected PropertyPrinter(final Collection<String> acceptedProperties) {
        ArgumentChecks.ensureNonNull("Printer accepted properties", acceptedProperties);
        properties = Collections.unmodifiableSet(new HashSet<>(acceptedProperties));
    }

    protected PropertyPrinter(final String first, final String... acceptedProperties) {
        ArgumentChecks.ensureNonNull("Printer accepted properties", first);
        if (acceptedProperties == null) {
            properties = Collections.singleton(first);
        } else {
            final Set tmpSet = new HashSet<>(acceptedProperties.length+1);
            tmpSet.add(first);
            tmpSet.addAll(Arrays.asList(acceptedProperties));
            properties = Collections.unmodifiableSet(tmpSet);
        }
    }

    public String print(final Element source, final String property, Function<Element, String> mapping) throws ReflectiveOperationException {
        ArgumentChecks.ensureNonNull("Object holding property to print", source);
        ArgumentChecks.ensureNonNull("Descriptor for the property to print", property);
        SirsCore.LOGGER.log(Level.INFO, "request printing pseudo property {0}", property);
        if (properties.contains(property)) {
            return mapping.apply(source);
        } else {
            throw new IllegalArgumentException("Given property name is not handled by current printer : "+property);
        }
    }

    public String print(final Object source, final PropertyDescriptor property) throws ReflectiveOperationException {
        ArgumentChecks.ensureNonNull("Object holding property to print", source);
        ArgumentChecks.ensureNonNull("Descriptor for the property to print", property);
        if (properties.contains(property.getName())) {
            property.getReadMethod().setAccessible(true);
            return printImpl(source, property);
        } else {
            throw new IllegalArgumentException("Given property name is not handled by current printer : "+property.getName());
        }
    }

    public String print(final Feature source, final String propertyToPrint) {
        ArgumentChecks.ensureNonNull("Feature holding property to print", source);
        if (propertyToPrint == null || propertyToPrint.isEmpty()) {
            throw new IllegalArgumentException("No property specified for printing.");
        }
        return printImpl(source, propertyToPrint);
    }

    protected abstract String printImpl(final Feature source, final String propertyToPrint);

    protected abstract String printImpl(final Object source, final PropertyDescriptor property) throws ReflectiveOperationException;
}
