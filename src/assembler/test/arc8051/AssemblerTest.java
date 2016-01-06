package assembler.test.arc8051;

import assembler.Assembler;
import assembler.Preprocessor;
import assembler.Tokenizer;
import assembler.arc8051.MC8051Library;
import assembler.arc8051.Preprocessor8051;
import assembler.arc8051.Tokenizer8051;
import assembler.util.Problem;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
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
            map.put("\\Tests For Testing \\Tests.", "Tests for testing Tests.");

            for (String test : map.keySet()) {
                System.out.print("Testing: \"" + test + ", Expecting: " + map.get(test) + "...");
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
                System.out.print("Testing: \"" + test + ", Expecting: " + map.get(test) + "...");
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
    public void testTokenizer_tokenize() {
        HashMap<String, String> map = new HashMap<>();

    }
}
