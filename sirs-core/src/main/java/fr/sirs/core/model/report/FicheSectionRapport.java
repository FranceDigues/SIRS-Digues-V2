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

import fr.sirs.core.InjectorCore;
import fr.sirs.core.SessionCore;
import fr.sirs.core.SirsCoreRuntimeException;
import fr.sirs.core.model.AbstractPhoto;
import fr.sirs.core.model.AvecPhotos;
import fr.sirs.core.model.Desordre;
import fr.sirs.core.model.Element;
import fr.sirs.core.model.ElementCreator;
import fr.sirs.util.odt.ODTUtils;
import fr.sirs.util.property.Reference;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.odftoolkit.simple.TextDocument;
import org.opengis.feature.Feature;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.w3c.dom.Text;

/**
 * Detailed element printing.
 *
 * @author Alexis Manin (Geomatys)
 */
@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class FicheSectionRapport extends AbstractSectionRapport {

    private int nbPhotos = 0;

    public int getNbPhotos() {
        return nbPhotos;
    }

    public void setNbPhotos(int newValue) {
        nbPhotos = newValue;
    }

    private final SimpleStringProperty modeleElementId = new SimpleStringProperty();

    /**
     *
     * @return ODT template used to print each element given in this section.
     */
    @Reference(ref=ModeleElement.class)
    public StringProperty ModeleElementIdProperty() {
        return modeleElementId;
    }

    public String getModeleElementId() {
        return modeleElementId.get();
    }

    public void setModeleElementId(final String newValue) {
        modeleElementId.set(newValue);
    }

    @Override
    public Element copy() {
        final FicheSectionRapport rapport = ElementCreator.createAnonymValidElement(FicheSectionRapport.class);
        super.copy(rapport);

        rapport.setNbPhotos(getNbPhotos());
        rapport.setModeleElementId(getModeleElementId());

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
     * 
     * @param ctx
     * @throws Exception 
     */
    @Override
    public void printSection(final PrintContext ctx) throws Exception {
        
        if (ctx.elements == null && ctx.queryResult == null) {
            return;
        }
        
        
        /*
        A- récupération du modèle de fiche
        =================================*/

        // Find ODT template
        if (modeleElementId.get() == null)
            throw new IllegalStateException("No model set for printing !");

        final SessionCore session = InjectorCore.getBean(SessionCore.class);
        ModeleElement model = session.getRepositoryForClass(ModeleElement.class).get(modeleElementId.get());
        byte[] odt = model.getOdt();
        if (odt == null || odt.length <= 0) {
            throw new IllegalStateException("No ODT template available.");
        }

        
        /*
        B- détermination des éléments à imprimer
        =======================================*/
        
        final Iterator iterator;
        if (ctx.elements != null) {
            // Print only elements managed by underlying model.
            final String targetClass = model.getTargetClass();
            if (targetClass != null && !targetClass.isEmpty()) {
                final Class tmpClass = Thread.currentThread().getContextClassLoader().loadClass(targetClass);
                iterator = ctx.elements.filter(input -> tmpClass.isAssignableFrom(input.getClass())).iterator();
            } else {
                iterator = ctx.elements.iterator();
            }
        } else {
            // No elements available. Print filter values.
            iterator = ctx.queryResult.iterator();
        }

        /**
         * If we've got elements to print, section template is read, then we fill
         * it for each element, and append it to context target.
         */
        if (iterator.hasNext()) {
            final Object first = iterator.next();

            try (final ByteArrayInputStream stream = new ByteArrayInputStream(odt);
                    final TextDocument doc = TextDocument.loadDocument(stream)) {

                final boolean isElement = first instanceof Element;
                if (first instanceof Element) {
                    ODTUtils.fillTemplate(doc, (Element)first);
                } else if (first instanceof Feature) {
                    ODTUtils.fillTemplate(doc, (Feature)first);
                } else throw new IllegalArgumentException("Unknown object given for printing !");

                // Forced to do it to avoid variable erasing at concatenation
                final Map<String, List<Text>> replaced = ODTUtils.replaceUserVariablesWithText(doc);

                ODTUtils.append(ctx.target, doc);

                if (isElement) {
                    printPhotos(ctx.target, (Element)first, nbPhotos);
                }

                // For next elements, we replace directly text attributes we've put instead of variables, to avoid reloading original template.
                iterator.forEachRemaining(next -> {
                    try {
                        if (isElement) {
                            ODTUtils.replaceTextContent((Element)next, (Map)replaced);
                        } else {
                            ODTUtils.replaceTextContent((Feature)next, (Map)replaced);
                        }

                        ODTUtils.append(ctx.target, doc);
                        if (isElement) {
                            printPhotos(ctx.target, (Element)next, nbPhotos);
                        }
                    } catch (RuntimeException ex) {
                        throw ex;
                    } catch (Exception ex) {
                        throw new SirsCoreRuntimeException(ex);
                    }
                });
            }
        }
    }

    private static void printPhotos(final TextDocument holder, final Element source, int nbPhotosToPrint) {
        if (nbPhotosToPrint > 0) {
            final Stream<? extends AbstractPhoto> photos;
            if (source instanceof AvecPhotos) {
                photos = ((AvecPhotos<? extends AbstractPhoto>) source).getPhotos().stream()
                        ;
            } else if (source instanceof Desordre) {
                photos = ((Desordre) source).observations.stream()
                        .flatMap(obs -> obs.getPhotos() == null? Stream.empty() : obs.getPhotos().stream());
            } else {
                return;
            }

            photos
                    .sorted(new PhotoComparator())
                    .limit(nbPhotosToPrint)
                    .forEachOrdered(photo -> {
                        try {
                            ODTUtils.appendImage(holder, null, photo, false);
                        } catch (IllegalArgumentException e) {
                            holder.addParagraph("Impossible de retrouver l'image ".concat(photo.getChemin()));
                        }
                    });
        }
    }

    private static class PhotoComparator implements Comparator<AbstractPhoto> {

        @Override
        public int compare(AbstractPhoto o1, AbstractPhoto o2) {
            if (o1 == null)
                return 1;
            else if (o2 == null)
                return -1;

            LocalDate date1 = o1.getDate();
            LocalDate date2 = o2.getDate();

            if (date1 == null)
                return 1;
            else if (date2 == null)
                return -1;
            // We want early dates first
            else return -date1.compareTo(date2);
        }

    }
}
