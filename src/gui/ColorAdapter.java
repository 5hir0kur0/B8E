package gui;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.awt.*;

/**
 * (inspired by <a href="https://jaxb.java.net/guide/XML_layout_and_in_memory_data_layout.html">
 *     https://jaxb.java.net/guide/XML_layout_and_in_memory_data_layout.html
 *     </a>)
 * @author Gordian
 */
class ColorAdapter extends XmlAdapter<String, Color> {
    public Color unmarshal(String s) {
        return new Color(Long.valueOf(s.substring(1), 16).intValue(), true);
    }
    public String marshal(Color c) {
        return "#"+Integer.toHexString(c.getRGB()).toUpperCase();
    }
}