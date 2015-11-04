package fr.sirs.util.odt;

import fr.sirs.core.InjectorCore;
import fr.sirs.core.SessionCore;
import fr.sirs.core.SirsCore;
import fr.sirs.core.component.Previews;
import fr.sirs.core.model.Element;
import fr.sirs.util.property.Reference;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.geometry.Insets;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.sis.util.ArgumentChecks;
import org.odftoolkit.odfdom.dom.element.text.TextUserFieldDeclElement;
import org.odftoolkit.odfdom.dom.element.text.TextUserFieldDeclsElement;
import org.odftoolkit.odfdom.dom.element.text.TextVariableDeclElement;
import org.odftoolkit.odfdom.dom.element.text.TextVariableDeclsElement;
import org.odftoolkit.odfdom.pkg.OdfElement;
import org.odftoolkit.simple.Document;
import org.odftoolkit.simple.TextDocument;
import org.odftoolkit.simple.common.field.AbstractVariableContainer;
import org.odftoolkit.simple.common.field.Field.FieldType;
import org.odftoolkit.simple.common.field.Fields;
import org.odftoolkit.simple.common.field.VariableField;
import org.odftoolkit.simple.style.MasterPage;
import org.odftoolkit.simple.style.StyleTypeDefinitions;

/**
 * Utility methods used to create ODT templates, or fill ODT templates.
 *
 * Note : For now, we do not use real ODT varaibles (see {@link VariableField})
 * in our templates, because iterating through does not look simple. We just use
 * text with special formatting.
 *
 * @author Alexis Manin (Geomatys)
 * @author Johann Sorel (Geomatys)
 */
public class ODTUtils {

    public static final String CLASS_KEY = "The.Sirs.Class";

    private static final String NOTE_MODELE_FICHE = "Note : Ci-dessous se trouve"
            + " la liste des champs utilisés par SIRS-Digues lors de la création"
            + " d'une fiche. Vous pouvez compléter ce modèle (Ajout de contenu,"
            + " mise en forme) et déplacer / copier les variables (les textes"
            + " surlignés de gris) où vous voulez dans le document. Elles seront"
            + " automatiquement remplacés à la génération du rapport.";

    private static Field USER_FIELD;
    private static Field SIMPLE_FIELD;

    /**
     * Generate a new template which will put "variables" into it to be easily
     * replaced when creating a report.
     *
     * Note : For the moment, we do not use real {@link VariableField}, because
     * iterating through does not look simple. We just use text with special
     * formatting.
     *
     * @param title Title for the document to create.
     * @param properties A map whose keys are properties to put in template, and values are titles for them.
     * @return A new template document with prepared variables.
     */
    public static TextDocument newSimplePropertyModel(final String title, final Map<String, String> properties) throws Exception {
        final TextDocument result = TextDocument.newTextDocument();
        result.addParagraph(title).applyHeading();
        result.addParagraph(NOTE_MODELE_FICHE);
        for (final Map.Entry<String, String> entry : properties.entrySet()) {
            appendUserVariable(result, entry.getKey(), entry.getValue(), entry.getValue());
        }

        return result;
    }

    /**
     * Save given class reference in input document, setting it as the class to
     * use when generating a report.
     * @param input The document to modify.
     * @param targetClass Class to put, or null to remove information from document.
     */
    public static void setTargetClass(final TextDocument input, final Class targetClass) {
        ArgumentChecks.ensureNonNull("Input document", input);
        if (targetClass == null) {
            removeVariable(input.getVariableFieldByName(CLASS_KEY));
        } else {
            Fields.createUserVariableField(input, CLASS_KEY, targetClass.getCanonicalName());
        }
    }

    /**
     * Analyze input document to find which type of object it is planned for.
     * @param source Source document to analyze.
     * @return The class which must be used for report creation, or null if we
     * cannot find information in given document.
     * @throws ReflectiveOperationException If we fail analyzing document.
     */
    public static Class getTargetClass(final TextDocument source) throws ReflectiveOperationException {
        final VariableField var = source.getVariableFieldByName(CLASS_KEY);
        if (var != null && FieldType.USER_VARIABLE_FIELD.equals(var.getFieldType())) {
            Object value = getVariableValue(var);
            if (value instanceof String) {
                return Thread.currentThread().getContextClassLoader().loadClass((String) value);
            }
        }

        return null;
    }

