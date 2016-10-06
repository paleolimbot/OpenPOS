package net.fishandwhistle.openpos.barcode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by dewey on 2016-10-03.
 */

public class UPCESpec extends UPCASpec {
    public UPCESpec() {
        super("UPC/E", digupce, 3, 0, 6, 33);
    }

    @Override
    public Barcode parse(int[] bars) throws BarcodeException {
        Barcode b = parse_common(bars) ;

        if(!b.isComplete()) throw new BarcodeException("Not all digits could be decoded", b);

        //add additional digits
        String parityPattern = "";
        for(BarcodeDigit d: b.digits) {
            parityPattern += d.tag;
        }

        if(!upceParities.containsKey(parityPattern)) throw new BarcodeException("Invalid UPC-E parity pattern", b);
        int[] numSysCheckDig = upceParities.get(parityPattern);


        int[] zeroPattern = upceZeroPatterns[Integer.valueOf(b.digits.get(b.digits.size()-1).digit)];
        List<BarcodeDigit> newdig = new ArrayList<>();
        //add number system
        newdig.add(new BarcodeDigit(String.valueOf(numSysCheckDig[0]), "PARITY-PATTERN"));

        // copy zeroPattern[0] numbers to the new digits
        for(int j=0; j<zeroPattern[0]; j++){
            newdig.add(b.digits.get(j));
        }
        // add the value of zeroPattern[1] to the new digits
        newdig.add(new BarcodeDigit(String.valueOf(zeroPattern[1]), "UPCE-ADDED"));

        // add zeroPattern[2] zeroes to the new digits
        for(int j=0; j<zeroPattern[2]; j++) {
            newdig.add(new BarcodeDigit("0", "UPCE-ADDED"));
        }

        // copy zeroPattern[3] additional digits from original code
        for(int j=0; j<zeroPattern[3]; j++) {
            newdig.add(b.digits.get(zeroPattern[0]+j));
        }

        //add check digit
        newdig.add(new BarcodeDigit(String.valueOf(numSysCheckDig[1]), "PARITY-PATTERN"));

        //reassign barcode digits
        b.digits = newdig;

        //try checksum
        if(!this.checksum(b, 1, 3)) throw new BarcodeException("Checksum failed for barcode", b);
        //checksum isn't part of the code, remove it
        b.digits.remove(b.digits.size()-1);
        b.isValid = true;

        return b;
    }

        /*
Last UPC-E digit 	UPC-E numerical pattern 	UPC-A equivalent
0 	XXNNN0 	0 or 1 + XX000-00NNN + check digit
1 	XXNNN1 	0 or 1 + XX100-00NNN + check digit
2 	XXNNN2 	0 or 1 + XX200-00NNN + check digit
3 	XXXNN3 	0 or 1 + XXX00-000NN + check digit
4 	XXXXN4 	0 or 1 + XXXX0-0000N + check digit
5 	XXXXX5 	0 or 1 + XXXXX-00005 + check digit
6 	XXXXX6 	0 or 1 + XXXXX-00006 + check digit
7 	XXXXX7 	0 or 1 + XXXXX-00007 + check digit
8 	XXXXX8 	0 or 1 + XXXXX-00008 + check digit
9 	XXXXX9 	0 or 1 + XXXXX-00009 + check digit
    */

    private static final int[][] upceZeroPatterns = new int[][] {
            new int[] {2, 0, 4, 3},
            new int[] {2, 1, 4, 3},
            new int[] {2, 2, 4, 3},
            new int[] {3, 0, 4, 2},
            new int[] {4, 0, 4, 1},
            new int[] {5, 0, 3, 1},
            new int[] {5, 0, 3, 1},
            new int[] {5, 0, 3, 1},
            new int[] {5, 0, 3, 1},
            new int[] {5, 0, 3, 1}
    };

    private static Map<String, int[]> upceParities = new HashMap<>();

