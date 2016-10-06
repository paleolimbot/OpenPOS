package net.fishandwhistle.openpos.barcode;

import java.util.ArrayList;

import static net.fishandwhistle.openpos.barcode.ArrayMath.lt;
import static net.fishandwhistle.openpos.barcode.ArrayMath.range;
import static net.fishandwhistle.openpos.barcode.ArrayMath.rescale;
import static net.fishandwhistle.openpos.barcode.ArrayMath.subset;

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
            int windowlength = Math.min(windowsize, data.length-i);
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


    public int[] getBars() {
        if(this.thresholded == null)
            throw new IllegalStateException("Cannot extract bars without thresholding");
        ArrayList<Integer> barsout = new ArrayList<>();
        int first = 0;
        // skip all false values at start so we start on first true value
        while((first < this.thresholded.length-1) && !thresholded[first]) {
            first += 1;
        }
        if(first == thresholded.length) {
            // there are no true values in the array
            return new int[] {0};
        }

        int run = 1;
        boolean val = true;
        for(int i=first+1; i<this.thresholded.length; i++) {
            if(val==this.thresholded[i]) {
                run += 1;
            } else {
                val = this.thresholded[i];
                barsout.add(run);
                run = 1;
            }
        }
        barsout.add(run);

        int[] out = new int[barsout.size()];
        for(int i=0; i<out.length; i++) {
            out[i] = barsout.get(i);
        }
        return out;
    }

    public BarcodeSpec.Barcode multiExtract(BarcodeSpec[] specs) {
        BarcodeSpec.Barcode best = null;
        for(BarcodeSpec spec: specs) {
            BarcodeSpec.Barcode b = multiExtract(spec);
            if(b.isValid) {
                return b;
            } else if (best != null){
                if(b.getValidDigits() > best.getValidDigits()) {
                    best = b;
                }
            } else {
                best = b;
            }
        }
        return best;
    }

    public BarcodeSpec.Barcode multiExtract(BarcodeSpec spec) {
        BarcodeSpec.Barcode best = null;
        double[] thresholds = new double[] {0.4, 0.5, 0.6};
        this.transform(10, 0.5);
        for(double d: thresholds) {
                this.threshold(d);
                int[] bars = this.getBars();
                for(int i=0; i<Math.min(5, bars.length); i+= 2) {
                    try {
                        return spec.parse(subset(bars, i, bars.length-i));
                    } catch(BarcodeSpec.BarcodeException e) {
                        if(best != null) {
                            if(e.partial.getValidDigits() > best.getValidDigits()) {
                                best = e.partial;
                                best.tag = e.getMessage();
                            }
                        } else {
                            best = e.partial;
                            best.tag = e.getMessage();
                        }
                    }
                }
        }
        return best;
    }

}
