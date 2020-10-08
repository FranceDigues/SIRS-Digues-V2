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
package fr.sirs.theme.ui.pojotable;

import fr.sirs.core.component.AbstractSIRSRepository;
import fr.sirs.core.model.Element;
import java.util.function.Consumer;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;

/**
 *
 * @author Samuel Andrés (Geomatys) [extraction de la PojoTable]
 */
public class Deletor implements Consumer<Element> {

    // indique si l'élément a été "créé" ; si oui, il faut le détruire, sinon c'est une simple référence à supprimer.
    private final BooleanProperty createNewProperty;
    
    private final ObjectProperty<Element> parentElementProperty;
    
    // élément référençant l'élément à supprimer
    private final ObjectProperty<Element> ownerElementProperty;
    
    // repository
    private final AbstractSIRSRepository repo;

    public Deletor(BooleanProperty createNewProperty, ObjectProperty<Element> parentElementProperty, ObjectProperty<Element> ownerElementProperty, AbstractSIRSRepository repo) {
        this.createNewProperty = createNewProperty;
        this.parentElementProperty = parentElementProperty;
        this.repo = repo;
        this.ownerElementProperty = ownerElementProperty;
    }
    

    @Override
    public void accept(Element pojo) {
        if (repo != null && createNewProperty.get()) {
            repo.remove(pojo);
        }

        if (parentElementProperty.get() != null) {
            parentElementProperty.get().removeChild(pojo);
        } else if(ownerElementProperty.get() != null){
            ownerElementProperty.get().removeChild(pojo);
        }
    }
    
}
