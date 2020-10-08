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
package fr.sirs.couchdb2;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.ektorp.ReplicationTask;
import org.ektorp.impl.StdActiveTask;
import org.ektorp.impl.StdReplicationTask;

/**
 * Remplacement de la tâche de réplication de couchDB 1 par une nouvelle implémentation supportant l'API REST de couchDB 2.
 *
 * @author Samuel Andrés (Geomatys)
 * @see StdReplicationTask
 */
public class Couchdb2ReplicationTask extends StdActiveTask implements ReplicationTask {
    
    private String replicationId;
    private String replicationDocumentId;
    private boolean isContinuous;
    private long writeFailures;
    private long totalReads;
    private long totalWrites;
    private long totalMissingRevisions;
    private long totalRevisionsChecked;
    private String sourceDatabase;
    private String targetDatabase;
    private String sourceSequenceId; // nouveau type dans couchDB 2 : chaîne de caractères à la place d'un entier
    private Long checkpointInterval;
    private String checkpointedSourceSequenceId; // nouveau type dans couchDB 2 : chaîne de caractères à la place d'un entier

    @Override
    public String getReplicationId() {
        return replicationId;
    }

    @JsonProperty(required = false, value = "replication_id")
    public void setReplicationId(String replicationId) {
        this.replicationId = replicationId;
    }

    @Override
    public String getReplicationDocumentId() {
        return replicationDocumentId;
    }

    @JsonProperty(required = false, value = "doc_id")
    public void setReplicationDocumentId(String replicationDocumentId) {
        this.replicationDocumentId = replicationDocumentId;
    }

    @Override
    public boolean isContinuous() {
        return isContinuous;
    }

    @JsonProperty(required = false, value = "continuous")
    public void setContinuous(boolean isContinuous) {
        this.isContinuous = isContinuous;
    }

    @Override
    public long getWriteFailures() {
        return writeFailures;
    }

    @JsonProperty(required = false, value = "doc_write_failures")
    public void setWriteFailures(long writeFailures) {
        this.writeFailures = writeFailures;
    }

    @Override
    public long getTotalReads() {
        return totalReads;
    }

    @JsonProperty(required = false, value = "docs_read")
    public void setTotalReads(long totalReads) {
        this.totalReads = totalReads;
    }

    @Override
    public long getTotalWrites() {
        return totalWrites;
    }

    @JsonProperty(required = false, value = "docs_written")
    public void setTotalWrites(long totalWrites) {
        this.totalWrites = totalWrites;
    }

    @Override
    public long getTotalMissingRevisions() {
        return totalMissingRevisions;
    }

    @JsonProperty(required = false, value = "missing_revisions_found")
    public void setTotalMissingRevisions(long totalMissingRevisions) {
        this.totalMissingRevisions = totalMissingRevisions;
    }

    @Override
    public long getTotalRevisionsChecked() {
        return totalRevisionsChecked;
    }

    @JsonProperty(required = false, value = "revisions_checked")
    public void setTotalRevisionsChecked(long totalRevisionsChecked) {
        this.totalRevisionsChecked = totalRevisionsChecked;
    }

    @Override
    public String getSourceDatabaseName() {
        return sourceDatabase;
    }

    @JsonProperty(required = false, value = "source")
    public void setSourceDatabase(String sourceDatabase) {
        this.sourceDatabase = sourceDatabase;
    }

    @Override
    public String getTargetDatabaseName() {
        return targetDatabase;
    }

    @JsonProperty(required = false, value = "target")
    public void setTargetDatabase(String targetDatabase) {
        this.targetDatabase = targetDatabase;
    }

    @Override
    public long getSourceSequenceId() {
        throw new UnsupportedOperationException("not supported by couchdb2");
    }

    public String getSourceSequenceId2() {
        return sourceSequenceId;
    }

    @JsonProperty(required = false, value = "source_seq")
    public void setSourceSequenceId2(String sourceSequenceId) {
        this.sourceSequenceId = sourceSequenceId;
    }

    @Override
    public long getCheckpointedSourceSequenceId() {
        throw new UnsupportedOperationException("not supported by couchdb2");
    }

    public String getCheckpointedSourceSequenceId2() {
        return checkpointedSourceSequenceId;
    }

    @JsonProperty(required = false, value = "checkpointed_source_seq")
    public void setCheckpointedSourceSequenceId2(String checkpointedSourceSequenceId) {
        this.checkpointedSourceSequenceId = checkpointedSourceSequenceId;
    }

    @Override
    public Long getCheckpointInterval() {
        return checkpointInterval;
    }

    @JsonProperty(required = false, value = "checkpoint_interval")
    public void setCheckpointInterval(Long checkpointInterval) {
        this.checkpointInterval = checkpointInterval;
    }
}
