package net.fishandwhistle.openpos.barcode;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by dewey on 2016-10-02.
 */

public class ISBNSpec extends ISBNUPCSpec {

    public ISBNSpec() {
        super("ISBN", digisbn);
    }

    private boolean checksum(Barcode b) {
        int[] numbers = new int[b.digits.size()];
        for(int i=0; i<numbers.length; i++) {
            numbers[i] = Integer.valueOf(b.digits.get(i).digit);
        }
        int oddsum = 0;
        for(int i=0; i<numbers.length-1; i+=2) {
            oddsum += numbers[i];
        }
        int evensum = 0;
        for(int i=1; i<numbers.length; i+=2) {
            evensum += numbers[i];
        }
        int s1 = evensum * 3 + oddsum;
        int checksum = 10*(s1/10+1) - s1;
        if(checksum == 10) checksum = 0;
        return checksum == numbers[numbers.length-1];
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
        if(dig1isbn.containsKey(firstSchemes)) {
            b.digits.add(0, dig1isbn.get(firstSchemes));
        } else {
            b.digits.add(0, null);
        }

        int validDigits = 0;
        for(int i=0; i<b.digits.size(); i++) {
            if(b.digits.get(i) != null)
                validDigits++;
        }
        b.validDigits = validDigits;

        if(!b.isComplete()) throw new BarcodeException("Not all digits could be decoded", b);

        //try checksum
        if(!this.checksum(b)) throw new BarcodeException("Checksum failed for barcode", b);
        b.isValid = true;

        return b;
    }

    private static Map<BarcodePattern, BarcodeDigit> digisbn = new HashMap<>();
    private static Map<String, BarcodeDigit> dig1isbn = new HashMap<>();
    static {
        digisbn.put(new BarcodePattern(new int[] {3, 2, 1, 1}, false), new BarcodeDigit("0", "A")) ;
        digisbn.put(new BarcodePattern(new int[] {2, 2, 2, 1}, false), new BarcodeDigit("1", "A")) ;
        digisbn.put(new BarcodePattern(new int[] {2, 1, 2, 2}, false), new BarcodeDigit("2", "A")) ;
        digisbn.put(new BarcodePattern(new int[] {1, 4, 1, 1}, false), new BarcodeDigit("3", "A")) ;
        digisbn.put(new BarcodePattern(new int[] {1, 1, 3, 2}, false), new BarcodeDigit("4", "A")) ;
        digisbn.put(new BarcodePattern(new int[] {1, 2, 3, 1}, false), new BarcodeDigit("5", "A")) ;
        digisbn.put(new BarcodePattern(new int[] {1, 1, 1, 4}, false), new BarcodeDigit("6", "A")) ;
        digisbn.put(new BarcodePattern(new int[] {1, 3, 1, 2}, false), new BarcodeDigit("7", "A")) ;
        digisbn.put(new BarcodePattern(new int[] {1, 2, 1, 3}, false), new BarcodeDigit("8", "A")) ;
        digisbn.put(new BarcodePattern(new int[] {3, 1, 1, 2}, false), new BarcodeDigit("9", "A")) ;
        digisbn.put(new BarcodePattern(new int[] {1, 1, 2, 3}, false), new BarcodeDigit("0", "B")) ;
        digisbn.put(new BarcodePattern(new int[] {1, 2, 2, 2}, false), new BarcodeDigit("1", "B")) ;
        digisbn.put(new BarcodePattern(new int[] {2, 2, 1, 2}, false), new BarcodeDigit("2", "B")) ;
        digisbn.put(new BarcodePattern(new int[] {1, 1, 4, 1}, false), new BarcodeDigit("3", "B")) ;
        digisbn.put(new BarcodePattern(new int[] {2, 3, 1, 1}, false), new BarcodeDigit("4", "B")) ;
        digisbn.put(new BarcodePattern(new int[] {1, 3, 2, 1}, false), new BarcodeDigit("5", "B")) ;
        digisbn.put(new BarcodePattern(new int[] {4, 1, 1, 1}, false), new BarcodeDigit("6", "B")) ;
        digisbn.put(new BarcodePattern(new int[] {2, 1, 3, 1}, false), new BarcodeDigit("7", "B")) ;
        digisbn.put(new BarcodePattern(new int[] {3, 1, 2, 1}, false), new BarcodeDigit("8", "B")) ;
        digisbn.put(new BarcodePattern(new int[] {2, 1, 1, 3}, false), new BarcodeDigit("9", "B")) ;
        digisbn.put(new BarcodePattern(new int[] {3, 2, 1, 1}, true), new BarcodeDigit("0", "RIGHT")) ;
        digisbn.put(new BarcodePattern(new int[] {2, 2, 2, 1}, true), new BarcodeDigit("1", "RIGHT")) ;
        digisbn.put(new BarcodePattern(new int[] {2, 1, 2, 2}, true), new BarcodeDigit("2", "RIGHT")) ;
        digisbn.put(new BarcodePattern(new int[] {1, 4, 1, 1}, true), new BarcodeDigit("3", "RIGHT")) ;
        digisbn.put(new BarcodePattern(new int[] {1, 1, 3, 2}, true), new BarcodeDigit("4", "RIGHT")) ;
        digisbn.put(new BarcodePattern(new int[] {1, 2, 3, 1}, true), new BarcodeDigit("5", "RIGHT")) ;
        digisbn.put(new BarcodePattern(new int[] {1, 1, 1, 4}, true), new BarcodeDigit("6", "RIGHT")) ;
        digisbn.put(new BarcodePattern(new int[] {1, 3, 1, 2}, true), new BarcodeDigit("7", "RIGHT")) ;
        digisbn.put(new BarcodePattern(new int[] {1, 2, 1, 3}, true), new BarcodeDigit("8", "RIGHT")) ;
        digisbn.put(new BarcodePattern(new int[] {3, 1, 1, 2}, true), new BarcodeDigit("9", "RIGHT")) ;

        dig1isbn.put("AAAAAA", new BarcodeDigit("0", "FIRSTDIGIT")) ;
        dig1isbn.put("AABABB", new BarcodeDigit("1", "FIRSTDIGIT")) ;
        dig1isbn.put("AABBAB", new BarcodeDigit("2", "FIRSTDIGIT")) ;
        dig1isbn.put("AABBBA", new BarcodeDigit("3", "FIRSTDIGIT")) ;
        dig1isbn.put("ABAABB", new BarcodeDigit("4", "FIRSTDIGIT")) ;
        dig1isbn.put("ABBAAB", new BarcodeDigit("5", "FIRSTDIGIT")) ;
        dig1isbn.put("ABBBAA", new BarcodeDigit("6", "FIRSTDIGIT")) ;
        dig1isbn.put("ABABAB", new BarcodeDigit("7", "FIRSTDIGIT")) ;
        dig1isbn.put("ABABBA", new BarcodeDigit("8", "FIRSTDIGIT")) ;
        dig1isbn.put("ABBABA", new BarcodeDigit("9", "FIRSTDIGIT")) ;
    }
}
