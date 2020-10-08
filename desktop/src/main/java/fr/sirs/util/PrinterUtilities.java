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

import fr.sirs.Injector;
import fr.sirs.SIRS;
import fr.sirs.core.SirsCoreRuntimeException;
import fr.sirs.core.component.Previews;
import fr.sirs.core.model.Desordre;
import fr.sirs.core.model.Element;
import fr.sirs.core.model.Objet;
import fr.sirs.core.model.OuvrageHydrauliqueAssocie;
import fr.sirs.core.model.Positionable;
import fr.sirs.core.model.ReseauHydrauliqueFerme;
import fr.sirs.core.model.TronconDigue;
import static fr.sirs.util.JRDomWriterDesordreSheet.PHOTOS_SUBREPORT;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRPrintPage;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.xml.JRXmlLoader;
import org.geotoolkit.data.FeatureCollection;
import org.geotoolkit.display2d.service.OutputDef;
import org.geotoolkit.feature.type.FeatureType;
import org.geotoolkit.report.CollectionDataSource;
import org.geotoolkit.report.JasperReportService;
import org.xml.sax.SAXException;

/**
 * <p>This class provides utilities for two purposes:</p>
 * <ul>
 * <li>generating Jasper Reports templates mapping the classes of the model.</li>
 * <li>generating portable documents (.pdf) based on the templates on the one
 * hand and the instances on the other hand.</li>
 * </ul>
 * <p>These are tools for printing functionnalities.</p>
 * @author Samuel Andrés (Geomatys)
 */
public class PrinterUtilities {

    private static final String JRXML_EXTENSION = ".jrxml";
    private static final String PDF_EXTENSION = ".pdf";
    private static final String LOGO_PATH = "/fr/sirs/images/icon-sirs.png";
    private static final String ORDERED_SEPARATOR = ") ";

    private static final List<String> FALSE_GETTERS = new ArrayList<>();
    static{
        FALSE_GETTERS.add("getClass");
        FALSE_GETTERS.add("isNew");
        FALSE_GETTERS.add("getAttachments");
        FALSE_GETTERS.add("getRevisions");
        FALSE_GETTERS.add("getConflicts");
        FALSE_GETTERS.add("getDocumentId");
    }

    ////////////////////////////////////////////////////////////////////////////
    // FICHES DÉTAILLÉES D'OUVRAGE HYDRAULIQUE ASSOCIE
    ////////////////////////////////////////////////////////////////////////////

    public static File printOuvrageAssocie(final List<String> avoidReseauFields,
            final List<JRColumnParameter> observationFields,
            final List<JRColumnParameter> reseauFields,
            final List<JRColumnParameter> desordreFields,
            final Previews previewLabelRepository,
            final SirsStringConverter stringConverter,
            final List<OuvrageHydrauliqueAssocie> ouvrages,
            final boolean printPhoto, final boolean printReseauFerme) throws IOException, ParserConfigurationException, SAXException, TransformerException, JRException {

        // Creates the Jasper Reports specific template from the generic template.
        final File templateFile = File.createTempFile(ReseauHydrauliqueFerme.class.getName(), JRXML_EXTENSION);
        templateFile.deleteOnExit();

        final JasperPrint print;
        try(final InputStream metaTemplateStream = PrinterUtilities.class.getResourceAsStream("/fr/sirs/jrxml/metaTemplateOuvrageAssocie.jrxml")) {

            final JRDomWriterOuvrageAssocieSheet templateWriter = new JRDomWriterOuvrageAssocieSheet(metaTemplateStream,
                    avoidReseauFields, observationFields, reseauFields, desordreFields, printPhoto, printReseauFerme);
            templateWriter.setOutput(templateFile);
            templateWriter.write();

            final JasperReport jasperReport = JasperCompileManager.compileReport(JRXmlLoader.load(templateFile));

            final Map<String, Object> parameters = new HashMap<>();
            if (printPhoto) {
                try (final InputStream photoTemplateStream = PrinterUtilities.class.getResourceAsStream("/fr/sirs/jrxml/photoTemplateOuvrageAssocie.jrxml")) {
                    parameters.put(PHOTOS_SUBREPORT, net.sf.jasperreports.engine.JasperCompileManager.compileReport(photoTemplateStream));
                }
            }

            ouvrages.sort(OBJET_LINEAR_COMPARATOR.thenComparing(new PRComparator()));
            final JRDataSource source = new OuvrageHydrauliqueAssocieDataSource(ouvrages, previewLabelRepository, stringConverter);
            print = JasperFillManager.fillReport(jasperReport, parameters, source);
        }

        // Generate the report -------------------------------------------------
        final File fout = File.createTempFile("RESEAU_HYDRAULIQUE_FERME_OBSERVATION", PDF_EXTENSION);
        fout.deleteOnExit();
        try (final FileOutputStream outStream = new FileOutputStream(fout)) {
            final OutputDef output = new OutputDef(JasperReportService.MIME_PDF, outStream);
            JasperReportService.generate(print, output);
        }
        return fout;
    }

