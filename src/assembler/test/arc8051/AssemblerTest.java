package assembler.test.arc8051;

import assembler.Assembler;
import assembler.Preprocessor;
import assembler.Tokenizer;
import assembler.arc8051.MC8051Library;
import assembler.arc8051.Preprocessor8051;
import assembler.arc8051.Tokenizer8051;
import assembler.tokens.Token;
import assembler.util.problems.ExceptionProblem;
import assembler.util.problems.Problem;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

/**
 * @author Noxgrim
 */
public class AssemblerTest {

    private Assembler testAssem;
    private Preprocessor prepr;
    private Tokenizer tokenizer;

    private Random random;

    @Before
    public void setUp() throws Exception {
        prepr = new Preprocessor8051();
        tokenizer = new Tokenizer8051();
        testAssem = new Assembler(MC8051Library.PROVIDER, prepr, tokenizer);
        random = new Random();
    }

    @Test
    public void testPreprocessor_LowerCase() {
        System.out.println("____________Testing Preprocessor8051.lowerCase()");
        try {
            Method m = prepr.getClass().getDeclaredMethod("lowerCase", String.class, List.class);
            m.setAccessible(true);

            HashMap<String, String> map = new HashMap<>(5);

            map.put("TY the \"Tasmanian\" 'Tiger' 4", "ty the \"Tasmanian\" 'Tiger' 4"); // Sorry
            map.put("Test test TE'ST'!", "test test te'ST'!");
            map.put("The \\\"test Tester\\\" tested \"Tony's Test\". ", "the \"test tester\" tested \"Tony's Test\". ");
            map.put("\"T'E\"S'T\"?'", "\"T'E\"s'T\"?'");
            map.put("\\Tests For Testing \\Tests.", "ests for testing ests.");

            for (String test : map.keySet()) {
                System.out.print("Testing: \"" + test + "\", Expecting: \"" + map.get(test) + "\"...");
                assertEquals(map.get(test), m.invoke(prepr, test, new ArrayList<Problem>()));
                System.out.println("Passed.");
            }

            m.setAccessible(false);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
            fail("Unexpected Exception.");
        }
    }

    @Test
    public void testTokenizer_GetNumber() {
        System.out.println("____________Testing Tokenizer8051.getNumber()");
        try {
            Method m = tokenizer.getClass().getDeclaredMethod("getNumber", Matcher.class, List.class);
            m.setAccessible(true);

            Pattern pattern = MC8051Library.ADDRESS_PATTERN;

            HashMap<String, String> map = new HashMap<>();
            int rand;

            map.put(Integer.toBinaryString(rand = random.nextInt(0x10000))+"b",  Integer.toString(rand));
            map.put(Integer.toOctalString(rand = random.nextInt(0x10000))+"o",   Integer.toString(rand));
            map.put(Integer.toOctalString(rand = random.nextInt(0x10000))+"q",   Integer.toString(rand));
            map.put(Integer.toString(rand = random.nextInt(0x10000))+"d",        Integer.toString(rand));
            map.put("0"+Integer.toHexString(rand = random.nextInt(0x10000))+"h", Integer.toString(rand));
            map.put("0b"+Integer.toBinaryString(rand = random.nextInt(0x10000)), Integer.toString(rand));
            map.put("0" +Integer.toOctalString(rand = random.nextInt(0x10000)),  Integer.toString(rand));
            map.put("0d"+Integer.toString(rand = random.nextInt(0x10000)),       Integer.toString(rand));
            map.put("0x"+Integer.toHexString(rand = random.nextInt(0x10000)),    Integer.toString(rand));

            for (String test : map.keySet()) {
                System.out.print("Testing: \"" + test + "\", Expecting: \"" + map.get(test) + "\"...");
                Matcher matcher = pattern.matcher(test);
                List<Problem> list = new ArrayList<>();
                if (matcher.matches()) {
                    list.clear();
                    String tested = (String) m.invoke(tokenizer, matcher, list);
                    for (Problem p : list)
                        System.out.println("\n"+ p);
                    assertEquals(map.get(test), tested);
                    System.out.println("Passed.");
                } else
                    fail("Matcher did'nt match!");
            }

            m.setAccessible(false);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
            fail("Unexpected Exception.");
        }
    }

