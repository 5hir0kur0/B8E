package assembler.test.arc8051;

import assembler.Preprocessor;
import assembler.Tokenizer;
import assembler.arc8051.Assembler8051;
import assembler.arc8051.MC8051Library;
import assembler.arc8051.Preprocessor8051;
import assembler.arc8051.Tokenizer8051;
import assembler.tokens.Token;
import assembler.util.AssemblerSettings;
import assembler.util.Regex;
import assembler.util.problems.ExceptionProblem;
import assembler.util.problems.Problem;
import misc.Settings;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Jannik
 */
public class AssemblerTest {

    private Assembler8051 testAssem;
    private Preprocessor prepr;
    private Tokenizer tokenizer;

    private Random random;

    @Before
    public void setUp() throws Exception {
        AssemblerSettings.init();
        prepr = new Preprocessor8051();
        tokenizer = new Tokenizer8051();
        testAssem = new Assembler8051();
        random = new Random();
    }

    @Test
    public void testPreprocessor_GetNumber() {
        System.out.println("____________Testing Tokenizer8051.getNumber()");
        try {
            Method m = prepr.getClass().getDeclaredMethod("getNumber", String.class);
            m.setAccessible(true);
            Field f = prepr.getClass().getDeclaredField("problems");
            f.setAccessible(true);

            List<Problem> list = (List<Problem>) f.get(prepr);

            HashMap<String, String> map = new HashMap<>();
            int rand;

            map.put(Integer.toBinaryString(rand = random.nextInt(0x10000)) + "b", Integer.toString(rand));
            map.put(Integer.toOctalString(rand = random.nextInt(0x10000)) + "o", Integer.toString(rand));
            map.put(Integer.toOctalString(rand = random.nextInt(0x10000)) + "q", Integer.toString(rand));
            map.put(Integer.toString(rand = random.nextInt(0x10000)) + "d", Integer.toString(rand));
            map.put("0" + Integer.toHexString(rand = random.nextInt(0x10000)) + "h", Integer.toString(rand));
//            map.put("0b"+Integer.toBinaryString(rand = random.nextInt(0x10000)), Integer.toString(rand));
//            map.put("0" +Integer.toOctalString(rand = random.nextInt(0x10000)),  Integer.toString(rand));
//            map.put("0d"+Integer.toString(rand = random.nextInt(0x10000)),       Integer.toString(rand));
            map.put("0x" + Integer.toHexString(rand = random.nextInt(0x10000)), Integer.toString(rand));

            for (String test : map.keySet()) {
                System.out.print("Testing: \"" + test + "\", Expecting: \"" + map.get(test) + "\"...");
                String tested = (String) m.invoke(prepr, test);
                for (Problem p : list)
                    System.out.println("\n" + p);
                assertEquals(map.get(test), tested);
                System.out.println("Passed.");
            }

            m.setAccessible(false);
            f.setAccessible(false);
        } catch (NoSuchMethodException | NoSuchFieldException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
            fail("Unexpected Exception.");
        }
    }

    @Test
    public void test_preprocess() {
        System.out.println("____________Testing Preprocessor8051.preprocess()");
        try {

            final Path dir = Paths.get("src/assembler/test/arc8051/");
            Settings.INSTANCE.setProperty(AssemblerSettings.INCLUDE_PATH, "src/assembler/include");

            List<String> output = new LinkedList<>();
            List<Problem> problems;

            problems = prepr.preprocess(dir, Paths.get(dir.toString(), "preprocess_test.asm"), output);

            System.out.println("Output: ");
            output.stream().forEach(System.out::println);
            System.out.println(":Output end");
            System.out.println();

            for (Problem p : problems)
                if (p instanceof ExceptionProblem) {
                    System.out.println(p);
                    ((ExceptionProblem) p).getCause().printStackTrace();
                } else
                    System.out.println(p);
            System.out.println("Total Problems: " + problems.size());
            if (problems.stream().anyMatch(Problem::isError))
                fail("Some problems occurred.");
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected Exception.");
        }
    }

    @Test
    public void test_Tokenizer() {
        System.out.println("____________Testing Tokenizer8051.tokenize()");
        String[] test =
                {
                        "  test_label:     ",
                        "test_label:    label:  ",
                        "l: mnemonic",
                        "   mnemonic  \n label: mn",
                        "                 ",
                        "/test",
                        "# test1 " +
                                "test2, a, c, r1, @r1, /r1, /c, " +
                                "/a.5, @a + pc, #42, 32.0",
                };
        // TODO add test and edge cases.
        List<Problem> problems = new LinkedList<>();
        List<Token> result = tokenizer.tokenize(Arrays.asList(test), problems);

        System.out.println("Output:");
        result.forEach(System.out::println);
        System.out.println("\nProblems:");
        problems.forEach(System.out::println);
        System.out.println("Total Problems: " + problems.size());
    }

    @Test
    public void test_assemble() {
        System.out.println("____________Testing Assembler_Old.assemble()");

        List<Problem> problems = new ArrayList<>();
        boolean ex = false, outProblems = false;
        try {
            System.out.println("Running assembler:");
            Path test = Paths.get("src/assembler/test/arc8051/");
            System.out.println("__Test directory: " + test.toAbsolutePath());

            byte[] result = testAssem.assemble(test, problems);
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
                 BufferedReader com = Files.newBufferedReader(Paths.get(test.toString(), "test.comp.hex"))) {
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
    public void testRegex() {
        System.out.println("____________Testing Regex()");
        List<Problem> problems = new ArrayList<>();
        try {
            Path file = Paths.get("src/assembler/test/arc8051/imaginary.asm");
            //TODO: Finish!
            Regex regex1 = new Regex("ws/(\\d+)/Oh noes!/~/g", file, 42, problems);
            Regex regex2 = new Regex("cs/(\\d+)/5/~/g", file, 43, problems);
            Regex regex3 = new Regex("cs/(\\d+)/4/~/g", file, 43, problems);
            Regex regex4 = new Regex("cs/(\\d+)/^(?!\\T{directive}).*?$/~/g", file, 44, problems);


            System.out.println("Result1(" + regex1 + "): " + regex1.perform("4 + 2 == 42"));
            System.out.println("Result2(" + regex2 + ": " + regex2.perform("4 + 2 == 42"));
            System.out.println("Result3(" + regex3 + ": " + regex3.perform("4 + 2 == 42"));
            System.out.println("Result4.1(" + regex4 + ": " +
                    regex4.perform("$test 4 + 2 == 42"));
            System.out.println("Result4.2(" + regex4 + ": " +
                    regex4.perform("/test 4 + 2 == 42"));


            System.out.println("Problems:");
            for (Problem p : problems)
                System.out.println(p);
            System.out.println("Total: " + problems.size());
        } catch (Exception e) {
            System.out.flush();
            e.printStackTrace();
            fail("Unexpected Exception!");
        }
    }
}
