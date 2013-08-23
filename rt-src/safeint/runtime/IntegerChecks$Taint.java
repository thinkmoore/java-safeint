package safeint.runtime;

import java.math.BigInteger;

import taintj.Method$Return$Stack;

abstract public class IntegerChecks$Taint extends taintj.java.lang.Object$Taint {
    
    public IntegerChecks$Taint(byte self$t) {
		super(self$t);
	}

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
    
    public static void log(String reason) {
    	String position = getPosition();
        System.err.println("SafeInt: " + reason + " at <" + position + ">");
    }
    
    public static byte getPosition$t() {
        byte return$t = (byte) taintj.Taints.NONE;
        Method$Return$Stack.push(new taintj.java.lang.String$Taint(return$t));
        return return$t;
    }
    
    public static void log$t(java.lang.String arg1, byte arg1$t, taintj.java.lang.String$Taint arg1$tt, java.lang.String arg2, byte arg2$t, taintj.java.lang.String$Taint arg2$tt) {  }
    
    public static void log$t(java.lang.String arg1, byte arg1$t, taintj.java.lang.String$Taint arg1$tt, java.lang.String arg2, byte arg2$t, taintj.java.lang.String$Taint arg2$tt, java.lang.String arg3, byte arg3$t, taintj.java.lang.String$Taint arg3$tt, java.lang.String arg4, byte arg4$t, taintj.java.lang.String$Taint arg4$tt) {  }
    
    public static byte minus$t(int n, byte n$t) {
    	int result = -n;
    	long expected = -(long)n;
    	byte return$t = n$t;
    	if (return$t != 0 && result != expected) {
    		return$t |= taintj.Taints.NUMERROR;
    	}
    	return return$t;
    }
    
    public static byte minus$t(long n, byte n$t) {
        long result = -n;
        BigInteger expected = BigInteger.valueOf(n).negate();
        byte return$t = n$t;
        if (return$t != 0 && !expected.equals(BigInteger.valueOf(result))) {
        	return$t |= taintj.Taints.NUMERROR;
        }
        return return$t;
    }
    
    public static byte add$t(int a, byte a$t, int b, byte b$t) {
        byte return$t = (byte) (a$t | b$t);
        int result = a + b;
        long expected = (long)a + (long)b;
        if (return$t != 0 && result != expected) {
        	return$t |= taintj.Taints.NUMERROR;
        }
        return return$t;
    }
    
    public static byte add$t(long a, byte a$t, long b, byte b$t) {
    	byte return$t = (byte) (a$t | b$t);
        long result = a + b;
        BigInteger expected = BigInteger.valueOf(a).add(BigInteger.valueOf(b));
        if (return$t != 0 && !expected.equals(BigInteger.valueOf(result))) {
        	return$t |= taintj.Taints.NUMERROR;
        }
        return return$t;
    }
    
    public static byte sub$t(int a, byte a$t, int b, byte b$t) {
        byte return$t = (byte) (a$t | b$t);
        int result = a - b;
        long expected = (long)a - (long)b;
        if (return$t != 0 && result != expected) {
        	return$t |= taintj.Taints.NUMERROR;
        }
        return return$t;
    }
    
    public static byte sub$t(long a, byte a$t, long b, byte b$t) {
        byte return$t = (byte) (a$t | b$t);
        long result = a - b;
        BigInteger expected = BigInteger.valueOf(a).subtract(BigInteger.valueOf(b));
        if (return$t != 0 && !expected.equals(BigInteger.valueOf(result))) {
        	return$t |= taintj.Taints.NUMERROR;
        }
        return return$t;
    }
    
    public static byte mul$t(int a, byte a$t, int b, byte b$t) {
        byte return$t = (byte) (a$t | b$t);
        int result = a * b;
        long expected = (long)a * (long)b;
        if (return$t != 0 && result != expected) {
        	return$t |= taintj.Taints.NUMERROR;
        }
        return return$t;
    }
    
    public static byte mul$t(long a, byte a$t, long b, byte b$t) {
        byte return$t = (byte) (a$t | b$t);
        long result = a * b;
        BigInteger expected = BigInteger.valueOf(a).multiply(BigInteger.valueOf(b));
        if (return$t != 0 && !expected.equals(BigInteger.valueOf(result))) {
        	return$t |= taintj.Taints.NUMERROR;
        }
        return return$t;
    }
    
    public static byte div$t(int a, byte a$t, int b, byte b$t) {
        byte return$t = (byte) (a$t | b$t);
        if (return$t != 0 && b == 0) {
        	return$t |= taintj.Taints.NUMERROR;
        	return return$t;
        }
        int result = a / b;
        long expected = (long)a / (long)b;
        if (return$t != 0 && result != expected) {
        	return$t |= taintj.Taints.NUMERROR;
        }
        return return$t;
    }
    
    public static byte div$t(long a, byte a$t, long b, byte b$t) {
        byte return$t = (byte) (a$t | b$t);
        if (return$t != 0 && b == 0) {
        	return$t |= taintj.Taints.NUMERROR;
        	return return$t;
        }
        long result = a / b;
        BigInteger expected = BigInteger.valueOf(a).divide(BigInteger.valueOf(b));
        if (return$t != 0 && !expected.equals(BigInteger.valueOf(result))) {
        	return$t |= taintj.Taints.NUMERROR;
        }
        return return$t;
    }
    
    public static byte mod$t(int a, byte a$t, int b, byte b$t) {
        byte return$t = (byte) (a$t | b$t);
        if (return$t != 0 && b == 0) {
        	return$t |= taintj.Taints.NUMERROR;
        	return return$t;
        }
        int result = a % b;
        long expected = (long)a / (long)b;
        if (return$t != 0 && result != expected) {
        	return$t |= taintj.Taints.NUMERROR;
        }
        return return$t;
    }
    
