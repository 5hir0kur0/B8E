package assembler.test.arc8051;

import assembler.Assembler;
import assembler.Preprocessor;
import assembler.Tokenizer;
import assembler.arc8051.MC8051Library;
import assembler.arc8051.OperandToken8051;
import static assembler.arc8051.OperandToken8051.OperandType8051;
import assembler.arc8051.Preprocessor8051;
import assembler.arc8051.Tokenizer8051;
import assembler.tokens.OperandToken;
import assembler.tokens.Token;
import assembler.util.Regex;
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
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

/**
 * @author Jannik
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
//            map.put("0b"+Integer.toBinaryString(rand = random.nextInt(0x10000)), Integer.toString(rand));
//            map.put("0" +Integer.toOctalString(rand = random.nextInt(0x10000)),  Integer.toString(rand));
//            map.put("0d"+Integer.toString(rand = random.nextInt(0x10000)),       Integer.toString(rand));
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
                    assertEquals(comLine, asmLine);
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
        System.out.println("____________Testing Tokenizer8051.addToken()");
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


    @Test
    public void testRegex() {
        System.out.println("____________Testing Regex()");
        List<Problem> problems = new ArrayList<>();
        try {
            //TODO: Finish!
            String regex = "ws/(\\d+)/Oh noes!/~/g";
            Regex r = new Regex(regex, Paths.get("src/assembler/test/arc8051/imaginary.asm"), 42, problems);

            System.out.println("Result: " + r.perform("4 + 2 == 42"));


            System.out.println("Problems:");
            for (Problem p : problems)
                System.out.println(p);
            System.out.println("Total: "+problems.size());
        } catch (Exception e) {
            System.out.flush();
            e.printStackTrace();
            fail("Unexpected Exception!");
        }
    }

    @Test
    public void test_ajmp() {
        final OperandToken8051[][] operands = generateAllOperands(1, 1);

    }


    private OperandToken8051[][] generateAllOperands(final int minLength, final int maxLength) {
        List<OperandToken8051[]> result   = new ArrayList<>();

        if (minLength == 0)
            result.add(new OperandToken8051[0]);
        OperandToken8051[][] last = new OperandToken8051[0][];
        for (int i = 0; i < maxLength; ++i) {
            last = generateOperandArray(last);
            if (i > minLength-2)
                for (OperandToken8051[] a : last)
                    result.add(a);
        }

        return result.toArray(new OperandToken8051[result.size()][]);
    }

    private OperandToken8051[][] generateOperandArray(OperandToken8051[][] source) {
        final OperandToken8051[] operands = generateAllOperandTokens();
              OperandToken8051[][] result;
        if (source.length == 0) {
            result = new OperandToken8051[operands.length][];
            for (int i = 0; i < result.length; ++i)
                result[i] = new OperandToken8051[]{operands[i]};
        } else {
            result = new OperandToken8051[source.length*operands.length][];
            for (int i = 0; i < source.length; ++i)
                for (int j = 0; j < operands.length; ++j) {
                    OperandToken8051[] preResult = new OperandToken8051[source[i].length+1];
                    for (int k = 0; k < source[i].length; ++k)
                        preResult[k] = source[i][k];
                    preResult[source[i].length] = operands[j];
                    result[operands.length*i+j] = preResult;
                }
        }
        return result;
    }

    private OperandToken8051[] generateAllOperandTokens() {
        List<OperandToken8051> result = new ArrayList<>();
        final String[] names = {
                "a", "c", "aptr", "ab",
                "r0", "r1", "r2", "r3", "r4", "r5", "r6", "r7",
                "e", "r8"
        };

        final String[] indirect_names = {
                "a+dptr", "a+pc", "dptr",
                "r0", "r1",
                "xptr", "r2"
        };

        int number = 0;

        for (OperandType8051 t : OperandType8051.values())
            if (t.isName())
                for (String name : names)
                    result.add(new OperandToken8051(t, name, number++));
            else if (t.isIndirectName())
                for (String indName : indirect_names)
                    result.add(new OperandToken8051(t, indName, number++));
            else {
                final int[] rand   = {
                        random.nextInt(0xFF+1),            // Range 0x00 <= x <= 0x00FF
                        random.nextInt(0xFFFF+1-0xFF)+0xFF // Range 0xFF <  x <= 0xFFFF
                };
                for (int i : rand) {
                    result.add(new OperandToken8051(t, Integer.toString(i), number++));
                    if (t.isAddressOffset())
                        result.add(new OperandToken8051(t, Integer.toString(i * -1), number++));
                }
            }

        return result.toArray(new OperandToken8051[31]);
    }

}
