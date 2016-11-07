package assembler.util.assembling;

import assembler.tokens.LabelToken;
import assembler.tokens.Token;
import assembler.util.problems.Problem;

import java.nio.file.Path;
import java.util.List;

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

    int compile(List<Problem<?>> problems, List<LabelToken> labels);

    LabelToken[] getLabels();
}
