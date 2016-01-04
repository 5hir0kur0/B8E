package assembler;

import assembler.util.Problem;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.List;

/**
 * This interface allows a class to turn any input in
 * a list of tokens that can be processed by the
 * assembler.
 *
 * @author Jannik
 */
public interface Tokenizer {

    /**
     * Turns a input stream into a list of tokens that then can be processed
     * by the assembler.
     *
     * @param input
     *      the input stream used as source for the tokenizing process.
     * @param problems
     *      a list of problems. Every problem that occurs while
     *      tokenizing will be stored in this list.
     *
     * @return
     *      a list of tokens that can be processed by the assembler.
     */
    List<Token> tokenize(StringReader input, List<Problem> problems);
}
