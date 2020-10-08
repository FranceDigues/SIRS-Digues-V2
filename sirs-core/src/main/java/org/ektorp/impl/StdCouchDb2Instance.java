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
package org.ektorp.impl;

import static java.lang.String.*;

import java.io.*;
import java.util.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.sirs.core.component.CouchSGBD;

import org.ektorp.*;
import org.ektorp.http.*;
import org.ektorp.util.*;
import org.slf4j.*;

/**
 * 
 * Cette adaptation de {@link StdCouchDbInstance} a pour but de s'adapter à la modification de l'API de configuration 
 * dans couchdb2 du fait de la prise en compte du clustering pour une configuration nœud par nœud.
 * 
 * http://docs.couchdb.org/en/stable/api/server/configuration.html
 *
 * @author Samuel Andrés (Geomatys) [Adaptation de {@link StdCouchDbInstance}]
 * 
 * @see StdCouchDbInstance
 */
public class StdCouchDb2Instance implements CouchDbInstance {

    private final static Logger LOG = LoggerFactory.getLogger(StdCouchDb2Instance.class);
    private final static TypeReference<List<String>> STRING_LIST_TYPE_DEF = new TypeReference<List<String>>() {};

    private final HttpClient client;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ObjectMapperFactory objectMapperFactory;
    private final String nodeName;

    public StdCouchDb2Instance(HttpClient client, String nodeName) {
            this(client, new StdObjectMapperFactory(), nodeName);
    }

    public StdCouchDb2Instance(HttpClient client, ObjectMapperFactory of, String nodeName) {
        Assert.notNull(client, "HttpClient may not be null");
        Assert.notNull(of, "ObjectMapperFactory may not be null");
        this.client = client;
        this.restTemplate = new RestTemplate(client);
        this.objectMapper = of.createObjectMapper();
        this.objectMapperFactory = of;
        this.nodeName = nodeName;
    }

    public ObjectMapperFactory getObjectMapperFactory() {
        return objectMapperFactory;
    }
    
    /**
     * Récupération des informations du SGBD.
     * 
     * @return 
     */
    public CouchSGBD getSGBDInfo(){
        return restTemplate.get("", new StdResponseHandler<CouchSGBD>() {
            @Override
            public CouchSGBD success(HttpResponse hr)
                    throws Exception {
                return objectMapper.readValue(hr.getContent(), CouchSGBD.class);
            }
            
        });
    }

    @Override
    public void createDatabase(String path) {
        createDatabase(DbPath.fromString(path));
    }

    @Override
    public void createDatabase(DbPath db) {
        if (!createDatabaseIfNotExists(db)) {
            throw new DbAccessException(format("A database with path %s already exists", db.getPath()));
        }
    }

    @Override
    public boolean createDatabaseIfNotExists(String path) {
        return createDatabaseIfNotExists(DbPath.fromString(path));
    }

    @Override
    public boolean createDatabaseIfNotExists(final DbPath db) {
        boolean databaseAlreadyExists = checkIfDbExists(db);
        if (databaseAlreadyExists) {
                return false;
        }
        LOG.debug("creating db path: {}", db.getPath());
        return restTemplate.put(db.getPath(), new StdResponseHandler<Boolean>() {
                @Override
                public Boolean error(HttpResponse hr) {
                        if (hr.getCode() == HttpStatus.PRECONDITION_FAILED) {
                                // 412 indicates existing database
                                // see http://docs.couchdb.org/en/latest/api/database/common.html#put--db
                                LOG.debug("database at db path {} already exists.", db.getPath());
                                return false;
                        }
                        throw StdResponseHandler.createDbAccessException(hr);
                }
                @Override
                public Boolean success(HttpResponse hr) throws Exception {
                        return checkResponseBodyOkAndReturnDefaultValue(hr, true, objectMapper);
                }
        });
    }

    @Override
    public void deleteDatabase(String path) {
        Assert.notNull(path);
        restTemplate.delete(DbPath.fromString(path).getPath());
    }

    @Override
    public boolean checkIfDbExists(String path) {
        return checkIfDbExists(DbPath.fromString(path));
    }

    @Override
    public boolean checkIfDbExists(DbPath db) {
        return restTemplate.head(db.getPath(), new StdResponseHandler<Boolean>() {
            @Override
            public Boolean error(HttpResponse hr) {
                    if(hr.getCode() == HttpStatus.NOT_FOUND) {
                            // only 404 is a valid response, anything else is an error
                            // see http://docs.couchdb.org/en/latest/api/database/common.html#head--db
                            return false;
                    }
                    throw StdResponseHandler.createDbAccessException(hr);
            }
            @Override
            public Boolean success(HttpResponse hr) throws Exception {
                    return true;
            }
        });
    }