    /**
     * Replace all variables defined in input document template with the one given
     * in parameter. For variables in document also present in given property mapping,
     * they're left as is. New properties to be put are added in paragraphs at the end
     * of the document.
     *
     * @param source Document template to modify.
     * @param properties Properties to set in given document. Keys are property names,
     * and values are associated title. If null, all user variables will be deleted
     * from input document.
     */
    public static void setVariables(final TextDocument source, final Map<String, String> properties) {
        Map<String, VariableField> vars = findAllVariables(source, VariableField.VariableType.USER);

        if (properties != null) {
            for (final Map.Entry<String, String> entry : properties.entrySet()) {
                if (vars.remove(entry.getKey()) == null) {
                    appendUserVariable(source, entry.getKey(), entry.getValue(), entry.getValue());
                }
            }
        }

        for (final VariableField var : vars.values()) {
            removeVariable(var);
        }
    }

    /**
     * Remove given variable from its holding document.
     * @param var the variable field to get rid of.
     * @return true if we succeeded, false otherwise.
     */
    public static boolean removeVariable(final VariableField var) {
        if (var == null)
            return false;
        final OdfElement varElement = var.getOdfElement();
        if (varElement == null)
            return false;
        return varElement.getParentNode().removeChild(varElement) != null;
    }

    /**
     * Fill given template with data originating from candidate object.
     *
     * @param templateData Source template to fill.
     * @param candidate The object to get data from to fill given template.
     * @return A Document formatted as input template, but filled with given
     * data.
     * @throws java.lang.Exception If we cannot load template, or element data cannot be read or converted.
     */
    public static TextDocument reportFromTemplate(final InputStream templateData, final Element candidate) throws Exception {
        return reportFromTemplate(TextDocument.loadDocument(templateData), candidate);
    }

    /**
     * Fill given template with data originating from candidate object.
     *
     * @param template Source template to fill.
     * @param candidate The object to get data from to fill given template.
     * @return A Document formatted as input template, but filled with given
     * data.
     * @throws java.beans.IntrospectionException If input candidate cannot be analyzed.
     * @throws java.lang.ReflectiveOperationException If we fail reading candidate properties.
     */
    public static TextDocument reportFromTemplate(final TextDocument template, final Element candidate) throws IntrospectionException, ReflectiveOperationException {
        //final TextNavigation search = new TextNavigation(VARIABLE_SEARCH, document);

        // We iterate through input properties to extract all mappable attributes.
        final PropertyDescriptor[] descriptors = Introspector.getBeanInfo(candidate.getClass()).getPropertyDescriptors();
        final HashMap<String, Object> properties = new HashMap<>(descriptors.length);
        final Previews previews = InjectorCore.getBean(SessionCore.class).getPreviews();
        for (final PropertyDescriptor desc : descriptors) {
            final Method readMethod = desc.getReadMethod();
            if (readMethod == null) {
                continue; // Non readble attribute, skip.
            } else {
                readMethod.setAccessible(true);
            }

            // Check if we've got a real data or a link.
            final Reference ref = readMethod.getAnnotation(Reference.class);
            final Class<?> refClass;
            if (ref != null) {
                refClass = ref.ref();
            } else {
                refClass = null;
            }

            Object value = readMethod.invoke(candidate);
            if (refClass != null && (value instanceof String)) {
                value = previews.get((String) value).getLibelle();
            }
            properties.put(desc.getName(), value);
        }

        Object value;
        for (final Map.Entry<String, Object> entry : properties.entrySet()) {
            value = entry.getValue();
            VariableField var = template.getVariableFieldByName(entry.getKey());
            if (var != null) {
                var.updateField(value == null ? "N/A" : value.toString(), null);
            } else {
                SirsCore.LOGGER.fine("No variable found for name "+entry.getKey());
            }
        }

//        while (search.hasNext()) {
//            final Selection selection = search.nextSelection();
//            if (selection instanceof TextSelection) {
//                final TextSelection tSelection = (TextSelection) selection;
//                final Matcher matcher = VARIABLE_BUILDER.matcher(tSelection.getText());
//                if (matcher.matches()) {
//                    final String varName = matcher.group(1);
//                    final Object value = properties.get(varName);
//                    if (value != null) {
//                        tSelection.replaceWith(value.toString()); // TODO : better string conversion ?
//                    }
//                }
//            }
//        }

        return template;
    }

