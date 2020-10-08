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
package fr.sirs.core.component;

import fr.sirs.core.LinearReferencingUtilities;
import fr.sirs.core.SessionCore;
import fr.sirs.core.SirsCore;
import fr.sirs.core.SirsCoreRuntimeException;
import fr.sirs.core.TronconUtils;
import fr.sirs.core.model.Positionable;
import fr.sirs.core.model.SystemeReperage;
import fr.sirs.core.model.SystemeReperageBorne;
import fr.sirs.core.model.TronconDigue;
import fr.sirs.util.StreamingIterable;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.apache.sis.util.ArgumentChecks;
import org.ektorp.CouchDbConnector;
import org.ektorp.DocumentNotFoundException;
import org.ektorp.DocumentOperationResult;
import org.ektorp.Options;
import org.geotoolkit.referencing.LinearReferencing;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("fr.sirs.core.component.SystemeReperageRepository")
public class SystemeReperageRepository extends AbstractSIRSRepository<SystemeReperage> {

    @Autowired SessionCore session;

    @Autowired
    private SystemeReperageRepository ( CouchDbConnector db) {
       super(SystemeReperage.class, db);
       initStandardDesignDocument();
   }

    @Override
    public Class<SystemeReperage> getModelClass() {
        return SystemeReperage.class;
    }

    @Override
    public SystemeReperage create() {
        if (session!=null) {
            return session.getElementCreator().createElement(SystemeReperage.class);
        } else {
            throw new SirsCoreRuntimeException("Pas de session courante");
        }
    }

    public List<SystemeReperage> getByLinear(final TronconDigue linear) {
        ArgumentChecks.ensureNonNull("Linear", linear);
        return this.getByLinearId(linear.getId());
    }

    public List<SystemeReperage> getByLinearId(final String linearId) {
        ArgumentChecks.ensureNonNull("Linear", linearId);
        return cacheList(globalRepo.getByLinearId(type, linearId));
    }

    public StreamingIterable<SystemeReperage> getByLinearStreaming(final TronconDigue linear) {
        ArgumentChecks.ensureNonNull("Linear", linear);
        return this.getByLinearIdStreaming(linear.getId());
    }

    public StreamingIterable<SystemeReperage> getByLinearIdStreaming(final String linearId) {
        ArgumentChecks.ensureNonNull("Linear", linearId);
        return new StreamingViewIterable(globalRepo.createByLinearIdQuery(type, linearId));
    }

    public void update(SystemeReperage entity, TronconDigue troncon) {
        ArgumentChecks.ensureNonNull("SR to update", entity);
        ArgumentChecks.ensureNonNull("Troncon bound to updated SR", troncon);
        final SystemeReperage previousVersion = get(entity.getId(), new Options().revision(entity.getRevision()));
        super.update(entity);
        constraintBorneInTronconListBorne(entity, troncon, false);
        updatePositionsForSR(troncon, entity, entity);
    }

    public void add(SystemeReperage entity, TronconDigue troncon) {
        add(entity, troncon, false);
    }

    public void add(SystemeReperage entity, TronconDigue troncon, final boolean forceDefaultSR) {
        ArgumentChecks.ensureNonNull("SR to add", entity);
        ArgumentChecks.ensureNonNull("Troncon bound to added SR", troncon);
        if (entity.getLinearId() == null && troncon != null) {
            entity.setLinearId(troncon.getId());
        }
        super.add(entity);
        constraintBorneInTronconListBorne(entity,troncon, forceDefaultSR);
    }

    @Override
    public void update(SystemeReperage entity) {
        final TronconDigue srConstaint;
        try {
            final Object tmp = session.getElement(entity.getLinearId()).orElse(null);
            if (tmp instanceof TronconDigue) {
                srConstaint = (TronconDigue) tmp;
            } else {
                throw new IllegalStateException("Cannot update SR because we cannot determine its linear.");
            }
        } catch (Exception e) {
            throw new RuntimeException("Cannot update following SR, because we cannot check its constraints :"+ entity, e);
        }
        update(entity, srConstaint);
    }

    @Override
    public void remove(SystemeReperage entity) {
        final TronconDigue srConstaint;
        try {
            final Object tmp = session.getElement(entity.getLinearId()).orElse(null);
            if (tmp instanceof TronconDigue) {
                srConstaint = (TronconDigue) tmp;
            } else {
                throw new IllegalStateException("Cannot remove SR because we cannot determine its linear.");
            }
        } catch (Exception e) {
            throw new RuntimeException("Cannot remove following SR, because we cannot check its constraints :"+ entity, e);
        }
        remove(entity, srConstaint);
    }

