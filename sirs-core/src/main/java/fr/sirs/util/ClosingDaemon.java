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
package fr.sirs.util;

import fr.sirs.core.SirsCore;
import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.WeakHashMap;
import java.util.logging.Level;

/**
 * A daemon which will run in background, closing all resources submitted.
 * Note : Give to the daemon an object to listen on, and aproperty on an {@link AutoCloseable}
 * object to close when the first object dies. That's all !
 *
 * @author Alexis Manin (Geomatys)
 */
public class ClosingDaemon {

    private static final ClosingDaemon INSTANCE = new ClosingDaemon();

    private final ReferenceQueue phantomQueue = new ReferenceQueue();

    public static final WeakHashMap<Object, PhantomReference> referenceCache = new WeakHashMap<>();

    private ClosingDaemon() {
        final Thread closer = new Thread(() -> {
            final Thread t = Thread.currentThread();
            while (!t.isInterrupted()) {
                try {
                    Reference removed = phantomQueue.remove();
                    if (removed instanceof AutoCloseable) {
                        ((AutoCloseable)removed).close();
                    }

                } catch (InterruptedException e) {
                    SirsCore.LOGGER.log(Level.WARNING, "Resource closer has been interrupted ! It could cause memory leaks.");
                    t.interrupt();
                    return;
                } catch (Exception e) {
                    SirsCore.LOGGER.log(Level.WARNING, "Some resource cannot be released. It's likely to cause memory leaks !");
                }
            }
        });
        closer.setName("SIRS resource disposer");
        closer.setDaemon(true);
        closer.start();
    }

    public static void watchResource(final Object toWatch, final AutoCloseable closeable) {
        referenceCache.put(toWatch, new ResourceReference(toWatch, INSTANCE.phantomQueue, closeable));
    }

    private static class ResourceReference extends PhantomReference implements AutoCloseable {

        private final AutoCloseable closeable;
        private ResourceReference(Object referent, ReferenceQueue q, AutoCloseable closeable) {
            super(referent, q);
            this.closeable = closeable;
        }

        @Override
        public void close() {
            try {
                if (closeable != null) {
                    closeable.close();
                }
            } catch (Exception e) {
                SirsCore.LOGGER.log(Level.WARNING, "A streamed CouchDB view result cannot be closed. It's likely to cause memory leaks.", e);
            }
        }
    }
}
