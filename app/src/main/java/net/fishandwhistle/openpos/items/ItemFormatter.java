package net.fishandwhistle.openpos.items;

import net.fishandwhistle.openpos.barcode.BarcodeSpec;
import net.fishandwhistle.openpos.barcode.Checksums;
import net.fishandwhistle.openpos.barcode.GS1Parser;

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
        if(b.extra != null) {
            item.putValue("supplement", b.extra);
        }

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
                    item.putValue("raw_code", bstr);
                } catch(GS1Parser.GS1Exception e) {
                    //strip [FNC1], [FNC2], [FNC3], [FNC4]
                    item.putValue("raw_code", bstr);
                    item.productCode = bstr.replace("[FNC1]", "")
                            .replace("[FNC2]", "")
                            .replace("[FNC3]", "")
                            .replace("[FNC4]", "");
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
            if(item.barcodeType.equals("EAN-13")) {
                addPriceInfo(item, item.getValue("supplement"));
            }
        } else if(ean13.startsWith("979")) {
            if(ean13.startsWith("9790")) {
                item.putValue("imsn", ean13);
            } else {
                item.putValue("isbn13", ean13);
            }
            if(item.barcodeType.equals("EAN-13")) {
                addPriceInfo(item, item.getValue("supplement"));
            }
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
            if(item.barcodeType.equals("EAN-13")) {
                addIssueNumber(item, item.getValue("supplement"));
            }
        }
    }

    private void addPriceInfo(ScannedItem item, String extra) {
        if(extra != null) {
            if(extra.length() == 5 && !extra.startsWith("9")) {
                item.putValue("retail_price", String.valueOf(Double.valueOf(extra.substring(1, 5))/100.0));
                switch(extra.substring(0, 1)) {
                    case "0":
                         item.putValue("price_curency", "GBP");
                         break;
                    case "1":
                        item.putValue("price_curency", "GBP");
                        break;
                    case "3":
                        item.putValue("price_curency", "AUD");
                        break;
                    case "4":
                        item.putValue("price_curency", "NZD");
                        break;
                    case "5":
                        item.putValue("price_curency", "USD");
                        break;
                    case "6":
                        item.putValue("price_curency", "CND");
                        break;
                }
            }
        }
    }

    private void addIssueNumber(ScannedItem item, String extra) {
        if(extra != null && extra.length() == 2) {
            item.putValue("issue_code", extra);
            item.productCode += "-" + extra;
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
