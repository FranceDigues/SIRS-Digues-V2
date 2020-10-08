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
package fr.sirs.plugin.reglementaire.ui;

import fr.sirs.Injector;
import fr.sirs.SIRS;
import fr.sirs.core.component.EtapeObligationReglementaireRepository;
import fr.sirs.core.component.ObligationReglementaireRepository;
import fr.sirs.core.component.RefEcheanceRappelObligationReglementaireRepository;
import fr.sirs.core.component.RefEtapeObligationReglementaireRepository;
import fr.sirs.core.component.RefTypeObligationReglementaireRepository;
import fr.sirs.core.model.EtapeObligationReglementaire;
import fr.sirs.core.model.ObligationReglementaire;
import fr.sirs.core.model.Preview;
import fr.sirs.core.model.RefEcheanceRappelObligationReglementaire;
import fr.sirs.core.model.RefEtapeObligationReglementaire;
import fr.sirs.core.model.RefTypeObligationReglementaire;
import fr.sirs.plugin.reglementaire.DocumentsTheme;
import fr.sirs.ui.calendar.CalendarEvent;
import fr.sirs.ui.calendar.CalendarView;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import org.geotoolkit.font.FontAwesomeIcons;
import org.geotoolkit.font.IconBuilder;

import java.time.LocalDate;
import java.util.logging.Level;

/**
 * Vue calendrier présentant les évènements construits à partir des obligations réglementaires.
 *
 * @author Cédric Briançon (Geomatys)
 */
