package gui;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.awt.*;

/**
 * Taken from https://jaxb.java.net/guide/XML_layout_and_in_memory_data_layout.html
 */
class ColorAdapter extends XmlAdapter<String, Color> {
    public Color unmarshal(String s) {
        return Color.decode(s);
    }
    public String marshal(Color c) {
        return "#"+Integer.toHexString(c.getRGB());
    }
}