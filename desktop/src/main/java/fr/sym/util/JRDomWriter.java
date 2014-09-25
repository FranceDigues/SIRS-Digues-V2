/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.sym.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Calendar;
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
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 *
 * @author Samuel Andrés (Geomatys)
 */
public class JRDomWriter {
    
    private final Document document;
    private final Element root;
    private final Element title;
    private final Element pageHeader;
    private final Element columnHeader;
    private final Element detail;
    private final Element columnFooter;
    private final Element pageFooter;
    private final Element lastPageFooter;
    private File output;
    
    
    private static final String FIELDS_VERTICAL_ALIGNMENT = "Middle";
    private static final String FIELDS_FONT_NAME = "Serif";
    private static final int FIELDS_HEIGHT = 16;
    private static final String DATE_PATTERN = "dd/MM/yyyy à hh:mm:ss";
    private static final int INDENT_LABEL = 10;
    private static final int LABEL_WIDTH = 140;
    private static final int PAGE_WIDTH = 595;
    private static final int PAGE_HEIGHT = 842;
    private static final int COLUMN_WIDTH = 555;
    private static final int LEFT_MARGIN = 20;
    private static final int RIGHT_MARGIN = 20;
    private static final int TOP_MARGIN = 20;
    private static final int BOTTOM_MARGIN = 20;
    private static final int FIELDS_INTERLINE = 8;
    
     
    private static final String URI_JRXML = "http://jasperreports.sourceforge.net/jasperreports";
    private static final String URI_XSI = "http://www.w3.org/2001/XMLSchema-instance";
    private static final String PREFIX_XSI = "xsi";
    private static final String ATT_XSI_SCHEMA_LOCATION = "schemaLocation";
    private static final String TAG_JASPER_REPORT = "jasperReport";
    private static final String TAG_PAGE_WIDTH = "pageWidth";
    private static final String TAG_PAGE_HEIGHT = "pageHeight";
    private static final String TAG_COLUMN_WIDTH = "columnWidth";
    private static final String TAG_LEFT_MARGIN = "leftMargin";
    private static final String TAG_RIGHT_MARGIN = "rightMargin";
    private static final String TAG_TOP_MARGIN = "topMargin";
    private static final String TAG_BOTTOM_MARGIN = "bottomMargin";
    private static final String TAG_FIELD = "field";
    private static final String TAG_FIELD_DESCRIPTION = "fieldDescription";
    private static final String TAG_TITLE = "title";
    private static final String TAG_PAGE_HEADER = "pageHeader";
    private static final String TAG_COLUMN_HEADER = "columnHeader";
    private static final String TAG_DETAIL = "detail";
    private static final String TAG_COLUMN_FOOTER = "colulmnFooter";
    private static final String TAG_PAGE_FOOTER = "pageFooter";
    private static final String TAG_LAST_PAGE_FOOTER = "lastPageFooter";
    private static final String TAG_BAND = "band";
    private static final String TAG_STATIC_TEXT = "staticText";
    private static final String TAG_TEXT_ELEMENT = "textElement";
    private static final String TAG_REPORT_ELEMENT = "reportElement";
    private static final String TAG_FONT = "font";
    private static final String TAG_TEXT_FIELD = "textField";
    private static final String TAG_TEXT_FIELD_EXPRESSION = "textFieldExpression";
    private static final String TAG_TEXT = "text";
    private static final String TAG_PATTERN = "pattern";
    private static final String TAG_BOX = "box";
    private static final String TAG_BOTTOM_PEN = "bottomPen";
    
    private static final String ATT_MODE = "mode";
    private static final String ATT_LINE_WIDTH = "lineWidth";
    private static final String ATT_LINE_COLOR = "lineColor";
    private static final String ATT_BACKCOLOR = "backcolor";
    private static final String ATT_NAME = "name";
    private static final String ATT_TEXT_ALIGNMENT = "textAlignment";
    private static final String ATT_VERTICAL_ALIGNMENT = "verticalAlignment";
    private static final String ATT_FONT_NAME = "fontName";
    private static final String ATT_IS_BOLD = "isBold";
    private static final String ATT_LANGUAGE = "language";
    private static final String ATT_PAGE_WIDTH = "pageWidth";
    private static final String ATT_PAGE_HEIGHT = "pageHeight";
    private static final String ATT_COLUMN_WIDTH = "columnWidth";
    private static final String ATT_LEFT_MARGIN = "leftMargin";
    private static final String ATT_RIGHT_MARGIN = "rightMargin";
    private static final String ATT_TOP_MARGIN = "topMargin";
    private static final String ATT_BOTTOM_MARGIN = "bottomMargin";
    private static final String ATT_UUID = "uuid";
    private static final String ATT_CLASS = "class";
    private static final String ATT_HEIGHT = "height";
    private static final String ATT_X = "x";
    private static final String ATT_Y = "y";
    private static final String ATT_WIDTH = "width";
    private static final String ATT_SPLIT_TYPE = "splitType";
    private static enum SplitType {
        STRETCH("Stretch"), PREVENT("Prevent"), IMMEDIATE("Immediate");
        private final String splitType;
        private SplitType(String splitType){this.splitType=splitType;}
        
