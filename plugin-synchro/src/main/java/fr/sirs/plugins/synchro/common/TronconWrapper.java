package fr.sirs.plugins.synchro.common;

import fr.sirs.core.model.Objet;
import fr.sirs.core.model.TronconDigue;
import java.util.Optional;
import org.apache.sis.util.ArgumentChecks;

/**
 *
 * @author Matthieu Bastianelli (Geomatys)
 */

class TronconWrapper {
    private final Object document;
    protected final Optional<String> tronconId;

    TronconWrapper(final Object document) {
        ArgumentChecks.ensureNonNull("document", document);
        this.document = document;
         this.tronconId = tryFindTronçon(document);
    }

    TronconWrapper(final Object document, final Optional<String> tronconId) {
        ArgumentChecks.ensureNonNull("document", document);
        this.document = document;
         this.tronconId = tronconId;
    }

    TronconWrapper(final TronconWrapper tronconWrapper) {
        this.document = tronconWrapper.getDocument();
        this.tronconId = tronconWrapper.getTronconId();
    }

    /**
     * Try to find the id of the "troncon" encompassing the the inpud document.
     *
     * This method currently only search if the document is an instance of
     * {@link Objet} and have a {@link Objet#parent}. It would be improve in
     * futur developments.
     *
     * @param document
     * @return
     */
    static Optional<String> tryFindTronçon(final Object document) {

        if (document instanceof TronconDigue) {
            return Optional.of(((TronconDigue) document).getId()); //Normally useless as photo can't be associated with 'tronçons'
        } else if (document instanceof Objet) {
           return Optional.ofNullable(((Objet) document).getForeignParentId() );
        } else {
           return Optional.empty();
        }
    }

    public final Optional<String> getTronconId() {
        return tronconId;
    }

    public final Object getDocument() {
        return document;
    }



}
