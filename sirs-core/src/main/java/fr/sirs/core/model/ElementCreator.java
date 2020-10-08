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

import fr.sirs.core.SessionCore;
import fr.sirs.core.SirsCore;
import fr.sirs.core.SirsCoreRuntimeException;
import fr.sirs.util.DesignationIncrementer;
import fr.sirs.util.property.SirsPreferences;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import javafx.concurrent.Task;
import org.geotoolkit.gui.javafx.util.TaskManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author Samuel Andrés (Geomatys)
 */
@Component
public class ElementCreator {

    @Autowired
    private SessionCore ownableSession;

    @Autowired
    private DesignationIncrementer incrementer;

    private ElementCreator() {
    }

    /**
     * Create a new element of type T.
     *
     * If possible, this method sets the correct validity and author dependant
     * on user's privileges of the session.
     *
     * Do not add the element to the database.
     *
     * Try to use autoincrement if this feature is activated.
     *
     * @param <T> Type of the object to create.
     * @param clazz Type of the object to create.
     * @return A new, empty element of queried class.
     * @see ElementCreator#createElement(java.lang.Class, boolean)
     * @see SirsPreferences.PROPERTIES#DESIGNATION_AUTO_INCREMENT
     */
    public <T extends Element> T createElement(final Class<T> clazz){
        return createElement(clazz, true);
    }

    /**
     * Create a new element of type T.
     *
     * If possible, this method sets the correct validity and author dependant
     * on user's privileges of the session.
     *
     * Do not add the element to the database.
     *
     * Try to use autoincrement if this feature is activated.
     *
     * @param <T> Type of the object to create.
     * @param clazz Type of the object to create.
     * @param tryAutoIncrement True to use autoincrement if this feature is activated. False to skip autoincremented designations.
     * @return A new, empty element of queried class.
     */
    public <T extends Element> T createElement(final Class<T> clazz, final boolean tryAutoIncrement){
        try {
            final Constructor<T> constructor = clazz.getConstructor();
            constructor.setAccessible(true);
            final T element = constructor.newInstance();

            element.setValid(ownableSession.createValidDocuments().get());
            final Utilisateur utilisateur = ownableSession.getUtilisateur();
            if (ownableSession.getUtilisateur() != null) {
                element.setAuthor(utilisateur.getId());
            }

            if (tryAutoIncrement) {
                // Determine an auto-incremented value for designation
                tryAutoIncrementDesignation(element);
            }

            return element;

        } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            throw new SirsCoreRuntimeException(ex.getMessage());
        }
    }

    public <T extends Element> void tryAutoIncrementDesignation(final T element){

        tryAutoIncrement(element.getClass()).ifPresent(t -> {
                    TaskManager.INSTANCE.submit(t);
                    try {
                        //TODO : change strategy to release the FX thread. (t.get() is blocking!).
                        final Integer value = t.get();
                        if (value != null) {
                            element.setDesignation(value.toString());
                            //SirsCore.fxRunAndWait(() -> element.setDesignation(value.toString()));
                        }
                    } catch (InterruptedException ex) {
                        SirsCore.LOGGER.log(Level.FINE, "Interruption while auto-incrementing value", ex);
                        Thread.currentThread().interrupt();
                    } catch (ExecutionException ex) {
                        SirsCore.LOGGER.log(Level.WARNING, "Cannot compute auto-increment value", ex);
                    }
                });
    }

    /**
     * Try to compute and affect an auto-incremented designation value for given
     * element.
     *
     * @param target The element to set an increment for.
     * @return A task ready to be startd, which will compute the increment. An
     * empty optional will be returned if user deactivated auto-increment from
     * application preferences.
     */
    public Optional<Task<Integer>> tryAutoIncrement(final Class<? extends Element> target) {
        try {
            final String propertyStr = SirsPreferences.INSTANCE.getProperty(SirsPreferences.PROPERTIES.DESIGNATION_AUTO_INCREMENT);
            if (Boolean.TRUE.equals(Boolean.valueOf(propertyStr))) {
                return Optional.of(incrementer.nextDesignation(target));
            }
        } catch (IllegalStateException e) {
            // If the property is not set, we consider it deactivated.
            SirsCore.LOGGER.log(Level.FINE, "Cannot determine an auto-increment", e);
        }

        return Optional.empty();
    }

    /**
     *
     * @param <T> Type of object to create.
     * @param clazz Type of object to create
     * @return A new element with no user / validation state / designation filled.
     * @deprecated Use of this method does not provide designation auto-increment
     * feature. You'd rather use {@link #createElement(java.lang.Class) }. You can
     * acquire an element creator by autowiring (see {@link Autowired}) it.
     */
    @Deprecated
    public static <T extends Element> T createAnonymValidElement(final Class<T> clazz){
        try{
            final Constructor<T> constructor = clazz.getConstructor();
            constructor.setAccessible(true);
            final T element = constructor.newInstance();
            element.setValid(true);
            return element;
        } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            throw new SirsCoreRuntimeException(ex.getMessage());
        }
    }
}