    @Override
    public void add(SystemeReperage entity) {
        final TronconDigue srConstaint;
        try {
            final Object tmp = session.getElement(entity.getLinearId()).orElse(null);
            if (tmp instanceof TronconDigue) {
                srConstaint = (TronconDigue) tmp;
            } else {
                throw new IllegalStateException("Cannot add SR because we cannot determine its linear.");
            }
        } catch (Exception e) {
            throw new RuntimeException("Cannot add following SR, because we cannot check its constraints :"+ entity, e);
        }
        add(entity, srConstaint);
    }

    /**
     * Remove this SR from database, and update all items whose linear position
     * is defined in this SR. All updated items will lose their linear information.
     *
     * @param source SR to delete.
     * @param troncon Troncon which contains the SR to remove.
     */
    public void remove(SystemeReperage source, TronconDigue troncon) {
        remove(source, troncon, null);
    }

    /**
     * Remove this SR from database, and update all items whose linear position
     * is defined in this SR. All updated items will lose their linear information
     * if alternative SR is null. Otherwise, their position is computed on it.
     *
     * @param source SR to delete.
     * @param troncon Troncon which contains the SR to remove.
     * @param alternative the new SR to affect to items bound to the old SR.
     */
    public void remove(SystemeReperage source, TronconDigue troncon, SystemeReperage alternative) {
        ArgumentChecks.ensureNonNull("SR to delete", source);
        ArgumentChecks.ensureNonNull("Troncon bound to deleted SR", troncon);
        if (alternative == source)
            alternative = null;
        if (source.getId().equals(troncon.getSystemeRepDefautId())) {
            troncon.setSystemeRepDefautId(alternative == null ? null : alternative.getId());
            AbstractSIRSRepository repo = session.getRepositoryForClass(troncon.getClass());
            repo.update(troncon);
        }

        updatePositionsForSR(troncon, source, alternative);

        // We can finally delete our sr safely.
        super.remove(source);
    }

    @Override
    public List<DocumentOperationResult> executeBulkDelete(Iterable<SystemeReperage> bulkList) {
        throw new UnsupportedOperationException("Forbidden due to SR integrity constraints.");
    }

    @Override
    public List<DocumentOperationResult> executeBulk(SystemeReperage... bulkList) {
        throw new UnsupportedOperationException("Forbidden due to SR integrity constraints.");
    }

    @Override
    public List<DocumentOperationResult> executeBulk(Collection<SystemeReperage> bulkList) {
        throw new UnsupportedOperationException("Forbidden due to SR integrity constraints.");
    }



    /**
     * Cette contrainte s'assure que les bornes du systeme de reperage sont
     * dans la liste des bornes du troncon.
     * Note : Cette méthode doit être appelée APRES la maj du SR en base.
     * @param entity Le système de repérage mis à jour / ajouté.
     * @param troncon Le troncon auquel appartient le SR.
     * @param forceDefaultSR Forcer le SR en paramètre à devenir le SR par défaut du tronçon parent.
     */
    private void constraintBorneInTronconListBorne(SystemeReperage entity, TronconDigue troncon, final boolean forceDefaultSR) {
        final String tcId = entity.getLinearId();
        if(tcId==null) return;
        if(entity.getSystemeReperageBornes().isEmpty()) return;

        if(troncon==null){
            try{
                final Object tmp = session.getElement(entity.getLinearId()).orElse(null);
                if (tmp instanceof TronconDigue) {
                    troncon = (TronconDigue) tmp;
                } else {
                    throw new IllegalStateException("Cannot update SR because we cannot determine its linear.");
                }
            }catch(DocumentNotFoundException ex){
                //le troncon n'existe pas
                return;
            }
        }

        final List<String> borneIds = troncon.getBorneIds();

        boolean needSave = false;
        for(SystemeReperageBorne srb : entity.getSystemeReperageBornes()){
            final String bid = srb.getBorneId();
            if(bid!=null && !borneIds.contains(bid)){
                borneIds.add(bid);
                needSave = true;
            }
        }

        if (troncon.getSystemeRepDefautId() == null || troncon.getSystemeRepDefautId().isEmpty() || forceDefaultSR) {
            troncon.setSystemeRepDefautId(entity.getId());
            needSave = true;
        }

        if (needSave || entity.getId().equals(troncon.getSystemeRepDefautId())) {
            AbstractSIRSRepository repo = session.getRepositoryForClass(troncon.getClass());
            if (repo instanceof AbstractTronconDigueRepository)
                ((AbstractTronconDigueRepository) repo).registerForPRComputing(troncon);
            repo.update(troncon);
        }
    }

