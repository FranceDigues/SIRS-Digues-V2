package fr.sirs.plugins.synchro.common;

import fr.sirs.SIRS;
import fr.sirs.core.model.AvecBornesTemporelles;
import fr.sirs.core.model.SIRSFileReference;
import java.nio.file.Files;
import java.time.LocalDate;

/**
 *
 * @author Alexis Manin (Geomatys)
 */
public class DocumentUtilities {

    public static boolean isFileAvailable(final SIRSFileReference fileRef) {
        final String chemin = fileRef.getChemin();
        return chemin != null &&
                !chemin.trim().isEmpty() &&
                Files.isRegularFile(SIRS.getDocumentAbsolutePath(fileRef));
    }

    public static boolean intersectsDate(final AvecBornesTemporelles target, final LocalDate dateFilter) {
        final LocalDate start = target.getDate_debut();
        final LocalDate end = target.getDate_fin();
        if (start == null && end == null)
            return false;
        else if (start == null) {
            return end.isEqual(dateFilter) || end.isAfter(dateFilter);
        } else if (end == null) {
            return start.isEqual(dateFilter) || start.isBefore(dateFilter);
        } else {
            return (start.isEqual(dateFilter) || start.isBefore(dateFilter)) &&
                    (end.isEqual(dateFilter) || end.isAfter(dateFilter));
        }
    }
}