    ////////////////////////////////////////////////////////////////////////////
    // FICHES DÉTAILLÉES DE RÉSEAUX HYDRAULIQUES FERMÉS
    ////////////////////////////////////////////////////////////////////////////

    public static File printReseauFerme(final List<String> avoidReseauFields,
            final List<JRColumnParameter> observationFields,
            final List<JRColumnParameter> reseauFields,
            final List<JRColumnParameter> desordreFields,
            final Previews previewLabelRepository,
            final SirsStringConverter stringConverter,
            final List<ReseauHydrauliqueFerme> reseaux,
            final boolean printPhoto, final boolean printReseauOuvrage) throws IOException, ParserConfigurationException, SAXException, TransformerException, JRException {

        // Creates the Jasper Reports specific template from the generic template.
        final File templateFile = File.createTempFile(ReseauHydrauliqueFerme.class.getName(), JRXML_EXTENSION);
        templateFile.deleteOnExit();

        final JasperPrint print;
        try(final InputStream metaTemplateStream = PrinterUtilities.class.getResourceAsStream("/fr/sirs/jrxml/metaTemplateReseauFerme.jrxml")){

            final JRDomWriterReseauFermeSheet templateWriter = new JRDomWriterReseauFermeSheet(metaTemplateStream,
                    avoidReseauFields, observationFields, reseauFields, desordreFields, printPhoto, printReseauOuvrage);
            
            templateWriter.setOutput(templateFile);
            templateWriter.write();

            final JasperReport jasperReport = JasperCompileManager.compileReport(JRXmlLoader.load(templateFile));

            final Map<String, Object> parameters = new HashMap<>();
            if (printPhoto) {
                try (final InputStream photoTemplateStream = PrinterUtilities.class.getResourceAsStream("/fr/sirs/jrxml/photoTemplateReseauFerme.jrxml")) {
                    parameters.put(PHOTOS_SUBREPORT, net.sf.jasperreports.engine.JasperCompileManager.compileReport(photoTemplateStream));
                }
            }

            reseaux.sort(OBJET_LINEAR_COMPARATOR.thenComparing(new PRComparator()));
            final JRDataSource source = new ReseauHydrauliqueFermeDataSource(reseaux, previewLabelRepository, stringConverter);
            print = JasperFillManager.fillReport(jasperReport, parameters, source);
        }

        // Generate the report -------------------------------------------------
        final File fout = File.createTempFile("RESEAU_HYDRAULIQUE_FERME_OBSERVATION", PDF_EXTENSION);
        fout.deleteOnExit();
        try (final FileOutputStream outStream = new FileOutputStream(fout)) {
            final OutputDef output = new OutputDef(JasperReportService.MIME_PDF, outStream);
            JasperReportService.generate(print, output);
        }
        return fout;
    }

    ////////////////////////////////////////////////////////////////////////////
    // FICHES DÉTAILLÉES DE DESORDRES
    ////////////////////////////////////////////////////////////////////////////

