/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.symadrem.sirs.component;

import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import fr.symadrem.sirs.core.CouchDBTestCase;
import fr.symadrem.sirs.core.component.TronconDigueRepository;
import fr.symadrem.sirs.core.model.Crete;
import fr.symadrem.sirs.core.model.Fondation;
import fr.symadrem.sirs.core.model.PiedDigue;
import fr.symadrem.sirs.core.model.Structure;
import fr.symadrem.sirs.core.model.TronconDigue;

import org.ektorp.CouchDbConnector;

/**
 *
 * @author Samuel Andrés (Geomatys)
 */
public class TronconRepositoryTest extends CouchDBTestCase {

	@Autowired
	private CouchDbConnector couchDbConnector;

	/**
	 * Test of getAll method, of class TronconDigueRepository.
	 */
	@Test
	public void testGetAll() {
		System.out.println("getAll");
		final TronconDigueRepository tronconRepository = new TronconDigueRepository(
				couchDbConnector);
		for (TronconDigue troncon : tronconRepository.getAll()) {
			System.out.println(troncon);
			for (Structure struct : troncon.getStuctures()) {
				System.out.println("DocuumentId: " + struct.getDocumentId());
				
			}
		}
	}

	@Test
	public void listAllFondations() {
		final TronconDigueRepository tronconRepository = new TronconDigueRepository(
				couchDbConnector);
		List<Fondation> all = tronconRepository.getAllFondations();
		dumpAllStructure(all);

	}

	@Test
	public void listAllCretes() {
		final TronconDigueRepository tronconRepository = new TronconDigueRepository(
				couchDbConnector);
		List<Crete> all = tronconRepository.getAllCretes();
		dumpAllStructure(all);

	}

	@Test
	public void listAllPiedDigue() {
		final TronconDigueRepository tronconRepository = new TronconDigueRepository(
				couchDbConnector);
		List<PiedDigue> all = tronconRepository.getAllPiedDigues();
		dumpAllStructure(all);

	}

	private void dumpAllStructure(List<? extends Structure> allFondations) {
		for (Structure fondation : allFondations) {
			System.out.println(fondation.getId() + " / "
					+ fondation.getDocumentId());
		}
	}
}
