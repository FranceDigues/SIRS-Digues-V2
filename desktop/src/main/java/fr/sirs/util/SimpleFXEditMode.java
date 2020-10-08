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
package fr.sirs.util;

import fr.sirs.FXEditMode;
import fr.sirs.Injector;
import fr.sirs.core.model.Role;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.BooleanBinding;

/**
 * Composant de controle de l'édition des panneaux de thèmes ?
 *
 * @author Alexis Manin (Geomatys)
 */
public class SimpleFXEditMode extends FXEditMode {

    public SimpleFXEditMode() {
        uiValidationBox.setVisible(false);
        uiValidationBox.setManaged(false);
        uiSave.setVisible(false);
        uiSave.setManaged(false);
    }

    /**
     * Dans le cas général des thèmes de tronçon, on interdit l'édition du panneau seulement si l'utilisateur est un invité
     * (afin que les utilisateurs "externes" puissent ajouter des objets sur les tronçons).
     * (Voir la demande de correctif SYM-1585.)
     *
     * @return Le binding de contrôle du bouton d'édition.
     */
    @Override
    protected BooleanBinding initEditionProhibition(){
        return Injector.getSession().roleBinding().isEqualTo(Role.GUEST);
    }
    
    /**
     * Dans le cas des panneaux de thèmes, on bloque l'édition dès que la
     * propriété d'interdiction d'édition s'invalide.
     * 
     * @return 
     */
    @Override
    protected InvalidationListener editionListener(){
        return (Observable observable) -> {
                setToConsult();
            };
    }
}
