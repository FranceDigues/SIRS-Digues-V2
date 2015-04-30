
package fr.sirs.util;

import fr.sirs.SIRS;
import static fr.sirs.util.JRUtils.ATT_CLASS;
import static fr.sirs.util.JRUtils.ATT_HEIGHT;
import static fr.sirs.util.JRUtils.ATT_KEY;
import static fr.sirs.util.JRUtils.ATT_NAME;
import static fr.sirs.util.JRUtils.ATT_STYLE;
import static fr.sirs.util.JRUtils.ATT_SUB_DATASET;
import static fr.sirs.util.JRUtils.ATT_WIDTH;
import static fr.sirs.util.JRUtils.ATT_X;
import static fr.sirs.util.JRUtils.ATT_Y;
import static fr.sirs.util.JRUtils.BOOLEAN_CANONICAL_NAME;
import static fr.sirs.util.JRUtils.BOOLEAN_PRIMITIVE_NAME;
import static fr.sirs.util.JRUtils.DOUBLE_CANONICAL_NAME;
import static fr.sirs.util.JRUtils.DOUBLE_PRIMITIVE_NAME;
import static fr.sirs.util.JRUtils.FLOAT_CANONICAL_NAME;
import static fr.sirs.util.JRUtils.FLOAT_PRIMITIVE_NAME;
import static fr.sirs.util.JRUtils.INTEGER_CANONICAL_NAME;
import static fr.sirs.util.JRUtils.INTEGER_PRIMITIVE_NAME;
import static fr.sirs.util.JRUtils.LONG_CANONICAL_NAME;
import static fr.sirs.util.JRUtils.LONG_PRIMITIVE_NAME;
import fr.sirs.util.JRUtils.Markup;
import static fr.sirs.util.JRUtils.TAG_BAND;
import static fr.sirs.util.JRUtils.TAG_COLUMN;
import static fr.sirs.util.JRUtils.TAG_COLUMN_FOOTER;
import static fr.sirs.util.JRUtils.TAG_COLUMN_HEADER;
import static fr.sirs.util.JRUtils.TAG_COMPONENT_ELEMENT;
import static fr.sirs.util.JRUtils.TAG_DATASET_RUN;
import static fr.sirs.util.JRUtils.TAG_DATA_SOURCE_EXPRESSION;
import static fr.sirs.util.JRUtils.TAG_DETAIL;
import static fr.sirs.util.JRUtils.TAG_DETAIL_CELL;
import static fr.sirs.util.JRUtils.TAG_FIELD;
import static fr.sirs.util.JRUtils.TAG_FIELD_DESCRIPTION;
import static fr.sirs.util.JRUtils.TAG_LAST_PAGE_FOOTER;
import static fr.sirs.util.JRUtils.TAG_PAGE_FOOTER;
import static fr.sirs.util.JRUtils.TAG_PAGE_HEADER;
import static fr.sirs.util.JRUtils.TAG_REPORT_ELEMENT;
import static fr.sirs.util.JRUtils.TAG_STATIC_TEXT;
import static fr.sirs.util.JRUtils.TAG_SUB_DATASET;
import static fr.sirs.util.JRUtils.TAG_TABLE;
import static fr.sirs.util.JRUtils.TAG_TABLE_FOOTER;
import static fr.sirs.util.JRUtils.TAG_TABLE_HEADER;
import static fr.sirs.util.JRUtils.TAG_TEXT;
import static fr.sirs.util.JRUtils.TAG_TEXT_FIELD;
import static fr.sirs.util.JRUtils.TAG_TEXT_FIELD_EXPRESSION;
import static fr.sirs.util.JRUtils.TAG_TITLE;
import static fr.sirs.util.JRUtils.URI_JRXML;
import static fr.sirs.util.JRUtils.URI_JRXML_COMPONENTS;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.geotoolkit.feature.type.AttributeType;
import org.geotoolkit.feature.type.FeatureType;
import org.geotoolkit.report.FeatureCollectionDataSource;
import org.opengis.feature.PropertyType;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 *
 * @author Samuel Andrés (Geomatys)
 */
public class JRDomWriter {
    
    // Template elements.
    private final Document document;
    private final Element root;
    private final Element subDataset;
    private final Element title;
    private final Element pageHeader;
    private final Element columnHeader;
    private final Element detail;
    private final Element columnFooter;
    private final Element pageFooter;
    private final Element lastPageFooter;
    private File output;
    private int columnWidth;
    
    // Dynamic template parameters.
    private int fields_interline;
    private int height_multiplicator;
    
