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

import fr.sirs.SIRS;
import fr.sirs.core.model.AvecForeignParent;
import fr.sirs.core.model.Element;
import java.util.logging.Level;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 *
 * @author Samuel Andrés (Geomatys)
 * @param <T>
 */
public class ForeignParentPojoTable<T extends AvecForeignParent> extends PojoTable {

    /**
     * The element to set as foreignParent for any created element using {@linkplain #createPojo()
     * }. On the contrary to the parent, the foreign parent purpose is not to
     * contain the created pojo, but nor to reference it as an owner element.
     * the foreignParent id is only refenced by the table elements.
     */
    protected final StringProperty foreignParentIdProperty = new SimpleStringProperty();

    /**
     * Définit l'élément en paramètre comme principal référent de tout élément
     * créé via cette table.
     *
     * @param foreignParentId L'id de l'élément qui doit devenir le parent
     * référencé de tout objet créé via la PojoTable.
     */
    public void setForeignParentId(final String foreignParentId) {
        foreignParentIdProperty.set(foreignParentId);
    }

    /**
     *
     * @return L'id de l'élément référencé par tout élément créé via cette
     * table.
     */
    public String getForeignParentId() {
        return foreignParentIdProperty.get();
    }

    /**
     *
     * @return La propriété contenant l'id de l'élément à référencer par tout
     * élément créé via cette table. Jamais nulle, mais peut-être vide.
     */
    public StringProperty foreignParentProperty() {
        return foreignParentIdProperty;
    }

    public ForeignParentPojoTable(Class<T> pojoClass, String title, ObjectProperty<? extends Element> container) {
        super(pojoClass, title, container);
    }

    @Override
    protected T createPojo() {
        T created = null;
        if (repo != null) {
            created = (T) repo.create();
        } else if (pojoClass != null) {
            try {
                created = (T) session.getElementCreator().createElement(pojoClass);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        if (created != null) {
            created.setForeignParentId(getForeignParentId());

            try {
                repo.add(created);
            } catch (NullPointerException e) {
                SIRS.LOGGER.log(Level.WARNING, "Repository introuvable", e);
            }
            getAllValues().add(created);
        }
        return created;
    }
}
