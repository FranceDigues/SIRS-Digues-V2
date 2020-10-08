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

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import fr.sirs.core.component.SystemeReperageRepository;
import fr.sirs.core.model.AvecBornesTemporelles;
import fr.sirs.core.model.BorneDigue;
import fr.sirs.core.model.Crete;
import fr.sirs.core.model.ElementCreator;
import fr.sirs.core.model.Objet;
import fr.sirs.core.model.SystemeReperage;
import fr.sirs.core.model.SystemeReperageBorne;
import fr.sirs.core.model.TronconDigue;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Predicate;
import org.apache.sis.test.DependsOnMethod;
import org.ektorp.DocumentOperationResult;
import org.geotoolkit.display2d.GO2Utilities;
import org.geotoolkit.referencing.LinearReferencing;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Assume;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class TronconUtilsTest extends CouchDBTestCase {

    private static final float DELTA = 0.0001f;
    private static final GeometryFactory GF = new GeometryFactory();

    private static TronconDigue troncon;
    private static BorneDigue borne0;
    private static BorneDigue borne1;
    private static BorneDigue borne2;
    private static SystemeReperage sr;
    private static Crete crete;

    @Test
    public void dataIntegrityTest() {
        //creation du troncon
        troncon = ElementCreator.createAnonymValidElement(TronconDigue.class);
        troncon.setLibelle("TC");
        troncon.setGeometry(GF.createLineString(new Coordinate[]{new Coordinate(0, 0),new Coordinate(100, 0)}));
        session.getRepositoryForClass(TronconDigue.class).add(troncon);

        //creation des bornes
        borne0 = ElementCreator.createAnonymValidElement( BorneDigue.class);
        borne0.setLibelle("B0");
        borne0.setGeometry(GF.createPoint(new Coordinate(1, 1)));
        borne1 = ElementCreator.createAnonymValidElement( BorneDigue.class);
        borne1.setLibelle("B1");
        borne1.setGeometry(GF.createPoint(new Coordinate(51, 2)));
        borne2 = ElementCreator.createAnonymValidElement( BorneDigue.class);
        borne2.setLibelle("B2");
        borne2.setGeometry(GF.createPoint(new Coordinate(99, -3)));
        session.getRepositoryForClass(BorneDigue.class).add(borne0);
        session.getRepositoryForClass(BorneDigue.class).add(borne1);
        session.getRepositoryForClass(BorneDigue.class).add(borne2);

        //creation du systeme de reperage
        sr = ElementCreator.createAnonymValidElement( SystemeReperage.class);
        sr.setLibelle("SR");
        sr.setLinearId(troncon.getDocumentId());
        final SystemeReperageBorne srb0 = ElementCreator.createAnonymValidElement( SystemeReperageBorne.class);
        srb0.setBorneId(borne0.getDocumentId());
        srb0.setValeurPR(0);
        final SystemeReperageBorne srb1 = ElementCreator.createAnonymValidElement( SystemeReperageBorne.class);
        srb1.setBorneId(borne1.getDocumentId());
        srb1.setValeurPR(10);
        final SystemeReperageBorne srb2 = ElementCreator.createAnonymValidElement( SystemeReperageBorne.class);
        srb2.setBorneId(borne2.getDocumentId());
        srb2.setValeurPR(20);
        sr.getSystemeReperageBornes().add(srb0);
        sr.getSystemeReperageBornes().add(srb1);
        sr.getSystemeReperageBornes().add(srb2);
        ((SystemeReperageRepository) session.getRepositoryForClass(SystemeReperage.class)).add(sr, troncon);

        session.getRepositoryForClass(TronconDigue.class).update(troncon);

        //on ajoute une crète
        crete = ElementCreator.createAnonymValidElement( Crete.class);
        crete.setLinearId(troncon.getId());
        crete.setBorne_debut_aval(false);
        crete.setBorne_debut_distance(0.5f);
        crete.setBorne_fin_distance(0.3f);
        crete.setBorne_fin_aval(true);
        crete.setSystemeRepId(sr.getDocumentId());
        crete.setBorneDebutId(borne0.getDocumentId());
        crete.setBorneFinId(borne2.getDocumentId());
        final Geometry creteGeometry = new TronconUtils.PosInfo(crete, troncon).getGeometry();
        session.getRepositoryForClass(Crete.class).add(crete);

        //le troncon doit etre a jour avec la liste des bornes
        troncon = session.getRepositoryForClass(TronconDigue.class).get(troncon.getDocumentId());
        final String[] tcbids = troncon.getBorneIds().toArray(new String[0]);
        assertEquals(3, tcbids.length);
        assertEquals(borne0.getId(), tcbids[0]);
        assertEquals(borne1.getId(), tcbids[1]);
        assertEquals(borne2.getId(), tcbids[2]);

        //on vérifie qu'on reconstruit bien la geometrie
        final LineString expected = GF.createLineString(new Coordinate[]{new Coordinate(1.5, 0),new Coordinate(98.7, 0)});
        final StringBuilder errorMessage = new StringBuilder("Computed geometry is not as expected !")
                .append(System.lineSeparator())
                .append("Expected :")
                .append(System.lineSeparator())
                .append(expected.toText())
                .append(System.lineSeparator())
                .append("Found :")
                .append(System.lineSeparator())
                .append(creteGeometry.toText());

        assertTrue(errorMessage.toString(), expected.equalsExact(creteGeometry, DELTA));
    }

    /**
     * Test du decoupage d'un troncon.
     */
    @Test
    @DependsOnMethod("dataIntegrityTest")
    public void cutTest() {
        //premiere decoupe -----------------------------------------------------
        final TronconDigue cut0 = TronconUtils.cutTroncon(troncon,
                GF.createLineString(new Coordinate[]{new Coordinate(0, 0),new Coordinate(50, 0)}),
                "TC[0]", session);
        assertEquals("TC[0]", cut0.getLibelle());
        assertEquals(GF.createLineString(new Coordinate[]{new Coordinate(0, 0),new Coordinate(50, 0)}), cut0.getGeometry());

        //on verifie le systeme de reperage
        final List<String> cut0brs = cut0.getBorneIds();
        // Bornes de début et fin, plus celle gardée au découpage.
        assertEquals(3, cut0brs.size());
        assertTrue(cut0brs.contains(borne0.getId()));
        assertFalse(cut0brs.contains(borne1.getId()));
        assertFalse(cut0brs.contains(borne2.getId()));

        final List<SystemeReperage> cut0Srs = ((SystemeReperageRepository) session.getRepositoryForClass(SystemeReperage.class)).getByLinear(cut0);
        assertEquals(2, cut0Srs.size()); // SR découpé + SR elementaire
        SystemeReperage cut0sr = null;
        for (final SystemeReperage sr : cut0Srs) {
            if (sr.getSystemeReperageBornes().filtered(srb -> srb.getBorneId().equals(borne0.getId())).size() > 0) {
                cut0sr = sr;
                break;
            }
        }
        assertNotNull("Cut SR cannot be found.", cut0sr);
        assertNotEquals(sr.getDocumentId(), cut0sr.getDocumentId());
        assertEquals("SR", cut0sr.getLibelle());
        final List<SystemeReperageBorne> cut0srbs = cut0sr.getSystemeReperageBornes();
        assertEquals(1, cut0srbs.size());
        final SystemeReperageBorne cut0srb = cut0srbs.get(0);
        assertEquals(0.0f, cut0srb.getValeurPR(), DELTA);
        final BorneDigue cut0b0 = session.getRepositoryForClass(BorneDigue.class).get(cut0srb.getBorneId());
        assertEquals(borne0.getDocumentId(), cut0b0.getDocumentId());
        assertEquals("B0", cut0b0.getLibelle());
        assertTrue(GF.createPoint(new Coordinate(1, 1)).equals(cut0b0.getGeometry()));

        //on verifie que la crete a été coupée
        final List<Objet> cut0Strs = TronconUtils.getObjetList(cut0);
        assertEquals(1, cut0Strs.size());
        final Crete cut0Crete = (Crete)cut0Strs.get(0);
        assertEquals(cut0sr.getDocumentId(), cut0Crete.getSystemeRepId());
        assertEquals(cut0b0.getDocumentId(), cut0Crete.getBorneDebutId());
        assertEquals(cut0b0.getDocumentId(), cut0Crete.getBorneFinId());
        assertEquals(0.5f, cut0Crete.getBorne_debut_distance(), DELTA);
        assertEquals(49.0f, cut0Crete.getBorne_fin_distance(), DELTA); // troncon coupé
        assertEquals(false, cut0Crete.getBorne_debut_aval());
        assertEquals(false, cut0Crete.getBorne_fin_aval());

        //la geometrie doit etre a jour aussi
        assertTrue(GF.createLineString(new Coordinate[]{new Coordinate(1.5, 0),new Coordinate(50, 0)}).equalsExact(
                cut0Crete.getGeometry(),DELTA));


        //le troncon d'origine ne doit pas avoir changé
        assertTrue(GF.createLineString(new Coordinate[]{new Coordinate(1.5, 0),new Coordinate(98.7, 0)}).equalsExact(
                LinearReferencingUtilities.buildGeometry(troncon.getGeometry(), crete, session.getRepositoryForClass(BorneDigue.class))
                ,DELTA));

        //seconde decoupe -----------------------------------------------------
        final TronconDigue cut1 = TronconUtils.cutTroncon(troncon,
                GF.createLineString(new Coordinate[]{new Coordinate(50, 0),new Coordinate(100, 0)}),
                "TC[1]", session);
        assertEquals("TC[1]", cut1.getLibelle());
        assertEquals(GF.createLineString(new Coordinate[]{new Coordinate(50, 0),new Coordinate(100, 0)}), cut1.getGeometry());

        //on verifie le systeme de reperage
        final List<String> cut1brs = cut1.getBorneIds();
        assertEquals(4, cut1brs.size());
        assertTrue("Cut troncon shares its bornes with its parent.", cut1brs.contains(borne1.getDocumentId()));
        assertTrue("Cut troncon shares its bornes with its parent.", cut1brs.contains(borne2.getDocumentId()));

        final List<SystemeReperage> cut1Srs = ((SystemeReperageRepository) session.getRepositoryForClass(SystemeReperage.class)).getByLinear(cut1);
        assertEquals(2, cut1Srs.size());
        SystemeReperage cut1sr = null;
        for (final SystemeReperage sr : cut1Srs) {
            if (sr.getSystemeReperageBornes().filtered(srb -> srb.getBorneId().equals(borne1.getId())).size() > 0) {
                cut1sr = sr;
                break;
            }
        }
        assertNotNull("Cut SR cannot be found.", cut1sr);
        assertNotEquals(sr.getDocumentId(), cut1sr.getDocumentId());
        assertEquals("SR", cut1sr.getLibelle());
        final List<SystemeReperageBorne> cut1srbs = cut1sr.getSystemeReperageBornes();
        cut1srbs.sort((SystemeReperageBorne first, SystemeReperageBorne second) -> {
            final float diff = first.getValeurPR() - second.getValeurPR();
            if (diff > 0) return -1;
            else if (diff < 0) return 1;
            else return 0;
        });
        assertEquals(2, cut1srbs.size());
        final SystemeReperageBorne cut1srb0 = cut1srbs.get(1);
        final SystemeReperageBorne cut1srb1 = cut1srbs.get(0);
        assertEquals(10.0f, cut1srb0.getValeurPR(), DELTA);
        assertEquals(20.0f, cut1srb1.getValeurPR(), DELTA);
        final BorneDigue cut1b0 = session.getRepositoryForClass(BorneDigue.class).get(cut1srb0.getBorneId());
        final BorneDigue cut1b1 = session.getRepositoryForClass(BorneDigue.class).get(cut1srb1.getBorneId());
        assertEquals("Cut troncon shares its bornes with its parent.", borne1.getDocumentId(), cut1b0.getDocumentId());
        assertEquals("Cut troncon shares its bornes with its parent.", borne2.getDocumentId(), cut1b1.getDocumentId());
        assertEquals("B1", cut1b0.getLibelle());
        assertEquals("B2", cut1b1.getLibelle());
        assertTrue(GF.createPoint(new Coordinate(51, 2)).equals(cut1b0.getGeometry()));
        assertTrue(GF.createPoint(new Coordinate(99,-3)).equals(cut1b1.getGeometry()));

        //on verifie que la crete a été coupée
        final List<Objet> cut1Strs = TronconUtils.getObjetList(cut1);
        assertEquals(1, cut1Strs.size());
        final Crete cut1Crete = (Crete)cut1Strs.get(0);
        assertEquals(cut1sr.getDocumentId(), cut1Crete.getSystemeRepId());
        assertEquals(cut1b0.getDocumentId(), cut1Crete.getBorneDebutId());
        assertEquals(cut1b1.getDocumentId(), cut1Crete.getBorneFinId());
        assertEquals(1.0f, cut1Crete.getBorne_debut_distance(), DELTA); // troncon coupé
        assertEquals(0.3f, cut1Crete.getBorne_fin_distance(), DELTA);
        assertTrue("Start borne is uphill start point.", cut1Crete.getBorne_debut_aval());
        assertTrue("End borne is uphill end point.", cut1Crete.getBorne_fin_aval());

        //la geometrie doit etre a jour aussi
        assertTrue(GF.createLineString(new Coordinate[]{new Coordinate(50.0, 0),new Coordinate(98.7, 0)}).equalsExact(
                cut1Crete.getGeometry(),DELTA));

        //le troncon d'origine ne doit pas avoir changé
        assertTrue(GF.createLineString(new Coordinate[]{new Coordinate(1.5, 0),new Coordinate(98.7, 0)}).equalsExact(
                LinearReferencingUtilities.buildGeometry(troncon.getGeometry(), crete, session.getRepositoryForClass(BorneDigue.class))
                ,DELTA));
    }


    @Test
    @DependsOnMethod("dataIntegrityTest")
    public void computePRTest() {
        TronconUtils.computePRs(crete, session);

        /*
         * Soit la fonction distanceProjetée(objet A, objet B) la distance entre
         * les objets A et B après projection sur le tronçon.
         * Soit la fonction PR(BorneDigue b) la valeur du PR de la borne b.
         */
        // PR début Crête = (PR(borne1) - PR(borne0)) / DistanceProjetée(borne0, borne1) * distanceProjetée(borne0, crête) + PR(borne0)
        // PR début Crête = (10 - 0) / 50 * 0.5 + 0
        // PR début Crête = 0.1
        assertEquals("PR de début de la Crête", 0.1, crete.getPrDebut(), DELTA);

        // PR fin Crête = (PR(borne2) - PR(borne1)) / DistanceProjetée(borne2, borne1) * distanceProjetée(borne2, crête) + PR(borne2)
        // PR fin Crête = (20 - 10) / 48 * -0.3 + 20
        // PR fin Crête = 19,9375
        assertEquals("PR de fin de la Crête", 19.9375, crete.getPrFin(), DELTA);

        LinearReferencing.SegmentInfo[] segments = LinearReferencingUtilities.buildSegments(LinearReferencingUtilities.asLineString(troncon.getGeometry()));
        float fictivePR = TronconUtils.computePR(segments, sr, GO2Utilities.JTS_FACTORY.createPoint(new Coordinate(19, 0)), session.getRepositoryForClass(BorneDigue.class));

        // PR fictif = (PR(borne1) - PR(borne0)) / DistanceProjetée(borne1, borne0) * distanceProjetée(borne0, Point fictif) + PR(borne0)
        // PR fictif = (10 - 0) / 50 * 18 + 0
        // PR fictif = 3.6
        assertEquals("PR de fin de la Crête", 3.6, fictivePR, DELTA);
    }

    @Test
    @DependsOnMethod("dataIntegrityTest")
    public void archive() {
        
        // Initial state : check all the archive dates to be null.
        assertNull("End date of \"TronconDigue\" differs from queried one", troncon.getDate_fin());
        assertNull("End date of \"Crete\" differs from queried one", crete.getDate_fin());
        final List<BorneDigue> bornes = session.getRepositoryForClass(BorneDigue.class).get(troncon.getBorneIds());
        Assume.assumeFalse("No borne to test archive operation on.", bornes.isEmpty());
        for (final BorneDigue b : bornes) {
            assertNull("End date of \"Borne\" differs from queried one", b.getDate_fin());
        }
        
        // First, we test that we can archive objects with no end date.
        final LocalDate now = LocalDate.now();
        final Predicate<AvecBornesTemporelles> archiveIf = new AvecBornesTemporelles.ArchivePredicate(null);
        List<DocumentOperationResult> errors = TronconUtils.archiveSectionWithTemporalObjects(troncon, session, now, archiveIf);
        errors.addAll(TronconUtils.archiveBornes(troncon.getBorneIds(), session, now, archiveIf));
        assertTrue("Errors occurred while update", errors.isEmpty());
        checkArchiveDate(now);

        // Secondly, we test that we can update archive date
        final LocalDate tomorrow = now.plusDays(1);
        final Predicate<AvecBornesTemporelles> updateIf = new AvecBornesTemporelles.UpdateArchivePredicate(now);
        errors = TronconUtils.archiveSectionWithTemporalObjects(troncon, session, tomorrow, updateIf);
        errors.addAll(TronconUtils.archiveBornes(troncon.getBorneIds(), session, tomorrow, updateIf));
//                updateArchiveSectionWithTemporalObjects(troncon, session, now, tomorrow);
        assertTrue("Errors occurred while update", errors.isEmpty());
        checkArchiveDate(tomorrow);

        // Finally, we try to remove previously set date.
//        errors = TronconUtils.unarchiveSectionWithTemporalObjects(troncon, session, tomorrow);
        final Predicate<AvecBornesTemporelles> unArchiveIf = new AvecBornesTemporelles.UnArchivePredicate(tomorrow);
        errors = TronconUtils.archiveSectionWithTemporalObjects(troncon, session, null, unArchiveIf);
        errors.addAll(TronconUtils.archiveBornes(troncon.getBorneIds(), session, null, unArchiveIf));
        assertTrue("Errors occurred while update", errors.isEmpty());
        assertNull("End date of updated \"TronconDigue\" differs from queried one", troncon.getDate_fin());
        assertNull("End date of updated \"Crete\" differs from queried one", crete.getDate_fin());

        Assume.assumeFalse("No borne to test archive operation on.", bornes.isEmpty());
        for (final BorneDigue b : bornes) {
            assertNull("End date of updated \"Borne\" differs from queried one", b.getDate_fin());
        }
    }

    /**
     * Check that {@link AvecBornesTemporelles#getDate_fin() } is equal to given
     * date for this test suite objects (i.e {@link #troncon} and {@link #crete}.
     * @param expected The date to check.
     */
    private void checkArchiveDate(final LocalDate expected) {
        assertEquals("End date of updated \"TronconDigue\" differs from queried one", expected, troncon.getDate_fin());
        assertEquals("End date of updated \"Crete\" differs from queried one", expected, crete.getDate_fin());

        final List<BorneDigue> bornes = session.getRepositoryForClass(BorneDigue.class).get(troncon.getBorneIds());
        Assume.assumeFalse("No borne to test archive operation on.", bornes.isEmpty());
        for (final BorneDigue b : bornes) {
            assertEquals("End date of updated \"Borne\" differs from queried one", expected, b.getDate_fin());
        }
    }
}
