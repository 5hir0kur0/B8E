package assembler.util.assembling;

import assembler.tokens.Token;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * @author Jannik
 */
public interface Assembled {

    byte[] getCodes();

    long getAddress();

    void moveAddress(long amount);

    long getOrigin();
}
