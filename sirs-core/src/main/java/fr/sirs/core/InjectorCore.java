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

import fr.sirs.core.component.DocumentChangeEmiter;
import fr.sirs.index.ElasticSearchEngine;
import java.util.logging.Level;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 *
 * @author Alexis Manin (Geomatys)
 * @author Olivier Nouguier (Géomatys)
 * @author Samuel Andrés (Géomatys)
 */
public class InjectorCore implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext ac) throws BeansException {
        SirsCore.LOGGER.log(Level.INFO, "Récupération du contexte d'application");
        applicationContext = ac;
    }

    public static void injectDependencies(Object o) {
        applicationContext.getAutowireCapableBeanFactory().autowireBean(o);
    }


    public static <T> T getBean(Class<T> clazz) {
        return applicationContext == null? null : applicationContext.getBean(clazz);
    }

    public static DocumentChangeEmiter getDocumentChangeEmiter(){
        return getBean(DocumentChangeEmiter.class);
    }

    public static ElasticSearchEngine getElasticSearchEngine(){
        return getBean(ElasticSearchEngine.class);
    }

}