    @Test
    public void test_assemble() {
        System.out.println("____________Testing Assembler.assemble()");

        List<Problem> problems = new ArrayList<>();
        boolean ex = false, outProblems = false;
        try {
            System.out.println("Running assembler:");
            Path test = Paths.get("src/assembler/test/arc8051/");
            System.out.println("__Test directory: "+test.toAbsolutePath());

            problems = testAssem.assemble(test, "test", new BufferedOutputStream(Files.newOutputStream(
                    Paths.get(test.toString(), "test.bin"))));
            for (Problem p : problems)
                if (p instanceof ExceptionProblem) {
                    System.out.println(p);
                    ((ExceptionProblem) p).getCause().printStackTrace();
                } else
                    System.out.println(p);
            System.out.println("Total Problems: " + problems.size());
            outProblems = true;


            System.out.println("__Comparing test.hex with text.comp.hex");

            try (BufferedReader asm = Files.newBufferedReader(Paths.get(test.toString(), "test.hex"));
                 BufferedReader com = Files.newBufferedReader(Paths.get(test.toString(), "test.comp.hex"))){
                String asmLine, comLine;
                int line = 0;
                while ((asmLine = asm.readLine()) != null | (comLine = com.readLine()) != null) {
                    System.out.print("Line: " + ++line + "...");
                    assertEquals(asmLine, comLine);
                    System.out.println("Passed.");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.err.flush();
            ex = true;
        }
        if (ex && !outProblems) {
            problems.addAll(MC8051Library.getProblems());
            for (Problem p : problems)
                if (p instanceof ExceptionProblem) {
                    System.out.println(p);
                    ((ExceptionProblem) p).getCause().printStackTrace();
                } else
                    System.out.println(p);
            System.out.println("Total Problems: " + problems.size());
        }
        if (ex || problems.stream().anyMatch(Problem::isError))
            fail("Some problems occurred.");

    }

    @Test
    public void testTokenizer_AddToken() {
        Method m = null;
        try {
            m = tokenizer.getClass().getDeclaredMethod("addToken",
                    String.class, List.class, List.class, Path.class, int.class);
            m.setAccessible(true);

            HashMap<String, String> map = new HashMap<>();

            map.put("#42",       "OperandToken8051(42)[OPERAND, CONSTANT, 42]");
            map.put("/42",       "OperandToken8051(42)[OPERAND, NEGATED_ADDRESS, 42]");
            map.put("42",        "OperandToken8051(42)[OPERAND, ADDRESS, 42]");
            map.put("25h.02o",   "OperandToken8051(42)[OPERAND, ADDRESS, 42]");
            map.put("0a8h.02o",  "OperandToken8051(42)[OPERAND, ADDRESS, 170]");
            map.put("+42",       "OperandToken8051(42)[OPERAND, ADDRESS_OFFSET, 42]");
            map.put("-42",       "OperandToken8051(42)[OPERAND, ADDRESS_OFFSET, -42]");
            map.put("forty_two", "SymbolToken(42)[SYMBOL, forty_two]");
            map.put("a",         "OperandToken8051(42)[OPERAND, NAME, a]");
            map.put("@r0",       "OperandToken8051(42)[OPERAND, INDIRECT_NAME, r0]");

            for (String test : map.keySet()) {
                System.out.print("Testing: \"" + test + "\", Expecting: \"" + map.get(test) + "\"...");
                List<Problem> list = new ArrayList<>();
                List<Token> token = new ArrayList<>(1);
                list.clear();
                m.invoke(tokenizer,test, token, list, null, 42);
                String tested = token.size() > 0 ? token.get(0).toString(): "???";
                for (Problem p : list)
                    System.out.println("\n" + p);
                assertEquals(map.get(test), tested);
                System.out.println("Passed.");
            }
        } catch (Exception e) {
            System.out.flush();
            e.printStackTrace();
            fail("Unexpected Exception!");
        }
        m.setAccessible(false);
    }
}
