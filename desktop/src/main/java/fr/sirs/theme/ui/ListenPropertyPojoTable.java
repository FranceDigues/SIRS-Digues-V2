package fr.sirs.theme.ui;

import fr.sirs.SIRS;
import fr.sirs.core.model.Element;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.WeakHashMap;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.Property;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;

/**
 *
 * This is a special kind of PojoTable that listen one specific property of its
 * items in order to know if they had to be removed from the table view.
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

    public ListenPropertyPojoTable(Class pojoClass, String title) {
        super(pojoClass, title);
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
            Logger.getLogger(ListenPropertyPojoTable.class.getName()).log(Level.SEVERE, null, ex);
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
