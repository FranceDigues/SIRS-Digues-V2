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
package fr.sirs.theme.ui;

import fr.sirs.Injector;
import fr.sirs.core.model.AbstractPositionDocument;
import fr.sirs.core.model.Element;
import fr.sirs.core.model.TronconDigue;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;

/**
 *
 * @author Samuel Andrés (Geomatys)
 *
 * @param <T> The type of the position of document.
 */
public class PositionDocumentPojoTable<T extends AbstractPositionDocument> extends ListenPropertyPojoTable<String> {

    public PositionDocumentPojoTable(Class<T> pojoClass, String title, ObjectProperty<? extends Element> container) {
        super(pojoClass, title, container);
    }

    @Override
    protected T createPojo() {
        final TronconDigue premierTroncon = Injector.getSession().getRepositoryForClass(TronconDigue.class).getOne();
        final T position = (T) super.createPojo(premierTroncon);

        try {
            ((Property<String>) propertyMethodToListen.invoke(position)).setValue(propertyReference);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            Logger.getLogger(PositionDocumentPojoTable.class.getName()).log(Level.SEVERE, null, ex);
        }
        return position;
    }
}
