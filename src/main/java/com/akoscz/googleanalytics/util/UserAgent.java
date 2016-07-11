package com.akoscz.googleanalytics.util;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class to build the UserAgent string.
 *
 * By default the following are included in the user agent string:
 *      System.getProperty("os.arch")
 *      System.getProperty("os.name")
 *      System.getProperty("os.version")
 *      System.getProperty("java.version")
 *      System.getProperty("java.vendor")
 *      System.getProperty("java.vm.name")
 *      System.getProperty("java.runtime.name")
 *
 * The format of the user agent string this class generates looks like this:
 *
 *      ProductName/ProductVersion (os.arch; os.name, os.version, options...)
 *      Java/java.version (java.vendor; java.vm.name; java.runtime.name)
 *
 * where ProductName, ProductVersion and options... are parameters specified in the constructor.
 *
 */
public class UserAgent {
    private final List<UserAgent.Field> fields = new ArrayList<UserAgent.Field>();

    public UserAgent(String productName, String productVersion, String... options) {
        if (productName == null || productName.isEmpty()) {
            throw new IllegalArgumentException("Product name cannot be null or empty!");
        }

        try {
            addField(new UserAgent.Field(
                    productName,
                    productVersion,
                    ArrayUtils.addAll(
                            new String[]{
                                    System.getProperty("os.arch"),
                                    System.getProperty("os.name"),
                                    System.getProperty("os.version")
                            },
                            options
                    )
            ));

            addField(new UserAgent.Field(
                    "Java",
                    System.getProperty("java.version"),
                    System.getProperty("java.vendor"),
                    System.getProperty("java.vm.name"),
                    System.getProperty("java.runtime.name")));
        } catch (RuntimeException ex) {
            Logger.getLogger(
                    UserAgent.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Add a field to the UserAgent.
     *
     * @param field The UserAgentField to add.
     * @throws RuntimeException If the field already exists.
     */
    public void addField(final UserAgent.Field field) {
        for (UserAgent.Field object : fields) {
            if (object.name.compareTo(field.name) == 0) {
                throw new RuntimeException("field with name '" + field.name + "' has already been set");
            }
        }
        fields.add(field);
    }

    /**
     * Get the string representation of UserAgent.
     *
     * @return String
     */
    @Override
    public final String toString() {
        Field[] elements = fields.toArray(new Field[0]);
        return StringUtils.join(elements, " ");
    }

    /**
     * UserAgent Field
     */
    public static class Field {

        private final String name;
        private final String version;
        private final String[] options;

        /**
         * Constructs a field object with the given parameters.
         *
         * @param fieldName    Name of the field.
         *                     Cannot be null or empty value
         * @param fieldVersion Version of the field.
         *                     Cannot be null or empty value
         * @param fieldOptions 0 or more options for the field.
         *                     Cannot be null. Empty string values will be ignored.
         */
        Field(final String fieldName,
              final String fieldVersion,
              final String... fieldOptions) {
            name = fieldName;
            version = fieldVersion;
            options = fieldOptions;
        }

        private static boolean containsNullOrEmptyStrings(String[] stringArray) {
            for(String str: stringArray) {
                if (str == null || str.isEmpty()) return true;
            }

            return false;
        }

        private static String[] filterNullAndEmptyStrings(String[] stringArray) {
            // if there are no nulls or empties return the original array
            if (!containsNullOrEmptyStrings(stringArray)) {
                return stringArray;
            }

            ArrayList<String> filteredList = new ArrayList<String>();
            for(String str: stringArray) {
                // remove empty string
                if (str != null && !str.isEmpty()) filteredList.add(str);
            }
            return filteredList.toArray(new String[filteredList.size()]);
        }

        @Override
        public final String toString() {
            String fieldString = name;
            if (version != null && !version.isEmpty()) {
                fieldString += "/" + version;
            }

            if (options != null && options.length > 0) {
                String[] filteredOptions = filterNullAndEmptyStrings(options);

                String part = null;
                if(filteredOptions != null && filteredOptions.length > 0) {
                    part = String.format("(%s)", StringUtils.join(filteredOptions, "; "));
                }

                if (part != null) fieldString = fieldString.concat(" ").concat(part);

            }
            return fieldString;
        }
    }
}