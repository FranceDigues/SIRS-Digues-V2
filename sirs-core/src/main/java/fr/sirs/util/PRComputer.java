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
package fr.sirs.util;

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import fr.sirs.core.InjectorCore;
import fr.sirs.core.LinearReferencingUtilities;
import fr.sirs.core.SessionCore;
import fr.sirs.core.SirsCore;
import fr.sirs.core.TronconUtils;
import static fr.sirs.core.TronconUtils.getGardeList;
import static fr.sirs.core.TronconUtils.getObjetList;
import static fr.sirs.core.TronconUtils.getPositionDocumentList;
import static fr.sirs.core.TronconUtils.getProprieteList;
import fr.sirs.core.component.AbstractSIRSRepository;
import fr.sirs.core.model.AbstractPositionDocument;
import fr.sirs.core.model.AvecForeignParent;
import fr.sirs.core.model.BorneDigue;
import fr.sirs.core.model.Desordre;
import fr.sirs.core.model.GardeTroncon;
import fr.sirs.core.model.Objet;
import fr.sirs.core.model.ObjetPhotographiable;
import fr.sirs.core.model.Observation;
import fr.sirs.core.model.Photo;
import fr.sirs.core.model.PositionProfilTravers;
import fr.sirs.core.model.Positionable;
import fr.sirs.core.model.ProprieteTroncon;
import fr.sirs.core.model.SystemeReperage;
import fr.sirs.core.model.TronconDigue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import javafx.concurrent.Task;
import org.apache.sis.util.ArgumentChecks;
import org.geotoolkit.referencing.LinearReferencing;

/**
 * Compute PRs of objects positioned on the given {@link TronconDigue}.
 *
 * TODO : Split computing in multiple bulk, to limit memory usage.
 *
 * @author Alexis Manin (Geomatys)
 */
public class PRComputer extends Task<Boolean> {

    private final TronconDigue troncon;

    private Thread thread;
    private SystemeReperage sr;
    private LineString trLine;
    private LinearReferencing.SegmentInfo[] linear;
    private AbstractSIRSRepository<BorneDigue> borneRepo;

    public PRComputer(final TronconDigue toOperateOn) {
        troncon = toOperateOn;
        updateTitle("Mise à jour des PRs");
    }

    @Override
    protected Boolean call() throws Exception {
        thread = Thread.currentThread();
        try {
            final SessionCore session = InjectorCore.getBean(SessionCore.class);
            String srid = troncon.getSystemeRepDefautId();
            if (srid == null) {
                cancel();
                return false;
            }
            sr = session.getRepositoryForClass(SystemeReperage.class).get(srid);

            borneRepo = session.getRepositoryForClass(BorneDigue.class);

            // For optimisation purpose, we compute linear geometry before iteration.
            updateMessage("Calcul des paramètres de projection");
            trLine = LinearReferencing.asLineString(troncon.getGeometry());
            ArgumentChecks.ensureNonNull("Linéaire de réference", trLine);
            linear = LinearReferencing.buildSegments(trLine);

            final Map<Class<? extends AvecForeignParent>, List> listes = new HashMap<>();
            final Consumer<AvecForeignParent> listFeeder = (AvecForeignParent current) -> {
                if (listes.get(current.getClass()) == null)
                    listes.put(current.getClass(), new ArrayList());
                listes.get(current.getClass()).add(current);
            };

            ArgumentChecks.ensureNonNull("SR par défaut", troncon.getSystemeRepDefautId());

            final List<Objet> objets = getObjetList(troncon);
            final List<AbstractPositionDocument> positionsDoc = getPositionDocumentList(troncon);
            final List<ProprieteTroncon> proprietes = getProprieteList(troncon);
            final List<GardeTroncon> gardes = getGardeList(troncon);

            final int progressMax = objets.size() + positionsDoc.size()
                    + proprietes.size() + gardes.size();
            int currentProgress = 0;

            updateMessage("Parcours des objets");
            updateProgress(currentProgress, progressMax);

            for (final Objet current : objets) {
                recomputePositionable(current); //Calcule et 'setting' Prs dans nouveau sr + geometry (pas de set SR ni calcul coord lin.)
                if (current instanceof ObjetPhotographiable) {
                    for (final Photo photo : ((ObjetPhotographiable) current).getPhotos()) {
                        recomputePositionable(photo);
                    }
                } // Les désordres ne contiennent pas directement les photos : ce sont les observations qui les contiennent.
                else if (current instanceof Desordre) {
                    for (final Observation observation : ((Desordre) current).getObservations()) {
                        for (final Photo photo : observation.getPhotos()) {
                            recomputePositionable(photo);
                        }
                    }
                }
                listFeeder.accept(current);
                updateProgress(currentProgress++, progressMax);
            }
            for (final AbstractPositionDocument current : positionsDoc) {
                recomputePositionable(current);
                if (current instanceof PositionProfilTravers) {
                    for (final Photo photo : ((PositionProfilTravers) current).getPhotos()) {
                        recomputePositionable(photo);
                    }
                }
                listFeeder.accept(current);
                updateProgress(currentProgress++, progressMax);
            }
            for (final ProprieteTroncon current : proprietes) {
                recomputePositionable(current);
                listFeeder.accept(current);
                updateProgress(currentProgress++, progressMax);
            }
            for (final GardeTroncon current : gardes) {
                recomputePositionable(current);
                listFeeder.accept(current);
                updateProgress(currentProgress++, progressMax);
            }

            updateMessage("Mise à jour de la base de données");
            for (final Class c : listes.keySet()) {
                InjectorCore.getBean(SessionCore.class).getRepositoryForClass(c).executeBulk(listes.get(c));
            }

            return true;
        } catch (InterruptedException e) {
            SirsCore.LOGGER.log(Level.WARNING, "Interruption de la mise à jour des PRS", e);
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void recomputePositionable(final Positionable current) throws InterruptedException {
        if (thread.isInterrupted()) {
            throw new InterruptedException();
        }
        try {
            final TronconUtils.PosInfo position = new TronconUtils.PosInfo(current, troncon, linear);

            final Point startPoint = position.getGeoPointStart();
            current.setPrDebut(TronconUtils.computePR(linear, sr, startPoint, borneRepo));

            final Point endPoint = position.getGeoPointEnd();
            if (endPoint == null || startPoint.equals(endPoint)) {
                current.setPrFin(current.getPrDebut());
            } else {
                current.setPrFin(TronconUtils.computePR(linear, sr, endPoint, borneRepo));
            }

            // Once we've updated PRs, we refresh object geometry.
            current.setGeometry(LinearReferencingUtilities.buildGeometryFromGeo(trLine, linear, startPoint, endPoint));

        } catch (RuntimeException ex) {
            SirsCore.LOGGER.log(Level.FINE, ex.getMessage());
        }
    }
}
