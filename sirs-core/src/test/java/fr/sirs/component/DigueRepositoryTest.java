/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.sirs.component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

import fr.sirs.core.CouchDBTestCase;
import fr.sirs.core.TronconUtils;
import fr.sirs.core.component.DigueRepository;
import fr.sirs.core.component.TronconDigueRepository;
import fr.sirs.core.model.Crete;
import fr.sirs.core.model.Digue;
import fr.sirs.core.model.ElementCreator;
import fr.sirs.core.model.Fondation;
import fr.sirs.core.model.Objet;
import fr.sirs.core.model.TronconDigue;

import org.junit.Ignore;

/**
 *
 * @author Samuel Andrés (Geomatys)
 */
public class DigueRepositoryTest extends CouchDBTestCase {
    
    DigueRepository digueRepository;
    
    @Before
    public void setUp() {
    	 digueRepository = new DigueRepository(connector);
    }
    
    /**
     * Test of getAll method, of class DigueRepository.
     */
    @Test
    public void testStoreDigueAndTroncons() {
        System.out.println("DigueRepositoryTest.testStoreDigueAndTroncons()");
        
        final TronconDigueRepository tronconRepository = new TronconDigueRepository(connector);
      
        final Digue digue = ElementCreator.createAnonymValidElement(Digue.class);
        digue.setLibelle("une digue");
        digue.setDateMaj(LocalDateTime.now());

        digueRepository.add(digue);

        for (int i = 0; i < 1; i++) {
            final TronconDigue troncon = ElementCreator.createAnonymValidElement(TronconDigue.class);
            troncon.setCommentaire("Troncon1");
            troncon.setDigueId(digue.getId());
            troncon.setGeometry(createPoint());
            troncon.setDateMaj(LocalDateTime.now());
            tronconRepository.add(troncon);

            final Fondation fondation = ElementCreator.createAnonymValidElement(Fondation.class);
            fondation.setLinearId(troncon.getId());
            
            final Crete crete = ElementCreator.createAnonymValidElement(Crete.class);
            crete.setBorneDebutId("8");
            crete.setCommentaire("Belle crete");
            crete.setLinearId(troncon.getId());
        }
    }

    @Test
    public void testGetAll() {
    	  List<Digue> result = digueRepository.getAll();
          for (Digue digue : result) {
              System.out.println(digue);
          }

          final TronconDigueRepository tronconRepository = new TronconDigueRepository(connector);
          for(TronconDigue troncon: tronconRepository.getAll()) {
              for(Objet str: TronconUtils.getObjetList(troncon)) {
                  System.out.println(str.getParent() + " " + str.getDocumentId());
              }
          }
          
    }
    
    @Test
    public void failDelete() {

        Digue digue = ElementCreator.createAnonymValidElement(Digue.class);
        digue.setLibelle("toDelete");
        digueRepository.add(digue);

        {
            final Digue digue2 = digueRepository.get(digue.getId());
            digue2.setLibelle("StillToDelete");
            digueRepository.update(digue2);
        }

        digue = digueRepository.get(digue.getId());
        digueRepository.remove(digue);

    }
    
    @Ignore
    @Test
    public void testInstances(){
        
        Digue digue = ElementCreator.createAnonymValidElement(Digue.class);
        digue.setLibelle("coucou");
        digueRepository.add(digue);
        
        System.out.println(digue.getLibelle());
        System.out.println(digue.getId());
        
        Digue digue1 = digueRepository.get(digue.getId());
        Digue digue2 = digueRepository.get(digue.getId());
        
        System.out.println(digue.hashCode());
        System.out.println(digue1.hashCode());
        System.out.println(digue2.hashCode());
        
        
        assert(digue==digue1);
        
        assert(digue==digue2);
    }
    
    @Ignore
    @Test
    public void uuid() {
    	
    }

    private Point createPoint(double i, double j) {
        // TODO Auto-generated method stub
        Point pt = new GeometryFactory().createPoint(new Coordinate(i, j));
        return pt;
    }

    private Point createPoint() {
        // random coord in france in 2154
        return createPoint(Math.random() * 900000 - 100000,
                Math.random() * 1000000 + 6100000);
    }

}