public final class ObligationsCalendarView extends CalendarView {
    private static final Image ICON_DOC = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_FILE, 16,
            FontAwesomeIcons.DEFAULT_COLOR), null);
    private static final Image ICON_ALERT = SwingFXUtils.toFXImage(IconBuilder.createImage(FontAwesomeIcons.ICON_BELL, 16,
            FontAwesomeIcons.DEFAULT_COLOR), null);
    private static final Image ICON_WORK = new Image(DocumentsTheme.class.getResourceAsStream("images/roadworks.png"));

    /**
     * Propriété pointant sur la liste des étape d'obligations réglementaires filtrées pour le calendrier.
     */
    private final ObservableList<EtapeObligationReglementaire> etapes;

    /**
     * En cas de changements sur une propriété d'un objet, mets à jour la vue du calendrier.
     */
    private final ChangeListener propChangeListener = (observable, oldValue, newValue) -> update();

    /**
     * Ecouteur sur les changements de la liste des étapes d'obligations. En cas d'ajout ou de retrait dans cette liste,
     * ajoute ou retire des écouteurs sur les futures changements de ces obligations, de manière à pouvoir mettre
     * à jour les vues les présentant.
     */
    private final ListChangeListener<EtapeObligationReglementaire> listChangeListener = c -> {
        update();
        while(c.next()) {
            for (final EtapeObligationReglementaire obl : c.getRemoved()) {
                removePropertyListener(obl);
            }

            for (final EtapeObligationReglementaire obl : c.getAddedSubList()) {
                attachPropertyListener(obl);
            }
        }
    };

    /**
     * Vue calendrier pour les obligations réglementaires, permettant d'afficher les évènements.
     *
     * @param etapes propriété pointant sur la liste des obligations réglementaires filtrées pour le calendrier.
     */
    public ObligationsCalendarView(final ObservableList<EtapeObligationReglementaire> etapes) {
        super();
        this.etapes = etapes;
        etapes.addListener(listChangeListener);
        update();
        for (final EtapeObligationReglementaire etape : etapes) {
            attachPropertyListener(etape);
        }
    }

    /**
     * Attache un écouteur de changements sur l'étape d'obligation reglémentaire.
     *
     * @param etape L'étape d'obligation réglementaire.
     */
    private void attachPropertyListener(final EtapeObligationReglementaire etape) {
        etape.dateEcheanceProperty().addListener(propChangeListener);
        etape.dateRealisationProperty().addListener(propChangeListener);
        etape.echeanceIdProperty().addListener(propChangeListener);
        etape.typeEtapeIdProperty().addListener(propChangeListener);
    }

    /**
     * Retire un écouteur de changements sur l'étape d'obligation reglémentaire.
     *
     * @param etape L'étape d'obligation réglementaire.
     */
    private void removePropertyListener(final EtapeObligationReglementaire etape) {
        etape.dateEcheanceProperty().removeListener(propChangeListener);
        etape.dateRealisationProperty().removeListener(propChangeListener);
        etape.echeanceIdProperty().removeListener(propChangeListener);
        etape.typeEtapeIdProperty().removeListener(propChangeListener);
    }

    /**
     * Met à jour les évènements sur le calendrier.
     */
    private void update() {
        getCalendarEvents().clear();

        if (etapes != null && !etapes.isEmpty()) {
            final ObligationReglementaireRepository orr = Injector.getBean(ObligationReglementaireRepository.class);
            final RefEcheanceRappelObligationReglementaireRepository rerorr = Injector.getBean(RefEcheanceRappelObligationReglementaireRepository.class);
            final RefEtapeObligationReglementaireRepository reorr = Injector.getBean(RefEtapeObligationReglementaireRepository.class);
            final RefTypeObligationReglementaireRepository rtorr = Injector.getBean(RefTypeObligationReglementaireRepository.class);
            EtapeObligationReglementaireRepository erRepo = null;

            ObligationReglementaire obligation = null;
            for (final EtapeObligationReglementaire etape : etapes) {
                if ((etape == null)||(etape.getObligationReglementaireId() == null) ) {
                    continue;
                }
                try {
                    obligation = orr.get(etape.getObligationReglementaireId());
                } catch (Exception e) {
                    SIRS.LOGGER.log(Level.WARNING, "Echec lors du chargement de l''obligation r\u00e9glementaire associ\u00e9e \u00e0 l''\u00e9tape : {0}\n{1}", new Object[]{etape.getDesignation(), e.getMessage()});
                    etape.setValid(false);
                    etape.setObligationReglementaireId(null);
                    if (erRepo == null) {
                        erRepo = Injector.getBean(EtapeObligationReglementaireRepository.class);
                    }
                    erRepo.update(etape);
                    continue;
                }
                if (obligation == null) {
                    continue;
                }
                final LocalDate eventDate = etape.getDateRealisation() != null ? etape.getDateRealisation() :
                        etape.getDateEcheance();
                if (eventDate != null) {
                    final StringBuilder sb = new StringBuilder();
                    Image image = ICON_DOC;
                    // Type d'obligation
                    if (obligation.getTypeId() != null) {
                        final RefTypeObligationReglementaire oblType = rtorr.get(obligation.getTypeId());
                        if (oblType != null) {
                            final String oblTypeAbreg = oblType.getAbrege();
                            sb.append(oblTypeAbreg);
                            if ("TRA".equalsIgnoreCase(oblTypeAbreg)) {
                                image = ICON_WORK;
                            }
                        }
                    }
                    // Nom du SE
                    if (obligation.getSystemeEndiguementId() != null) {
                        final Preview previewSE = Injector.getSession().getPreviews().get(obligation.getSystemeEndiguementId());
                        if (previewSE != null) {
                            if (!sb.toString().isEmpty()) {
                                sb.append(" - ");
                            }
                            sb.append(previewSE.getLibelle());
                        }
                    }
                    // Année de l'obligation
                    if (obligation.getAnnee() != 0) {
                        if (!sb.toString().isEmpty()) {
                            sb.append(" - ");
                        }
                        sb.append(obligation.getAnnee());
                    }
                    // Etape de l'obligation
                    if (etape.getTypeEtapeId() != null) {
                        final RefEtapeObligationReglementaire oblEtape = reorr.get(etape.getTypeEtapeId());
                        if (oblEtape != null) {
                            if (!sb.toString().isEmpty()) {
                                sb.append(" - ");
                            }
                            sb.append(oblEtape.getLibelle());
                        }
                    }

                    // Création de l'évènement sur le calendrier pour cette obligation
                    getCalendarEvents().add(new CalendarEvent(etape, eventDate, sb.toString(), image));

                    // Si l'obligation a une date de rappel d'échéance de configurée, on ajoute une alerte à cette date
                    if (etape.getEcheanceId() != null) {
                        LocalDate firstDateRappel = LocalDate.from(eventDate);
                        final RefEcheanceRappelObligationReglementaire period = rerorr.get(etape.getEcheanceId());
                        firstDateRappel = firstDateRappel.minusMonths(period.getNbMois());
                        sb.append(" - ").append(period.getLibelle());
                        getCalendarEvents().add(new CalendarEvent(etape, true, firstDateRappel, sb.toString(), ICON_ALERT));
                    }
                }
            }
        }
    }

    /**
     * Affiche une fenêtre présentant les choix possibles pour cet évènement sur le calendrier.
     *
     * @param calendarEvent Evènement du calendrier concerné.
     * @param parent Noeud parent sur lequel la fenêtre sera accrochée.
     */
    @Override
    public void showCalendarPopupForEvent(final CalendarEvent calendarEvent, final Node parent) {
        final ObligationsCalendarEventStage stage = new ObligationsCalendarEventStage(calendarEvent, etapes);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setIconified(false);
        stage.setMaximized(false);
        stage.setResizable(false);
        stage.getIcons().add(SIRS.ICON);
        stage.initOwner(Injector.getSession().getFrame().getScene().getWindow());
        final Point2D popupPos = parent.localToScreen(20, 40);
        if (popupPos != null) {
            stage.sizeToScene();
            stage.setX(popupPos.getX());
            stage.setY(popupPos.getY());
        }
        stage.show();
    }
}