    static {
        upceParities.put("EEEOOO", new int[] {0, 0});
        upceParities.put("EEOEOO", new int[] {0, 1});
        upceParities.put("EEOOEO", new int[] {0, 2});
        upceParities.put("EEOOOE", new int[] {0, 3});
        upceParities.put("EOEEOO", new int[] {0, 4});
        upceParities.put("EOOEEO", new int[] {0, 5});
        upceParities.put("EOOOEE", new int[] {0, 6});
        upceParities.put("EOEOEO", new int[] {0, 7});
        upceParities.put("EOEOOE", new int[] {0, 8});
        upceParities.put("EOOEOE", new int[] {0, 9});

        upceParities.put("OOOEEE", new int[] {1, 0});
        upceParities.put("OOEOEE", new int[] {1, 1});
        upceParities.put("OOEEOE", new int[] {1, 2});
        upceParities.put("OOEEEO", new int[] {1, 3});
        upceParities.put("OEOOEE", new int[] {1, 4});
        upceParities.put("OEEOOE", new int[] {1, 5});
        upceParities.put("OEEEOO", new int[] {1, 6});
        upceParities.put("OEOEOE", new int[] {1, 7});
        upceParities.put("OEOEEO", new int[] {1, 8});
        upceParities.put("OEEOEO", new int[] {1, 9});
    }

    /*
UPC-A
check digit 	UPC-E parity pattern for UPC-A

number system 0
	UPC-E parity pattern for UPC-A

check   ns0 ns1
0 	EEEOOO 	OOOEEE
1 	EEOEOO 	OOEOEE
2 	EEOOEO 	OOEEOE
3 	EEOOOE 	OOEEEO
4 	EOEEOO 	OEOOEE
5 	EOOEEO 	OEEOOE
6 	EOOOEE 	OEEEOO
7 	EOEOEO 	OEOEOE
8 	EOEOOE 	OEOEEO
9 	EOOEOE 	OEEOEO
     */

    protected static Map<BarcodePattern, BarcodeDigit> digupce = new HashMap<>();
    static {
        digupce.put(new BarcodePattern(new int[]{3, 2, 1, 1}, false), new BarcodeDigit("0", "O"));
        digupce.put(new BarcodePattern(new int[]{2, 2, 2, 1}, false), new BarcodeDigit("1", "O"));
        digupce.put(new BarcodePattern(new int[]{2, 1, 2, 2}, false), new BarcodeDigit("2", "O"));
        digupce.put(new BarcodePattern(new int[]{1, 4, 1, 1}, false), new BarcodeDigit("3", "O"));
        digupce.put(new BarcodePattern(new int[]{1, 1, 3, 2}, false), new BarcodeDigit("4", "O"));
        digupce.put(new BarcodePattern(new int[]{1, 2, 3, 1}, false), new BarcodeDigit("5", "O"));
        digupce.put(new BarcodePattern(new int[]{1, 1, 1, 4}, false), new BarcodeDigit("6", "O"));
        digupce.put(new BarcodePattern(new int[]{1, 3, 1, 2}, false), new BarcodeDigit("7", "O"));
        digupce.put(new BarcodePattern(new int[]{1, 2, 1, 3}, false), new BarcodeDigit("8", "O"));
        digupce.put(new BarcodePattern(new int[]{3, 1, 1, 2}, false), new BarcodeDigit("9", "O"));

        digupce.put(new BarcodePattern(new int[]{1, 1, 2, 3}, false), new BarcodeDigit("0", "E"));
        digupce.put(new BarcodePattern(new int[]{1, 2, 2, 2}, false), new BarcodeDigit("1", "E"));
        digupce.put(new BarcodePattern(new int[]{2, 2, 1, 2}, false), new BarcodeDigit("2", "E"));
        digupce.put(new BarcodePattern(new int[]{1, 1, 4, 1}, false), new BarcodeDigit("3", "E"));
        digupce.put(new BarcodePattern(new int[]{2, 3, 1, 1}, false), new BarcodeDigit("4", "E"));
        digupce.put(new BarcodePattern(new int[]{1, 3, 2, 1}, false), new BarcodeDigit("5", "E"));
        digupce.put(new BarcodePattern(new int[]{4, 1, 1, 1}, false), new BarcodeDigit("6", "E"));
        digupce.put(new BarcodePattern(new int[]{2, 1, 3, 1}, false), new BarcodeDigit("7", "E"));
        digupce.put(new BarcodePattern(new int[]{3, 1, 2, 1}, false), new BarcodeDigit("8", "E"));
        digupce.put(new BarcodePattern(new int[]{2, 1, 1, 3}, false), new BarcodeDigit("9", "E"));
    }

}
