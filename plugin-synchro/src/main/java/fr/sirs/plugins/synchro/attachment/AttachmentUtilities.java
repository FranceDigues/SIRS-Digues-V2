package fr.sirs.plugins.synchro.attachment;

import fr.sirs.SIRS;
import fr.sirs.core.model.Element;
import fr.sirs.core.model.SIRSFileReference;
import fr.sirs.plugins.synchro.common.PhotoAndTroncon;
import fr.sirs.util.property.DocumentRoots;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javafx.concurrent.Task;
import javax.activation.MimetypesFileTypeMap;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import org.apache.sis.util.ArgumentChecks;
import org.ektorp.AttachmentInputStream;
import org.ektorp.CouchDbConnector;
import org.ektorp.DocumentNotFoundException;
import static org.ektorp.util.Documents.setRevision;
import org.geotoolkit.gui.javafx.util.TaskManager;
import org.geotoolkit.image.io.XImageIO;

/**
 *
 * @author Alexis Manin (Geomatys)
 */
public class AttachmentUtilities {

    private static final double MAX_IMAGE_SIZE = 1e6;

    private static final List<Function<Path, String>> MIME_STRATEGIES = Arrays.asList(
            AttachmentUtilities::cTypeFromNio,
            AttachmentUtilities::cTypeFromURL,
            AttachmentUtilities::cTypeFromJavax
    );

    /**
     * Create a data stream from given file. The stream is specialized for
     * attachment creation on CouchDB server.
     *
     * @param identifier The identifier to affect to created attachment. If null,
     * the file name will be used as identifier.
     * @param input Path to the file to access/send to CouchDB.
     * @return A stream ready to be sent to CouchDB.
     * @throws IOException If we cannot access given file, or we cannot guess its
     * mime type, or if it's an empty file.
     */
    public static AttachmentInputStream create(String identifier, final Path input) throws IOException {
        final long fileSize = Files.size(input);
        if (fileSize <= 0) {
            throw new IOException("Cannot create an attachment from empty file : " + input);
        }
        final String mime = guessMimeType(input)
                .orElseThrow(() -> new IOException("Cannot find a mime-type for " + input));
        if (identifier == null) {
            identifier = input.getFileName().toString();
        }
        final InputStream fileInput = Files.newInputStream(input);
        try {
            return new AttachmentInputStream(identifier, fileInput, mime, fileSize);
        } catch (Exception e) {
            try {
                fileInput.close();
            } catch (Exception bis) {
                e.addSuppressed(bis);
            }
            throw e;
        }
    }

    /**
     * Try to deduce the mime-type of the given file.
     *
     * @param input The file (must not be a directory, read access is needed) to
     * find mime-type for.
     * @return A string representing the deduced mime-type.
     * @throws java.io.IOException If an error occured while analysing given file.
     */
    public static Optional<String> guessMimeType(final Path input) throws IOException {
        String cType = null;
        final Iterator<Function<Path, String>> tries = MIME_STRATEGIES.iterator();
        while (cType == null && tries.hasNext()) {
            try {
                cType = tries.next().apply(input);
            } catch (UncheckedIOException e) {
                throw e.getCause();
            }
        }

        return Optional.ofNullable(cType);
    }