    public static File printDisorders(final List<String> avoidDesordreFields,
            final List<JRColumnParameter> observationFields,
            final List<JRColumnParameter> prestationFields,
            final List<JRColumnParameter> reseauFields,
            final Previews previewLabelRepository,
            final SirsStringConverter stringConverter,
            final List<Desordre> desordres,
            final boolean printPhoto, final boolean printReseauOuvrage, final boolean printVoirie)
        throws ParserConfigurationException, SAXException, JRException, TransformerException, IOException {

        // Creates the Jasper Reports specific template from the generic template.
        final File templateFile = File.createTempFile(Desordre.class.getName(), JRXML_EXTENSION);
        templateFile.deleteOnExit();

        final JasperPrint print;
        try(final InputStream metaTemplateStream = PrinterUtilities.class.getResourceAsStream("/fr/sirs/jrxml/metaTemplateDesordre.jrxml")) {
            final JRDomWriterDesordreSheet templateWriter = new JRDomWriterDesordreSheet(metaTemplateStream, avoidDesordreFields,
                    observationFields, prestationFields, reseauFields, printPhoto, printReseauOuvrage, printVoirie);
            templateWriter.setOutput(templateFile);
            templateWriter.write();

            final JasperReport jasperReport = JasperCompileManager.compileReport(JRXmlLoader.load(templateFile));

            final Map<String, Object> parameters = new HashMap<>();
            if (printPhoto) {
                try (final InputStream photoTemplateStream = PrinterUtilities.class.getResourceAsStream("/fr/sirs/jrxml/photoTemplateDesordre.jrxml")) {
                    parameters.put(PHOTOS_SUBREPORT, JasperCompileManager.compileReport(photoTemplateStream));
                }
            }

            desordres.sort(OBJET_LINEAR_COMPARATOR.thenComparing(new PRComparator()));
            final JRDataSource source = new DesordreDataSource(desordres, previewLabelRepository, stringConverter);
            print = JasperFillManager.fillReport(jasperReport, parameters, source);
        }

        // Generate the report -------------------------------------------------
        final File fout = File.createTempFile("DESORDRE_OBSERVATION", PDF_EXTENSION);
        fout.deleteOnExit();
        try (final FileOutputStream outStream = new FileOutputStream(fout)) {
            final OutputDef output = new OutputDef(JasperReportService.MIME_PDF, outStream);
            JasperReportService.generate(print, output);
        }

        return fout;
    }

    ////////////////////////////////////////////////////////////////////////////
    // FICHES DE RESULTATS DE REQUETES
    ////////////////////////////////////////////////////////////////////////////

    /**
     *
     * @param avoidFields
     * @param featureCollection
     * @return
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws JRException
     * @throws TransformerException
     */
    public static File print(List<String> avoidFields, final FeatureCollection featureCollection)
        throws IOException, ParserConfigurationException, SAXException, JRException, TransformerException {

        if(avoidFields==null) avoidFields=new ArrayList<>();

        // Creates the Jasper Reports specific template from the generic template.
        final File template;
        try(final InputStream templateInputStream =PrinterUtilities.class.getResourceAsStream("/fr/sirs/jrxml/metaTemplateQuery.jrxml")){
            final JRDomWriterQueryResultSheet writer = new JRDomWriterQueryResultSheet(templateInputStream);
            writer.setFieldsInterline(2);
            template = File.createTempFile(featureCollection.getFeatureType().getName().tip().toString(), JRXML_EXTENSION);
            template.deleteOnExit();
            writer.setOutput(template);
            writer.write(featureCollection.getFeatureType(), avoidFields);
        }

        // Retrives the compiled template and the feature type -----------------
        final Map.Entry<JasperReport, FeatureType> entry = JasperReportService.prepareTemplate(template);
        final JasperReport report = entry.getKey();

        // Generate the report -------------------------------------------------
        final File fout = File.createTempFile(featureCollection.getFeatureType().getName().tip().toString(), PDF_EXTENSION);
        fout.deleteOnExit();
        try (final FileOutputStream outStream = new FileOutputStream(fout);
                final InputStream logoStream = PrinterUtilities.class.getResourceAsStream(LOGO_PATH)) {
            final OutputDef output = new OutputDef(JasperReportService.MIME_PDF, outStream);
            final Map<String, Object> parameters = new HashMap<>();
            parameters.put("logo", logoStream);
            parameters.put(JRDomWriterQueryResultSheet.TABLE_DATA_SOURCE, new CollectionDataSource(featureCollection));

            final JasperPrint print = JasperFillManager.fillReport(report, parameters, new JREmptyDataSource());
            JasperReportService.generate(print, output);
//        JasperReportService.generateReport(report, featureCollection, parameters, output);
        }
        return fout;
    }

    ////////////////////////////////////////////////////////////////////////////
    // FICHES SYNOPTIQUES D'ELEMENTS DU MODELE
    ////////////////////////////////////////////////////////////////////////////

