package assembler.util.assembling;

import assembler.tokens.Token;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * @author Noxgrim
 */
public interface Assembled {

    byte[] getCodes();

    long getAddress();

    void moveAddress(long amount);

    long getOrigin();

    Token[] getTokens();

    Path getFile();
}
