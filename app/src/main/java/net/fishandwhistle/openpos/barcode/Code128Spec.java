package net.fishandwhistle.openpos.barcode;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.fishandwhistle.openpos.barcode.ArrayMath.div;
import static net.fishandwhistle.openpos.barcode.ArrayMath.round;
import static net.fishandwhistle.openpos.barcode.ArrayMath.subset;
import static net.fishandwhistle.openpos.barcode.ArrayMath.sum;

/**
 * Created by dewey on 2016-10-12.
 */

public class Code128Spec extends DualWidthSpec {

    private int[] startVals ;

    public Code128Spec() {
        this(5, false, "ABC");
    }

    public Code128Spec(int minLength, boolean fixedLength, String startChars) {
        super("Code128", digc128, minLength, fixedLength);
        startVals = new int[startChars.length()];
        for(int i=0; i<startChars.length(); i++) {
            String sub = startChars.substring(i, i+1);
            if(sub.equals("A")) {
                startVals[i] = 103;
            } else if(sub.equals("B")) {
                startVals[i] = 104;
            } else if(sub.equals("C")) {
                startVals[i] = 105;
            }
        }
    }

    @Override
    public Barcode parse(int[] bars) throws BarcodeException {
        Barcode b = new Barcode(this.getType());
        if(bars.length < (6 + ((minLength+1) * 6) + 7)) throw new BarcodeException("Too few bars to decode", b);

        int startIndex = -1;
        int endIndex = -1;
        int starti = 0;
        int checksumTotal = 0;

        while(true) {
            if((starti+6) > bars.length) break;
            Code128Digit d = (Code128Digit)this.getDigit(subset(bars, starti, 6), true);

            if((startIndex == -1) && (d != null)) {
                //look for start characters
                for(int startVal: startVals) {
                    if(startVal == d.value) {
                        startIndex = starti;
                        b.digits.add(d);
                        checksumTotal += d.value;
                        break;
                    }
                }
            } else {
                //use as general character, if next 7 don't match stop character
                if((starti+7) > bars.length) break;
                if(STOP.equals(getBarcodePattern(subset(bars, starti, 7), true))) {
                    endIndex = starti;
                    break;
                } else {
                    if(!partial && (d == null)) {
                        throw new BarcodeException("Undecodable digit", b);
                    } else if(d != null) {
                        checksumTotal += d.value * b.digits.size();
                    }
                    b.digits.add(d);
                }

            }
            starti += 6;
        }

        // try to decode end digit
        if(startIndex == -1) throw new BarcodeException("No start character encountered", b);
        if(endIndex == -1) throw new BarcodeException("No end character encountered", b);
        if(b.digits.size() < minLength) throw new BarcodeException("Encoded string is too short", b);
        if(!b.isComplete()) throw new BarcodeException("Not all digits could be decoded", b);

        // try checksum
        int lastVal = ((Code128Digit)b.digits.get(b.digits.size()-1)).value;
        checksumTotal -=  lastVal * (b.digits.size()-1);
        if((checksumTotal % 103) != lastVal) throw new BarcodeException("Checksum failed", b);
        // remove check digit
        b.digits.remove(b.digits.size()-1);

        // transform to regular barcode digits
        int system = -1;
        List<BarcodeDigit> newdigs = new ArrayList<>();
        boolean shift = false;

        for(BarcodeDigit digit: b.digits) {
            Code128Digit d = (Code128Digit)digit;

            if(system == -1) {
                system = d.value;
                //don't add code to digits
                continue;
            } else if(d.value == 99 && (system == 103 || system == 104)) {
                //switch to code C
                system = 105;
                //newdigs.add(new BarcodeDigit("[CodeC]"));
                continue;
            } else if(d.value == 100 && (system == 103 || system == 105)) {
                //switch to code B
                system = 104;
                //newdigs.add(new BarcodeDigit("[CodeB]"));
                continue;
            } else if(d.value == 101 && (system == 104 || system == 105)) {
                //switch to code A
                system = 103;
                //newdigs.add(new BarcodeDigit("[CodeA]"));
                continue;
            }

            switch(system) {
                case 103:
                    if(d.value == 98) {
                        shift = !shift;
                    } else if(shift) {
                        newdigs.add(new BarcodeDigit(d.getCodeB()));
                        shift = false;
                    } else {
                        newdigs.add(new BarcodeDigit(d.getCodeA()));
                    }
                    break;
                case 104:
                    if(d.value == 98) {
                        shift = !shift;
                    } else if(shift) {
                        newdigs.add(new BarcodeDigit(d.getCodeA()));
                        shift = false;
                    } else {
                        newdigs.add(new BarcodeDigit(d.getCodeB()));
                    }
                    break;
                case 105:
                    newdigs.add(new BarcodeDigit(d.getCodeC()));
                    break;
            }
        }

        //Possibly parse GS1-128 here using https://en.wikipedia.org/wiki/GS1-128

        b.digits = newdigs;
        // now check for exact length
        this.checkLength(b);
        b.isValid = true;

        return b;
    }

