package misc;

import gui.DummyAttributeSet;

import javax.xml.bind.annotation.*;
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
        return getClass().getName() + "<" + x.toString() + ", " + y.toString() + ">@" + Integer.toHexString(hashCode());
    }
}
