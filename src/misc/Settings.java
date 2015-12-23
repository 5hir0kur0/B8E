package misc;

import java.io.*;
import java.util.Properties;
import java.util.function.IntPredicate;
import java.util.function.Predicate;

/**
 * A very simple class to store settings as key-value pairs.
 * Before using this class, you should store your default values using the setDefault(...) method.
 * (Alternatively you can always use the getXXX(..., defaultValue) methods.)
 * @author 5hir0kur0
 */
public enum Settings {
    INSTANCE;
    private Properties settings;
    private Properties defaults;

    //ctors of enums are private by definition
    Settings() {
        defaults = new Properties();
        settings = new Properties(defaults);
    }

    /** @see java.util.Properties#getProperty(String) */
    public String getProperty(String key) {
        return this.settings.getProperty(key);
    }

    /** @see java.util.Properties#getProperty(String, String) */
    public String getProperty(String key, String defaultValue) {
        return this.settings.getProperty(key, defaultValue);
    }

    /**
     * @param key the desired property's key
     * @param defaultValue the default value to be returned if Properties#getProperty(key) yields {@code null} or
     *                     {@code valueIsValid.test(result)} yields {@code false}.
     * @param valueIsValid if the {@code test()} method yields false, the default value will be returned.
     * @return either the value found at {@code key} or {@code defaultValue}
     */
    public String getProperty(String key, String defaultValue, Predicate<String> valueIsValid) {
        String res = this.settings.getProperty(key);
        if (null == res) return defaultValue;
        if (!valueIsValid.test(res)) return defaultValue;
        return res;
    }

    /**
     * Get an integral property.
     * @param key the property's key
     * @param defaultValue the default value to be returned if the key does not have a value associated with it or if
     *                     the value is not a valid integer.
     * @return the integer found at {@code key} or {@code defaultValue}
     */
    public int getIntProperty(String key, int defaultValue) {
        String res = this.settings.getProperty(key);
        if (null == res || res.trim().isEmpty()) return defaultValue;
        try {
            return Integer.parseInt(this.settings.getProperty(key));
        } catch (NumberFormatException e) {
            //TODO: log exception
            return defaultValue;
        }
    }

    /**
     * Get an integral property.
     * @param key the property's key
     * @param defaultValue the default value to be returned if the key does not have a value associated with it, if
     *                     the value is not a valid integer or if {@code valueIsValid.test()} yields {@code false}.
     * @param valueIsValid if the {@code test()} method yields {@code false}, {@code defaultValue} will be returned.
     * @return the integer found at {@code key} or {@code defaultValue}
     */
    public int getIntProperty(String key, int defaultValue, IntPredicate valueIsValid) {
        String res = this.settings.getProperty(key);
        if (null == res || res.trim().isEmpty()) return defaultValue;
        int result;
        try {
            result = Integer.parseInt(this.settings.getProperty(key));
        } catch (NumberFormatException e) {
            //TODO: log exception
            return defaultValue;
        }
        if (!valueIsValid.test(result)) return defaultValue;
        return result;
    }

    /**
     * Set a default value for a key.
     * @param key the key to be used
     * @param value the value to be set as the default for the given key
     * @throws IllegalArgumentException when there already is a default for this key
     */
    public void setDefault(String key, String value) throws IllegalArgumentException {
        if (this.defaults.getProperty(key) != null)
            throw new IllegalArgumentException("Cannot overwrite default value.");
        this.defaults.setProperty(key, value);
    }

    /**
     * The only class which should use this method is the settings dialogue in the GUI, because the key-value pairs
     * set through this method will be saved to disk when store(...) is called.
     * @see java.util.Properties#setProperty(String, String)
     */
    public void setProperty(String key, String value) {
        this.settings.setProperty(key, value);
    }

    /** @see java.util.Properties#load(InputStream) */
    public void load(InputStream is) throws IOException {
        this.settings.load(is);
    }

    /**
     * Save all key-value pairs into a file.<br>
     * NOTE 1: This does <b>not</b> include the default values (as they are hardcoded anyway).<br>
     * NOTE 2: This method will only create a file if there are settings actually to be saved.
     * @see java.util.Properties#store(OutputStream, String)
     */
    public void store(OutputStream os, String comments) throws IOException {
        if (!this.settings.isEmpty()) this.settings.store(os, comments);
    }
}