package com.akoscz.googleanalytics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class to build the UserAgent string.
 */
public class UserAgent {

    private final List<UserAgent.Field> fields = new ArrayList<UserAgent.Field>();

    public UserAgent(String productName, String productVersion, String... options) {
        if (productName == null || productName.isEmpty()) {
            throw new IllegalArgumentException("Product name cannot be null or empty!");
        }

        try {
            addField(
                    new UserAgent.Field(
                            productName,
                            productVersion,
                            concatAll(
                                    new String[]{
                                            System.getProperty("os.arch"),
                                            System.getProperty("os.name"),
                                            System.getProperty("os.version")
                                    },
                                    options
                            )
                    ));

            addField(
                    new UserAgent.Field(
                            "Java",
                            System.getProperty("java.version"),
                            new String[]{
                                    System.getProperty("java.vendor"),
                                    System.getProperty("java.vm.name"),
                                    System.getProperty("java.runtime.name")
                            }
                    ));
        } catch (RuntimeException ex) {
            Logger.getLogger(
                    UserAgent.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Concactinate two string arrays.
     *
     * @param first The first string array
     * @param rest  The rest of the string arrays
     * @param <T>
     * @return
     */
    private static <T> T[] concatAll(T[] first, T[]... rest) {
        if (first == null && rest != null) {
            first = (T[]) new Object[0];
        } else if (rest == null || rest.length == 0) {
            return first;
        }

        int totalLength = first.length;
        for (T[] array : rest) {
            totalLength += array.length;
        }
        T[] result = Arrays.copyOf(first, totalLength);
        int offset = first.length;
        for (T[] array : rest) {
            System.arraycopy(array, 0, result, offset, array.length);
            offset += array.length;
        }
        return result;
    }

    /**
     * Combine the elements of a string array into a single string where each element
     * is separated by the specified separator string.
     *
     * @param elements  The strings to combine.
     * @param separator The separator string.
     * @return String The combined string.
     */
    private static <T> String combine(final T[] elements, final String separator) {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(elements[0]);

        for (int i = 1; i < elements.length; i++) {
            stringBuilder.append(separator);
            stringBuilder.append(elements[i]);
        }

        return stringBuilder.toString();
    }

    /**
     * Add a field to the UserAgent.
     *
     * @param field The UserAgentField to add.
     * @throws RuntimeException If the field already exists.
     */
    public final void addField(final UserAgent.Field field) {
        for (UserAgent.Field object : fields) {
            if (object.name.compareTo(field.name) == 0) {
                throw new RuntimeException("field has already been set: " + field.name);
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
        return UserAgent.combine(elements, " ");
    }

    /**
     * UserAgent Field
     */
    private static class Field {

        private final String name;
        private final String version;
        private final String[] options;

        /**
         * Constructor with optional values.
         *
         * @param fieldName    Name of the field.
         * @param fieldVersion Version of the field.
         * @param fieldOptions 0 or more options for the field.
         */
        public Field(
                final String fieldName,
                final String fieldVersion,
                final String... fieldOptions) {
            name = fieldName;
            version = fieldVersion;
            options = fieldOptions;
        }

        @Override
        public final String toString() {
            String result = name;
            if (version != null && !version.isEmpty()) {
                result += "/" + version;
            }

            if (options.length > 0) {
                String part = String.format("(%s)", UserAgent.combine(options, "; "));

                result = result.concat(" ").concat(part);
            }
            return result;
        }
    }
}