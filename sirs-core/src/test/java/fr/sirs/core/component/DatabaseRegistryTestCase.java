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

import org.junit.Test;

import fr.sirs.core.CouchDBTestCase;
import fr.sirs.util.property.SirsPreferences;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.sis.test.DependsOnMethod;
import org.ektorp.ReplicationTask;
import org.junit.Assert;
import org.junit.BeforeClass;

public class DatabaseRegistryTestCase extends CouchDBTestCase {

    private static String REPLICATION_DEST;

    @BeforeClass
    public static void init() {
        REPLICATION_DEST = "sirs-test-dup".concat(UUID.randomUUID().toString());
        deleteAfterClass(REPLICATION_DEST);
    }

    @Test
    public void databaseList() throws IOException {
        Assert.assertTrue(
                "Database list does not contain target databasse !",
                REGISTRY.listSirsDatabases().contains(DB_NAME));
    }

    @DependsOnMethod("databaseList")
    @Test
    public void initDatabaseFromRemote() throws IOException {
        REGISTRY.synchronizeSirsDatabases(
                SirsPreferences.INSTANCE.getProperty(SirsPreferences.PROPERTIES.COUCHDB_LOCAL_ADDR) + DB_NAME, 
                SirsPreferences.INSTANCE.getProperty(SirsPreferences.PROPERTIES.COUCHDB_LOCAL_ADDR) + REPLICATION_DEST, true);
    }

    @DependsOnMethod("initDatabaseFromRemote")
    @Test
    public void getReplicationTasks() throws IOException, InterruptedException {
        Thread.sleep(4000l);
        Assert.assertTrue("No replication task found !", REGISTRY.getReplicationTasks().findAny().isPresent());
    }

    @DependsOnMethod("getReplicationTasks")
    @Test
    public void getSynchronizationTask() throws IOException, InterruptedException {
        Thread.sleep(4000l);
        final List<ReplicationTask> collect = REGISTRY.getSynchronizationTasks(SirsPreferences.INSTANCE.getProperty(SirsPreferences.PROPERTIES.COUCHDB_LOCAL_ADDR)+REPLICATION_DEST).collect(Collectors.toList());
        Assert.assertEquals(2, collect.size());
    }

    @DependsOnMethod("getSynchronizationTask")
    @Test
    public void dropDatabase() throws IOException {
        REGISTRY.dropDatabase(REPLICATION_DEST);
        Assert.assertFalse(
                "Database list contains a deleted database !",
                REGISTRY.listSirsDatabases().contains(REPLICATION_DEST));
    }
    
    @Test
    public void testCleanDatabaseName(){
        Assert.assertEquals("toto",DatabaseRegistry.cleanDatabaseName("toto"));
        Assert.assertEquals("localhost:5984/toto",DatabaseRegistry.cleanDatabaseName("http://localhost:5984/toto"));
        Assert.assertEquals("localhost:5984/toto",DatabaseRegistry.cleanDatabaseName("http://localhost:5984/toto/"));
        Assert.assertEquals("localhost:5984/toto",DatabaseRegistry.cleanDatabaseName("http://geouser:geopw@localhost:5984/toto"));
        Assert.assertEquals("localhost:5984/toto",DatabaseRegistry.cleanDatabaseName("http://geouser:geopw@localhost:5984/toto/"));
        Assert.assertEquals("france-digues.fr:5984/toto",DatabaseRegistry.cleanDatabaseName("http://france-digues.fr:5984/toto"));
        Assert.assertEquals("france-digues.fr:5984/toto",DatabaseRegistry.cleanDatabaseName("http://france-digues.fr:5984/toto/"));
        Assert.assertEquals("france-digues.fr:5984/toto",DatabaseRegistry.cleanDatabaseName("http://geouser:geopw@france-digues.fr:5984/toto"));
        Assert.assertEquals("france-digues.fr:5984/toto",DatabaseRegistry.cleanDatabaseName("http://geouser:geopw@france-digues.fr:5984/toto/"));
    }
}
