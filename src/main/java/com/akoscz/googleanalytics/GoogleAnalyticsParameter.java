package com.akoscz.googleanalytics;

import lombok.SneakyThrows;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * A NameValuePair immutable data class.
 * To build one use:
 *      GoogleAnalyticsParameter.of(String key, String value)
 *
 * The toString() method returns a simple "name=value" String representation.
 * The toStringWithPrefix(String prefix) convenience method prepends the prefix string to the result of toString().
 */
@Value(staticConstructor="of")
class GoogleAnalyticsParameter implements NameValuePair {

    private static final String ENCODING = "UTF-8";
    public static final GoogleAnalyticsParameter EMPTY = new GoogleAnalyticsParameter(null, null);

    String name;
    String value;

    @SneakyThrows(UnsupportedEncodingException.class)
    public String toString() {
        if (StringUtils.isEmpty(value) || StringUtils.isEmpty(name)) {
            return null;
        }
        return name + "=" + URLEncoder.encode(value, ENCODING);
    }


    /* package */ int countBytes() {
        String str = toString();
        int bytesCount = 0;
        if (StringUtils.isNotEmpty(str)) {
            bytesCount = str.getBytes().length;
        }
        return bytesCount;
    }

    /**
     * Return the string value of this GoogleAnalyticsPrameter and prepend the 'prefix' string to the string value.
     * If the prexix is null or empty, the prefix is ignored and toString() is returned.
     *
     * @param prefix The prefix to prepend
     * @return The string value prepended with the prefix.  This method will return null if the toString() method returns null or the empty string.
     */
    public String toStringWithPrefix(String prefix) {
        // if prefix is empty return to string
        if (StringUtils.isEmpty(prefix)) return toString();

        String stringValue = toString();
        // if the string value is empty return null
        if (StringUtils.isEmpty(stringValue)) return null;
        return (StringUtils.isEmpty(prefix) ? "" : prefix) + stringValue;
    }
}
