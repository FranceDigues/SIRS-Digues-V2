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

import java.util.List;
import org.ektorp.CouchDbConnector;
import org.ektorp.support.View;
import org.springframework.beans.factory.annotation.Autowired;

import fr.sirs.core.SirsViewIterator;
import fr.sirs.core.SirsCoreRuntimeException;
import fr.sirs.core.model.TronconDigue;
import java.util.ArrayList;
import org.ektorp.support.Views;
import org.springframework.stereotype.Component;

/**
 *
 * @author Alexis Manin (Geomatys)
 */
@Views({
@View(name = AbstractTronconDigueRepository.STREAM_LIGHT, map = "classpath:TronconDigueLight-map.js"),
@View(name = AbstractTronconDigueRepository.ALL_TRONCON_IDS, map = "classpath:TronconDigue_ids.js")
})
@Component
public class TronconDigueRepository extends AbstractTronconDigueRepository<TronconDigue> {

    @Autowired
    private TronconDigueRepository(CouchDbConnector db) {
        super(db, TronconDigue.class);
    }

    /**
     * Note : As the objects returned here are incomplete, they're not cached.
     *
     * @return a light version of the tronçon, without sub-structures.
     */
    public List<TronconDigue> getAllLight() {
        final List<TronconDigue> lst = new ArrayList<>();
        try (final SirsViewIterator<TronconDigue> ite = SirsViewIterator.create(TronconDigue.class, db.queryForStreamingView(createQuery(STREAM_LIGHT)))) {
            while (ite.hasNext()) {
                lst.add(ite.next());
            }
        } catch (Exception e) {
            throw new SirsCoreRuntimeException("Cannot build Troncon view.", e);
        }
        return lst;
    }

    public SirsViewIterator<TronconDigue> getAllLightIterator() {
        return SirsViewIterator.create(TronconDigue.class,
                db.queryForStreamingView(createQuery(STREAM_LIGHT)));
    }


    public List<TronconDigue> getAllIdsAndDesignations() {
        final List<TronconDigue> lst = new ArrayList<>();
        try (final SirsViewIterator<TronconDigue> ite = SirsViewIterator.create(TronconDigue.class, db.queryForStreamingView(createQuery(ALL_TRONCON_IDS)))) {
            while (ite.hasNext()) {
                lst.add(ite.next());
            }
        } catch (Exception e) {
            throw new SirsCoreRuntimeException("Cannot build Troncon view.", e);
        }
        return lst;
    }


    public SirsViewIterator<TronconDigue> getAllIdsAndDesignationsIterator() {
        return SirsViewIterator.create(TronconDigue.class,
                db.queryForStreamingView(createQuery(ALL_TRONCON_IDS)));
    }


}
