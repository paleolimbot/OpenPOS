package net.fishandwhistle.openpos;

import net.fishandwhistle.openpos.barcode.BarcodeSpec;
import net.fishandwhistle.openpos.barcode.Checksums;
import net.fishandwhistle.openpos.barcode.GS1Parser;
import net.fishandwhistle.openpos.items.ScannedItem;

import java.util.List;

/**
 * Created by dewey on 2016-10-17.
 */

public class ItemFormatter {

    public ItemFormatter() {

    }

    public ScannedItem format(BarcodeSpec.Barcode b) {
        String bstr = b.toString();
        ScannedItem item = new ScannedItem(b.type, bstr);

        switch (b.type) {
            case "ITF-14":
                //add GTIN metadata
                item.putValue("gtin14", bstr);
                String ean13 = GTINtoEAN(bstr);
                item.putValue("gtin13", ean13);
                addISBNData(item, ean13);
                break;
            case "EAN-13":
                item.putValue("gtin13", bstr);
                addISBNData(item, bstr);
                break;
            case "EAN-8":
                addISBNData(item, bstr);
                item.putValue("gtin8", bstr);
                break;
            case "UPC-E":
                if(b.tag != null) {
                    item.putValue("gtin13", b.tag);
                }
                break;
            case "Code128":
                try {
                    GS1Parser parser = new GS1Parser(b);
                    item = parser.parse();
                    List<String> keys = item.getKeys();
                    if(keys.contains("gtin14")) {
                        String eanVal = GTINtoEAN(item.getValue("gtin14"));
                        item.putValue("gtin13", eanVal);
                        addISBNData(item, eanVal);
                    }
                } catch(GS1Parser.GS1Exception e) {
                    //strip [FNC1], [FNC2], [FNC3], [FNC4]?
                }
                break;
        }

        return item;
    }

    private void addISBNData(ScannedItem item, String ean13) {
        if(ean13.startsWith("978")) {
            int[] numbers = new int[10];
            for (int i = 3; i <= 11; i++) {
                numbers[i - 3] = Integer.valueOf(ean13.substring(i, i + 1));
            }
            int check = Checksums.ISBNISSNDigit(numbers);
            String checkDigit;
            if (check == 10) {
                checkDigit = "X";
            } else {
                checkDigit = String.valueOf(check);
            }
            item.putValue("isbn10", ean13.substring(3, 12) + checkDigit);
            item.putValue("isbn13", ean13);
            if(!item.barcodeType.equals("ISBN-13")) {
                item.putValue("isbn13", ean13);
            }
        } else if(ean13.startsWith("979") && !item.barcodeType.equals("ISBN-13")) {
            item.putValue("isbn13", ean13);
        } else if (ean13.startsWith("977")) {
            //ISSN
            int[] numbers = new int[8];
            for (int i = 3; i <= 10; i++) {
                numbers[i - 3] = Integer.valueOf(ean13.substring(i, i + 1));
            }
            int check = Checksums.ISBNISSNDigit(numbers);
            String checkDigit;
            if (check == 10) {
                checkDigit = "X";
            } else {
                checkDigit = String.valueOf(check);
            }
            item.putValue("issn", ean13.substring(3, 10) + checkDigit);
            item.putValue("issn_variant", ean13.substring(10, 12));
        }
    }

    private String GTINtoEAN(String gtin) {
        String ean = gtin.substring(1, 13);
        int[] numbers = new int[ean.length()+1];
        for(int i=0; i<ean.length(); i++) {
            numbers[i] = Integer.valueOf(ean.substring(i, i+1));
        }
        int checkDigit = Checksums.checksumDigit(numbers, 3, 1);
        return ean + String.valueOf(checkDigit);
    }

}
