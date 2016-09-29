package net.fishandwhistle.openpos.barcode;

/**
 * Created by dewey on 2016-09-29.
 */

public class ArrayMath {

    public static int[] subset(int[] in, int start, int length) {
        int[] out = new int[length];
        System.arraycopy(in, start, out, 0, length);
        return out;
    }

    public static boolean[] subset(boolean[] in, int start, int length) {
        boolean[] out = new boolean[length];
        System.arraycopy(in, start, out, 0, length);
        return out;
    }

    public static int[] concatenate(int[] ar1, int[] ar2) {
        int[] out = new int[ar1.length + ar2.length] ;
        System.arraycopy(ar1, 0, out, 0, ar1.length);
        System.arraycopy(ar2, 0, out, ar1.length, ar2.length);
        return out;
    }

    public static boolean[] concatenate(boolean[] ar1, boolean[] ar2) {
        boolean[] out = new boolean[ar1.length + ar2.length] ;
        System.arraycopy(ar1, 0, out, 0, ar1.length);
        System.arraycopy(ar2, 0, out, ar1.length, ar2.length);
        return out;
    }

    public static int[] round(double[] in) {
        int[] out = new int[in.length];
        for(int i=0; i<in.length; i++) {
            out[i] = (int)Math.round(in[i]);
        }
        return out;
    }

    public static int[] moddiv(int[] in, int by) {
        int[] out = new int[in.length];
        for (int i = 0; i < in.length; i++) {
            out[i] = in[i] / by;
        }
        return out;
    }

    public static double[] div(int[] in, double by) {
        double[] out = new double[in.length];
        for (int i = 0; i < in.length; i++) {
            out[i] = in[i] / by;
        }
        return out;
    }

    public static int[] mod(int[] in, int by) {
        int[] out = new int[in.length];
        for (int i = 0; i < in.length; i++) {
            out[i] = in[i] % by;
        }
        return out;
    }

    public static double[] rescale(double[] in, double frommin, double fromrange) {
        double[] out = new double[in.length];
        for(int i=0; i<in.length; i++) {
            out[i] = rescale(in[i], frommin, fromrange);
        }
        return out;
    }

    public static double rescale(double in, double frommin, double fromrange) {
        return (in - frommin) / fromrange ;
    }

    public static boolean[] eq(int[] in, int num) {
        boolean[] out = new boolean[in.length];
        for (int i = 0; i < in.length; i++) {
            out[i] = in[i] == num;
        }
        return out;
    }

    public static boolean[] gt(int[] in, int num) {
        boolean[] out = new boolean[in.length];
        for (int i = 0; i < in.length; i++) {
            out[i] = in[i] > num;
        }
        return out;
    }

    public static boolean[] lt(int[] in, int num) {
        boolean[] out = new boolean[in.length];
        for (int i = 0; i < in.length; i++) {
            out[i] = in[i] < num;
        }
        return out;
    }

    public static boolean[] gt(double[] in, double num) {
        boolean[] out = new boolean[in.length];
        for (int i = 0; i < in.length; i++) {
            out[i] = in[i] > num;
        }
        return out;
    }

    public static boolean[] lt(double[] in, double num) {
        boolean[] out = new boolean[in.length];
        for (int i = 0; i < in.length; i++) {
            out[i] = in[i] < num;
        }
        return out;
    }

    public static boolean any(boolean[] in) {
        boolean out = false;
        for (boolean anIn : in) {
            out = anIn || out;
        }
        return out;
    }

    public static boolean all(boolean[] in) {
        boolean out = true;
        for (boolean anIn : in) {
            out = anIn && out;
        }
        return out;
    }


    public static int sum(int[] in) {
        int cumsum = 0;
        for(int anIn : in) {
            cumsum += anIn;
        }
        return cumsum;
    }

    public static double mean(int[] in) {
        return sum(in) / (double)in.length;
    }

    public static int max(int[] in) {
        int max = in[0];
        for (int i = 1; i < in.length; i++) {
            if(in[i] > max) max = in[i];
        }
        return max;
    }

    public static int min(int[] in) {
        int min = in[0];
        for (int i = 1; i < in.length; i++) {
            if(in[i] < min) min = in[i];
        }
        return min;
    }

    public static int[] range(int[] in) {
        int max = in[0];
        int min = in[0];
        for (int i = 1; i < in.length; i++) {
            if(in[i] > max) max = in[i];
            if(in[i] < min) min = in[i];
        }
        return new int[] {min, max};
    }

    public static double[] range(double[] in) {
        return range(in, 0, in.length);
    }

    public static double[] range(double[] in, int start, int length) {
        double max = in[start];
        double min = in[start];
        for (int i = start+1; i < (start+length); i++) {
            if(in[i] > max) max = in[i];
            if(in[i] < min) min = in[i];
        }
        return new double[] {min, max};
    }
    
}
