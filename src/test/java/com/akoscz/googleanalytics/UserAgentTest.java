package com.akoscz.googleanalytics;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import static org.junit.Assert.*;

public class UserAgentTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testProductName_Null() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Product name cannot be null or empty!");

        new UserAgent(null, "Version1", "Option1", "Option2");
    }

    @Test
    public void testProductName_Empty() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Product name cannot be null or empty!");

        new UserAgent("", "Version1", "Option1", "Option2");
    }

    @Test
    public void testField_AllNulls() {
        UserAgent.Field field = new UserAgent.Field(null, null, (String[])null);
        assertNull(field.toString());
    }

    @Test
    public void testField_NullVersionOptions() {
        String expectedProductName = "testProduct";

        UserAgent.Field field = new UserAgent.Field(expectedProductName, null, (String[])null);
        assertEquals(expectedProductName, field.toString());
    }

    @Test
    public void testField_EmptyVersionOptions() {
        String expectedProductName = "testProduct";

        UserAgent.Field field = new UserAgent.Field(expectedProductName, "", "");
        assertEquals(expectedProductName, field.toString());
    }


    @Test
    public void testField_NullOptions() {
        String expectedProductName = "testProduct";
        String expectedVersion = "testVersion1";

        UserAgent.Field field = new UserAgent.Field(expectedProductName, expectedVersion, null, null, null);
        assertEquals(expectedProductName + "/" + expectedVersion, field.toString());
    }


    @Test
    public void testField_NullAndEmptyOptions() {
        String expectedProductName = "testProduct";
        String expectedVersion = "testVersion1";

        UserAgent.Field field = new UserAgent.Field(expectedProductName, expectedVersion, "", "", null, null);
        assertEquals(expectedProductName + "/" + expectedVersion, field.toString());
    }

    @Test
    public void testField_WithOptions() {
        String expectedProductName = "testProduct";
        String expectedVersion = "testVersion1";
        String option1 = "option1";
        String option2 = "option2";

        UserAgent.Field field = new UserAgent.Field(expectedProductName, expectedVersion, option1, option2);
        assertEquals(expectedProductName + "/" + expectedVersion+ " (" + option1 +  "; " + option2 + ")", field.toString());
    }

    @Test
    public void testSystemPropertiesDefaults() {
        String expectedProductName = "testProduct";
        String expectedVersion = "testVersion1";
        String option1 = "option1";
        String option2 = "option2";
        String osArch = System.getProperty("os.arch");
        String osName = System.getProperty("os.name");
        String osVersion = System.getProperty("os.version");
        String javaVersion = System.getProperty("java.version");
        String javaVendor = System.getProperty("java.vendor");
        String javaVmName = System.getProperty("java.vm.name");
        String javaRuntimeName = System.getProperty("java.runtime.name");

        UserAgent userAgent = new UserAgent(expectedProductName, expectedVersion, option1, option2);
        String expectedUserAgentString = new StringBuilder()
                .append(expectedProductName)
                .append("/")
                .append(expectedVersion)
                .append(" ")
                .append("(")
                .append(osArch)
                .append(";")
                .append(" ")
                .append(osName)
                .append(";")
                .append(" ")
                .append(osVersion)
                .append(";")
                .append(" ")
                .append(option1)
                .append(";")
                .append(" ")
                .append(option2)
                .append(")")
                .append(" ")
                .append("Java")
                .append("/")
                .append(javaVersion)
                .append(" ")
                .append("(")
                .append(javaVendor)
                .append(";")
                .append(" ")
                .append(javaVmName)
                .append(";")
                .append(" ")
                .append(javaRuntimeName)
                .append(")")
                .toString();

        assertEquals(expectedUserAgentString, userAgent.toString());
    }

    @Test
    public void test_addField() {
        String expectedProductName = "testProduct";
        String expectedVersion = "testVersion1";
        String option1 = "option1";
        String option2 = "option2";
        String osArch = System.getProperty("os.arch");
        String osName = System.getProperty("os.name");
        String osVersion = System.getProperty("os.version");
        String javaVersion = System.getProperty("java.version");
        String javaVendor = System.getProperty("java.vendor");
        String javaVmName = System.getProperty("java.vm.name");
        String javaRuntimeName = System.getProperty("java.runtime.name");

        UserAgent userAgent = new UserAgent(expectedProductName, expectedVersion, option1, option2);

        String expectedCustomProductName = "customTestProduct";
        String expectedCustomVersion = "customTestVersion1";
        String customOption1 = "customOption1";
        String customOption2 = "customOption2";

        UserAgent.Field customField = new UserAgent.Field(expectedCustomProductName, expectedCustomVersion, customOption1, customOption2);
        userAgent.addField(customField);

        String expectedUserAgentString = new StringBuilder()
                .append(expectedProductName)
                .append("/")
                .append(expectedVersion)
                .append(" ")
                .append("(")
                .append(osArch)
                .append(";")
                .append(" ")
                .append(osName)
                .append(";")
                .append(" ")
                .append(osVersion)
                .append(";")
                .append(" ")
                .append(option1)
                .append(";")
                .append(" ")
                .append(option2)
                .append(")")
                .append(" ")
                .append("Java")
                .append("/")
                .append(javaVersion)
                .append(" ")
                .append("(")
                .append(javaVendor)
                .append(";")
                .append(" ")
                .append(javaVmName)
                .append(";")
                .append(" ")
                .append(javaRuntimeName)
                .append(")")
                .toString();

        assertEquals(expectedUserAgentString + " " + customField.toString(), userAgent.toString());
    }


    @Test
    public void test_fieldNameAlreadySet() {
        String expectedProductName = "testProduct";
        String expectedVersion = "testVersion1";
        String option1 = "option1";
        String option2 = "option2";

        UserAgent userAgent = new UserAgent(expectedProductName, expectedVersion, option1, option2);

        String expectedCustomProductName = expectedProductName;
        String expectedCustomVersion = "customTestVersion1";
        String customOption1 = "customOption1";
        String customOption2 = "customOption2";

        thrown.expect(RuntimeException.class);
        thrown.expectMessage("field with name '" + expectedCustomProductName + "' has already been set");

        UserAgent.Field customField = new UserAgent.Field(expectedCustomProductName, expectedCustomVersion, customOption1, customOption2);
        userAgent.addField(customField);
    }
}
