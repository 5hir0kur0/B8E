package misc;

import java.io.*;
import java.util.*;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * A very simple class to store settings as key-value pairs.
 * Before using this class, you should store your default values using the setDefault(...) method.
 * (Alternatively you can always use the getXXX(..., defaultValue) methods.)
 * @author 5hir0kur0
 */
public enum Settings {
    INSTANCE;
    private Properties settings;
    private Properties settingsFile;
    private Properties defaults;

    //constructors of enums are private by definition
    Settings() {
        this.defaults = new Properties();
        this.settingsFile = new Properties(this.defaults);
        this.settings = new Properties(this.settingsFile);
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
     * @param key the desired property's key
     * @param valueIsValid if the {@code test()} method yields false, the default value will be returned.
     * @return either the value found at {@code key} or {@code defaultValue} of the property.
     *         Note: If no default value is specified {@code null} will be returned.
     */
    public String getProperty(String key, Predicate<String> valueIsValid) {
        return this.getProperty(key, this.getDefault(key), valueIsValid);
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
     * @param valueIsValid if the {@code test()} method yields {@code false}, {@code defaultValue} will be returned.
     * @return the integer found at {@code key}, {@code defaultValue} or {@code 0} if no value could be found
     */
    public int getIntProperty(String key, IntPredicate valueIsValid) {
        int defaultVal;
        try {
            defaultVal = Integer.parseInt(this.getDefault(key));
        } catch (NumberFormatException | NullPointerException e) {
            //TODO: log exception
            return 0;
        }
        return getIntProperty(key, defaultVal, valueIsValid);
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
     * Get a boolean property.
     * @param key the property's key
     * @param defaultValue the default value to be returned if the key does not have a value associated with it, if
     *                     the value is not a valid integer.
     * @return either the value found at {@code key} or {@code defaultValue} of the property.
     *         Note: If no default value is specified {@code true} will be returned.
     */
    public boolean getBoolProperty(String key, boolean defaultValue) {
        String res = this.settings.getProperty(key);
        if (null == res || res.trim().isEmpty()) return defaultValue;
        return Boolean.parseBoolean(res);
    }

    /**
     * Get a boolean property.
     * @param key the property's key
     * @return the boolean found at {@code key} or {@code defaultValue}
     */
    public boolean getBoolProperty(String key) {
        String res = this.settings.getProperty(key);
        if (null == res || res.trim().isEmpty())
            return Boolean.parseBoolean(this.getDefault(key));
        return Boolean.parseBoolean(res);
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
        if (!defaults.containsKey(key))
            Logger.log("Property '" + key + "' does not exist!", Settings.class, Logger.LogLevel.WARNING);
    }

    /**
     * Sets a value with the same magnitude weight as externally loaded setting files.
     * @see java.util.Properties#setProperty(String, String)
     */
    public void setFileProperty(String key, String value) {
        this.settingsFile.setProperty(key, value);
        if (!defaults.containsKey(key))
            Logger.log("Property '" + key + "' does not exist!", Settings.class, Logger.LogLevel.WARNING);
    }

    /** @see java.util.Properties#load(Reader) */
    public void load(Reader r) throws IOException {
        this.settings.load(r);
    }

    /** @see java.util.Properties#load(Reader) */
    public void loadSettingsFile(Reader r) throws IOException {
        this.settingsFile.load(r);
    }

    /**
     * Save all key-value pairs into a file.<br>
     * NOTE 1: This does <b>not</b> include the default values (as they are hardcoded anyway).<br>
     * NOTE 2: This method will only create a file if there are settings actually to be saved.
     * @see java.util.Properties#store(Writer, String)
     */
    public void store(Writer w, String comments) throws IOException {
        if (!this.settings.isEmpty()) this.settings.store(w, comments);
    }

    /** @see java.util.Properties#list(PrintStream) */
    public void listDefaults(PrintStream out) {
        this.defaults.list(out);
    }

    public Set<String> getKeys() {
        final Set<Object> tmp = new HashSet<>();
        tmp.addAll(this.defaults.keySet());
        tmp.addAll(this.settingsFile.keySet());
        tmp.addAll(this.settings.keySet());
        return tmp.stream().map(Object::toString).collect(Collectors.toSet());
    }

    /**
     * Tries to get a key from {@code settingsFile]. If it is not present
     * the value in {@code defaults} will be returned.
     */
    private String getDefault(String key) {
        if (settingsFile.containsKey(key))
            return settingsFile.getProperty(key);
        else
            return defaults.getProperty(key);
    }
}
