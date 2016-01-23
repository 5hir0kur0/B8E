package simplemath;

import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.*;

/**
 * Simple test for {@code SimpleMath}
 * NOTE: This class only tests for simple and valid expressions and does not check operator precedence very well and it
 *       only uses {@code long}s for tests. Also, it does not test "&lt;" and "&gt;".
 * @author Gordian
 */
public class SimpleMathTest {
    private static Random r = new Random();

    @Test
    public void testPlusOperator() {
        long r1 = r.nextInt(10000);
        long r2 = r.nextInt(10000);
        long result = r1 + r2;
        String[] exprs = {r1+"+"+r2, "("+r1+")"+"+("+r2+")", "("+r1+"+"+r2+")", "(((((("+r1+")))"+"+("+r2+"))))"};
        long[] results = {result,result,result,result};
        assertExprs(exprs, results);
    }

    @Test
    public void testMinusOperator() {
        long r1 = r.nextInt(10000);
        long r2 = r.nextInt(10000);
        long result = r1 - r2;
        String[] exprs = {r1+"-"+r2, "("+r1+")"+"-("+r2+")", "("+r1+"-"+r2+")", "(((((("+r1+")))"+"-("+r2+"))))"};
        long[] results = {result,result,result,result};
        assertExprs(exprs, results);
    }

    @Test
    public void testUnaryPlusAndMinus() {
        long r1 = r.nextInt(10000);
        long r2 = r.nextInt(10000);
        String[] exprs = {
                "-"+r1,
                "+"+r2,
                "+(-(-(-(-(-(-(-(-(-(-"+r1+"))))))))))",
                "+(-(-(-(+(-(-(+(-(-(-"+r1+"))))))))))",
                "-((("+r1+")))",
                r1+"+(-"+r2+")"
        };
        long[] results = {
                -r1,
                r2,
                (-(-(-(-(-(-(-(-(-(-r1)))))))))),
                (-(-(-(+(-(-(+(-(-(-r1)))))))))),
                -r1,
                r1 - r2
        };
        assertExprs(exprs, results);
    }

    @Test
    public void testMultiplicationOperator() {
        long r1 = r.nextInt(10000);
        long r2 = r.nextInt(10000);
        String[] exprs = {
                r1+"*"+r2,
                r2+"*"+r1,
                "(("+r1+"))*("+r2+"*"+r2+")"
        };
        long[] results = {
                r1*r2,
                r2*r1,
                r1*r2*r2
        };
        assertExprs(exprs, results);
    }

    @Test
    public void testDivisionOperator() {
        long r1 = r.nextInt(10000);
        long r2 = r.nextInt(10000);
        String[] exprs = {
                r1+"/"+r2,
                r2+"/"+r1,
                "(("+r1+"))/("+r2+"*"+r2+")"
        };
        long[] results = {
                r1/r2,
                r2/r1,
                r1/(r2*r2)
        };
        assertExprs(exprs, results);
    }

    @Test
    public void testModulusOperator() {
        long r1 = r.nextInt(10000);
        long r2 = r.nextInt(10000);
        String[] exprs = {
                r1+"%"+r2,
                r2+"%"+r1,
                "(("+r1+"))%("+r2+"/"+r2+")"
        };
        long[] results = {
                r1%r2,
                r2%r1,
                r1%(r2/r2)
        };
        assertExprs(exprs, results);
    }

    @Test
    public void testExponentOperator() {
        long r1 = r.nextInt(10);
        long r2 = r.nextInt(5);
        String[] exprs = {
                r1+"^"+r2,
                r1+"*"+r2+"^"+"("+r2+"+2)"
        };
        long[] results = {
                (long)Math.pow(r1, r2),
                (long)(r1*Math.pow(r2, r2+2))
        };
        assertExprs(exprs, results);
    }

    @Test
    public void testBinaryNotOperator() {
        long rand = r.nextInt(222222);
        String[] exprs = {
                "~"+rand,
                "~(-"+rand+")",
                "~"+rand+"/2"
        };
        long[] results = {
                ~rand,
                ~(-rand),
                ~rand/2
        };
        assertExprs(exprs, results);
    }

    @Test
    public void testShifting() {
        long r1 = r.nextInt(1242000);
        long r2 = r.nextInt(20);
        String[] exprs = {
                r1+">>"+r2,
                r1+"<<"+r2,
                r1+"+"+r1+"<<"+r2,
                r1+"*"+r1+"<<"+r2
        };
        long[] results = {
                r1 >> r2,
                r1 << r2,
                r1+r1<<r2,
                r1*r1<<r2
        };
        assertExprs(exprs, results);
    }

    @Test
    public void testBinaryAndAndOr() {
        long r1 = r.nextInt(12340000);
        long r2 = r.nextInt(12340000);
        String[] exprs = {
                r1+"|"+r2+"&"+r1,
                r1+"&"+r1,
                r1+"&"+r2,
                r1+"|"+r2,
                r1+"|"+"~"+r2
        };
        long[] results = {
                r1|r2&r1,
                r1,
                r1&r2,
                r1|r2,
                r1|~r2
        };
        assertExprs(exprs, results);
    }

    @Test
    public void testComplexExpression() {
        String[] exprs = {
                "42+(23^2-3*4/(3+2))%222/5*240>>((3+2*2)/1)|7357",
                "(42+(23^2-3*4/(3+2))%222/5*240>>((3+2*2)/1)|7357)^1",
                "(42+(23^2-3*4/(3+2))%222/5*240>>((3+2*2)/1)|7357)/(42+(23^2-3*4/(3+2))%222/5*240>>((3+2*2)/1)|7357)+5",
                "1337|234&20>>3<<3-42+1111*21/3%10^(~10*~2)",
        };
        long[] results = {
                7359L,
                7359L,
                6,
                1337,
        };
        assertExprs(exprs, results);
    }

    private void assertExprs(String[] exprs, long[] results) {
        if (exprs.length != results.length) throw new IllegalArgumentException();
        for (int i = 0; i < exprs.length; ++i) {
            long result = (long)SimpleMath.evaluate(exprs[i]);
            System.out.println("Evaluating \""+exprs[i]+"\"\n"+
            "Result:         "+result+"\n"+
            "Desired result: "+results[i]+"\n");
            assertTrue(result == results[i]);
        }
    }
}