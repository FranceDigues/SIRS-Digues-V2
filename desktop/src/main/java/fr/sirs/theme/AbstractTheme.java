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
package fr.sirs.theme;

import fr.sirs.Injector;
import fr.sirs.SIRS;
import fr.sirs.core.component.AbstractPositionableRepository;
import fr.sirs.core.model.Positionable;
import javafx.collections.ObservableList;
import javafx.scene.Parent;

import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.function.Function;

import static fr.sirs.SIRS.BUNDLE_KEY_CLASS;
import java.util.ArrayList;

/**
 * @author Cédric Briançon (Geomatys)
 * @author Johann Sorel (Geomatys)
 */
public abstract class AbstractTheme extends Theme {

    /**
     * Handle theme
     * @param <T>
     */
    public static class ThemeManager<T> {
        private final String name;
        private final String tableTitle;
        private final Class<T> dataClass;
        private final Function<String, ObservableList<T>> extractor;
        private final Consumer<T> deletor;

        public ThemeManager(String name, Class<T> dataClass, Function<String, ObservableList<T>> extractor, Consumer<T> deletor) {
            this(name,null,dataClass,extractor,deletor);
        }

        public ThemeManager(String name, String tableTitle, Class<T> dataClass, Function<String, ObservableList<T>> extractor, Consumer<T> deletor) {
            this.name = name;
            this.tableTitle = tableTitle;
            this.dataClass = dataClass;
            this.extractor = extractor;
            this.deletor = deletor;
        }

        public String getName() {
            return name;
        }

        public String getTableTitle() {
            return tableTitle;
        }

        public Class<T> getDataClass() {
            return dataClass;
        }

        public Function<String, ObservableList<T>> getExtractor() {
            return extractor;
        }

        public Consumer getDeletor() {
            return deletor;
        }

    }

    protected ThemeManager[] managers;

    /**
     *
     * @return Classes associated with the current {@link AbstractTheme}
     *         Never null; Can be empty.
     */
    public List<Class> getDataClasses() {
        final List<Class> dataClasses = new ArrayList<>();
        if (managers != null) {
            for (ThemeManager manager : managers) {
                dataClasses.add(manager.dataClass);
            }
        }
        return dataClasses;
    }

    public AbstractTheme(String name, Class... classes) {
        super(name, Type.LOCALIZED);
        final ThemeManager[] mngrs = new ThemeManager[classes.length];
        for(int i = 0; i<classes.length; i++){
            mngrs[i] = generateThemeManager(classes[i]);
        }
        this.managers = mngrs;
        initThemeManager(mngrs);
    }

    protected AbstractTheme(String name, ThemeManager... managers) {
        super(name, Type.LOCALIZED);
        this.managers = managers;
        initThemeManager(managers);
    }

    protected void setManagers(final ThemeManager[] managers) {
        this.managers = managers;
    }


    public static <T extends Positionable> ThemeManager<T> generateThemeManager(final Class<T> themeClass){
        return generateThemeManager(null, themeClass);
    }

    public static <T extends Positionable> ThemeManager<T> generateThemeManager(String tableTitle, final Class<T> themeClass){
        return generateThemeManager(null, tableTitle, themeClass);
    }

    public static <T extends Positionable> ThemeManager<T> generateThemeManager(String tabName, String tableTitle, final Class<T> themeClass){
        final ResourceBundle bundle = ResourceBundle.getBundle(themeClass.getCanonicalName(), Locale.getDefault(),
                Thread.currentThread().getContextClassLoader());
        final Function<String, ObservableList<T>> extractor = (String linearId) -> {
            final List<T> result = ((AbstractPositionableRepository<T>) Injector.getSession().getRepositoryForClass(themeClass)).getByLinearId(linearId);
            return SIRS.observableList(result);
        };
        final Consumer<T> deletor = (T themeElement) -> Injector.getSession().getRepositoryForClass(themeClass).remove(themeElement);

        if (tableTitle == null) {
            tableTitle = "Thème "+bundle.getString(BUNDLE_KEY_CLASS);
        }

        if(tabName==null){
            tabName = bundle.getString(BUNDLE_KEY_CLASS);
        }

        return new ThemeManager<>(tabName,
                tableTitle,
                themeClass, extractor, deletor);
    }

    @Override
    public abstract Parent createPane();

    protected abstract void initThemeManager(final ThemeManager... managers);
}