    /**
     * <p>Generate the specific Jasper Reports template for a given class.
     * This method is based on a meta-template defined in
     * src/main/resources/fr/sirs/jrxml/metaTemplate.jrxml
     * and produce a specific template : ClassName.jrxml".</p>
     *
     * <p>This specific template is used to print objects of the model.</p>
     *
     * @param elements Pojos to print. The list must contain at least one element.
     * If it contains more than one, they must be all of the same class.
     * @param avoidFields Names of the fields to avoid printing.
     * @param previewLabelRepository
     * @param stringConverter
     * @return
     * @throws java.io.IOException
     * @throws javax.xml.parsers.ParserConfigurationException
     * @throws org.xml.sax.SAXException
     * @throws net.sf.jasperreports.engine.JRException
     * @throws javax.xml.transform.TransformerException
     */
    public static File print(final List<String> avoidFields,
            final Previews previewLabelRepository, final SirsStringConverter stringConverter, final List<? extends Element> elements)
            throws IOException, ParserConfigurationException, SAXException, JRException, TransformerException {

        // Creates the Jasper Reports specific template from the generic template.
        final JasperReport jasperReport;
        final Path templatePath = Files.createTempFile("printableElement", JRXML_EXTENSION);
        final File templateFile = templatePath.toFile();
        try {
            final JRDomWriterElementSheet templateWriter;
            try (final InputStream template = PrinterUtilities.class.getResourceAsStream("/fr/sirs/jrxml/metaTemplateElement.jrxml")) {
                templateWriter = new JRDomWriterElementSheet(template);
            }
            templateWriter.setFieldsInterline(2);
            templateWriter.setOutput(templateFile);
            templateWriter.write(elements.get(0).getClass(), avoidFields);

            jasperReport = JasperCompileManager.compileReport(JRXmlLoader.load(templateFile));
        } finally {
            Files.delete(templatePath);
        }


        JasperPrint finalPrint = null;
        for(final Element element : elements){
            final JRDataSource source = new ObjectDataSource(Collections.singletonList(element), previewLabelRepository, stringConverter);

            final Map<String, Object> parameters = new HashMap<>();
            parameters.put("logo", PrinterUtilities.class.getResource(LOGO_PATH));
            final JasperPrint print = JasperFillManager.fillReport(jasperReport, parameters, source);
            if(finalPrint==null) finalPrint=print;
            else{
                for(final JRPrintPage page : print.getPages()){
                    finalPrint.addPage(page);
                }
            }
        }

        // Generate the report -------------------------------------------------
        final File fout = File.createTempFile(elements.get(0).getClass().getSimpleName(), PDF_EXTENSION);
        try (final FileOutputStream outStream = new FileOutputStream(fout)) {
            final OutputDef output = new OutputDef(JasperReportService.MIME_PDF, outStream);
            JasperReportService.generate(finalPrint, output);
        }
        return fout;
    }

    /*

    Pour l'impression de plusieurs documents, plusieurs solutions (http://stackoverflow.com/questions/24115885/combining-two-jasper-reports)


    solution 1

    List<JasperPrint> jasperPrints = new ArrayList<JasperPrint>();
// Your code to get Jasperreport objects
JasperReport jasperReportReport1 = JasperCompileManager.compileReport(jasperDesignReport1);
jasperPrints.add(jasperReportReport1);
JasperReport jasperReportReport2 = JasperCompileManager.compileReport(jasperDesignReport2);
jasperPrints.add(jasperReportReport2);
JasperReport jasperReportReport3 = JasperCompileManager.compileReport(jasperDesignReport3);
jasperPrints.add(jasperReportReport3);

JRPdfExporter exporter = new JRPdfExporter();
//Create new FileOutputStream or you can use Http Servlet Response.getOutputStream() to get Servlet output stream
// Or if you want bytes create ByteArrayOutputStream
ByteArrayOutputStream out = new ByteArrayOutputStream();
exporter.setParameter(JRExporterParameter.JASPER_PRINT_LIST, jasperPrints);
exporter.setParameter(JRExporterParameter.OUTPUT_STREAM, out);
exporter.exportReport();
byte[] bytes = out.toByteArray();


    solution 2 (adoptée pour le moment) :

    JasperPrint jp1 = JasperFillManager.fillReport(url.openStream(), parameters,
                    new JRBeanCollectionDataSource(inspBean));
JasperPrint jp2 = JasperFillManager.fillReport(url.openStream(), parameters,
                    new JRBeanCollectionDataSource(inspBean));

List pages = jp2 .getPages();
        for (int j = 0; j < pages.size(); j++) {
        JRPrintPage object = (JRPrintPage)pages.get(j);
        jp1.addPage(object);

}
JasperViewer.viewReport(jp1,false);

    */

    ////////////////////////////////////////////////////////////////////////////
    // METHODES UTILITAIRES
    ////////////////////////////////////////////////////////////////////////////
    /**
     * <p>This method detects if a method is a getter.</p>
     * @param method
     * @return true if the method is a getter.
     */
    static public boolean isGetter(final Method method){
        if (method == null)
            return false;
        else
            return (method.getName().startsWith("get")
                || method.getName().startsWith("is"))
                && method.getParameterTypes().length == 0
                && !FALSE_GETTERS.contains(method.getName());
    }

