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

import fr.sirs.core.InjectorCore;
import fr.sirs.core.SessionCore;
import fr.sirs.core.SirsCore;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.apache.sis.util.Static;

/**
 *
 * @author Alexis Manin (Geomatys)
 */
public class Printers extends Static {

    private static Map<String, PropertyPrinter> PRINTERS;

    private static PropertyPrinter DEFAULT_PRINTER;

    public static PropertyPrinter getPrinter(final String propertyName) {
        if (PRINTERS == null) {
            /**
             * We create list of available printers only once. If we find multiple
             * printers for a single property, we make a list of all found doublons,
             * then warn user that we've found multiple implementations for the same
             * property.
             */
            final Map<String, PropertyPrinter> beans = InjectorCore.getBean(SessionCore.class)
                    .getApplicationContext().getBeansOfType(PropertyPrinter.class);

            PRINTERS = new HashMap<>(beans.size());
            final Map<String, Set<PropertyPrinter>> doublons = new HashMap<>();
            for (final PropertyPrinter p : beans.values()) {
                for (final String pName : p.properties) {
                    final PropertyPrinter doublon = PRINTERS.put(pName, p);
                    if (doublon != null) {
                        final Set<PropertyPrinter> tmpSet = doublons.get(pName);
                        tmpSet.add(p);
                        tmpSet.add(doublon);
                    }
                }
            }

            if (!doublons.isEmpty()) {
                final String sep = System.lineSeparator();
                for (final Map.Entry<String, Set<PropertyPrinter>> entry : doublons.entrySet()) {
                        SirsCore.LOGGER.warning(() -> {
                            final StringBuilder msg = new StringBuilder("Multiple printers registered for property ")
                                    .append(entry.getKey())
                                    .append(sep)
                                    .append("Class names : ");

                            for (final PropertyPrinter printer : entry.getValue()) {
                                msg.append(sep).append(printer.getClass().getCanonicalName());
                            }

                            return msg.toString();
                        });
                }
            }
        }

        final PropertyPrinter found = PRINTERS.get(propertyName);
        if (found == null) {
            if (DEFAULT_PRINTER == null) {
                DEFAULT_PRINTER = new DefaultPrinter();
            }
            return DEFAULT_PRINTER;
        } else {
            return found;
        }
    }
}
