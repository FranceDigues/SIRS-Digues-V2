/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.sirs.migration.upgrade.v2and23;

import fr.sirs.util.property.Reference;
import java.lang.reflect.Method;
import javafx.collections.ObservableList;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.collection.BackingStoreException;

/**
 * Classe aiming to find the {@linkplain Method getter} for an attribut of the
 * {@linkplain Class clazz} in the {@linkplain Class fromClass} definition.
 *
 * The getter is searched using the {@link Reference} annotation.
 *
 * We didn't ensure, the method to manage getting the getter as the {@link Reference}
 * annotation must be present. An {@link Exception} is thrown if we can't get it.
 *
 * @author Matthieu Bastianelli (Geomatys)
 */
class ClassAndItsGetter {

    final Class fromClass;
    final Class clazz;
    final Method getter;

    final Class expectedRetunedType;
    final Class expectedRetunedGeneric;

    final String expectedTypeName;

    /**
     * By default it expect the getter method to return an
     * {@link ObservableList}{@literal <}{@link String} {@literal >}.
     *
     * If an other returned type is expected, use :
     * {@linkPlain #ClassAndItsGetter(java.lang.Class, java.lang.Class, java.lang.Class, java.lang.Class) ClassAndItsGetter(...)}
     * insteed.
     *
     * @param clazz
     * @param fromClass
     */
    ClassAndItsGetter(final Class clazz, final Class fromClass) {
        this(clazz, fromClass, ObservableList.class, String.class);
    }

    /**
     *
     * @param clazz
     * @param fromClass
     * @param expectedRetunedType expected returned type;
     * @param expectedRetunedGeneric expected returned type's generic ({@literal <} T {@literal >}; null for none;
     */
    ClassAndItsGetter(final Class clazz, final Class fromClass, final Class expectedRetunedType, final Class expectedRetunedGeneric) {
        this.clazz = clazz;
        this.fromClass = fromClass;

        this.expectedRetunedType = expectedRetunedType;
        this.expectedRetunedGeneric = expectedRetunedGeneric;

        this.expectedTypeName = expectedRetunedGeneric == null? expectedRetunedType.getName() : expectedRetunedType.getName()+"<"+expectedRetunedGeneric.getName()+">";

        try {
            this.getter = findGetterOfClazzFromClass();
        } catch (Exception e) {
            throw new BackingStoreException(e);
        }
    }

    private Method findGetterOfClazzFromClass() throws Exception{
        ArgumentChecks.ensureNonNull("linkSide1", fromClass);
        ArgumentChecks.ensureNonNull("linkSideN", clazz);
        for(final Method iMethod : fromClass.getMethods()){
            final Reference ref = iMethod.getAnnotation(Reference.class);
            if(ref!=null && ref.ref()!=null && ref.ref().equals(clazz)) {
                // Les annotations Reference portent sur les accesseurs.
                final Method getter = fromClass.getMethod(iMethod.getName());
                final String tested = getter != null?
                                            getter.getGenericReturnType()!= null?
                                                  getter.getGenericReturnType().getTypeName() != null?
                                                        getter.getGenericReturnType().getTypeName()
                                                  :null
                                            :null
                                      :null;

                if ( (tested != null) && tested.equals(expectedTypeName) ) {
                    return getter;
                }
            }
        }
        throw new Exception("Echec de l'identification de l'accesseur/getter de "+clazz.getCanonicalName()+" depuis la classe : "+fromClass.getCanonicalName());
    }

}
