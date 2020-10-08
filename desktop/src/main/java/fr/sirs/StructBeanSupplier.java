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
import fr.sirs.core.component.DocumentChangeEmiter;
import fr.sirs.core.component.DocumentListener;
import fr.sirs.core.model.Element;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import org.geotoolkit.data.bean.BeanFeatureSupplier;
import org.geotoolkit.display2d.GO2Utilities;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.Id;
import org.opengis.filter.identity.FeatureId;

/**
 * A data supplier which provides feature store wrapping a set of java beans. It listens on application {@link DocumentChangeEmiter} to be notified when data is updated.
 *
 * @author Johann Sorel (Geomatys)
 */
public class StructBeanSupplier extends BeanFeatureSupplier implements DocumentListener {

    private static final FilterFactory2 FF = GO2Utilities.FILTER_FACTORY;

    public StructBeanSupplier(final Class clazz, final Supplier<Iterable> callable) {
        this(clazz, "id", callable);
    }

    public StructBeanSupplier(final Class clazz, final String idField, final Supplier<Iterable> callable) {
        super(clazz, idField, hasField(clazz,"geometry")?"geometry":null, CorePlugin.MAP_PROPERTY_PREDICATE, null, Injector.getSession().getProjection(), callable::get);
        try {
            Injector.getDocumentChangeEmiter().addListener(this);
        } catch (Exception e) {
            SirsCore.LOGGER.warning("Feature store supplier for class "+ clazz.getCanonicalName() +" cannot listen on database changes.");
        }
    }

    private static boolean hasField(Class clazz, String field){
        try {
            for(PropertyDescriptor desc : Introspector.getBeanInfo(clazz).getPropertyDescriptors()){
                if(desc.getName().equals(field)){
                    return true;
                }
            }
        } catch (IntrospectionException e) {
            return false;
        }
        return false;
    }

    @Override
    public void documentCreated(Map<Class, List<Element>> added) {
        if (added == null) {
            return;
        }
        final Id filter = getIdFilter(added);
        if (filter != null) {
            fireFeaturesAdded(filter);
        }
    }

    @Override
    public void documentChanged(Map<Class, List<Element>> changed) {
        if (changed == null) {
            return;
        }
        final Id filter = getIdFilter(changed);
        if (filter != null) {
            fireFeaturesUpdated(filter);
        }
    }

    @Override
    public void documentDeleted(final Set<String> deleted) {
        if (deleted == null) {
            return;
        }
        final Id filter = getIdFilter(deleted);
        if (filter != null) {
            fireFeaturesDeleted(filter);
        }
    }

    private final Id getIdFilter(final Map<Class, List<Element>> elementMap) {
        final List<Element> elements = elementMap.get(getBeanClass());
        if (elements == null || elements.isEmpty()) {
            return null;
        }
        final Set<FeatureId> fIds = new HashSet<>();
        for (Element e : elements) {
            fIds.add(FF.featureId(e.getId()));
        }
        return FF.id(fIds);
    }

    private Id getIdFilter(final Set<String> elements) {
        final Set<FeatureId> fIds = new HashSet<>();
        for (String id : elements) {
            fIds.add(FF.featureId(id));
        }
        return FF.id(fIds);
    }
}
