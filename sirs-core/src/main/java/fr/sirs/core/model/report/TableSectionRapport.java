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

import fr.sirs.core.SirsCore;
import fr.sirs.core.model.Element;
import fr.sirs.core.model.ElementCreator;
import fr.sirs.util.odt.ODTUtils;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Spliterators;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.geotoolkit.data.FeatureIterator;
import org.geotoolkit.data.FeatureStoreRuntimeException;
import org.geotoolkit.feature.Feature;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Used for printing brut table reports.
 *
 * @author Alexis Manin (Geomatys)
 */
@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class TableSectionRapport extends AbstractSectionRapport {

    @Override
    public Element copy() {
        final TableSectionRapport rapport = ElementCreator.createAnonymValidElement(TableSectionRapport.class);
        super.copy(rapport);
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
     * Pour l'impression des tableaux, le comportement est le suivant :
     * 
     * S'il y a une requête, le résultat à imprimer se base sur le résultat de la requête. Si les propriétés de la requête
     * incluent un identifiant ET que le flux d'éléments n'est pas null, on ne retient pour impression que les tuples de
     * la requête dont l'identifiant peut être retrouvé parmi les identifiants des éléments. Sinon, tous les tuples sont 
     * retenus pour impression.
     * 
     * S'il n'y a pas de requête, le résultat à imprimer se base sur les éléments.
     * 
     * @param ctx
     * @throws Exception 
     */
    @Override
    protected void printSection(final PrintContext ctx) throws Exception {
        // HACK : For tables, filter is considered to be data, and elements only
        // serve to filter request data by Ids.
        if (ctx.queryResult!=null) {
            
            // récupération des propriétés du résultat de la requête
            final List<String> properties = propertyNames(ctx.queryResult.getFeatureType());
            
            // on vérifie qu'un identifiant est présent dans les résultats de la requête
            final boolean idPresent = properties.remove(SirsCore.ID_FIELD);

            try (final FeatureIterator it = ctx.queryResult.iterator()) {
                final FeatureIterator tmpIt;
                
                /*
                si on a des éléments et qu'on a un identifiant dans le résultat de la requête, on filtre les tuples
                résultant de la requête en ne retenant que ceux relatifs aux éléments dont on dispose.
                */
                if (ctx.elements != null && idPresent){
                    
                    // récupération des identifiants des éléments
                    final Set<String> idFilter = ctx.elements
                        .map(elt -> elt.getId())
                        .filter(id -> id != null)
                        .collect(Collectors.toSet());
                    
                    tmpIt = new FilteredFeatureIterator(it, feat -> idFilter.contains(feat.getPropertyValue(SirsCore.ID_FIELD)));
                }
                else {
                    tmpIt = it;
                }
                
                // ajout des éléments filtrés
                ODTUtils.appendTable(ctx.target, tmpIt, properties);
            }   
        } 
        // If there's no SQL query, we try to print elements
        else if (ctx.elements != null) {
            ODTUtils.appendTable(ctx.target, Spliterators.iterator(ctx.elements.spliterator()), ctx.propertyNames, Collections.emptyMap());
        }
    }
    
    private static class FilteredFeatureIterator implements FeatureIterator {

        private final FeatureIterator source;
        private final Predicate<Feature> filter;

        private Feature next;

        public FilteredFeatureIterator(FeatureIterator source, Predicate<Feature> filter) {
            this.source = source;
            this.filter = filter;
        }

        @Override
        public Feature next() throws FeatureStoreRuntimeException {
            if (hasNext()) {
                final Feature tmpNext = next;
                next = null;
                return tmpNext;
            }

            throw new FeatureStoreRuntimeException("No more elements !");
        }

        @Override
        public boolean hasNext() throws FeatureStoreRuntimeException {
            while (next == null && source.hasNext()) {
                next = source.next();
                if (!filter.test(next)) {
                    next = null;
                }
            }

            return next != null;
        }

        @Override
        public void close() {
            source.close();
        }
    }
}
