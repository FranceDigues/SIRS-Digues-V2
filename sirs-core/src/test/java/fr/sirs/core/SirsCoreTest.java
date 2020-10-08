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
package fr.sirs.core;

import fr.sirs.core.model.Positionable;
import java.beans.IntrospectionException;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Matthieu Bastianelli (Geomatys)
 */
public class SirsCoreTest {

    /**
     * Test visant à s'assurer que les noms des propriétées liées aux
     * coordonnées Linéaire des Elements Positionable Correspondent bien au
     * constantes associées de la classe SirsCore.
     *
     * Ce test est essentiel pour le calcul des coordonnées dans la classe lors
     * de l'appel à la méthode : public void setOnPropertyCommit(final
     * TableColumn.CellEditEvent<Element, Object> event)
     * 
     * @throws IntrospectionException 
     */
    @Test
    public void test_Nom_Methodes_Positionable_Valides() throws Exception{
        
        final Set<String> positionableKeys = SirsCore.listSimpleProperties(Positionable.class).keySet();

        assertTrue(positionableKeys.contains(SirsCore.BORNE_DEBUT_ID));
        assertTrue(positionableKeys.contains(SirsCore.BORNE_FIN_ID));

        assertTrue(positionableKeys.contains(SirsCore.BORNE_DEBUT_DISTANCE));
        assertTrue(positionableKeys.contains(SirsCore.BORNE_FIN_DISTANCE));

        assertTrue(positionableKeys.contains(SirsCore.BORNE_DEBUT_AVAL));
        assertTrue(positionableKeys.contains(SirsCore.BORNE_FIN_AVAL));

        assertTrue(positionableKeys.contains(SirsCore.PR_DEBUT_FIELD));
        assertTrue(positionableKeys.contains(SirsCore.PR_FIN_FIELD));

        assertTrue(positionableKeys.contains(SirsCore.POSITION_DEBUT_FIELD));
        assertTrue(positionableKeys.contains(SirsCore.POSITION_FIN_FIELD));

        assertTrue(positionableKeys.contains(SirsCore.PR_DEBUT_FIELD));
        assertTrue(positionableKeys.contains(SirsCore.PR_FIN_FIELD));

    }

}
