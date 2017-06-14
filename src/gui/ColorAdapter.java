package gui;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.awt.Color;

/**
 * (inspired by <a href="https://jaxb.java.net/guide/XML_layout_and_in_memory_data_layout.html">
 *     https://jaxb.java.net/guide/XML_layout_and_in_memory_data_layout.html
 *     </a>)
 * @author 5hir0kur0
 */
class ColorAdapter extends XmlAdapter<String, Color> {
    @Override
    public Color unmarshal(String s) {
        return new Color(Long.valueOf(s.substring(1), 16).intValue(), true);
    }
    @Override
    public String marshal(Color c) {
        return c == null ? null : "#"+Integer.toHexString(c.getRGB()).toUpperCase();
    }
}