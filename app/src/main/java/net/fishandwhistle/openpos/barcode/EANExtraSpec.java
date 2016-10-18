package net.fishandwhistle.openpos.barcode;

import java.util.HashMap;
import java.util.Map;

import static net.fishandwhistle.openpos.barcode.ArrayMath.any;
import static net.fishandwhistle.openpos.barcode.ArrayMath.div;
import static net.fishandwhistle.openpos.barcode.ArrayMath.gt;
import static net.fishandwhistle.openpos.barcode.ArrayMath.subset;
import static net.fishandwhistle.openpos.barcode.ArrayMath.sum;

/**
 * Created by dewey on 2016-10-18.
 */

public class EANExtraSpec extends EANSpec {


    public EANExtraSpec() {
        super("EAN-Extra", digean, 0, 0, 0, 0);
    }

    @Override
    public Barcode parse(int[] bars) throws BarcodeException {
        Barcode b = new Barcode(this.getType());
        boolean[] vals = new boolean[bars.length];
        for(int i=0; i<vals.length; i++) {
            vals[i] = (i%2) == 0 ;
        }
        // 31 for 5-digit code, 13 for 2-digit code
        if(bars.length < 13) throw new BarcodeException("Not enough bars to create code", b);
        // test start code
        int[] leftGuard = subset(bars, 0, 3);
        double barsize = sum(leftGuard)/4.0;
        if(any(gt(div(leftGuard, barsize), 3.0))) throw new BarcodeException("Left guard has irregular barsize", b);

        //look for digits
        for(int i=0; i<5; i++) {
            if((3+i*6+4) >= bars.length) throw new BarcodeException("Not enough bars to decode next digit", b);
            b.digits.add(getDigit(subset(bars, 3+i*6, 4), false));
            if((3+i*6+6) >= bars.length) {
                //no inter-character space
                break;
            } else {
                int[] interChar = subset(bars, 3+i*6+4, 2);
                barsize = (barsize + sum(interChar)/2.0) / 2.0;
                if(any(gt(div(leftGuard, barsize), 3.0))) throw new BarcodeException("Intercharacter space has irregular barsize", b);
            }
        }
        if(!b.isComplete()) throw new BarcodeException("Not all digits could be decoded", b);

        if(b.digits.size() == 2) {
            int checkRemainder = Integer.valueOf(b.toString()) % 4;
            String parities = b.digits.get(0).tag + b.digits.get(1).tag;
            boolean error;
            switch (checkRemainder) {
                case 0:
                    error = !parities.equals("AA");
                    break;
                case 1:
                    error = !parities.equals("AB");
                    break;
                case 2:
                    error = !parities.equals("BA");
                    break;
                case 3:
                    error = !parities.equals("BB");
                    break;
                default: throw new RuntimeException("Unexpected value for checkRemainder: " + checkRemainder);
            }
            if(error) throw new BarcodeException("Parity encoding doesn't match remainder value", b);
        } else if(b.digits.size() == 5) {
            String schemes = "";
            int[] nums = new int[6];
            for(int i=0; i<5; i++) {
                BarcodeDigit d = b.digits.get(i);
                schemes += d.tag;
                nums[i] = Integer.valueOf(d.digit);
            }
            if(!digCheckEANExtra.containsKey(schemes)) throw new BarcodeException("Extra digit encoding not found", b);
            int checkVal = Integer.valueOf(digCheckEANExtra.get(schemes).digit);
            int calcCheckVal = (nums[0]*3 + nums[1]*9 + nums[2]*3 + nums[3]*9 + nums[4]*3) % 10;
            if(checkVal != calcCheckVal) throw new BarcodeException("Checksum failed", b);
        } else {
            throw new BarcodeException("Unexpected number of digits in barcode (" + b.digits.size() + ")", b);
        }

        b.isValid = true;
        return b;
    }

    private static Map<String, BarcodeDigit> digCheckEANExtra = new HashMap<>();
    static {
        digCheckEANExtra.put("BBAAA", new BarcodeDigit("0", "EXTRACHECK"));
        digCheckEANExtra.put("BABAA", new BarcodeDigit("1", "EXTRACHECK"));
        digCheckEANExtra.put("BAABA", new BarcodeDigit("2", "EXTRACHECK"));
        digCheckEANExtra.put("BAAAB", new BarcodeDigit("3", "EXTRACHECK"));
        digCheckEANExtra.put("ABBAA", new BarcodeDigit("4", "EXTRACHECK"));
        digCheckEANExtra.put("AABBA", new BarcodeDigit("5", "EXTRACHECK"));
        digCheckEANExtra.put("AAABB", new BarcodeDigit("6", "EXTRACHECK"));
        digCheckEANExtra.put("ABABA", new BarcodeDigit("7", "EXTRACHECK"));
        digCheckEANExtra.put("ABAAB", new BarcodeDigit("8", "EXTRACHECK"));
        digCheckEANExtra.put("AABAB", new BarcodeDigit("9", "EXTRACHECK"));
    }

}
