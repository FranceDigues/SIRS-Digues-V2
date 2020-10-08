package fr.sirs.plugins.synchro.attachment;

import java.util.Set;

/**
 *
 * @author Matthieu Bastianelli (Geomatys)
 */
public class AttachmentsSizeAndTroncons {

    private final long count;
    private final long size;
    private final Set<String> tronconIds;

//    public AttachmentsSizeAndTroncons() {
//        this.tronconIds = new HashSet<>();
//    }

    public AttachmentsSizeAndTroncons(final long count, final long size, final Set<String> tronconIds) {
        this.count = count;
        this.size = size;
        this.tronconIds = tronconIds;
    }

    public void addTroncon(final String id) {
        tronconIds.add(id);
    }

    public Set<String> getTronconIds() {
        return tronconIds;
    }
    
    public long getCount() {
        return count;
    }

    public long getSize() {
        return size;
    }

}