        @Override
        public String toString(){return splitType;}
    }; 
    
    private JRDomWriter(){
        this.document = null;
        this.root = null; 
        this.title = null; 
        this.pageHeader = null;
        this.columnHeader = null;
        this.detail = null;
        this.columnFooter = null;
        this.pageFooter = null;
        this.lastPageFooter = null;
    }
    
    public JRDomWriter(final InputStream stream) throws ParserConfigurationException, SAXException, IOException {
        
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder constructeur = factory.newDocumentBuilder();
        this.document = constructeur.parse(stream);
        stream.close();
        
        this.root = this.document.getDocumentElement();
        this.title = (Element) this.root.getElementsByTagName(TAG_TITLE).item(0);
        this.pageHeader = (Element) this.root.getElementsByTagName(TAG_PAGE_HEADER).item(0);
        this.columnHeader = (Element) this.root.getElementsByTagName(TAG_COLUMN_HEADER).item(0);
        this.detail = (Element) this.root.getElementsByTagName(TAG_DETAIL).item(0);
        this.columnFooter = (Element) this.root.getElementsByTagName(TAG_COLUMN_FOOTER).item(0);
        this.pageFooter = (Element) this.root.getElementsByTagName(TAG_PAGE_FOOTER).item(0);
        this.lastPageFooter = (Element) this.root.getElementsByTagName(TAG_LAST_PAGE_FOOTER).item(0);
        
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
     * @param classToMap
     * @throws TransformerException
     * @throws IOException
     */
    public void write(final Class classToMap) throws TransformerException, IOException, Exception {
        
        // Remove elements before inserting fields.-----------------------------
        this.root.removeChild(this.title);
        this.root.removeChild(this.pageHeader);
        this.root.removeChild(this.columnHeader);
        this.root.removeChild(this.detail);
        
        // Modifies the template, based on the given class.---------------------
        this.writeObject(classToMap);
        
        // Serializes the document.---------------------------------------------
        //DomUtilities.write(this.document, this.output);
        final Source source = new DOMSource(this.document);
        final Result result = new StreamResult(this.output);
        final TransformerFactory factory = TransformerFactory.newInstance();
        final Transformer trs = factory.newTransformer();
        trs.setOutputProperty(OutputKeys.INDENT, "yes");
        trs.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        trs.transform(source, result);
    }

    /**
     * <p>This method modifies the body of the DOM.</p>
     * @param classToMap
     * @throws Exception 
     */
    private void writeObject(final Class classToMap) throws Exception {
        
        // Sets the initial fields used by the template.------------------------
        final Method[] methods = classToMap.getMethods();
        for (final Method method : methods){
            if(PrinterUtilities.isSetter(method)){
                this.writeField(method);
            }
        }
        
        // Modifies the title block.--------------------------------------------
        this.writeTitle(classToMap);
        
        // Writes the headers.--------------------------------------------------
        this.writePageHeader();
        this.writeColumnHeader();
        
        // Builds the body of the Jasper Reports template.----------------------
        this.writeDetail(classToMap);
    }
        
    /**
     * <p>This method writes the fiels user by the Jasper Reports template.</p>
     * @param method must be a setter method starting by "set"
     */
    private void writeField(final Method method) {
        
        // Builds the name of the field.----------------------------------------
        final String fieldName = method.getName().substring(3, 4).toLowerCase() 
                        + method.getName().substring(4);
        
        // Creates the field element.-------------------------------------------
        final Element field = document.createElement(TAG_FIELD);
        field.setAttribute(ATT_NAME, fieldName);
        if(!method.getParameterTypes()[0].isPrimitive())
            field.setAttribute(ATT_CLASS, method.getParameterTypes()[0].getCanonicalName());
        
        final Element fieldDescription = document.createElement(TAG_FIELD_DESCRIPTION);
        final CDATASection description = document.createCDATASection("Mettre ici une description du champ.");
        
        // Builds the DOM tree.-------------------------------------------------
        fieldDescription.appendChild(description);
        field.appendChild(fieldDescription);
        this.root.appendChild(field);
    }
    
    /**
     * <p>This method writes the title of the template.</p>
     * @param classToMap 
     */
    private void writeTitle(final Class classToMap) {
        
        // Looks for the title content.-----------------------------------------
        final Element band = (Element) this.title.getElementsByTagName(TAG_BAND).item(0);
        final Element staticText = (Element) band.getElementsByTagName(TAG_STATIC_TEXT).item(0);
        final Element text = (Element) staticText.getElementsByTagName(TAG_TEXT).item(0);
        
        // Sets the title.------------------------------------------------------
        ((CDATASection) text.getChildNodes().item(0)).setData(
                "Fiche synoptique de " + classToMap.getSimpleName());
        
        // Builds the DOM tree.-------------------------------------------------
        this.root.appendChild(this.title);
    }
    
    private void writePageHeader(){
        this.root.appendChild(this.pageHeader);
    }
    
    private void writeColumnHeader(){
        this.root.appendChild(this.columnHeader);
    }
    
    /**
     * <p>This method writes the content of the detail element.</p>
     * @param classToMap
     * @throws Exception 
     */
    private void writeDetail(final Class classToMap) throws Exception{
        
        // Loops over the method looking for setters (based on the field names).
        final Method[] methods = classToMap.getMethods();
        int i = 0;
        for (final Method method : methods){
            if(PrinterUtilities.isSetter(method)){
                final String fieldName = method.getName().substring(3, 4).toLowerCase() 
                        + method.getName().substring(4);
                
                // Writes a colored band.---------------------------------------
                this.writeDetailRow(i);
                
                // Writes the label.--------------------------------------------
                this.writeDetailLabel(fieldName, i);
                
                // Writes the variable field.-----------------------------------
                this.writeDetailValue(fieldName, method.getParameterTypes()[0], i);
                i++;
            }
        }
        
        // Sizes the detail element givent the field number.--------------------
        ((Element) this.detail.getElementsByTagName(TAG_BAND).item(0))
                .setAttribute(ATT_HEIGHT, String.valueOf((FIELDS_HEIGHT+FIELDS_INTERLINE)*i));
        
        // Builds the DOM tree.-------------------------------------------------
        this.root.appendChild(this.detail);
    }
    
    /**
     * <p>This method writes the backgroud of a row.</p>
     * @param order 
     */
    private void writeDetailRow(int order){
        
        // Looks for the band element.------------------------------------------
        final Element band = (Element) this.detail.getElementsByTagName(TAG_BAND).item(0);
        final Element staticText = this.document.createElement(TAG_STATIC_TEXT);
        
        // Sets the field.------------------------------------------------------
        final Element reportElement = this.document.createElement(TAG_REPORT_ELEMENT);
        reportElement.setAttribute(ATT_X, "0");
        reportElement.setAttribute(ATT_Y, String.valueOf((FIELDS_HEIGHT+FIELDS_INTERLINE)*order));
        reportElement.setAttribute(ATT_WIDTH, String.valueOf(COLUMN_WIDTH));
        reportElement.setAttribute(ATT_HEIGHT, String.valueOf(FIELDS_HEIGHT));
        reportElement.setAttribute(ATT_MODE, "Opaque");
        reportElement.setAttribute(ATT_BACKCOLOR, "#F0F0F0");
        
        final Element box = this.document.createElement(TAG_BOX);
        
        final Element bottomPen = this.document.createElement(TAG_BOTTOM_PEN);
        bottomPen.setAttribute(ATT_LINE_WIDTH, "0.25");
        bottomPen.setAttribute(ATT_LINE_COLOR, "#CCCCCC");
        
        final Element text = this.document.createElement(TAG_TEXT);
        final CDATASection labelField = this.document.createCDATASection("");
        
        // Builds the DOM tree.-------------------------------------------------
        text.appendChild(labelField);
        staticText.appendChild(reportElement);
        box.appendChild(bottomPen);
        staticText.appendChild(box);
        staticText.appendChild(text);
        band.appendChild(staticText);
    }
    /**
     * <p>This method writes the label of a given field.</p>
     * @param label
     * @param order 
     */
    private void writeDetailLabel(final String label, int order){
        
        // Looks for the band element.------------------------------------------
        final Element band = (Element) this.detail.getElementsByTagName(TAG_BAND).item(0);
        
        // Sets the field.------------------------------------------------------
        final Element staticText = this.document.createElement(TAG_STATIC_TEXT);
        
        final Element reportElement = this.document.createElement(TAG_REPORT_ELEMENT);
        reportElement.setAttribute(ATT_X, String.valueOf(INDENT_LABEL));
        reportElement.setAttribute(ATT_Y, String.valueOf((FIELDS_HEIGHT+FIELDS_INTERLINE)*order));
        reportElement.setAttribute(ATT_WIDTH, String.valueOf(LABEL_WIDTH));
        reportElement.setAttribute(ATT_HEIGHT, String.valueOf(FIELDS_HEIGHT));
        
        final Element textElement = this.document.createElement(TAG_TEXT_ELEMENT);
        textElement.setAttribute(ATT_VERTICAL_ALIGNMENT, FIELDS_VERTICAL_ALIGNMENT);
        textElement.setAttribute(ATT_TEXT_ALIGNMENT, "Left");
        
        final Element font = this.document.createElement(TAG_FONT);
        font.setAttribute(ATT_IS_BOLD, "true");
        font.setAttribute(ATT_FONT_NAME, FIELDS_FONT_NAME);
        
        final Element text = this.document.createElement(TAG_TEXT);
        final CDATASection labelField = this.document.createCDATASection(label);
        
        // Builds the DOM tree.-------------------------------------------------
        text.appendChild(labelField);
        staticText.appendChild(reportElement);
        textElement.appendChild(font);
        staticText.appendChild(textElement);
        staticText.appendChild(text);
        band.appendChild(staticText);
    }
    
    /**
     * <p>This method writes the variable of a given field.</p>
     * @param field
     * @param c
     * @param order 
     */
    private void writeDetailValue(final String field, final Class c, int order){
        
        // Looks for the band element.------------------------------------------
        final Element band = (Element) this.detail.getElementsByTagName(TAG_BAND).item(0);
        
        // Sets the field.------------------------------------------------------
        final Element textField = this.document.createElement(TAG_TEXT_FIELD);
        if (c==Calendar.class)
            textField.setAttribute(TAG_PATTERN, DATE_PATTERN);
        
        final Element reportElement = this.document.createElement(TAG_REPORT_ELEMENT);
        reportElement.setAttribute(ATT_X, String.valueOf(INDENT_LABEL+LABEL_WIDTH));
        reportElement.setAttribute(ATT_Y, String.valueOf((FIELDS_HEIGHT+FIELDS_INTERLINE)*order));
        reportElement.setAttribute(ATT_WIDTH, String.valueOf(COLUMN_WIDTH-(INDENT_LABEL+LABEL_WIDTH)));
        reportElement.setAttribute(ATT_HEIGHT, String.valueOf(FIELDS_HEIGHT));
        
        final Element textElement = this.document.createElement(TAG_TEXT_ELEMENT);
        textElement.setAttribute(ATT_VERTICAL_ALIGNMENT, FIELDS_VERTICAL_ALIGNMENT);
        textElement.setAttribute(ATT_TEXT_ALIGNMENT, "Left");
        
        final Element font = this.document.createElement(TAG_FONT);
        font.setAttribute(ATT_FONT_NAME, FIELDS_FONT_NAME);
        
        final Element textFieldExpression = this.document.createElement(TAG_TEXT_FIELD_EXPRESSION);
        
        // The content of the field is specific in case of Calendar field.------
        final CDATASection valueField;
        if (c==Calendar.class) 
            valueField = this.document.createCDATASection("$F{"+field+"}.getTime()");
        else 
            valueField = this.document.createCDATASection("$F{"+field+"}");
        
        // Builds the DOM tree.-------------------------------------------------
        textFieldExpression.appendChild(valueField);
        textField.appendChild(reportElement);
        textElement.appendChild(font);
        textField.appendChild(textElement);
        textField.appendChild(textFieldExpression);
        band.appendChild(textField);
    }
}
