package fr.sirs.core;

import java.util.UUID;

import org.ektorp.support.CouchDbDocument;

@SuppressWarnings("serial")
public class SirsDBInfo extends CouchDbDocument {

    private String version;
    
    private String uuid;
    
    private String epsgCode = "EPSG:2154";

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;       
    }
    
    public String getUuid() {
        return uuid;
    }

    public String getEpsgCode() {
        return epsgCode;
    }

    public void setEpsgCode(String epsgCode) {
        this.epsgCode = epsgCode;
    }
}
