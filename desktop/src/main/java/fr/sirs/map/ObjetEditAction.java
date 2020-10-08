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
package fr.sirs.map;

import fr.sirs.FXMainFrame;
import fr.sirs.Injector;
import fr.sirs.Plugins;
import fr.sirs.SIRS;
import fr.sirs.theme.AbstractTheme;
import fr.sirs.theme.Theme;
import java.util.List;
import java.util.logging.Level;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import org.controlsfx.control.action.ActionUtils;
import org.geotoolkit.gui.javafx.render2d.FXMap;
import org.geotoolkit.gui.javafx.render2d.FXMapAction;

/**
 * @author Cédric Briançon (Geomatys)
 * @author Matthieu Bastianelli (Geomatys)
 */
public class ObjetEditAction extends FXMapAction {

    public ObjetEditAction(FXMap map) {
        super(map, "Objet", "Création / Modification d'objet d'un tronçon", SIRS.ICON_EDITION);

        this.disabledProperty().bind(Injector.getSession().geometryEditionProperty().not());

        map.getHandlerProperty().addListener((observable, oldValue, newValue) -> {
            selectedProperty().set(newValue instanceof AbstractOnTronconEditHandler);
        });

    }

    @Override
    public void accept(ActionEvent event) {
        if (map != null && !(map.getHandler() instanceof AbstractOnTronconEditHandler)) {
            throw new RuntimeException("PASSE HERE  BY MISTAKE !"); //TODO : remove after tests
            // Choix du type d'objet à éditer à éditer.
//            map.setHandler(new AbstractOnTronconEditHandler(map));
//            map.setHandler(new AbstractOnTronconEditHandler());
        }
    }

    MenuButton createMenuButton() {
        final MenuButton uiThemesLocalized = this.createMenuButton(ActionUtils.ActionTextBehavior.HIDE);
        // Load themes
        final Theme[] themes = Plugins.getThemes();
        for (final Theme theme : themes) {
            if (Theme.Type.LOCALIZED.equals(theme.getType())) {
                uiThemesLocalized.getItems().add(toMenuItem(theme));
            }
        }
        return uiThemesLocalized;
    }


    /**
     * Créé un item de menu et son arborescence pour le thème choisi.
     *
     * ATTENTION Code dupplication avec {@link FXMainFrame#toMenuItem(fr.sirs.theme.Theme)}
     *
     * => Todo implements a ThemeEventHandlerCreator prenant en entrée un input.
     *
     * @param theme Le thème à afficher dans un menu.
     * @return Un élément de meu dont le rôle est d'ouvrir le panneau associé au
     * thème lors d'un clic.
     */
    private MenuItem toMenuItem(final Theme theme) {
        final List<Theme> subs = theme.getSubThemes();
        final MenuItem item;
        // Atomic case
        if (subs.isEmpty()) {
            item = new MenuItem(theme.getName());
            item.setOnAction(new EditThemeObject(theme));
        // container case
        } else {
            item = new Menu(theme.getName());
//            //action avec tous les sous-panneaux
//            final MenuItem all = new MenuItem("Ouvrir l'ensemble");
//            all.setGraphic(new ImageView(ICON_ALL));
//            all.setOnAction(new EditThemeObject(theme));
//            ((Menu) item).getItems().add(all);

            for (final Theme sub : subs) {
//                System.out.println(sub.getName());  // Debbug
                ((Menu) item).getItems().add(toMenuItem(sub));
            }
        }

        return item;
    }


    private class EditThemeObject implements EventHandler<ActionEvent> {

        private final Theme theme;

        public EditThemeObject(final Theme theme) {
            this.theme = theme;
        }

        @Override
        public void handle(ActionEvent event) {

            if (map != null) {
                if (theme instanceof AbstractTheme) {
                    final List<Class> themeClasses = ((AbstractTheme) theme).getDataClasses();
                    if (themeClasses.size() != 1) {
                        throw new IllegalStateException("Current theme Classes must have exactly 1 associated Class : "+theme.getName());
                    }

                    // Choix du type d'objet à éditer.
                    SIRS.LOGGER.log(Level.INFO, "Ouverture de L''\u00e9dition pour la classe :{0}", themeClasses.toString());
                    map.setHandler(new ObjetOnTronconEditHandler(map, themeClasses.get(0)));
                }

            }
        }

    }
}
