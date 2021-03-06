package misc;

import gui.DummyAttributeSet;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Simple generic class to store a pair of items.
 * @author 5hir0kur0
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlSeeAlso({Pattern.class, DummyAttributeSet.class})
public class Pair<X, Y> {
    public X x;
    public Y y;
    public Pair(X x, Y y) {
        this.x = x;
        this.y = y;
    }
    @SuppressWarnings("unused") private Pair() { } // for JAXB

    @Override
    public String toString() {
        return getClass().getName() + "<" + Objects.toString(x) + ", " + Objects.toString(y) + ">@" +
                Integer.toHexString(hashCode());
    }
}
