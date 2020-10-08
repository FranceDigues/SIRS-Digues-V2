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
package fr.sirs.core.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fr.sirs.util.property.Reference;
import java.lang.reflect.Method;

/**
 *
 * @author Samuel Andrés (Geomatys)
 */
public interface AvecForeignParent extends Element {

    //Le nom de la méthode est appelé en dur dans la méthode {getForeignParentClass} 
    String getForeignParentId();

    void setForeignParentId(final String id);

    /**
     * Méthode statique permettant de retrouver la classe 'parent' des éléments
     * appartenant à la classe en entrée.
     *
     * La classe 'parent' est obtenue à partir de l'annotation située au dessus
     * de l'implémentation de la méthode 'getForeignParentId()'. Attention, le
     * nom de cette méthode est appelé en dure.
     *
     * @param currentClass : la classe des éléments pour lesquels on recherche
     * la classe parent.
     * @return la Classe 'parent' pour la 'currentClass' donnée.
     * @throws NoSuchMethodException: envoyé lorsque la méthode
     * getForeignParentId n'a pas été trouvée.
     */
    @JsonIgnore
    public static Class getForeignParentClass(Class currentClass) throws ReflectiveOperationException {

        Method readMethod = currentClass.getMethod("getForeignParentId");
        
        if (readMethod != null) {

            final Reference annot = readMethod.getAnnotation(Reference.class);
            if (annot != null) {
                return annot.ref();
            } else {
                throw new ReflectiveOperationException("Unfound Reference annotation on getForeignParent method");
            }
        } else {
            throw new ReflectiveOperationException("Unfound getForeignParent method");
        } 
    }
    
    /**
     * Méthode permettant de retrouver la classe 'parent' d'une instance.
     * 
     * @return
     * @throws ReflectiveOperationException 
     */
    @JsonIgnore
    public default Class getForeignParentClass() throws ReflectiveOperationException {
            return getForeignParentClass(this.getClass());
    }

}
