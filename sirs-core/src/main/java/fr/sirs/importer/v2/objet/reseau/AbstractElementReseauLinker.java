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
package fr.sirs.importer.v2.objet.reseau;

import fr.sirs.importer.v2.*;
import com.healthmarketscience.jackcess.Row;
import fr.sirs.core.SessionCore;
import fr.sirs.core.model.Element;
import fr.sirs.core.model.ObjetReseau;
import fr.sirs.importer.AccessDbImporterException;
import java.io.IOException;
import java.util.Iterator;
import org.springframework.beans.factory.annotation.Autowired;

import fr.sirs.util.property.Reference;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import org.apache.sis.util.ArgumentChecks;
import org.springframework.stereotype.Component;

/**
 *
 * Create links between object using an MS-Access join table.
 *
 * @author Alexis Manin (Geomatys)
 */
@Component
public abstract class AbstractElementReseauLinker implements Linker<ObjetReseau, ObjetReseau>, WorkMeasurable {

    private final String tableName;
    private final String firstColumn;
    private final String secondColumn;

    private final SimpleIntegerProperty count = new SimpleIntegerProperty(0);

    protected AbstractElementReseauLinker(final String tableName, final String firstColumn, final String secondColumn) {
        ArgumentChecks.ensureNonNull("Table name", tableName);
        ArgumentChecks.ensureNonNull("First column name", firstColumn);
        ArgumentChecks.ensureNonNull("Second column name", secondColumn);

        this.tableName = tableName;
        this.firstColumn = firstColumn;
        this.secondColumn = secondColumn;
    }

    @Override
    public Class<ObjetReseau> getTargetClass() {
        return ObjetReseau.class;
    }

    @Override
    public Class<ObjetReseau> getHolderClass() {
        return ObjetReseau.class;
    }

    @Autowired
    protected ImportContext context;

    @Autowired
    protected SessionCore session;

    @Override
    public void link() throws AccessDbImporterException, IOException {

        Iterator<Row> iterator = context.inputDb.getTable(tableName).iterator();

        final AbstractImporter<ObjetReseau> reseauImporter = context.importers.get(ObjetReseau.class);
        if (reseauImporter == null) {
            throw new AccessDbImporterException("No importer found for type " + ObjetReseau.class.getCanonicalName());
        }

        final HashMap<String, Element> toUpdate = new HashMap<>();

        String firstId, secondId;
        Element first;
        Element second;
        Row current;
        while (iterator.hasNext()) {

            // Split execution in bulks
            while (iterator.hasNext() && toUpdate.size() < context.bulkLimit) {
                current = iterator.next();

                // Those fields should be SQL join table keys, so they should never be null.
                try {
                    firstId = reseauImporter.getImportedId(current.get(firstColumn));
                } catch (IllegalStateException e) {
                    context.reportError(new ErrorReport(e, current, tableName, firstColumn, null, null, "No imported object found for input Id.", CorruptionLevel.ROW));
                    continue;
                }

                try {
                    secondId = reseauImporter.getImportedId(current.get(secondColumn));
                } catch (IllegalStateException e) {
                    context.reportError(new ErrorReport(e, current, tableName, secondColumn, null, null, "No imported object found for input Id.", CorruptionLevel.ROW));
                    continue;
                }

                first = toUpdate.get(firstId);
                if (first == null) {
                    first = session.getElement(firstId).orElse(null);
                }

                second = toUpdate.get(secondId);
                if (second == null) {
                    second = session.getElement(secondId).orElse(null);
                }

                try {
                    link(first, second, toUpdate);
                } catch (Exception e) {
                    context.reportError(tableName, current, e);
                }
            }

            context.executeBulk(toUpdate.values());
            toUpdate.clear();
        }
        count.set(1);
    }

    @Override
    public int getTotalWork() {
        return 1;
    }

    @Override
    public IntegerProperty getWorkDone() {
        return count;
    }

    /**
     * Analyze input elements and bind them if we find references to each other classes.
     * @param first The first element to analyze.
     * @param second The second element to analyze.
     * @param updates A map in which we will put elements which have been modified after the analysis.
     * @throws IntrospectionException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    private void link(Element first, Element second, final Map<String, Element> updates) throws IntrospectionException, IllegalAccessException, InvocationTargetException {
        if (putLink(first, second)) {
            updates.put(first.getId(), first);
        }

        if (putLink(second, first)) {
            updates.put(second.getId(), second);
        }
    }

    /**
     * Try to find in holder element a property to hold a link to the target input.
     * @param holder The element which must hold the link.
     * @param target The element to point to.
     * @return True if a link to the target has been set in holder parameter. False otherwise.
     * @throws IntrospectionException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    private boolean putLink(final Element holder, final Element target) throws IntrospectionException, IllegalAccessException, InvocationTargetException {
        final Class holderClass = holder.getClass();
        final Class targetClass = target.getClass();

        final BeanInfo info = Introspector.getBeanInfo(holderClass);
        for (final PropertyDescriptor desc : info.getPropertyDescriptors()) {
            final Method readMethod = desc.getReadMethod();
            final Reference annot = readMethod.getAnnotation(Reference.class);
            if (annot != null && annot.ref().isAssignableFrom(targetClass)) {
                final Method writeMethod = desc.getWriteMethod();
                if (Collection.class.isAssignableFrom(readMethod.getReturnType())) {
                    readMethod.setAccessible(true);
                    ((Collection)readMethod.invoke(holder)).add(target.getId());
                } else if (writeMethod != null) {
                    writeMethod.setAccessible(true);
                    writeMethod.invoke(holder, target.getId());
                }
                return true;
            }
        }
        return false;
    }
}
