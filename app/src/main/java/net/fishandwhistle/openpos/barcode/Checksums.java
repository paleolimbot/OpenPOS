package net.fishandwhistle.openpos.barcode;

/**
 * Created by dewey on 2016-10-11.
 */

public class Checksums {

    public static boolean checksum(BarcodeSpec.Barcode b, int evenweight, int oddweight) {
        int[] numbers = new int[b.digits.size()];
        for(int i=0; i<numbers.length; i++) {
            numbers[i] = Integer.valueOf(b.digits.get(i).digit);
        }
        int checksum = checksumDigit(numbers, evenweight, oddweight);
        return checksum == numbers[numbers.length-1];
    }

    public static int checksumDigit(int[] numbers, int evenweight, int oddweight) {
        int oddsum = 0;
        for(int i=0; i<numbers.length-1; i+=2) {
            oddsum += numbers[i];
        }
        int evensum = 0;
        for(int i=1; i<numbers.length-1; i+=2) {
            evensum += numbers[i];
        }
        int s1 = evensum * evenweight + oddsum * oddweight;
        int checksum = 10*(s1/10+1) - s1;
        if(checksum == 10) checksum = 0;
        return checksum;
    }

}