    /**
     * Find variable with given name into input document, and update its value with specified one.
     * If we cannot find a matching variable into given document, this method just returns.
     *
     * @param doc The document to search into.
     * @param varName Name of the variable to find.
     * @param newValue Value to put into found variable.
     */
    public static void findAndReplaceVariable(final TextDocument doc, final String varName, final Object newValue) {
        try {
            VariableField var = doc.getVariableFieldByName(varName);
            if (var != null) {
                var.updateField(newValue == null ? "N/A" : newValue.toString(), null);
            }
        } catch (IllegalArgumentException e) {
            // No variable found for given name.
        }
    }

    /**
     * Add a new variable in given document, and put a new paragraph containing
     * it at the end of the document.
     *
     * @param doc The document to add variable into.
     * @param varName Name of the variable to create. Should not be null.
     * @param value Default value to set to the variable. Null accepted.
     * @param text Text to put in the paragraph to create. If null, no paragraph
     * will be created, so variabl will not be displayed in document body.
     * @return The created variable.
     */
    public static VariableField appendUserVariable(final TextDocument doc, final String varName, final String value, final String text) {
        final VariableField field = Fields.createUserVariableField(doc, varName, value);
        if (text != null) {
            field.displayField(doc.addParagraph(text + " : ").getOdfElement());
        }
        return field;
    }

    /**
     * Search in input document for all declared variables of a given type.
     *
     * For algorithm, see {@link AbstractVariableContainer#getVariableFieldByName(java.lang.String) }
     *
     * @param source Document to search into.
     * @param type Type of variable to retrieve. If null, all type of variables
     * will be returned.
     * @return A map of all found variables. Keys are variable names, values are concrete variables. Never null, but can be empty.
     */
    public static Map<String, VariableField> findAllVariables(final TextDocument source, final VariableField.VariableType type) {
        final OdfElement variableContainer = source.getVariableContainerElement();
        final HashMap<String, VariableField> result = new HashMap<>();

        VariableField tmpField;
        // First, find all user variable methods.
        if (type == null || VariableField.VariableType.USER.equals(type)) {
            TextUserFieldDeclsElement userVariableElements = OdfElement.findFirstChildNode(TextUserFieldDeclsElement.class, variableContainer);
            if (userVariableElements != null) {
                TextUserFieldDeclElement userVariable = OdfElement.findFirstChildNode(TextUserFieldDeclElement.class, userVariableElements);
                Object value;
                while (userVariable != null) {
                    // really crappy...
                    value = getValue(userVariable);

                    // even crappier ...
                    tmpField = Fields.createUserVariableField(source, userVariable.getTextNameAttribute(), value.toString());
                    result.put(tmpField.getVariableName(), tmpField);

                    userVariable = OdfElement.findNextChildNode(TextUserFieldDeclElement.class, userVariable);
                }
            }
        }

        // then look for simple variables.
        if (type == null || VariableField.VariableType.SIMPLE.equals(type)) {
            TextVariableDeclsElement userVariableElements = OdfElement.findFirstChildNode(TextVariableDeclsElement.class, variableContainer);
            if (userVariableElements != null) {
                TextVariableDeclElement variable = OdfElement.findFirstChildNode(TextVariableDeclElement.class, userVariableElements);
                while (variable != null) {
                    tmpField = Fields.createSimpleVariableField(source, variable.getTextNameAttribute());
                    result.put(tmpField.getVariableName(), tmpField);
                    variable = OdfElement.findNextChildNode(TextVariableDeclElement.class, variable);
                }
            }
        }

        return result;
    }

    /**
     * Try to extract value from given field.
     *
     * IMPORTANT ! For now, only fields of {@link VariableField.VariableType#USER}
     * type are supported.
     *
     * @param field Object to extract value from.
     * @return Found value, or null if we cannot find any.
     * @throws ReflectiveOperationException If an error occurred while analyzing
     * input variable.
     * @throws UnsupportedOperationException If input variable type is not {@link VariableField.VariableType#USER}
     */
    public static Object getVariableValue(final VariableField field) throws ReflectiveOperationException {
        ArgumentChecks.ensureNonNull("input variable field", field);
        if (FieldType.USER_VARIABLE_FIELD.equals(field.getFieldType())) {
            Field userVariableField = getUserVariableField();
            return getValue((TextUserFieldDeclElement) userVariableField.get(field));
        } else {
            throw new UnsupportedOperationException("Not done yet.");
        }
    }