    private static String cTypeFromNio(final Path input) {
        try {
            return Files.probeContentType(input);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private static String cTypeFromURL(final Path input) {
        try (final InputStream stream = Files.newInputStream(input)) {
            return URLConnection.guessContentTypeFromStream(stream);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private static String cTypeFromJavax(final Path input) {
        return MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(input.toFile());
    }

    public static boolean isAvailable(final CouchDbConnector connector, final SIRSFileReference document) {
        try (final AttachmentInputStream in = AttachmentUtilities.download(connector, document)) {
            return true;
        } catch (DocumentNotFoundException e) {
            SIRS.LOGGER.log(Level.FINEST, e, () -> String.format("No attachment available for document%n%s", document));
        } catch (IOException e) {
            SIRS.LOGGER.log(Level.WARNING, "An http resource cannot be closed.", e);
        }

        return false;
    }

    private static AttachmentInputStream download(final CouchDbConnector connector, final SIRSFileReference document) {
        final Element realDoc = document.getCouchDBDocument();
        if (realDoc == null) {
            throw new DocumentNotFoundException("No Root document for given object.");
        }

        final String revision = getRevision(realDoc)
                .orElseThrow(() -> new DocumentNotFoundException("No valid reference found."));
        return AttachmentUtilities.download(connector, new AttachmentReference(realDoc.getId(), revision, document.getId()));
    }

    private static AttachmentInputStream download(final CouchDbConnector connector, final AttachmentReference ref) {
        ArgumentChecks.ensureNonNull("Attachment reference", ref);
        ArgumentChecks.ensureNonEmpty("Document id", ref.getParentId());
        ArgumentChecks.ensureNonEmpty("Attachment id", ref.getId());

        final AttachmentInputStream stream;
        if (ref.getRevision() == null || ref.getRevision().isEmpty()) {
            stream = connector.getAttachment(ref.getParentId(), ref.getId());
        } else {
            stream = connector.getAttachment(ref.getParentId(), ref.getId(), ref.getRevision());
        }

        return stream;
    }

    public static void download(final CouchDbConnector connector, final SIRSFileReference doc, final Path destination) throws IOException {
        final Path tmpFile = Files.createTempFile(doc.getId(), ".img");
        try (final AttachmentInputStream stream = AttachmentUtilities.download(connector, doc)) {
            Files.copy(stream, tmpFile, StandardCopyOption.REPLACE_EXISTING);
            Files.move(tmpFile, destination);
            final Optional<Path> root = DocumentRoots.getRoot(doc);
            synchronized (doc.getCouchDBDocument()) {
                if (root.isPresent()) {
                    doc.setChemin(root.get().relativize(destination).toString());
                } else {
                    doc.setChemin(destination.toString());
                }
                connector.update(doc.getCouchDBDocument());
            }
        } finally {
            Files.deleteIfExists(tmpFile); // In case of error, we clear the unmoved temporary file.
        }
    }

    /**
     * Send a file to CouchDB server as an attachment.
     *
     * @param connector Database connection to use for upload.
     * @param ref The reference object specifies on which document we should
     * attach the uploaded file. It is mandatory, and the parent id must not be
     * null. However, if no identifier is specified for the attachment, we will
     * use the name of the provided file.
     * @param data The file to send content to CouchDb.
     * @return The revision created.
     * @throws IOException If an error occurs while analyzing or sending given
     * file content.
     */
    private static String upload(final CouchDbConnector connector, final AttachmentReference ref, final Path data) throws IOException {
        ArgumentChecks.ensureNonNull("Attachment reference", ref);
        ArgumentChecks.ensureNonNull("File to upload", data);

        final long fileSize = Files.size(data);
        Path tmpFile = null;boolean useTmpFile = false;
        int limitSize = 4096;
        // HACK : There's a problem with PouchDB (js db for mobile devices):
        // it does not support attachments superior to 1MB. So, as a
        // workaround, we try to compress all images over this size before
        // sending them.
        if (fileSize > MAX_IMAGE_SIZE) {
            final String outputName = "subsample"+UUID.randomUUID().toString();
            int retry = 0;
            // Make it progressively. We subsample our image until it reach
            // expected size.
            do {
                limitSize /= 2;
                final Optional<Path> subsampled = subsample(data, outputName, limitSize);
                useTmpFile = subsampled.isPresent();
                tmpFile = subsampled.orElse(null);
            } while (++retry < 3 && useTmpFile && Files.size(tmpFile) > MAX_IMAGE_SIZE);
        }

        try (final AttachmentInputStream ath = AttachmentUtilities.create(ref.getId(), useTmpFile? tmpFile : data)) {
            if (ref.getRevision() == null || ref.getRevision().isEmpty()) {
                return connector.createAttachment(ref.getParentId(), ath);
            } else {
                return connector.createAttachment(ref.getParentId(), ref.getRevision(), ath);
            }
        } finally {
            if (tmpFile != null) {
                Files.deleteIfExists(tmpFile);
            }
        }
    }

    public static void upload(final CouchDbConnector connector, final SIRSFileReference data) throws IOException {
        final Path file = SIRS.getDocumentAbsolutePath(data);
        final Element e = data.getCouchDBDocument();
        if (e == null)
            throw new IOException("Cannot upload an attachment for a document which does not exists in database.");
        /* If multiple uploads are launched on the same document, there would be
         * a revision conflict. To avoid so, we synchronize uploads by document.
         */
        synchronized (e) {
            String revision = getRevision(e)
                .orElseThrow(() -> new IOException("Cannot find a valid revision to upload data"));
            revision = upload(connector, new AttachmentReference(e.getId(), revision, data.getId()), file);
            setRevision(e, revision);
        }
    }

    private static Optional<String> getRevision(final Object input) {
        try {
            return Optional.ofNullable(input.getClass().getMethod("getRevision").invoke(input))
                    .map(Object::toString);
        } catch (SecurityException|ReflectiveOperationException ex) {
            SIRS.LOGGER.log(Level.FINE, "No revision available", ex);
        }

        return Optional.empty();
    }

    public static void delete(final CouchDbConnector connector, final SIRSFileReference ref) {
        final Element e = ref.getCouchDBDocument();
        if (e == null)
            throw new IllegalArgumentException("Cannot delete an attachment for a document which does not exists in database.");
        /* If multiple modifications are launched on the same document, there
         * would be a revision conflict. To avoid so, we synchronize by document.
         */
        synchronized (e) {
            String revision = getRevision(e).orElseThrow(() -> new IllegalArgumentException("Cannot find a valid revision to delete attachment."));
            revision = connector.deleteAttachment(e.getId(), revision, ref.getId());
            // HACK : deleting attachments from database does not update in-memory
            // attachment map, so we have to do it manually, because if the object
            // is used and send back to database, we'll be in trouble.
            try {
                final Object obj = e.getClass().getMethod("getAttachments").invoke(e);
                if (obj instanceof Map) {
                    ((Map)obj).remove(ref.getId());
                }
            } catch (ReflectiveOperationException ex) {
                SIRS.LOGGER.log(Level.WARNING, "Cannot update attachments", ex);
            }
            setRevision(e, revision);
        }
    }

    public static long size(final CouchDbConnector connector, final SIRSFileReference doc) {
        final AttachmentInputStream in = download(connector, doc);
        try {
            return in.getContentLength();
        } finally {
            // Use finally here, because bad closing is not a real problem as long
            // as we've got the size.
            try {
                in.close();
            } catch (IOException ex) {
                SIRS.LOGGER.log(Level.WARNING, "A resource cannot be closed properly.", ex);
            }
        }
    }
        public static long sizeAndTroncon(final CouchDbConnector connector, final SIRSFileReference doc) {
        final AttachmentInputStream in = download(connector, doc);
        try {
            return in.getContentLength();
        } finally {
            // Use finally here, because bad closing is not a real problem as long
            // as we've got the size.
            try {
                in.close();
            } catch (IOException ex) {
                SIRS.LOGGER.log(Level.WARNING, "A resource cannot be closed properly.", ex);
            }
        }
    }

    public static Task<Map.Entry<Long, Long>> estimateSize(final CouchDbConnector connector, final Stream<? extends SIRSFileReference> attachments) {
        return new TaskManager.MockTask("Estimation...", () -> {
            final Thread th = Thread.currentThread();
            try {
                final AtomicLong count = new AtomicLong();
                final long size = attachments
                        .peek(ath -> {
                            if (th.isInterrupted())
                                throw new RuntimeException(new InterruptedException());
                        })
                        .peek(ath -> count.incrementAndGet())
                        .mapToLong(ath -> AttachmentUtilities.size(connector, ath))
                        .sum();
                return new AbstractMap.SimpleEntry<>(count.get(), size);
            } catch (RuntimeException e) {
                if (e.getCause() instanceof InterruptedException) {
                    throw (InterruptedException) e.getCause();
                } else {
                    throw e;
                }
            }
        });
    }

    public static Task<AttachmentsSizeAndTroncons> estimateSizeAndTroncons(final CouchDbConnector connector, final Stream<? extends PhotoAndTroncon> attachments) {
        return new TaskManager.MockTask("Estimation...", () -> {
            final Thread th = Thread.currentThread();
            try {
                final List<PhotoAndTroncon> photosWithTronçon = attachments
                        .peek(ath -> {
                            if (th.isInterrupted())
                                throw new RuntimeException(new InterruptedException());
                        })
                        .collect(Collectors.toList());

                final long count = photosWithTronçon.size();
                final long size  = photosWithTronçon.stream()
                        .mapToLong(ath -> AttachmentUtilities.size(connector, ath.getPhoto()))
                        .sum();
                final Set<String> tronconIds = photosWithTronçon.stream()
                        .map(pNt -> pNt.getTronconId().orElse(null))
                        .collect(Collectors.toSet());
                return new AttachmentsSizeAndTroncons(count, size, tronconIds);

            } catch (RuntimeException e) {
                SIRS.LOGGER.log(Level.WARNING, "Exception lors de l'estimation de la taille des photos à importer et des tronçons d'appartenance.", e);
                if (e.getCause() instanceof InterruptedException) {
                    throw (InterruptedException) e.getCause();
                } else {
                    throw e;
                }
            }
        });
    }

    /**
     * Subsample input image to ensure its dimension are less than given size.
     * This method will keep aspect ratio. Note that we'll try to write jpeg
     * image if no alpha canal is detected. Otherwise, png format is selected.
     * Also, given limit size won't be perfectly fit. We'll determine a subsample
     * parameter allowing to reduce image size under given limit size.
     *
     * @param source The source image to subsample.
     * @param outName Name of the output image, without extension.
     * @param limitSize The maximum width or height autorized.
     * @return If we were able to write the image, path to the subsampled image
     * file. Otherwise, an empty optional. Write failure can be caused by
     * missing image driver (currently,  png or jpeg used), or if an error
     * happens while writing.
     */
    private static Optional<Path> subsample(final Path source, final String outName, final int limitSize) {
        try {
            final ImageReader reader = XImageIO.getReader(source, false, true);
            int subsample = 1;
            final int sourceWidth = reader.getWidth(0);
            final int sourceHeight = reader.getHeight(0);
            if (sourceWidth > limitSize || sourceHeight > limitSize) {
                final int subsampleX = (sourceWidth + limitSize - 1) / limitSize;
                final int subsampleY = (sourceHeight + limitSize -1) / limitSize;
                subsample = Math.max(subsampleX, subsampleY);
            }
            final ImageReadParam readParam = new ImageReadParam();
            readParam.setSourceSubsampling(subsample, subsample, 0, 0);
            final BufferedImage img = reader.read(0, readParam);
            final String outFormat;
            final Path destination;
            if (img.getColorModel().hasAlpha()) {
                outFormat = "PNG";
                destination = source.getParent().resolve(outName+".png");
            } else {
                outFormat = "JPEG";
                destination = source.getParent().resolve(outName+".jpg");
            }
            final boolean isPresent = ImageIO.write(img, outFormat, destination.toFile());
            if (isPresent) {
                return Optional.of(destination);
            } else {
                return Optional.empty();
            }
        } catch (Exception e) {
            SIRS.LOGGER.log(Level.WARNING, "Cannot subsample image", e);
        }

        return Optional.empty();
    }
}
