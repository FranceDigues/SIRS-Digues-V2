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
package fr.sirs.core.authentication;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import fr.sirs.core.SirsCore;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import org.apache.sis.util.ArgumentChecks;
import org.geotoolkit.internal.GeotkFX;

/**
 * Registry which contains connection information users has already provided.
 * @author Alexis Manin (Geomatys)
 */
public class AuthenticationWallet {

    private static AuthenticationWallet INSTANCE;

    private final Path walletPath = SirsCore.CONFIGURATION_PATH.resolve("authWallet.json");

    private final ObservableMap<String, Entry> wallet = FXCollections.observableMap(new HashMap<String, Entry>());
    private final ReentrantReadWriteLock walletLock = new ReentrantReadWriteLock();

    private AuthenticationWallet() throws IOException {

        boolean isEmpty = true;
        boolean createNewFile = true;
        if (Files.isRegularFile(walletPath)) {
            // check if file is empty, because jackson explode on empty files.
            BasicFileAttributes pathAttr = Files.getFileAttributeView(walletPath, BasicFileAttributeView.class).readAttributes();
            if (pathAttr.size() > 0) {
                createNewFile = false;
                isEmpty = false;
            }
        }

        if (createNewFile) {
            try (final InputStream resourceAsStream = AuthenticationWallet.class.getResourceAsStream("/fr/sirs/core/authentication/defaultWallet.json")) {
                Files.copy(resourceAsStream, walletPath, StandardCopyOption.REPLACE_EXISTING);
                isEmpty = false;
            } catch (IOException e) {
                isEmpty = true; // File is corrupted. Mark it as empty.
                SirsCore.LOGGER.log(Level.WARNING, "");
                if (!Files.isRegularFile(walletPath))
                    Files.createFile(walletPath);
            }
        }

        // Read existing entries from configuration file.
        if (!isEmpty) {
            // Does not erase existing file if an error occurs, to allow password backup manually.
            try (final InputStream walletStream = Files.newInputStream(walletPath, StandardOpenOption.READ)) {
                final ObjectMapper mapper = new ObjectMapper();
                ObjectReader reader = mapper.reader(Entry.class);
                JsonNode root = mapper.readTree(walletStream);
                if (root.isArray()) {
                    Iterator<JsonNode> iterator = root.iterator();
                    while (iterator.hasNext()) {
                        Entry entry = reader.readValue(iterator.next());
                        wallet.put(toServiceId(entry), entry);
                    }
                }
            }
        }

        /*
         * When cached wallet is modified, we update wallet on file system. We can
         * use only a read lock here, because file is only read at initialization.
         */
        wallet.addListener((MapChangeListener.Change<? extends String, ? extends Entry> change) -> {
            walletLock.readLock().lock();
            try (final OutputStream walletStream = Files.newOutputStream(walletPath, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
                new ObjectMapper().writeValue(walletStream, wallet.values());
            } catch (IOException e) {
                SirsCore.LOGGER.log(Level.WARNING, "Password wallet cannot be updated !", e);
            } finally {
                walletLock.readLock().unlock();
            }
        });
    }

    public Entry get(final URL service) {
        walletLock.readLock().lock();
        try {
            Entry found = wallet.get(toServiceId(service));
            if (found == null) {
                found = wallet.get(toServiceId("*", service.getPort()));
            }
            return found;
        } finally {
            walletLock.readLock().unlock();
        }
    }

    public Entry get(final String host, final int port) {
        walletLock.readLock().lock();
        try {
            Entry found = wallet.get(toServiceId(host, port));
            if (found == null) {
                found = wallet.get(toServiceId("*", port));
            }
            return found;
        } finally {
            walletLock.readLock().unlock();
        }
    }

    public Entry put(final Entry authenticationInfo) {
        // Check if it doesn't exist already, to avoid useless update.
        walletLock.writeLock().lock();
        try {
            if (authenticationInfo == null)
                return put(authenticationInfo);
            final String serviceId = toServiceId(authenticationInfo);
            final Entry existing = wallet.get(serviceId);
            if (existing == null || !existing.equals(authenticationInfo)) {
                return wallet.put(serviceId, authenticationInfo);
            } else {
                return existing;
            }
        } finally {
            walletLock.writeLock().unlock();
        }
    }

    /**
     * Return a view of wallet entries. The view is backed by the wallet, so every
     * change on the wallet will be applied on returned list.
     *
     * IMPORTANT : To protect wallet from inconsiderate changes on the list, returned
     * list is unmodifiable.
     *
     * @return A mirror of wallet values.
     */
    public ObservableList<Entry> values() {
        walletLock.readLock().lock();
        try {
            final ObservableList follower = FXCollections.observableArrayList(wallet.values());
            wallet.addListener(new FollowListener(wallet, follower));
            return FXCollections.unmodifiableObservableList(follower);
        } finally {
            walletLock.readLock().unlock();
        }
    }

    public boolean remove(final Entry entry) {
        walletLock.writeLock().lock();
        try {
            return wallet.remove(toServiceId(entry), entry);
        } finally {
            walletLock.writeLock().unlock();
        }
    }

    public Entry removeForAddress(final URL service) {
        walletLock.writeLock().lock();
        try {
            return wallet.remove(toServiceId(service));
        } finally {
            walletLock.writeLock().unlock();
        }
    }

    /**
     *
     * @return Default registered password container, or null if an error occurred while initializing it.
     */
    public static AuthenticationWallet getDefault() {
        if (INSTANCE == null) {
            try {
                INSTANCE = new AuthenticationWallet();
            } catch (IOException e) {
                SirsCore.LOGGER.log(Level.WARNING, "Password wallet cannot be initialized !", e);
                Runnable r = () -> GeotkFX.newExceptionDialog("Impossible d'initialiser le portefeuille de mots de passe.", e).show();
                if (Platform.isFxApplicationThread()) {
                    r.run();
                } else {
                    Platform.runLater(r);
                }
            }
        }
        return INSTANCE;
    }

    public static String toServiceId(final URL url) {
        int port = url.getPort();
        if (url.getPort() < 0)
            port = url.getDefaultPort(); // If even default port is -1, we let it as is, no need to return a wrong entry.
        return url.getHost()+":"+port;
    }

    public static String toServiceId(final String host, final int port) {
        return host+":"+port;
    }

    public static String toServiceId(final Entry entry) {
        return entry.host+":"+entry.port;
    }


    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class Entry implements Cloneable {
        public String host;
        public int port;
        public String login;
        public boolean checked = false;

        @JsonSerialize(using=PasswordSerializer.class)
        @JsonDeserialize(using=PasswordDeserializer.class)
        public String password;

        public Entry(){};

        public Entry(final String host, final int port, final String login, final String password) {
            this.host = host;
            this.port = port;
            this.login = login;
            this.password = password;
        }

        public Entry(final URL service, final String login, final String password) {
            host = service.getHost();
            port = service.getPort();
            if (port < 0)
                port = service.getDefaultPort(); // If even default port is -1, we let it as is, no need to return a wrong entry.
            this.login = login;
            this.password = password;
        }

        public Entry(final Entry toClone) {
            host = toClone.host;
            port = toClone.port;
            login = toClone.login;
            password = toClone.password;
        }

        @Override
        public Entry clone() {
            return new Entry(this);
        }

        @Override
        public int hashCode() {
            return 31 * (31 * Objects.hashCode(this.host) + port) + Objects.hashCode(this.login);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            final Entry other = (Entry) obj;
            if (!Objects.equals(this.host, other.host))
                return false;
            if (this.port != other.port)
                return false;
            if (!Objects.equals(this.login, other.login))
                return false;
            return Objects.equals(this.password, other.password);
        }
    }

    /**
     * A listener which report all additions/suppressions which happen in a map
     * to the given list.
     * The listener does not register itself automatically, but it will unregister
     * when target list will be garbage collected.
     */
    private static class FollowListener implements MapChangeListener<String, Entry> {

        private final WeakReference<ObservableMap> source;
        private final WeakReference<List> follower;

        public FollowListener(final ObservableMap<String, Entry> toFollow, final List follower) {
            ArgumentChecks.ensureNonNull("Map to listen on", toFollow);
            this.source = new WeakReference<>(toFollow);
            this.follower = new WeakReference<>(follower);
        }

        @Override
        public void onChanged(Change<? extends String, ? extends Entry> change) {
            final List tmpFollower = follower.get();
            // follower has been detroyed, no need to listen anymore
            if (tmpFollower == null) {
                ObservableMap tmpSource = source.get();
                if (tmpSource != null) {
                    tmpSource.removeListener(this);
                }
            } else {
                if (change.wasAdded()) {
                    tmpFollower.add(change.getValueAdded());
                } else if (change.wasRemoved()) {
                    tmpFollower.remove(change.getValueRemoved());
                }
            }
        }
    }
}