    /**
     * Update all {@link Positionable} object from given {@link TronconDigue}
     * whose linear position is defined on first given {@link SystemeReperage}.
     * Second SR will be used to compute new linear position.
     *
     * Note : If second SR is null, linear information will be deleted. Also,
     * if no geographic position exists in such a case, we compute it to avoid
     * loss of position info.
     *
     * @param troncon Troncon on which the two given SRs are defined.
     * @param oldSR The original SR to get rid of. Cannot be null.
     * @param newSR The new SR to use to compute linear referencing. If null,
     * linear information will be kept empty.
     */
    private void updatePositionsForSR(final TronconDigue troncon, final SystemeReperage oldSR, final SystemeReperage newSR) {

        // We must update position of objects whose position was relative to the SR
        LinearReferencing.SegmentInfo[] tronconSegments = LinearReferencingUtilities.buildSegments(LinearReferencingUtilities.asLineString(troncon.getGeometry()));
        List<Positionable> positionableList = TronconUtils.getPositionableList(troncon);
        Iterator<Positionable> iterator = positionableList.iterator();
        final Set<String> newSrBorneIds;
        if (newSR != null)
            newSrBorneIds = newSR.getSystemeReperageBornes().stream()
                    .map(borne -> borne.getId())
                    .collect(Collectors.toSet());
        else
            newSrBorneIds = Collections.EMPTY_SET;

        /*
         * Before computing a new referencing for the current object, we ensure
         * it's needed :
         * - The object linear position uses given SR.
         * - The new SR is different from old SR, or :
         *      - bornes used for positioning are not found in new SR.
         */
        final Predicate<Positionable> updateNeeded = p -> {
            return oldSR.getId().equals(p.getSystemeRepId()) &&
                    (
                        (newSR == null || !p.getSystemeRepId().equals(newSR.getId())) ||
                        (
                            p.getBorneDebutId() != null && !newSrBorneIds.contains(p.getBorneDebutId()) ||
                            p.getBorneFinId() != null && !newSrBorneIds.contains(p.getBorneFinId())
                        )
                    );
        };
        while (iterator.hasNext()) {
            final Positionable p = iterator.next();
            
            if(p.getGeometry()==null){
                SirsCore.LOGGER.log(Level.WARNING, "The {0} {1} has no geometry\u2026 Position update is going to be skipped", new Object[]{p.getClass().getSimpleName(), p.getDesignation()});
            }
            
            if (p.getGeometry()!=null && updateNeeded.test(p)) {
                if (newSR != null && !newSrBorneIds.isEmpty()) {
                    // We must update linear information.
                    final TronconUtils.PosInfo pInfo = new TronconUtils.PosInfo(p, troncon, tronconSegments);
                    TronconUtils.PosSR forSR = pInfo.getForSR(newSR);
                    p.setSystemeRepId(newSR.getId());
                    p.setBorneDebutId(forSR.borneStartId);
                    p.setBorneFinId(forSR.borneEndId);
                    p.setBorne_debut_distance(forSR.distanceStartBorne);
                    p.setBorne_fin_distance(forSR.distanceEndBorne);
                    p.setBorne_debut_aval(forSR.startAval);
                    p.setBorne_fin_aval(forSR.endAval);
                } else {
                    /*
                     * We must remove linear information. First, if no geographic
                     * position is defined. We compute it now, to avoid losing
                     * position information. Once done, we reset linear referencement
                     * attributes.
                     */
                    if (p.getPositionDebut() == null && p.getPositionFin() == null) {
                        final TronconUtils.PosInfo pInfo = new TronconUtils.PosInfo(p, troncon, tronconSegments);
                        p.setPositionDebut(pInfo.getGeoPointStart());
                        p.setPositionFin(pInfo.getGeoPointEnd());
                    }

                    // reset linear referencement.
                    p.setSystemeRepId(null);
                    p.setBorneDebutId(null);
                    p.setBorneFinId(null);
                    p.setBorne_debut_distance(Double.NaN);
                    p.setBorne_fin_distance(Double.NaN);
                    p.setBorne_debut_aval(false);
                    p.setBorne_fin_aval(false);
                }

            } else {
                iterator.remove();
            }
        }

        // TODO : analyse operation list and raise error if needed.
        final List<DocumentOperationResult> response = session.executeBulk((List) positionableList);
        if (!response.isEmpty()) {
            SirsCore.LOGGER.log(Level.WARNING, () -> {
                final String sep = System.lineSeparator();
                final StringBuilder errorMsg = new StringBuilder("Following position updates failed : ");
                for (final DocumentOperationResult result : response) {
                    errorMsg.append(sep)
                            .append("Doc: ").append(result.getId())
                            .append(sep)
                            .append("Error: ").append(result.getError())
                            .append(sep)
                            .append("Reason: ").append(result.getReason())
                            .append(sep).append(" ---- ");
                }
                return errorMsg.toString();
            });
        }
    }
}

