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

import static org.junit.Assert.*;

/**
 * @author Jannik
 */
public class AssemblerTest {

    private Assembler testAssem;
    private Preprocessor prepr;
    private Tokenizer tokenizer;

    @Before
    public void setUp() throws Exception {
        prepr = new Preprocessor8051();
        tokenizer = new Tokenizer8051();
        testAssem = new Assembler(MC8051Library.mnemonics, prepr, tokenizer);
    }

    @Test
    public void testLowerCase() {
        try {
            Method m = prepr.getClass().getDeclaredMethod("lowerCase", String.class, List.class);
            m.setAccessible(true);

            HashMap<String, String> map = new HashMap<>(5);

            map.put("TY the \"Tasmanian\" 'Tiger' 4", "ty the \"Tasmanian\" 'Tiger' 4"); // Sorry
            map.put("Test test TE'ST'!", "test test te'ST'!");
            map.put("The \\\"test Tester\\\" tested \"Tony's Test\". ", "the \"test tester\" tested \"Tony's Test\". ");
            map.put("\"T'E\"S'T\"?'", "\"T'E\"s'T\"?'");
            map.put("\\Tests For Testing \\Tests.", "Tests for testing Tests.");

            for (String test : map.keySet())
                assertEquals(map.get(test), m.invoke(prepr, test, new ArrayList<Problem>()));

            m.setAccessible(false);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}