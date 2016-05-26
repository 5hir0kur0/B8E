package assembler.util.assembling;

import assembler.tokens.LabelToken;
import assembler.tokens.Token;
import assembler.util.problems.Problem;

import java.nio.file.Path;
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

    Token[] getTokens();

    Path getFile();

    int compile(List<Problem<?>> problems, List<LabelToken> labels);
}
