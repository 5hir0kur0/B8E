package assembler.arc8051;

import assembler.Assembler;
import assembler.tokens.LabelToken;
import assembler.tokens.Token;
import assembler.util.assembling.Assembled;
import assembler.util.problems.Problem;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Noxgrim
 */
public class Assembler8051 implements Assembler {

    private Preprocessor8051 preprocessor;
    private Tokenizer8051  tokenizer;

    private byte[] result;

    public Assembler8051() {
        this.preprocessor  = new Preprocessor8051();
        this.tokenizer = new Tokenizer8051();
    }

    private List<Token> resolve(Path source, List<Problem> problems) {
        Path directory = Paths.get("."); // TODO: Use Project.getWorkingDirectory
        List<String> inputOutput = new LinkedList<>();
        problems.addAll(preprocessor.preprocess(directory, source, inputOutput));


        return tokenizer.tokenize(inputOutput, problems);
    }

    @Override
    public byte[] assemble(Path source, List<Problem> problems) {
        result = new byte[0xFFFF+1];

        List<LabelToken> labels = new LinkedList<>();

        List<Assembled> unresolved = new LinkedList<>();



        return result;
    }
}
