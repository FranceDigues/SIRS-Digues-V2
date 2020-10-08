package fr.sirs.util;

import fr.sirs.core.component.DatabaseRegistry;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javafx.concurrent.Task;
import org.ektorp.ReplicationStatus;
import org.ektorp.ReplicationTask;

/**
 * A task to manually query synchronisation of two databases. The aim is to
 * provide a way to ensure full replication is performed at a given instant. To
 * do so, we stop all replications running on current database, then trigger a
 * non continuous replication, to ensure all changes are processed. After that,
 * if user configured it, we launch back a continuous synchronisation between the
 * databases.
 *
 * Note : This task runs until the non-continuous replications are done, which
 * allow to report synchronisation progress.
 *
 * @author Alexis Manin (Geomatys)
 */
public class SynchroTask extends Task<Void> {

    final DatabaseRegistry registry;
    final String localDb;
    final String distant;

    final boolean launchContinuous;

    /**
     *
     * @param registry Database handle.
     * @param localDb Name of the local database to use for synchronisation.
     * @param distant URL of the distant database to use for synchronisation.
     * @param launchContinuous If we should resume a continuous synchronisation
     * AFTER the non continuous one has been performed.
     */
    public SynchroTask(DatabaseRegistry registry, String localDb, String distant, boolean launchContinuous) {
        this.registry = registry;
        this.localDb = localDb;
        this.distant = distant;
        this.launchContinuous = launchContinuous;
    }

    @Override
    protected Void call() throws Exception {
        updateTitle("Synchronisation");
        updateMessage("Arrêt des synchronisation en cours sur la base locale");
        registry.cancelAllSynchronizations(localDb);

        updateMessage("Réplication bilatérale");
        final List<ReplicationStatus> tasks = registry.synchronizeSirsDatabases(distant, localDb, false);

        final Set<String> taskIds = tasks.stream()
                .map(ReplicationStatus::getId)
                .collect(Collectors.toSet());

        boolean stillRunning = true;
        while (stillRunning && !isCancelled()) {
            synchronized (this) {
                wait(500);
            }

            final List<ReplicationTask> running = registry.getReplicationTasks()
                    .filter(t -> taskIds.contains(t.getReplicationId()))
                    .collect(Collectors.toList());

            stillRunning = !running.isEmpty();
            if (stillRunning) {
                running.stream()
                        .mapToInt(ReplicationTask::getProgress)
                        .average()
                        .ifPresent(progress -> updateProgress(progress, 100));
            }
        }

        if (isCancelled()) {
            for (final ReplicationStatus status : tasks) {
                registry.cancelCopy(status);
            }

        } else {
            if (launchContinuous) {
                updateMessage("Relance la synchronisation continue");
                registry.synchronizeSirsDatabases(distant, localDb, true);
            }
        }

        return null;
    }
}
