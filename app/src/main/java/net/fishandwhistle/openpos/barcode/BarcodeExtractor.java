package net.fishandwhistle.openpos.barcode;

import java.util.ArrayList;

import static net.fishandwhistle.openpos.barcode.ArrayMath.lt;
import static net.fishandwhistle.openpos.barcode.ArrayMath.range;
import static net.fishandwhistle.openpos.barcode.ArrayMath.rescale;

/**
 * Created by dewey on 2016-09-29.
 */

public class BarcodeExtractor {

    private double[] data;
    private double[] transformed;
    private boolean[] thresholded;

    public BarcodeExtractor(double[] data) {
        this.data = data;
        this.transformed = null;
        this.thresholded = null;
    }

    public void transform(int nwindows, double minrange) {
        this.transformed = new double[data.length];
        //set params
        //int nwindows = 15;
        //double minrange = 0.5;

        //rescale data
        double[] datarange = range(data);
        double[] rescaled = rescale(data, datarange[0], datarange[1]-datarange[0]);

        //do floating range contrast enhancer
        int windowsize = data.length / nwindows;
        for(int i=0; i<data.length; i+=windowsize) {
            int windowlength = Math.min(windowsize, data.length-1-i+windowsize);
            double[] localrange = range(rescaled, i, windowlength);
            if((localrange[1] - localrange[0]) < minrange) {
                localrange[0] = 0;
            }
            for(int j=0; j<windowlength; j++) {
                transformed[i+j] = rescale(rescaled[i+j], localrange[0], localrange[1]-localrange[0]);
            }
        }
    }

    public void threshold(double value) {
        if(this.transformed == null) {
            this.thresholded = lt(this.data, value);
        } else {
            this.thresholded = lt(this.transformed, value);
        }
    }


    private int[] getBars() {
        if(this.thresholded == null)
            throw new IllegalStateException("Cannot extract bars without thresholding");
        ArrayList<Integer> barsout = new ArrayList<>();
        int first = 0;
        // skip all false values at start so we start on first true value
        while(!thresholded[first]) {
            first += 1;
        }
        int run = 0;
        boolean val = true;
        for(int i=first; i<this.thresholded.length; i=run) {
            if(val==this.thresholded[i]) {
                run += 1;
            } else {
                val = !val;
                barsout.add(run);
            }
        }
        barsout.add(run);

        int[] out = new int[barsout.size()];
        for(int i=0; i<out.length; i++) {
            out[i] = barsout.get(i);
        }
        return out;
    }
}
