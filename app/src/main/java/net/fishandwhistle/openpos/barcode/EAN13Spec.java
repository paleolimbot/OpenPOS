package net.fishandwhistle.openpos.barcode;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by dewey on 2016-10-02.
 */

public class EAN13Spec extends EANSpec {

    public EAN13Spec() {
        super("EAN-13", digean, 3, 5, 3, 59);
    }

    public EAN13Spec(String type, int begGuardLength, int middleGuardLength, int endGuardLength, int nbars) {
        super(type, digean, begGuardLength, middleGuardLength, endGuardLength, nbars);
    }

    public Barcode parse(int[] bars) throws BarcodeException {
        Barcode b = this.parse_common(bars);

        //decode first digit
        String firstSchemes = "";
        for(int i=0; i<6; i++) {
            BarcodeDigit d = b.digits.get(i);
            if(d==null) {
                break;
            } else {
                firstSchemes += d.tag;
            }
        }
        if(dig1ean.containsKey(firstSchemes)) {
            b.digits.add(0, dig1ean.get(firstSchemes));
        } else {
            b.digits.add(0, null);
        }

        if(!b.isComplete()) throw new BarcodeException("Not all digits could be decoded", b);

        //try checksum
        if(!Checksums.checksum(b, 3, 1)) throw new BarcodeException("Checksum failed for barcode", b);

        String bstr = b.toString();
        if(bstr.startsWith("978") || bstr.startsWith("979")) {
            b.type = "ISBN-13";
        }
        b.isValid = true;

        return b;
    }


    @Override
    public void initialize() {
        digean.containsValue(new BarcodeDigit("0"));
        dig1ean.containsValue(new BarcodeDigit("1"));
    }

    protected static Map<BarcodePattern, BarcodeDigit> digean = new HashMap<>();
    protected static Map<String, BarcodeDigit> dig1ean = new HashMap<>();
    static {
        digean.put(new BarcodePattern(new int[] {3, 2, 1, 1}, false), new BarcodeDigit("0", "A")) ;
        digean.put(new BarcodePattern(new int[] {2, 2, 2, 1}, false), new BarcodeDigit("1", "A")) ;
        digean.put(new BarcodePattern(new int[] {2, 1, 2, 2}, false), new BarcodeDigit("2", "A")) ;
        digean.put(new BarcodePattern(new int[] {1, 4, 1, 1}, false), new BarcodeDigit("3", "A")) ;
        digean.put(new BarcodePattern(new int[] {1, 1, 3, 2}, false), new BarcodeDigit("4", "A")) ;
        digean.put(new BarcodePattern(new int[] {1, 2, 3, 1}, false), new BarcodeDigit("5", "A")) ;
        digean.put(new BarcodePattern(new int[] {1, 1, 1, 4}, false), new BarcodeDigit("6", "A")) ;
        digean.put(new BarcodePattern(new int[] {1, 3, 1, 2}, false), new BarcodeDigit("7", "A")) ;
        digean.put(new BarcodePattern(new int[] {1, 2, 1, 3}, false), new BarcodeDigit("8", "A")) ;
        digean.put(new BarcodePattern(new int[] {3, 1, 1, 2}, false), new BarcodeDigit("9", "A")) ;
        digean.put(new BarcodePattern(new int[] {1, 1, 2, 3}, false), new BarcodeDigit("0", "B")) ;
        digean.put(new BarcodePattern(new int[] {1, 2, 2, 2}, false), new BarcodeDigit("1", "B")) ;
        digean.put(new BarcodePattern(new int[] {2, 2, 1, 2}, false), new BarcodeDigit("2", "B")) ;
        digean.put(new BarcodePattern(new int[] {1, 1, 4, 1}, false), new BarcodeDigit("3", "B")) ;
        digean.put(new BarcodePattern(new int[] {2, 3, 1, 1}, false), new BarcodeDigit("4", "B")) ;
        digean.put(new BarcodePattern(new int[] {1, 3, 2, 1}, false), new BarcodeDigit("5", "B")) ;
        digean.put(new BarcodePattern(new int[] {4, 1, 1, 1}, false), new BarcodeDigit("6", "B")) ;
        digean.put(new BarcodePattern(new int[] {2, 1, 3, 1}, false), new BarcodeDigit("7", "B")) ;
        digean.put(new BarcodePattern(new int[] {3, 1, 2, 1}, false), new BarcodeDigit("8", "B")) ;
        digean.put(new BarcodePattern(new int[] {2, 1, 1, 3}, false), new BarcodeDigit("9", "B")) ;
        digean.put(new BarcodePattern(new int[] {3, 2, 1, 1}, true), new BarcodeDigit("0", "RIGHT")) ;
        digean.put(new BarcodePattern(new int[] {2, 2, 2, 1}, true), new BarcodeDigit("1", "RIGHT")) ;
        digean.put(new BarcodePattern(new int[] {2, 1, 2, 2}, true), new BarcodeDigit("2", "RIGHT")) ;
        digean.put(new BarcodePattern(new int[] {1, 4, 1, 1}, true), new BarcodeDigit("3", "RIGHT")) ;
        digean.put(new BarcodePattern(new int[] {1, 1, 3, 2}, true), new BarcodeDigit("4", "RIGHT")) ;
        digean.put(new BarcodePattern(new int[] {1, 2, 3, 1}, true), new BarcodeDigit("5", "RIGHT")) ;
        digean.put(new BarcodePattern(new int[] {1, 1, 1, 4}, true), new BarcodeDigit("6", "RIGHT")) ;
        digean.put(new BarcodePattern(new int[] {1, 3, 1, 2}, true), new BarcodeDigit("7", "RIGHT")) ;
        digean.put(new BarcodePattern(new int[] {1, 2, 1, 3}, true), new BarcodeDigit("8", "RIGHT")) ;
        digean.put(new BarcodePattern(new int[] {3, 1, 1, 2}, true), new BarcodeDigit("9", "RIGHT")) ;

        dig1ean.put("AAAAAA", new BarcodeDigit("0", "FIRSTDIGIT")) ;
        dig1ean.put("AABABB", new BarcodeDigit("1", "FIRSTDIGIT")) ;
        dig1ean.put("AABBAB", new BarcodeDigit("2", "FIRSTDIGIT")) ;
        dig1ean.put("AABBBA", new BarcodeDigit("3", "FIRSTDIGIT")) ;
        dig1ean.put("ABAABB", new BarcodeDigit("4", "FIRSTDIGIT")) ;
        dig1ean.put("ABBAAB", new BarcodeDigit("5", "FIRSTDIGIT")) ;
        dig1ean.put("ABBBAA", new BarcodeDigit("6", "FIRSTDIGIT")) ;
        dig1ean.put("ABABAB", new BarcodeDigit("7", "FIRSTDIGIT")) ;
        dig1ean.put("ABABBA", new BarcodeDigit("8", "FIRSTDIGIT")) ;
        dig1ean.put("ABBABA", new BarcodeDigit("9", "FIRSTDIGIT")) ;
    }

}
