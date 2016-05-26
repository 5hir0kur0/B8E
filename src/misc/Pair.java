package misc;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

/**
 * Simple generic class to store a pair of items.
 * @author Gordian
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class Pair<X, Y> {
    public X x;
    public Y y;
    public Pair(X x, Y y) {
        this.x = x;
        this.y = y;
    }
}
