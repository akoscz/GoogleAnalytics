package com.akoscz.googleanalytics.util;

/**
 * A custom string builder that does not accept null or empty character sequences in the append() method.
 */
public class NonNullOrEmptyStringBuilder {
    StringBuilder builder;

    public NonNullOrEmptyStringBuilder() {
        builder = new StringBuilder();
    }

    public NonNullOrEmptyStringBuilder append(CharSequence charSequence) {
        // do not append null or empty character sequences
        if (charSequence != null && charSequence.length() >= 0) {
            builder.append(charSequence);
        }
        return this;
    }

    public String toString() {
        return builder.toString();
    }
}