    // Static template parameters.
    private static final String FIELDS_VERTICAL_ALIGNMENT = "Middle";
    private static final String FIELDS_FONT_NAME = "Serif";
    private static final int FIELDS_HEIGHT = 16;
    //private static final String DATE_PATTERN = "dd/MM/yyyy à hh:mm:ss";
    private static final int INDENT_LABEL = 10;
    private static final int LABEL_WIDTH = 140;
    private static final int PAGE_HEIGHT = 595;
    private static final int PAGE_WIDTH = 842;
    private static final int COLUMN_WIDTH = 555;
    private static final int LEFT_MARGIN = 20;
    private static final int RIGHT_MARGIN = 20;
    private static final int TOP_MARGIN = 20;
    private static final int BOTTOM_MARGIN = 20;
    
    
    private JRDomWriter(){
        document = null;
        root = null; 
        subDataset = null;
        title = null; 
        pageHeader = null;
        columnHeader = null;
        detail = null;
        columnFooter = null;
        pageFooter = null;
        lastPageFooter = null;
        
        fields_interline = 8;
        height_multiplicator = 1;
    }
    
    public JRDomWriter(final InputStream stream) throws ParserConfigurationException, SAXException, IOException {
        
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder constructeur = factory.newDocumentBuilder();
        factory.setNamespaceAware(true);
        
        document = constructeur.parse(stream);
        stream.close();
        
        root = document.getDocumentElement();
        subDataset = (Element) root.getElementsByTagName(TAG_SUB_DATASET).item(0);
        title = (Element) root.getElementsByTagName(TAG_TITLE).item(0);
        pageHeader = (Element) root.getElementsByTagName(TAG_PAGE_HEADER).item(0);
        columnHeader = (Element) root.getElementsByTagName(TAG_COLUMN_HEADER).item(0);
        detail = (Element) this.root.getElementsByTagName(TAG_DETAIL).item(0);
        columnFooter = (Element) root.getElementsByTagName(TAG_COLUMN_FOOTER).item(0);
        pageFooter = (Element) root.getElementsByTagName(TAG_PAGE_FOOTER).item(0);
        lastPageFooter = (Element) root.getElementsByTagName(TAG_LAST_PAGE_FOOTER).item(0);
        
        fields_interline = 8;
        height_multiplicator = 1;
    }
    
    /**
     * This setter changes the default fields interline.
     * @param fieldsInterline 
     */
    public void setFieldsInterline(int fieldsInterline){
        this.fields_interline = fieldsInterline;
    }
    
    /**
     * This setter changes the default height multiplicator for comments or 
     * description fields.
     * @param heightMultiplicator 
     */
    public void setHeightMultiplicator(int heightMultiplicator){
        this.height_multiplicator = heightMultiplicator;
    }
    
    /**
     * <p>This method sets the output to write the modified DOM in.</p>
     * @param output 
     */
    public void setOutput(final File output) {
        this.output = output;
    } 
    
    /**
     * <p>This method writes a Jasper Reports template mapping the parameter class.</p>
     * @param featureType
     * @param avoidFields field names to avoid.
     * @throws TransformerException
     * @throws IOException
     */
    public void write(final FeatureType featureType, final List<String> avoidFields) throws TransformerException, IOException, Exception {
        
        columnWidth = (PAGE_WIDTH - 40)/featureType.getProperties(true).size();
                
        // Remove elements before inserting fields.-----------------------------
        root.removeChild(title);
        root.removeChild(pageHeader);
        root.removeChild(columnHeader);
        root.removeChild(detail);
        
        // Modifies the template, based on the given class.---------------------
        writeObject(featureType, avoidFields);
        
        // Serializes the document.---------------------------------------------
        //DomUtilities.write(this.document, this.output);
        final Source source = new DOMSource(document);
        final Result result = new StreamResult(output);
        final TransformerFactory factory = TransformerFactory.newInstance();
        final Transformer trs = factory.newTransformer();
        trs.setOutputProperty(OutputKeys.INDENT, "yes");
        trs.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        trs.transform(source, result);
    }

