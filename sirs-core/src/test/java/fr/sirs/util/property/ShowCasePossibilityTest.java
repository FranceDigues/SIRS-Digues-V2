/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.sirs.util.property;

import java.util.ArrayList;
import javafx.util.StringConverter;
import org.apache.sis.util.NullArgumentException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Matthieu Bastianelli (Geomatys)
 */
public class ShowCasePossibilityTest {

    final private String abstractString = "Abrégé";
    final private String fullNameString = "Nom Complet";
    final private String bothString = "Abrégé : Nom Complet";

    final private ArrayList<String> testedList = new ArrayList();

    @Before
    public void before() {
        testedList.add("Mauvais nom");
        testedList.add("");
        testedList.add(null);
    }

    @Test
    public void getFromName_Test() {
        //Tests
        Assert.assertEquals(ShowCasePossibility.ABSTRACT, ShowCasePossibility.getFromName(abstractString));
        Assert.assertEquals(ShowCasePossibility.FULL_NAME, ShowCasePossibility.getFromName(fullNameString));
        Assert.assertEquals(ShowCasePossibility.BOTH, ShowCasePossibility.getFromName(bothString));

        // Tests des valeurs Booleans
        Assert.assertEquals(Boolean.TRUE, ShowCasePossibility.getFromName(abstractString).booleanValue);
        Assert.assertEquals(Boolean.FALSE, ShowCasePossibility.getFromName(fullNameString).booleanValue);
        Assert.assertNull(ShowCasePossibility.getFromName(bothString).booleanValue);

        int val0 = 0;

        //Tests des exceptions
        for (String testedString : testedList) {
            try {
                ShowCasePossibility.getFromName(testedString);
                val0++;
            } catch (IllegalArgumentException iae) {
                Assert.assertNotNull(testedString);
                Assert.assertEquals(testedString + " n'est pas un nom valide pour l'énum ShowCase_Possibility", iae.getMessage());
            } catch (NullArgumentException ne) {
                Assert.assertNull(testedString);
                final String message = ne.getMessage();
                Assert.assertTrue(((message!=null) && (message.contains("searchedString"))));
            }
        }

        // On vérifie que val0 n'a pas été incrémenté :
        Assert.assertEquals(0, val0);
    }

    @Test
    public void getConverter_Test() {
        StringConverter<ShowCasePossibility> converter = ShowCasePossibility.getConverter();

        //====================
        //Tests fromString() :
        //====================
        Assert.assertEquals(ShowCasePossibility.ABSTRACT, converter.fromString(abstractString));
        Assert.assertEquals(ShowCasePossibility.FULL_NAME, converter.fromString(fullNameString));
        Assert.assertEquals(ShowCasePossibility.BOTH, converter.fromString(bothString));

        // Tests des valeurs Booleans
        Assert.assertEquals(Boolean.TRUE, converter.fromString(abstractString).booleanValue);
        Assert.assertEquals(Boolean.FALSE, converter.fromString(fullNameString).booleanValue);
        Assert.assertEquals(null, converter.fromString(bothString).booleanValue);

        // Test de la valeur par défaut :
        testedList.forEach(wrongString -> {
            Assert.assertEquals(ShowCasePossibility.BOTH, converter.fromString(wrongString));
            Assert.assertEquals(null, converter.fromString(bothString).booleanValue);
        });


        //====================
        //Tests toString() :
        //====================

        Assert.assertEquals(abstractString, converter.toString(ShowCasePossibility.ABSTRACT));
        Assert.assertEquals(fullNameString, converter.toString(ShowCasePossibility.FULL_NAME));
        Assert.assertEquals(bothString, converter.toString(ShowCasePossibility.BOTH));

    }

}