    /**
     * <p>This method detects if a method is a setter.</p>
     * @param method
     * @return true if the method is a setter.
     */
    static public boolean isSetter(final Method method){
        if (method == null)
            return false;
        else
            return method.getName().startsWith("set")
                && method.getParameterTypes().length == 1
                && void.class.equals(method.getReturnType());
    }

    public static String getFieldNameFromSetter(final Method setter) {
        return setter.getName().substring(3, 4).toLowerCase()
                            + setter.getName().substring(4);
    }

    /**
     * Construit une chaîne de caractères énumérant le contenu d'un {@link Iterable}.
     *
     * @param <E>
     * @param iterable
     * @param ordered
     * @param startIndex
     * @return
     */
    public static <E> String printList(final Iterable<E> iterable, final boolean ordered, int startIndex){

        final Iterator<E> it = iterable.iterator();
        if (! it.hasNext())
            return "";

        final StringBuilder sb = new StringBuilder();

        for (;;) {
            final E e = it.next();
            if(ordered){
                sb.append(startIndex++);
                sb.append(ORDERED_SEPARATOR);
            }
            sb.append(e == iterable ? "(this Iterable)" : e);
            if (! it.hasNext()) return sb.toString();
            else sb.append('\n');
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    private PrinterUtilities(){}

    /**
     * Compare objects according to their PRs. They're sorted by ascending order
     * of their {@link Positionable#getPrDebut() } value, and then by {@link Positionable#getPrFin() } (length).
     */
    private static class PRComparator<P extends Positionable> implements Comparator<P> {

        @Override
        public int compare(final P o1, final P o2) {
            if (o1 == null || Float.isNaN(o1.getPrDebut())) {
                return 1;
            } else if (o2 == null || Float.isNaN(o2.getPrDebut())) {
                return -1;
            } else {
                final int compare = Float.compare(o1.getPrDebut(), o2.getPrDebut());
                if (compare == 0) {
                    return Float.compare(o1.getPrFin(), o2.getPrFin());
                } else {
                    return compare;
                }
            }
        }
    }


    /**
     * Groupe par tronçon.
     */
    static final Comparator<Objet> OBJET_LINEAR_GROUPER = (Objet d1, Objet d2)->{
        final String lin1 = d1.getLinearId();
        final String lin2 = d2.getLinearId();
        if(lin1==null && lin2==null) return 0; // Ne devrait jamais se produire pour un objet.
        else if(lin1==null || lin2==null) return (lin1==null) ? 1 : -1; // Ne devrait jamais se produire pour un objet.
        else return lin1.compareTo(lin2);
    };

    /**
     * Groupe par tronçon en classant par désignation croissante de tronçon selon l'ordre alphabétique.
     * Si la désignation n'est pas disponible, on groupe par identifiant.
     */
    static final Comparator<Objet> OBJET_LINEAR_COMPARATOR = (Objet d1, Objet d2) -> {
        final String lin1 = d1.getLinearId();
        final String lin2 = d2.getLinearId();
        if(lin1==null && lin2==null) return 0; // Ne devrait jamais se produire pour un objet.
        else if(lin1==null || lin2==null) return (lin1==null) ? 1 : -1; // Ne devrait jamais se produire pour un objet.
        else {
            try{
                final TronconDigue troncon1 = Injector.getSession().getRepositoryForClass(TronconDigue.class).get(lin1);
                final TronconDigue troncon2 = Injector.getSession().getRepositoryForClass(TronconDigue.class).get(lin2);

                if(troncon1==null || troncon2==null)
                    throw new SirsCoreRuntimeException("Un des tronçons est null : "+lin1+" ou "+lin2+".");

                final String des1 = troncon1.getDesignation();
                final String des2 = troncon2.getDesignation();
                if(des1==null && des2==null) return 0;
                else if(des1==null || des2==null) return (des1==null) ? 1 : -1;
                else return des1.compareTo(des2);
            }
            catch(IllegalArgumentException e){
                // SYM-1735 : problème d'impression des fiches de désordres attachés à des berges lorsque le module berges n'est pas chargé
                SIRS.LOGGER.log(Level.INFO, "un problème a été rencontré lors de la comparaison des tronçons", e);
                return 0;
            }
        }
    };
}