    /**
     * <p>This method modifies the body of the DOM.</p>
     * @param featureType
     * @param avoidFields field names to avoid.
     * @throws Exception 
     */
    private void writeObject(final FeatureType featureType, List<String> avoidFields) throws Exception {
        
        if(avoidFields==null) avoidFields=new ArrayList<>();
        writeSubDataset(featureType, avoidFields);
        
        // Modifies the title block.--------------------------------------------
        writeTitle(featureType);
        
        // Writes the headers.--------------------------------------------------
        writePageHeader();
        writeColumnHeader();
        
        // Builds the body of the Jasper Reports template.----------------------
//        this.writeDetail(featureType, avoidFields);
        writeComponentElement(featureType);
    }
    
    
    private void writeComponentElement(final FeatureType featureType){
        final Element band = (Element) detail.getElementsByTagName(TAG_BAND).item(0);
        band.setAttribute(ATT_HEIGHT, String.valueOf(200));
        
        // Set the component element
        final Element componentElement = document.createElement(TAG_COMPONENT_ELEMENT);
        final Element componentElementReportElement = document.createElement(TAG_REPORT_ELEMENT);
        componentElementReportElement.setAttribute(ATT_KEY, "table");
        componentElementReportElement.setAttribute(ATT_STYLE, "table");
        componentElementReportElement.setAttribute(ATT_X, String.valueOf(0));
        componentElementReportElement.setAttribute(ATT_Y, String.valueOf(0));
        componentElementReportElement.setAttribute(ATT_WIDTH, String.valueOf(180));
        componentElementReportElement.setAttribute(ATT_HEIGHT, String.valueOf(140));
        
        // Set the table element
        final Element table = document.createElementNS(URI_JRXML_COMPONENTS, TAG_TABLE);
        
        final Element datasetRun = document.createElementNS(URI_JRXML, TAG_DATASET_RUN);
        datasetRun.setAttribute(ATT_SUB_DATASET, "Query Dataset");
        final Element datasourceExpression = document.createElementNS(URI_JRXML, TAG_DATA_SOURCE_EXPRESSION);
        
        final CDATASection datasourceExpressionField = document.createCDATASection("(("+FeatureCollectionDataSource.class.getCanonicalName()+") $P{REPORT_DATA_SOURCE}).cloneDataSource()");
        
        datasourceExpression.appendChild(datasourceExpressionField);
        datasetRun.appendChild(datasourceExpression);
        
        table.appendChild(datasetRun);
        for(final PropertyType propertyType : featureType.getProperties(true)){
            writeColumn(propertyType, table);
        }
        
        componentElement.appendChild(componentElementReportElement);
        componentElement.appendChild(table);
        
        band.appendChild(componentElement);
        
        root.appendChild(detail);
    }
    
    private void writeColumn(final PropertyType propertyType, final Element table){
        
        final Element column = document.createElementNS(URI_JRXML_COMPONENTS, TAG_COLUMN);
        column.setAttribute(ATT_WIDTH, String.valueOf(columnWidth));
        
        
        final Element tableHeader = document.createElementNS(URI_JRXML_COMPONENTS, TAG_TABLE_HEADER);
        tableHeader.setAttribute(ATT_STYLE, "table_TH");
        tableHeader.setAttribute(ATT_HEIGHT, String.valueOf(5));
        final Element tableFooter = document.createElementNS(URI_JRXML_COMPONENTS, TAG_TABLE_FOOTER);
        tableFooter.setAttribute(ATT_STYLE, "table_TH");
        tableFooter.setAttribute(ATT_HEIGHT, String.valueOf(5));
        
        // Column header
        final Element jrColumnHeader = document.createElementNS(URI_JRXML_COMPONENTS, TAG_COLUMN_HEADER);
        jrColumnHeader.setAttribute(ATT_STYLE, "table_CH");
        jrColumnHeader.setAttribute(ATT_HEIGHT, String.valueOf(40));
        
        final Element staticText = document.createElementNS(URI_JRXML, TAG_STATIC_TEXT);
        
        final Element staticTextReportElement = document.createElementNS(URI_JRXML, TAG_REPORT_ELEMENT);
        staticTextReportElement.setAttribute(ATT_X, String.valueOf(0));
        staticTextReportElement.setAttribute(ATT_Y, String.valueOf(0));
        staticTextReportElement.setAttribute(ATT_WIDTH, String.valueOf(columnWidth));
        staticTextReportElement.setAttribute(ATT_HEIGHT, String.valueOf(30));
//        staticTextReportElement.setAttribute(ATT_POSITION_TYPE, PositionType.FLOAT.toString());
        
        final Element text = document.createElementNS(URI_JRXML, TAG_TEXT);
        final CDATASection labelField = document.createCDATASection(propertyType.getName().toString());
        text.appendChild(labelField);
        
        staticText.appendChild(staticTextReportElement);
        staticText.appendChild(text);
        jrColumnHeader.appendChild(staticText);
        
        // Column footer
        final Element jrColumnFooter = document.createElementNS(URI_JRXML_COMPONENTS, TAG_COLUMN_FOOTER);
        jrColumnFooter.setAttribute(ATT_STYLE, "table_CH");
        jrColumnFooter.setAttribute(ATT_HEIGHT, String.valueOf(5));
        
        
        // Detail cell
        final Element detailCell = document.createElementNS(URI_JRXML_COMPONENTS, TAG_DETAIL_CELL);
        detailCell.setAttribute(ATT_STYLE, "table_TD");
        detailCell.setAttribute(ATT_HEIGHT, String.valueOf(40));
        
        final Element textField = document.createElementNS(URI_JRXML, TAG_TEXT_FIELD);
        
        final Element textFieldReportElement = document.createElement(TAG_REPORT_ELEMENT);
        textFieldReportElement.setAttribute(ATT_X, String.valueOf(0));
        textFieldReportElement.setAttribute(ATT_Y, String.valueOf(0));
        textFieldReportElement.setAttribute(ATT_WIDTH, String.valueOf(columnWidth));
        textFieldReportElement.setAttribute(ATT_HEIGHT, String.valueOf(30));
//        textFieldReportElement.setAttribute(ATT_POSITION_TYPE, PositionType.FLOAT.toString());
        
        final Element textFieldExpression = document.createElement(TAG_TEXT_FIELD_EXPRESSION);
        final CDATASection valueField = document.createCDATASection("$F{"+propertyType.getName().toString()+"}");
        textFieldExpression.appendChild(valueField);
        
        textField.appendChild(textFieldReportElement);
        textField.appendChild(textFieldExpression);
        detailCell.appendChild(textField);
        
        column.appendChild(tableHeader);
        column.appendChild(tableFooter);
        column.appendChild(jrColumnHeader);
        column.appendChild(jrColumnFooter);
        column.appendChild(detailCell);
        
        table.appendChild(column);
    }
    
