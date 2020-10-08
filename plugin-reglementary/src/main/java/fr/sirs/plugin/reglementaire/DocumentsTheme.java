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
package fr.sirs.plugin.reglementaire;

import fr.sirs.SIRS;
import fr.sirs.core.component.DocumentChangeEmiter;
import fr.sirs.core.component.DocumentListener;
import fr.sirs.core.component.EtapeObligationReglementaireRepository;
import fr.sirs.core.component.ObligationReglementaireRepository;
import fr.sirs.core.model.Element;
import fr.sirs.core.model.EtapeObligationReglementaire;
import fr.sirs.core.model.ObligationReglementaire;
import fr.sirs.plugin.reglementaire.ui.EtapesPojoTable;
import fr.sirs.plugin.reglementaire.ui.ObligationsCalendarView;
import fr.sirs.plugin.reglementaire.ui.ObligationsPojoTable;
import fr.sirs.ui.calendar.CalendarView;
import fr.sirs.Injector;
import fr.sirs.theme.ui.AbstractPluginsButtonTheme;
import fr.sirs.util.SimpleFXEditMode;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Parent;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javafx.beans.property.ObjectProperty;
import javafx.geometry.Insets;

/**
 * Panneau regroupant les fonctionnalités de suivi de documents.
 *
 * @author Cédric Briançon (Geomatys)
 */
public final class DocumentsTheme extends AbstractPluginsButtonTheme {
    private static final Image BUTTON_IMAGE = new Image(
            DocumentsTheme.class.getResourceAsStream("images/suivi_doc.png"));

    public DocumentsTheme() {
        super("Suivi des documents", "Suivi des documents", BUTTON_IMAGE);
    }

    /**
     * Panneau déployé au clic sur le bouton du menu correspondant à ce thème.
     */
    @Override
    public Parent createPane() {
        final BorderPane borderPane = new BorderPane();
        final TabPane tabPane = new TabPane();

        // Onglet liste des obligations réglementaires
        final Tab obligationsTab = new Tab("Obligations réglementaires");
        obligationsTab.setClosable(false);
        // Gestion du bouton consultation / édition pour la pojo table
        final Separator separatorOR = new Separator();
        separatorOR.setVisible(false);
        final SimpleFXEditMode editModeOR = new SimpleFXEditMode();
        final HBox topORPane = new HBox(separatorOR, editModeOR);
        HBox.setHgrow(separatorOR, Priority.ALWAYS);
        final ObligationReglementaireRepository orrRepo = Injector.getBean(ObligationReglementaireRepository.class);
        final ObservableList<ObligationReglementaire> allObls = FXCollections.observableList(orrRepo.getAll());
        // Ajoute un listener sur tous les ajouts/suppression d'étapes d'obligations pour mettre à jour la liste et donc la table.
        final ListDocumentListener<ObligationReglementaire> oblListener =
                new ListDocumentListener<>(ObligationReglementaire.class, allObls);
        Injector.getBean(DocumentChangeEmiter.class).addListener(oblListener);
        final ObligationsPojoTable obligationsPojoTable = new ObligationsPojoTable((ObjectProperty<? extends Element>) null);
        obligationsPojoTable.setTableItems(() -> (ObservableList) allObls);
        obligationsPojoTable.editableProperty().bind(editModeOR.editionState());
        obligationsTab.setContent(new BorderPane(obligationsPojoTable, topORPane, null, null, null));

        // Onglet liste des étapes obligations réglementaires
        final Tab etapesTab = new Tab("Etapes d'obligations réglementaires");
        etapesTab.setClosable(false);
        // Gestion du bouton consultation / édition pour la pojo table
        final Separator separatorEtape = new Separator();
        separatorEtape.setVisible(false);
        final SimpleFXEditMode editEtapeMode = new SimpleFXEditMode();
        final HBox topEtapePane = new HBox(separatorEtape, editEtapeMode);
        HBox.setHgrow(separatorEtape, Priority.ALWAYS);
        final EtapeObligationReglementaireRepository eorrRepo = Injector.getBean(EtapeObligationReglementaireRepository.class);
        final ObservableList<EtapeObligationReglementaire> allEtapes = FXCollections.observableList(eorrRepo.getAll());
        // Ajoute un listener sur tous les ajouts/suppression d'étapes d'obligations pour mettre à jour la liste et donc la table.
        final ListDocumentListener<EtapeObligationReglementaire> etapeListener =
                new ListDocumentListener<>(EtapeObligationReglementaire.class, allEtapes);
        Injector.getBean(DocumentChangeEmiter.class).addListener(etapeListener);
        final EtapesPojoTable etapesPojoTable = new EtapesPojoTable(tabPane, (ObjectProperty<? extends Element>) null);
        etapesPojoTable.setTableItems(() -> (ObservableList) allEtapes);
        etapesPojoTable.editableProperty().bind(editEtapeMode.editionState());
        etapesTab.setContent(new BorderPane(etapesPojoTable, topEtapePane, null, null, null));

        // Onglet calendrier des obligations reglémentaires
        final Tab calendarTab = new Tab("Calendrier");
        calendarTab.setClosable(false);
        final CalendarView calendarView = new ObligationsCalendarView(allEtapes);
        calendarView.getStylesheets().add(SIRS.CSS_PATH_CALENDAR);
        calendarView.setShowTodayButton(false);
        calendarView.getCalendar().setTime(new Date());
        calendarView.setPadding(new Insets(20));
        calendarView.setFillWidth(true);
        calendarView.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        final BorderPane bp = new BorderPane(calendarView);
        bp.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        calendarTab.setContent(bp);

        // Ajout des onglets
        tabPane.getTabs().add(obligationsTab);
        tabPane.getTabs().add(etapesTab);
        tabPane.getTabs().add(calendarTab);
        borderPane.setCenter(tabPane);
        return borderPane;
    }

    /**
     * Ecouteur d'ajouts et suppressions d'étapes d'obligations réglementaires sur la base, pour mettre à jour les vues
     * montrant ces objets.
     */
    private class ListDocumentListener<T extends Element> implements DocumentListener {
        private final ObservableList<T> list;
        private final Class<T> clazz;

        public ListDocumentListener(final Class<T> clazz, final ObservableList<T> list) {
            this.list = list;
            this.clazz = clazz;
        }

        /**
         * A la création de documents, mise à jour de la liste en conséquence.
         *
         * @param added Nouveaux éléments à ajouter.
         */
        @Override
        public void documentCreated(Map<Class, List<Element>> added) {
            final List addedObl = added.get(clazz);
            if (addedObl == null || addedObl.isEmpty()) {
                return;
            }
            // On enlève les éléments déjà présents dans la liste de base, pour ne garder que les nouveaux
            // et ne pas les ajouter plusieurs fois dans la liste.
            addedObl.removeAll(list);
            final Runnable addRun = () -> list.addAll(addedObl);
            if (!Platform.isFxApplicationThread()) {
                Platform.runLater(addRun);
            } else {
                addRun.run();
            }
        }

        @Override
        public void documentChanged(Map<Class, List<Element>> changed) {
        }

        /**
         * Suppression des objets dans la liste.
         *
         * @param deletedObject Liste d'éléments à supprimer de la liste.
         */
        @Override
        public void documentDeleted(Set<String> deleted) {
            if (deleted == null || deleted.isEmpty()) {
                return;
            }
            final Runnable delRun = () -> list.removeIf(e -> deleted.contains(e.getId()));
            if (!Platform.isFxApplicationThread()) {
                Platform.runLater(delRun);
            } else {
                delRun.run();
            }
        }
    }
}