    @Override
    public List<String> getAllDatabases() {
        return restTemplate.get("/_all_dbs", new StdResponseHandler<List<String>>(){
            @Override
            public List<String> success(HttpResponse hr) throws Exception {
                LOG.info("Success of the request /_all_dbs");
                return objectMapper.readValue(hr.getContent(), STRING_LIST_TYPE_DEF);
            }

            @Override
            public List<String> error(HttpResponse hr) {
                return super.error(hr); //To change body of generated methods, choose Tools | Templates.
            }
            
            
        });
    }

    @Override
    public ReplicationStatus replicate(ReplicationCommand cmd) {
        try {
            return restTemplate.post("/_replicate", objectMapper.writeValueAsString(cmd), new StdResponseHandler<ReplicationStatus>() {
                @Override
                public ReplicationStatus success(HttpResponse hr)
                        throws Exception {
                    return objectMapper.readValue(hr.getContent(), ReplicationStatus.class);
                }
            });
        } catch (IOException e) {
            throw Exceptions.propagate(e);
        }
    }

    @Override
    public HttpClient getConnection() {
        return client;
    }
    
    public RestTemplate getRestTemplate(){
        return restTemplate;
    }

    @Override
    public CouchDbConnector createConnector(String path,
                    boolean createIfNotExists) {
        CouchDbConnector db = new StdCouchDbConnector(path, this, objectMapperFactory);
        if (createIfNotExists) db.createDatabaseIfNotExists();
        return db;
    }

    @Override
    public CouchDbConnector getReplicatorConnector()
    {
        return createConnector("_replicator", false);
    }

    @Override
    public <T> T getConfiguration(final Class<T> c) {
       return getConfiguration(c, null, null);
    }

   @Override
   public <T> T getConfiguration(final Class<T> c, String section) {
       return getConfiguration(c, section, null);
   }

   @Override
   public <T> T getConfiguration(final Class<T> c, String section, String key) {
       Assert.notNull(c, "Class may not be null");
       String url = "/_node/"+nodeName+"/_config";
       if(section != null) {
           url = url + "/" + section;
           if(key != null) {
               url = url + "/" + key;
           }
       }
       return restTemplate.get(url,
          new StdResponseHandler<T>() {
             @Override
             public T success(HttpResponse hr) throws Exception {
                return objectMapper.readValue(hr.getContent(), c);
             }
          });
   }

   @Override
   public String getConfiguration(String section, String key) {
      return getConfiguration(String.class, section, key);
   }

   @Override
   public String setConfiguration(String section, String key, String value) {
      Assert.notNull(section, "Section may not be null");
      Assert.notNull(key, "Key may not be null");
      String url = "/_node/"+nodeName+"/_config/" + section + "/" + key;
      String content;
      try {
            content = objectMapper.writeValueAsString(value);
      } catch (JsonProcessingException e) {
            throw Exceptions.propagate(e);
      }
      return restTemplate.put(url, content,
         new StdResponseHandler<String>() {
            @Override
            public String success(HttpResponse hr) throws Exception {
               return objectMapper.readValue(hr.getContent(), String.class);
            }
         });
    }

   @Override
   public String deleteConfiguration(String section, String key) {
       Assert.notNull(section, "Section may not be null");
       Assert.notNull(key, "Key may not be null");
       String url = "/_node/"+nodeName+"/_config/" + section + "/" + key;
       return restTemplate.delete(url,
          new StdResponseHandler<String>() {
             @Override
             public String success(HttpResponse hr) throws Exception {
                return objectMapper.readValue(hr.getContent(), String.class);
             }
         });
   }

   @Override
   public Collection<ActiveTask> getActiveTasks() {
      String url = "/_active_tasks";
      List<StdActiveTask> tasks = restTemplate.get(url,
         new StdResponseHandler<List<StdActiveTask>>() {
         @Override
         public List<StdActiveTask> success(HttpResponse hr) throws Exception {
            return objectMapper.readValue(hr.getContent(), new TypeReference<List<StdActiveTask>>() {});
         }
      });

      // We have to copy the list here because Java lacks covariance (i.e. we can't just return
      // the List<StdActiveTask> because it's not a Collection<ActiveTask>).
      Collection<ActiveTask> ret = new ArrayList<>();
      for (StdActiveTask task : tasks) {
          ret.add(task);
      }

      return ret;
   }
}