    private void writeSubDataset(final FeatureType featureType, final List<String> avoidFields){
        
         for(final PropertyType propertyType : featureType.getProperties(true)){
            final String fieldName = propertyType.getName().toString();
            // Provides a multiplied height for comment and description fields.
            final int heightMultiplicator;
            final Markup markup;
            if (fieldName.contains("escript") || fieldName.contains("omment")){
                heightMultiplicator=this.height_multiplicator;
                markup = Markup.HTML;
            } else {
                heightMultiplicator=1;
                markup = Markup.NONE;
            }

             if (!avoidFields.contains(fieldName)) {
                SIRS.LOGGER.log(Level.FINE, fieldName);
                writeField(propertyType);
            }
        }
    }
        
    /**
     * <p>This method writes the fiels user by the Jasper Reports template.</p>
     * @param propertyType must be a setter method starting by "set"
     */
    private void writeField(final PropertyType propertyType) {
        
        // Builds the name of the field.----------------------------------------
//        final String fieldName = method.getName().substring(3, 4).toLowerCase() 
//                        + method.getName().substring(4);
        final String fieldName = propertyType.getName().toString();
        
        // Creates the field element.-------------------------------------------
        final Element field = document.createElement(TAG_FIELD);
        field.setAttribute(ATT_NAME, fieldName);
        if(propertyType instanceof AttributeType){
            final AttributeType attributeType = (AttributeType) propertyType;
            final Class attributeClass = attributeType.getValueClass();
        
            if(!attributeClass.isPrimitive()){
                field.setAttribute(ATT_CLASS, attributeClass.getCanonicalName());
            } else {
                switch(attributeClass.getCanonicalName()){
                    case BOOLEAN_PRIMITIVE_NAME: field.setAttribute(ATT_CLASS, BOOLEAN_CANONICAL_NAME);break;
                    case FLOAT_PRIMITIVE_NAME: field.setAttribute(ATT_CLASS, FLOAT_CANONICAL_NAME);break;
                    case DOUBLE_PRIMITIVE_NAME: field.setAttribute(ATT_CLASS, DOUBLE_CANONICAL_NAME);break;
                    case INTEGER_PRIMITIVE_NAME: field.setAttribute(ATT_CLASS, INTEGER_CANONICAL_NAME);break;
                    case LONG_PRIMITIVE_NAME: field.setAttribute(ATT_CLASS, LONG_CANONICAL_NAME);break;
                }
            }
        }
        
        final Element fieldDescription = document.createElement(TAG_FIELD_DESCRIPTION);
        final CDATASection description = document.createCDATASection("Mettre ici une description du champ.");
        
        // Builds the DOM tree.-------------------------------------------------
        fieldDescription.appendChild(description);
        field.appendChild(fieldDescription);
        subDataset.appendChild(field);
    }
    
    /**
     * <p>This method writes the title of the template.</p>
     * @param featureType 
     */
    private void writeTitle(final FeatureType featureType) {
        
        // Looks for the title content.-----------------------------------------
        final Element band = (Element) this.title.getElementsByTagName(TAG_BAND).item(0);
        final Element staticText = (Element) band.getElementsByTagName(TAG_STATIC_TEXT).item(0);
        final Element text = (Element) staticText.getElementsByTagName(TAG_TEXT).item(0);
        
        // Sets the title.------------------------------------------------------
        ((CDATASection) text.getChildNodes().item(0)).setData("Fiche synoptique de " + featureType.getName().getLocalPart());
        
        // Builds the DOM tree.-------------------------------------------------
        this.root.appendChild(this.title);
    }
    
    private void writePageHeader(){
        this.root.appendChild(this.pageHeader);
    }
    
    private void writeColumnHeader(){
        this.root.appendChild(this.columnHeader);
    }
}