    public static byte mod$t(long a, byte a$t, long b, byte b$t) {
        byte return$t = (byte) (a$t | b$t);
        if (return$t != 0 && b == 0) {
        	return$t |= taintj.Taints.NUMERROR;
        	return return$t;
        }
        long result = a % b;
        BigInteger expected = BigInteger.valueOf(a).divide(BigInteger.valueOf(b));
        if (return$t != 0 && !expected.equals(BigInteger.valueOf(result))) {
        	return$t |= taintj.Taints.NUMERROR;
        }
        return return$t;
    }
    
    public static byte shl$t(int a, byte a$t, int b, byte b$t) {
        byte return$t = (byte) (a$t | b$t);
        int result = a << b;
        b = b & 0x1f; 
        long expected = ((long)a) << b;
        if (return$t != 0 && result != expected) {
        	return$t |= taintj.Taints.NUMERROR;
        }
        return return$t;
    }
    
    public static byte shl$t(long a, byte a$t, long b, byte b$t) {
        byte return$t = (byte) (a$t | b$t);
        long result = a << b;
        int shiftAmount = (int)(b & 0x3f); 
        BigInteger ba = BigInteger.valueOf(a);
        BigInteger expected = ba.shiftLeft(shiftAmount);
        if (return$t != 0 && !expected.equals(BigInteger.valueOf(result))) {
        	return$t |= taintj.Taints.NUMERROR;
        }
        return return$t;
    }
    
    public static byte shr$t(int arg1, byte arg1$t, int arg2, byte arg2$t) {
        byte return$t = (byte) (arg1$t | arg2$t);
        ;
        return return$t;
    }
    
    public static byte shr$t(long arg1, byte arg1$t, long arg2, byte arg2$t) {
        byte return$t = (byte) (arg1$t | arg2$t);
        ;
        return return$t;
    }
    
    public static byte ushr$t(int arg1, byte arg1$t, int arg2, byte arg2$t) {
        byte return$t = (byte) (arg1$t | arg2$t);
        ;
        return return$t;
    }
    
    public static byte ushr$t(long arg1, byte arg1$t, long arg2, byte arg2$t) {
        byte return$t = (byte) (arg1$t | arg2$t);
        ;
        return return$t;
    }
    
    public static byte neg$t(int n, byte n$t) {
        byte return$t = (byte) n$t;
        int result = -n;
        long expected = -((long)n);
        if (return$t != 0 && result != expected) {
        	return$t |= taintj.Taints.NUMERROR;
        }
        return return$t;
    }
    
    public static byte neg$t(long n, byte n$t) {
        byte return$t = (byte) n$t;
        long result = -n;
        BigInteger expected = BigInteger.valueOf(n).negate();
        if (return$t != 0 && !expected.equals(BigInteger.valueOf(result))) {
        	return$t |= taintj.Taints.NUMERROR;
        }
        return return$t;
    }
    
    public static byte shortCast$t(long n, byte n$t) {
        byte return$t = (byte) n$t;
        short result = (short)n;
        if (return$t != 0 && result != n) {
        	return$t |= taintj.Taints.NUMERROR;
        }
        return return$t;
    }
    
    public static byte shortCast$t(int n, byte n$t) {
        byte return$t = (byte) n$t;
        short result = (short)n;
        if (return$t != 0 && result != n) {
        	return$t |= taintj.Taints.NUMERROR;
        }
        return return$t;
    }
    
    public static byte shortCast$t(double n, byte n$t) {
        byte return$t = (byte) n$t;
        ;
        return return$t;
    }
    
    public static byte charCast$t(long n, byte n$t) {
        byte return$t = (byte) n$t;
        char result = (char)n;
        if (return$t != 0 && result != n) {
        	return$t |= taintj.Taints.NUMERROR;
        }
        return return$t;
    }
    
    public static byte charCast$t(int n, byte n$t) {
        byte return$t = (byte) n$t;
        char result = (char)n;
        if (return$t != 0 && result != n) {
        	return$t |= taintj.Taints.NUMERROR;
        }
        return return$t;
    }
    
    public static byte charCast$t(double n, byte n$t) {
        byte return$t = (byte) n$t;
        ;
        return return$t;
    }
    
    public static byte byteCast$t(long n, byte n$t) {
        byte return$t = (byte) n$t;
        byte result = (byte)n;
        if (return$t != 0 && result != n) {
        	return$t |= taintj.Taints.NUMERROR;
        }
        return return$t;
    }
    
    public static byte byteCast$t(int n, byte n$t) {
        byte return$t = (byte) n$t;
        byte result = (byte)n;
        if (return$t != 0 && result != n) {
        	return$t |= taintj.Taints.NUMERROR;
        }
        return return$t;
    }
    
    public static byte byteCast$t(double n, byte n$t) {
        byte return$t = (byte) n$t;
        ;
        return return$t;
    }
    
    public static byte intCast$t(long n, byte n$t) {
        byte return$t = (byte) n$t;
        int result = (int)n;
        if (return$t != 0 && result != n) {
        	return$t |= taintj.Taints.NUMERROR;
        }
        return return$t;
    }
    
    public static byte intCast$t(double n, byte n$t) {
        byte return$t = (byte) n$t;
        ;
        return return$t;
    }
    
    public static byte longCast$t(long n, byte n$t) {
        byte return$t = (byte) n$t;
        ;
        return return$t;
    }
    
    public static byte longCast$t(double n, byte n$t) {
        byte return$t = (byte) n$t;
        ;
        return return$t;
    }
    
    public static void checkTaint$t(long n, byte n$t) {
    	if ((n$t & taintj.Taints.NUMERROR) != 0) {
    		log("tainted value reached check!");
    	}
    }
}
