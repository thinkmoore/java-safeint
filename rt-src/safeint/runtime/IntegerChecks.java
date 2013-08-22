package safeint.runtime;

import java.math.BigInteger;

public abstract class IntegerChecks {
    public static String getPosition() {
        Exception e = new Exception();
        e.fillInStackTrace();
        StackTraceElement[] trace = e.getStackTrace();
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement t = trace[i];
            if (t.getClassName().equals(IntegerChecks.class.getCanonicalName())) {
                continue;
            } else {
                return t.getClassName() + "." + t.getMethodName() + ":" + t.getFileName() + " line " + t.getLineNumber();
            }
        }
        return "unknown";
    }
    
    public static void log(String reason, String expression) {
        String position = getPosition();
        System.err.println("SafeInt: arithmetic check failed at <" + position + "> :");
        System.err.println("\t" + reason);
        System.err.println("\t" + expression);
    }
    
    public static void log(String reason, String expression, String result, String expected) {
        String position = getPosition();
        System.err.println("SafeInt: arithmetic check failed at <" + position + "> :");
        System.err.println("\t" + reason);
        System.err.println("\t" + expression + " = " + result + ", expected " + expected);
    }
    
    // Unary minus
    public static int minus(int n) {
        int result = -n;
        long expected = -(long)n;
        if (result != expected) {
            log("Integer overflow", "-(int)" + n, Integer.toString(result), Long.toString(expected));
        }
        return result;
    }
    
    public static long minus(long n) {
        long result = -n;
        BigInteger expected = BigInteger.valueOf(n).negate();
        if (!expected.equals(BigInteger.valueOf(result))) {
            log("Integer overflow", "-(int)" + n, Long.toString(result), expected.toString());
        }
        return result;
    }
    
    // Addition
    public static int add(int a, int b) {
        int result = a + b;
        long expected = (long)a + (long)b;
        if (result != expected) {
            log("Integer overflow", "(int)" + a + " + (int)" + b, Integer.toString(result), Long.toString(expected));
        }
        return result;
    }
    
    public static long add(long a, long b) {
        long result = a + b;
        BigInteger expected = BigInteger.valueOf(a).add(BigInteger.valueOf(b));
        if (!expected.equals(BigInteger.valueOf(result))) {
            log("Integer overflow", "(long)" + a + " + (long)" + b, Long.toString(result), expected.toString());
        }
        return result;
    }
    
    // Subtraction
    public static int sub(int a, int b) {
        int result = a - b;
        long expected = (long)a - (long)b;
        if (result != expected) {
            log("Integer overflow", "(int)" + a + " - (int)" + b, Integer.toString(result), Long.toString(expected));
        }
        return result;
    }
    
    public static long sub(long a, long b) {
        long result = a - b;
        BigInteger expected = BigInteger.valueOf(a).subtract(BigInteger.valueOf(b));
        if (!expected.equals(BigInteger.valueOf(result))) {
            log("Integer overflow", "(long)" + a + " - (long)" + b, Long.toString(result), expected.toString());
        }
        return result;
    }
    
    // Multiplication
    public static int mul(int a, int b) {
        int result = a * b;
        long expected = (long)a * (long)b;
        if (result != expected) {
            log("Integer overflow", "(int)" + a + " * (int)" + b, Integer.toString(result), Long.toString(expected));
        }
        return result;
    }
    
    public static long mul(long a, long b) {
        long result = a * b;
        BigInteger expected = BigInteger.valueOf(a).multiply(BigInteger.valueOf(b));
        if (!expected.equals(BigInteger.valueOf(result))) {
            log("Integer overflow", "(long)" + a + " * (long)" + b, Long.toString(result), expected.toString());
        }
        return result;
    }
    
    // Division
    public static int div(int a, int b) {
        if (b == 0) {
            log("Division by zero", "(int)" + a + " / (int)" + b);
        }
        int result = a / b;
        long expected = (long)a / (long)b;
        if (result != expected) {
            log("Integer overflow", "(int)" + a + " / (int)" + b, Integer.toString(result), Long.toString(expected));
        }
        return result;
    }
    
    public static long div(long a, long b) {
        if (b == 0) {
            log("Division by zero", "(long)" + a + " / (long)" + b);
        }
        long result = a / b;
        BigInteger expected = BigInteger.valueOf(a).divide(BigInteger.valueOf(b));
        if (!expected.equals(BigInteger.valueOf(result))) {
            log("Integer overflow", "(long)" + a + " / (long)" + b, Long.toString(result), expected.toString());
        }
        return result;
    }
    
    // mod / rem
    public static int mod(int a, int b) {
        if (b == 0) {
            log("Division by zero", "(int)" + a + " % (int)" + b);
        }
        int result = a % b;
        long expected = (long)a % (long)b;
        if (result != expected) {
            log("Integer overflow", "(int)" + a + " % (int)" + b, Integer.toString(result), Long.toString(expected));
        }
        return result;
    }
    
    public static long mod(long a, long b) {
        if (b == 0) {
            log("Division by zero", "(long)" + a + " % (long)" + b);
        }
        long result = a % b;
        BigInteger expected = BigInteger.valueOf(a).remainder(BigInteger.valueOf(b));
        if (!expected.equals(BigInteger.valueOf(result))) {
            log("Integer overflow", "(long)" + a + " % (long)" + b, Long.toString(result), expected.toString());
        }
        return result;
    }
    
    // Left shift
    public static int shl(int a, int b) {
        int result = a << b;
        b = b & 0x1f; 
        long expected = ((long)a) << b;
        if (result != expected) {
            log("Integer overflow", "(int)" + a + " << (int)" + b, Integer.toString(result), Long.toString(expected));
        }
        return result;
    }
    public static long shl(long a, long b) {
        long result = a << b;
        int shiftAmount = (int)(b & 0x3f); 
        BigInteger ba = BigInteger.valueOf(a);
        BigInteger expected = ba.shiftLeft(shiftAmount);
        if (!expected.equals(BigInteger.valueOf(result))) {
            log("Integer overflow", "(long)" + a + " << (long)" + b, Long.toString(result), expected.toString());
        }
        return result;
    }
    
    // Right shifts (no ops)
    public static int shr(int a, int b) {
        return a >> b;
    }
    public static long shr(long a, long b) {
        return a >> b;
    }
    public static int ushr(int a, int b) {
        return a >>> b;
    }
    public static long ushr(long a, long b) {
        return a >>> b;
    }
    
    // negation
    public static int neg(int n) {
        int result = -n;
        long expected = -((long)n);
        if (result != expected) {
            log("Integer overflow", "-(int)" + n, Integer.toString(result), Long.toString(expected));
        }
        return result;
    }
    public static long neg(long n) {
        long result = -n;
        BigInteger expected = BigInteger.valueOf(n).negate();
        if (!expected.equals(BigInteger.valueOf(result))) {
            log("Integer overflow", "-(long)" + n, Long.toString(result), expected.toString());
        }
        return result;
    }
    
    // Casts
    public static short shortCast(long n) {
        short result = (short)n;
        if (result != n) {
            log("Truncation", "(short)((long)" + n + ")", Short.toString(result), Long.toString(n));
        }
        return result;
    }
    
    public static short shortCast(int n) {
        short result = (short)n;
        if (result != n) {
            log("Truncation", "(short)((int)" + n + ")", Short.toString(result), Integer.toString(n));
        }
        return result;
    }
    
    // don't check casts from float or double
    public static short shortCast(double n) {
    	return (short) n;
    }
    
    public static char charCast(long n) {
        char result = (char)n;
        if (result != n) {
            log("Truncation", "(char)((long)" + n + ")", Integer.toString(result), Long.toString(n));
        }
        return result;
    }
    
    public static char charCast(int n) {
        char result = (char)n;
        if (result != n) {
            log("Truncation", "(char)((int)" + n + ")", Integer.toString(result), Integer.toString(n));
        }
        return result;
    }
    
    // don't check casts from float or double
    public static char charCast(double n) {
    	return (char) n;
    }
    
    public static byte byteCast(long n) {
        byte result = (byte)n;
        if (result != n) {
            log("Truncation", "(byte)((long)" + n + ")", Byte.toString(result), Long.toString(n));
        }
        return result;
    }
    
    public static byte byteCast(int n) {
        byte result = (byte)n;
        if (result != n) {
            log("Truncation", "(byte)((int)" + n + ")", Byte.toString(result), Integer.toString(n));
        }
        return result;
    }
    
    // don't check casts from float or double
    public static byte byteCast(double n) {
    	return (byte) n;
    }
    
    public static int intCast(long n) {
        int result = (int)n;
        if (result != n) {
            log("Truncation", "(int)((long)" + n + ")", Integer.toString(result), Long.toString(n));
        }
        return result;
    }
    
    // don't check casts from float or double
    public static int intCast(double n) {
    	return (int) n;
    }
    
    // no op
    public static long longCast(long l) {
        return l;
    }
    
    // don't check casts from float or double
    public static long longCast(double n) {
    	return (long) n;
    }
}
