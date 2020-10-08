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
import fr.sirs.core.model.Element;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.WeakHashMap;
import java.util.function.Supplier;
import java.util.logging.Level;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;

/**
 *
 * This is a special kind of PojoTable which listens one specific property of its
 * items in order to know if they must be removed from the table view.
 *
 * This table is useful for links without opposite in order to detect an object
 * that were associated to the "virtual container" of the table list is no
 * longer associated, or, may be, associated again.
 *
 * For instance, let us consider two classes A and B, linked by an
 * unidirectional association : A to B.
 *
 * So, A has a list of B ids and can observe it in order to update UIs when the
 * content of the list changes. On the contrary, B has not its own list of A ids.
 * If a new link is added from an instance of A to an instance of B, this last
 * one cannot know the updates of this link because it doesn't handle it.
 *
 * This table provides some mechanisms of listening between entities that are
 * known to have been associated.
 *
 * <ol>
 * <li> It adds a listener to the objects it is initially linked with, or that are
 * added to the table list.</li>
 *
 * <li> It continues to listen the objects that have been removed from the table
 * in order to detect if they are associated again.</li>
 *
 * <li> But it does not listen other objects, and so, it cannot know if they are
 * associated for the first time to the "virtual container".</li>
 * </ol>
 *
 * @author Samuel Andrés (Geomatys)
 *
 * @param <T> The type of the listen property.
 */
public class ListenPropertyPojoTable<T> extends PojoTable {

    private final WeakHashMap<Element, ChangeListener<T>> listeners = new WeakHashMap<>();
    protected Method propertyMethodToListen;
    protected T propertyReference;

    public ListenPropertyPojoTable(Class pojoClass, String title, ObjectProperty<? extends Element> container) {
        super(pojoClass, title, container);
        tableUpdaterProperty.addListener(new ChangeListener<Task>() {
            @Override
            public void changed(ObservableValue<? extends Task> observable, Task oldValue, Task newValue) {
                newValue.setOnSucceeded(event -> {
                    if (propertyMethodToListen != null) {
                        for (Element element : getAllValues()) {
                            addListener(element);
                        }
                    }
                });
            }
        });
    }

    @Override
    public synchronized void setTableItems(Supplier<ObservableList<Element>> producer) {
        clearListeners();
        super.setTableItems(producer);
    }

    public void setPropertyToListen(String propertyToListen, T propertyReference){
        clearListeners();
        try {
            propertyMethodToListen = pojoClass.getMethod(propertyToListen);
        } catch (NoSuchMethodException | SecurityException ex) {
            SIRS.LOGGER.log(Level.WARNING, "No such property "+propertyToListen+" to listen…", ex);
        }
        this.propertyReference = propertyReference;
    }

    /**
     *
     * @return the reference value of the listened property.
     */
    public T getPropertyReference(){return propertyReference;}

    private void addListener(final Element element) {
        try {
            final Property<T> property = (Property<T>) propertyMethodToListen.invoke(element);
            if(listeners.get(element)==null){

                final ChangeListener<T> changeListener = new ChangeListener<T>() {
                    @Override
                    public void changed(ObservableValue<? extends T> observable, T oldValue, T newValue) {
                        if(newValue.equals(propertyReference)){
                            if(!getAllValues().contains(element)){
                                getAllValues().add(element);
                            }
                        }else{
                            if(getAllValues().contains(element)){
                                getAllValues().remove(element);
                            }
                        }
                    }
                };
                property.addListener(changeListener);
                listeners.put(element, changeListener);
            }
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            SIRS.LOGGER.log(Level.WARNING, null, ex);
        }
    }

    private void removeListener(final Element e) {
        try {
            ChangeListener<T> l = listeners.remove(e);
            final Property<T> property = (Property<T>) propertyMethodToListen.invoke(e);
            if (property != null) {
                property.removeListener(l);
            }
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            SIRS.LOGGER.log(Level.WARNING, null, ex);
        }
    }

    private void clearListeners() {
        if (propertyMethodToListen != null) {
            for (final Element e : listeners.keySet()) {
                removeListener(e);
            }
        }
    }
}