    protected BarcodePattern getBarcodePattern(int[] bars, boolean start) {
        if(bars.length == 6) {
            return new BarcodePattern(round(div(bars, sum(bars) / 11.0)), start);
        } else if(bars.length == 7) {
            return new BarcodePattern(round(div(bars, sum(bars) / 13.0)), start);
        } else {
            throw new IllegalArgumentException("Incorrect length of bars input");
        }
    }

    public static class Code128Digit extends BarcodeDigit {

        public int value;
        public String codeC;

        public Code128Digit(int value, String codeA, String codeB, String codeC) {
            super(codeA, codeB);
            this.value = value;
            this.codeC = codeC;
        }

        public String getCodeA() {
            return digit;
        }

        public String getCodeB() {
            return tag;
        }

        public String getCodeC() {
            return codeC;
        }
    }

    private static Map<BarcodePattern, BarcodeDigit> digc128 = new HashMap<>();
    static {
        digc128.put(new BarcodePattern(new int[] {2, 1, 2, 2, 2, 2}, true), new Code128Digit(0, " ", " ", "00"));
        digc128.put(new BarcodePattern(new int[] {2, 2, 2, 1, 2, 2}, true), new Code128Digit(1, "!", "!", "01"));
        digc128.put(new BarcodePattern(new int[] {2, 2, 2, 2, 2, 1}, true), new Code128Digit(2, "\"", "\"", "02"));
        digc128.put(new BarcodePattern(new int[] {1, 2, 1, 2, 2, 3}, true), new Code128Digit(3, "#", "#", "03"));
        digc128.put(new BarcodePattern(new int[] {1, 2, 1, 3, 2, 2}, true), new Code128Digit(4, "$", "$", "04"));
        digc128.put(new BarcodePattern(new int[] {1, 3, 1, 2, 2, 2}, true), new Code128Digit(5, "%", "%", "05"));
        digc128.put(new BarcodePattern(new int[] {1, 2, 2, 2, 1, 3}, true), new Code128Digit(6, "&", "&", "06"));
        digc128.put(new BarcodePattern(new int[] {1, 2, 2, 3, 1, 2}, true), new Code128Digit(7, "'", "'", "07"));
        digc128.put(new BarcodePattern(new int[] {1, 3, 2, 2, 1, 2}, true), new Code128Digit(8, "(", "(", "08"));
        digc128.put(new BarcodePattern(new int[] {2, 2, 1, 2, 1, 3}, true), new Code128Digit(9, ")", ")", "09"));
        digc128.put(new BarcodePattern(new int[] {2, 2, 1, 3, 1, 2}, true), new Code128Digit(10, "*", "*", "10"));
        digc128.put(new BarcodePattern(new int[] {2, 3, 1, 2, 1, 2}, true), new Code128Digit(11, "+", "+", "11"));
        digc128.put(new BarcodePattern(new int[] {1, 1, 2, 2, 3, 2}, true), new Code128Digit(12, ",", ",", "12"));
        digc128.put(new BarcodePattern(new int[] {1, 2, 2, 1, 3, 2}, true), new Code128Digit(13, "-", "-", "13"));
        digc128.put(new BarcodePattern(new int[] {1, 2, 2, 2, 3, 1}, true), new Code128Digit(14, ".", ".", "14"));
        digc128.put(new BarcodePattern(new int[] {1, 1, 3, 2, 2, 2}, true), new Code128Digit(15, "/", "/", "15"));
        digc128.put(new BarcodePattern(new int[] {1, 2, 3, 1, 2, 2}, true), new Code128Digit(16, "0", "0", "16"));
        digc128.put(new BarcodePattern(new int[] {1, 2, 3, 2, 2, 1}, true), new Code128Digit(17, "1", "1", "17"));
        digc128.put(new BarcodePattern(new int[] {2, 2, 3, 2, 1, 1}, true), new Code128Digit(18, "2", "2", "18"));
        digc128.put(new BarcodePattern(new int[] {2, 2, 1, 1, 3, 2}, true), new Code128Digit(19, "3", "3", "19"));
        digc128.put(new BarcodePattern(new int[] {2, 2, 1, 2, 3, 1}, true), new Code128Digit(20, "4", "4", "20"));
        digc128.put(new BarcodePattern(new int[] {2, 1, 3, 2, 1, 2}, true), new Code128Digit(21, "5", "5", "21"));
        digc128.put(new BarcodePattern(new int[] {2, 2, 3, 1, 1, 2}, true), new Code128Digit(22, "6", "6", "22"));
        digc128.put(new BarcodePattern(new int[] {3, 1, 2, 1, 3, 1}, true), new Code128Digit(23, "7", "7", "23"));
        digc128.put(new BarcodePattern(new int[] {3, 1, 1, 2, 2, 2}, true), new Code128Digit(24, "8", "8", "24"));
        digc128.put(new BarcodePattern(new int[] {3, 2, 1, 1, 2, 2}, true), new Code128Digit(25, "9", "9", "25"));
        digc128.put(new BarcodePattern(new int[] {3, 2, 1, 2, 2, 1}, true), new Code128Digit(26, ":", ":", "26"));
        digc128.put(new BarcodePattern(new int[] {3, 1, 2, 2, 1, 2}, true), new Code128Digit(27, ";", ";", "27"));
        digc128.put(new BarcodePattern(new int[] {3, 2, 2, 1, 1, 2}, true), new Code128Digit(28, "<", "<", "28"));
        digc128.put(new BarcodePattern(new int[] {3, 2, 2, 2, 1, 1}, true), new Code128Digit(29, "=", "=", "29"));
        digc128.put(new BarcodePattern(new int[] {2, 1, 2, 1, 2, 3}, true), new Code128Digit(30, ">", ">", "30"));
        digc128.put(new BarcodePattern(new int[] {2, 1, 2, 3, 2, 1}, true), new Code128Digit(31, "?", "?", "31"));
        digc128.put(new BarcodePattern(new int[] {2, 3, 2, 1, 2, 1}, true), new Code128Digit(32, "@", "@", "32"));
        digc128.put(new BarcodePattern(new int[] {1, 1, 1, 3, 2, 3}, true), new Code128Digit(33, "A", "A", "33"));
        digc128.put(new BarcodePattern(new int[] {1, 3, 1, 1, 2, 3}, true), new Code128Digit(34, "B", "B", "34"));
        digc128.put(new BarcodePattern(new int[] {1, 3, 1, 3, 2, 1}, true), new Code128Digit(35, "C", "C", "35"));
        digc128.put(new BarcodePattern(new int[] {1, 1, 2, 3, 1, 3}, true), new Code128Digit(36, "D", "D", "36"));
        digc128.put(new BarcodePattern(new int[] {1, 3, 2, 1, 1, 3}, true), new Code128Digit(37, "E", "E", "37"));
        digc128.put(new BarcodePattern(new int[] {1, 3, 2, 3, 1, 1}, true), new Code128Digit(38, "F", "F", "38"));
        digc128.put(new BarcodePattern(new int[] {2, 1, 1, 3, 1, 3}, true), new Code128Digit(39, "G", "G", "39"));
        digc128.put(new BarcodePattern(new int[] {2, 3, 1, 1, 1, 3}, true), new Code128Digit(40, "H", "H", "40"));
        digc128.put(new BarcodePattern(new int[] {2, 3, 1, 3, 1, 1}, true), new Code128Digit(41, "I", "I", "41"));
        digc128.put(new BarcodePattern(new int[] {1, 1, 2, 1, 3, 3}, true), new Code128Digit(42, "J", "J", "42"));
        digc128.put(new BarcodePattern(new int[] {1, 1, 2, 3, 3, 1}, true), new Code128Digit(43, "K", "K", "43"));
        digc128.put(new BarcodePattern(new int[] {1, 3, 2, 1, 3, 1}, true), new Code128Digit(44, "L", "L", "44"));
        digc128.put(new BarcodePattern(new int[] {1, 1, 3, 1, 2, 3}, true), new Code128Digit(45, "M", "M", "45"));
        digc128.put(new BarcodePattern(new int[] {1, 1, 3, 3, 2, 1}, true), new Code128Digit(46, "N", "N", "46"));
        digc128.put(new BarcodePattern(new int[] {1, 3, 3, 1, 2, 1}, true), new Code128Digit(47, "O", "O", "47"));
        digc128.put(new BarcodePattern(new int[] {3, 1, 3, 1, 2, 1}, true), new Code128Digit(48, "P", "P", "48"));
        digc128.put(new BarcodePattern(new int[] {2, 1, 1, 3, 3, 1}, true), new Code128Digit(49, "Q", "Q", "49"));
        digc128.put(new BarcodePattern(new int[] {2, 3, 1, 1, 3, 1}, true), new Code128Digit(50, "R", "R", "50"));
        digc128.put(new BarcodePattern(new int[] {2, 1, 3, 1, 1, 3}, true), new Code128Digit(51, "S", "S", "51"));
        digc128.put(new BarcodePattern(new int[] {2, 1, 3, 3, 1, 1}, true), new Code128Digit(52, "T", "T", "52"));
        digc128.put(new BarcodePattern(new int[] {2, 1, 3, 1, 3, 1}, true), new Code128Digit(53, "U", "U", "53"));
        digc128.put(new BarcodePattern(new int[] {3, 1, 1, 1, 2, 3}, true), new Code128Digit(54, "V", "V", "54"));
        digc128.put(new BarcodePattern(new int[] {3, 1, 1, 3, 2, 1}, true), new Code128Digit(55, "W", "W", "55"));
        digc128.put(new BarcodePattern(new int[] {3, 3, 1, 1, 2, 1}, true), new Code128Digit(56, "X", "X", "56"));
        digc128.put(new BarcodePattern(new int[] {3, 1, 2, 1, 1, 3}, true), new Code128Digit(57, "Y", "Y", "57"));
        digc128.put(new BarcodePattern(new int[] {3, 1, 2, 3, 1, 1}, true), new Code128Digit(58, "Z", "Z", "58"));
        digc128.put(new BarcodePattern(new int[] {3, 3, 2, 1, 1, 1}, true), new Code128Digit(59, "[", "[", "59"));
        digc128.put(new BarcodePattern(new int[] {3, 1, 4, 1, 1, 1}, true), new Code128Digit(60, "\\", "\\", "60"));
        digc128.put(new BarcodePattern(new int[] {2, 2, 1, 4, 1, 1}, true), new Code128Digit(61, "]", "]", "61"));
        digc128.put(new BarcodePattern(new int[] {4, 3, 1, 1, 1, 1}, true), new Code128Digit(62, "^", "^", "62"));
        digc128.put(new BarcodePattern(new int[] {1, 1, 1, 2, 2, 4}, true), new Code128Digit(63, "_", "_", "63"));
        digc128.put(new BarcodePattern(new int[] {1, 1, 1, 4, 2, 2}, true), new Code128Digit(64, "[NUL]", "`", "64"));
        digc128.put(new BarcodePattern(new int[] {1, 2, 1, 1, 2, 4}, true), new Code128Digit(65, "[SOH]", "a", "65"));
        digc128.put(new BarcodePattern(new int[] {1, 2, 1, 4, 2, 1}, true), new Code128Digit(66, "[STX]", "b", "66"));
        digc128.put(new BarcodePattern(new int[] {1, 4, 1, 1, 2, 2}, true), new Code128Digit(67, "[ETX]", "c", "67"));
        digc128.put(new BarcodePattern(new int[] {1, 4, 1, 2, 2, 1}, true), new Code128Digit(68, "[EOT]", "d", "68"));
        digc128.put(new BarcodePattern(new int[] {1, 1, 2, 2, 1, 4}, true), new Code128Digit(69, "[ENQ]", "e", "69"));
        digc128.put(new BarcodePattern(new int[] {1, 1, 2, 4, 1, 2}, true), new Code128Digit(70, "[ACK]", "f", "70"));
        digc128.put(new BarcodePattern(new int[] {1, 2, 2, 1, 1, 4}, true), new Code128Digit(71, "[BEL]", "g", "71"));
        digc128.put(new BarcodePattern(new int[] {1, 2, 2, 4, 1, 1}, true), new Code128Digit(72, "[BS]", "h", "72"));
        digc128.put(new BarcodePattern(new int[] {1, 4, 2, 1, 1, 2}, true), new Code128Digit(73, "[HT]", "i", "73"));
        digc128.put(new BarcodePattern(new int[] {1, 4, 2, 2, 1, 1}, true), new Code128Digit(74, "[LF]", "j", "74"));
        digc128.put(new BarcodePattern(new int[] {2, 4, 1, 2, 1, 1}, true), new Code128Digit(75, "[VT]", "k", "75"));
        digc128.put(new BarcodePattern(new int[] {2, 2, 1, 1, 1, 4}, true), new Code128Digit(76, "[FF]", "l", "76"));
        digc128.put(new BarcodePattern(new int[] {4, 1, 3, 1, 1, 1}, true), new Code128Digit(77, "[CR]", "m", "77"));
        digc128.put(new BarcodePattern(new int[] {2, 4, 1, 1, 1, 2}, true), new Code128Digit(78, "[SO]", "n", "78"));
        digc128.put(new BarcodePattern(new int[] {1, 3, 4, 1, 1, 1}, true), new Code128Digit(79, "[SI]", "o", "79"));
        digc128.put(new BarcodePattern(new int[] {1, 1, 1, 2, 4, 2}, true), new Code128Digit(80, "[DLE]", "p", "80"));
        digc128.put(new BarcodePattern(new int[] {1, 2, 1, 1, 4, 2}, true), new Code128Digit(81, "[DC1]", "q", "81"));
        digc128.put(new BarcodePattern(new int[] {1, 2, 1, 2, 4, 1}, true), new Code128Digit(82, "[DC2]", "r", "82"));
        digc128.put(new BarcodePattern(new int[] {1, 1, 4, 2, 1, 2}, true), new Code128Digit(83, "[DC3]", "s", "83"));
        digc128.put(new BarcodePattern(new int[] {1, 2, 4, 1, 1, 2}, true), new Code128Digit(84, "[DC4]", "t", "84"));
        digc128.put(new BarcodePattern(new int[] {1, 2, 4, 2, 1, 1}, true), new Code128Digit(85, "[NAK]", "u", "85"));
        digc128.put(new BarcodePattern(new int[] {4, 1, 1, 2, 1, 2}, true), new Code128Digit(86, "[SYN]", "v", "86"));
        digc128.put(new BarcodePattern(new int[] {4, 2, 1, 1, 1, 2}, true), new Code128Digit(87, "[ETB]", "w", "87"));
        digc128.put(new BarcodePattern(new int[] {4, 2, 1, 2, 1, 1}, true), new Code128Digit(88, "[CAN]", "x", "88"));
        digc128.put(new BarcodePattern(new int[] {2, 1, 2, 1, 4, 1}, true), new Code128Digit(89, "[EM]", "y", "89"));
        digc128.put(new BarcodePattern(new int[] {2, 1, 4, 1, 2, 1}, true), new Code128Digit(90, "[SUB]", "z", "90"));
        digc128.put(new BarcodePattern(new int[] {4, 1, 2, 1, 2, 1}, true), new Code128Digit(91, "[ESC]", "{", "91"));
        digc128.put(new BarcodePattern(new int[] {1, 1, 1, 1, 4, 3}, true), new Code128Digit(92, "[FS]", "|", "92"));
        digc128.put(new BarcodePattern(new int[] {1, 1, 1, 3, 4, 1}, true), new Code128Digit(93, "[GS]", "}", "93"));
        digc128.put(new BarcodePattern(new int[] {1, 3, 1, 1, 4, 1}, true), new Code128Digit(94, "[RS]", "~", "94"));
        digc128.put(new BarcodePattern(new int[] {1, 1, 4, 1, 1, 3}, true), new Code128Digit(95, "[US]", "[DEL]", "95"));
        digc128.put(new BarcodePattern(new int[] {1, 1, 4, 3, 1, 1}, true), new Code128Digit(96, "[FNC3]", "[FNC3]", "96"));
        digc128.put(new BarcodePattern(new int[] {4, 1, 1, 1, 1, 3}, true), new Code128Digit(97, "[FNC2]", "[FNC2]", "97"));
        digc128.put(new BarcodePattern(new int[] {4, 1, 1, 3, 1, 1}, true), new Code128Digit(98, "[ShiftB]", "[ShiftA]", "98"));
        digc128.put(new BarcodePattern(new int[] {1, 1, 3, 1, 4, 1}, true), new Code128Digit(99, "[CodeC]", "[CodeC]", "99"));
        digc128.put(new BarcodePattern(new int[] {1, 1, 4, 1, 3, 1}, true), new Code128Digit(100, "[CodeB]", "[FNC4]", "[CodeB]"));
        digc128.put(new BarcodePattern(new int[] {3, 1, 1, 1, 4, 1}, true), new Code128Digit(101, "[FNC4]", "[CodeA]", "[CodeA]"));
        digc128.put(new BarcodePattern(new int[] {4, 1, 1, 1, 3, 1}, true), new Code128Digit(102, "[FNC1]", "[FNC1]", "[FNC1]"));
        digc128.put(new BarcodePattern(new int[] {2, 1, 1, 4, 1, 2}, true), new Code128Digit(103, "[StartA]", "[StartA]", "[StartA]"));
        digc128.put(new BarcodePattern(new int[] {2, 1, 1, 2, 1, 4}, true), new Code128Digit(104, "[StartB]", "[StartA]", "[StartA]"));
        digc128.put(new BarcodePattern(new int[] {2, 1, 1, 2, 3, 2}, true), new Code128Digit(105, "[StartC]", "[StartA]", "[StartA]"));
    }

    private BarcodePattern STOP = new BarcodePattern(new int[] {2, 3, 3, 1, 1, 1, 2}, true);
}