    private static Field getUserVariableField() {
        if (USER_FIELD == null) {
            try {
                USER_FIELD = VariableField.class.getDeclaredField("userVariableElement");
            } catch (NoSuchFieldException ex) {
                throw new IllegalStateException("Cannot access user field.", ex);
            }
            USER_FIELD.setAccessible(true);
        }
        return USER_FIELD;
    }

    private static Field getSimpleVariableField() {
        if (SIMPLE_FIELD == null) {
            try {
                SIMPLE_FIELD = VariableField.class.getDeclaredField("simpleVariableElement");
            } catch (NoSuchFieldException ex) {
                throw new IllegalStateException("Cannot access user field.", ex);
            }
            SIMPLE_FIELD.setAccessible(true);
        }
        return SIMPLE_FIELD;
    }

    /**
     * Analyze input element tto find contained value.
     * @param input
     * @return value hold by given object, or null.
     */
    private static Object getValue(TextUserFieldDeclElement userVariable) {
        // really crappy...
        Object value = userVariable.getOfficeStringValueAttribute();
        if (value == null) {
            value = userVariable.getOfficeTimeValueAttribute();
            if (value == null) {
                value = userVariable.getOfficeDateValueAttribute();
                if (value == null) {
                    value = userVariable.getOfficeBooleanValueAttribute();
                    if (value == null) {
                        value = userVariable.getOfficeValueAttribute();
                    }
                }
            }
        }

        return value;
    }

    /**
     * Get master page with same orientation / margin properties as inputs, or
     * create a new one if we cannot find any.
     *
     * TODO : Check footnote settings
     *
     * @param doc Document to search for existing master pages.
     * @param orientation Orientation wanted for the returned page configuration. If null, portrait orientation is used.
     * @param margin Margins to set to the master page. If null, default style margins are used.
     * @return Found master page, or a new one.
     * @throws Exception If we cannot read given document.
     */
    public static MasterPage getOrCreateOrientationMasterPage(Document doc, StyleTypeDefinitions.PrintOrientation orientation, Insets margin) throws Exception {
        if (orientation == null) {
            orientation = StyleTypeDefinitions.PrintOrientation.PORTRAIT;
        }

        final String masterName = orientation.name() + (margin == null? "" : " " + margin.toString());

        final MasterPage masterPage = MasterPage.getOrCreateMasterPage(doc, masterName);
        masterPage.setPrintOrientation(orientation);
        switch (orientation) {
            case LANDSCAPE:
                masterPage.setPageHeight(210);
                masterPage.setPageWidth(297);
                break;
            case PORTRAIT:
                masterPage.setPageWidth(210);
                masterPage.setPageHeight(297);
        }
        if (margin != null) {
            masterPage.setMargins(margin.getTop(), margin.getBottom(), margin.getLeft(), margin.getRight());
        }
        masterPage.setFootnoteMaxHeight(0);
        return masterPage;
    }

    /**
     * Aggregation dans un seul fichier ODT de tous les fichiers fournis.
     * Fichier supportés :
     * - images
     * - odt
     * - pdf
     *
     * Supporte aussi les objets de type :
     * - File
     * - Path
     * - TextDocument
     *
     * @param output fichier ODT de sortie
     * @param candidates
     */
    public static void concatenateFiles(Path output, Object ... candidates) throws Exception {
        final TextDocument doc = TextDocument.newTextDocument();

        for(Object candidate : candidates){
            //concatenateFile(doc, candidate);
        }

        try (final OutputStream stream = Files.newOutputStream(output, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)) {
            doc.save(stream);
        } catch (Exception e) {
            try {
                Files.deleteIfExists(output);
            } catch (Exception ignored) {
                e.addSuppressed(ignored);
            }
        }
    }


        public static Path convertPDFToODT(final Path input) throws IOException {
            ArgumentChecks.ensureNonNull("Input document", input);
            try (final InputStream in = Files.newInputStream(input, StandardOpenOption.READ)) {
                    final List<PDPage> pages = PDDocument.load(in).getDocumentCatalog().getAllPages();
                    for (final PDPage page : pages) {
                        final PDStream contents = page.getContents();
                        if (contents == null)
                            continue;
                        new PDFStreamParser(contents);

                        //contents.getStream().
        //List<PDAnnotation> annotations = page.getAnnotations();
//                        for (final PDAnnotation annot : annotations) {
//
//                        }
                    }
                return null;
            }

        }
}
