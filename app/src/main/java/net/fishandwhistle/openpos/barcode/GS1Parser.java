package net.fishandwhistle.openpos.barcode;

import android.util.Log;

import net.fishandwhistle.openpos.items.ScannedItem;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by dewey on 2016-10-15.
 */

public class GS1Parser {

    private static final String TAG = "GS1Parser";

    private BarcodeSpec.Barcode b;

    public GS1Parser(BarcodeSpec.Barcode barcode) {
        this.b = barcode;
    }

    public ScannedItem parse() throws GS1Exception {
        if(!b.digits.get(0).digit.equals("[FNC1]")) throw new GS1Exception("No [FNC1] at start of code");

        List<String> strings = new ArrayList<>(); //one string per [FNC1]
        int i = 1;
        while(i<b.digits.size()) {
            String str = "";
            for(int j=i; j<b.digits.size(); j++) {
                String d = b.digits.get(j).digit;
                if(d.equals("[FNC1]")) {
                    strings.add(str);
                    i = j+1;
                    break;
                } else if(j == (b.digits.size()-1)) {
                    str += d;
                    strings.add(str);
                    i = j+1;
                } else {
                    str += d;
                }
            }
        }

        ScannedItem item = new ScannedItem("GS1-128", "");

        for(String s: strings) {
            AI ai;
            int starti = 0;
            while(starti < s.length()) {
                ai = extractAI(s.substring(starti));
                item.putValue(ai.shortName, ai.dataVal);
                item.productCode += ai.toString();

                starti += ai.aiCode.length() + ai.data.length();
            }
        }

        item.jsonSource = "GS1 Parser";
        item.jsonTime = System.currentTimeMillis();

        return item;
    }

    private static AI extractAI(String s) throws GS1Exception {
        AI ai = null;
        String sub = "";
        for(int aiLen=2; aiLen <= 4; aiLen++) {
            sub = s.substring(0, aiLen);
            if(gs1.containsKey(sub)) {
                ai = gs1.get(sub).copy();
                break;
            }
        }
        if(ai == null) throw new GS1Exception("AI Identifier not found in text: " + sub);
        ai.aiCode = s.substring(0, ai.aiLength);
        if(ai.aiStart.length() != ai.aiCode.length()) {
            ai.extraN = Integer.valueOf(ai.aiCode.substring(ai.aiLength-1, ai.aiLength));
        }
        ai.data = s.substring(ai.aiLength, Math.min(s.length(), ai.aiLength+ai.maxDataLen));
        if(ai.data.length() < ai.minDataLen) throw new GS1Exception("Data is too short for AI");

        if(ai.divValue) {
            try {
                ai.dataVal = String.valueOf(Double.valueOf(ai.data) / Math.pow(10, ai.extraN));
            } catch(NumberFormatException e) {
                throw new GS1Exception("Number parse error: " + ai.data);
            }
        } else {
            ai.dataVal = ai.data;
        }

        return ai;
    }

    public static class GS1Exception extends Exception {

        public GS1Exception(String message) {
            super(message);
        }
    }

    private static class AI {

        private String longCode;
        private String description;
        private int aiLength;
        private String aiStart;
        private int minDataLen;
        private int maxDataLen;
        private boolean divValue;
        private String shortName;

        public String aiCode;
        public String data;
        public String dataVal;
        public int extraN;


        public AI(String longCode, String description, int aiLength, String aiStart, int minDataLen,
                  int maxDataLen, boolean divValue, String shortName) {
            this.longCode = longCode;
            this.description = description;
            this.aiLength = aiLength;
            this.aiStart = aiStart;
            this.minDataLen = minDataLen;
            this.maxDataLen = maxDataLen;
            this.divValue = divValue;
            this.shortName = shortName;
        }

        public AI copy() {
            return new AI(longCode, description, aiLength, aiStart, minDataLen,
                maxDataLen, divValue, shortName);
        }

        public String toString() {
            return String.format("(%s)%s", aiCode, data);
        }
    }

    public static void initialize() {
        gs1.containsKey("00");
    }

    private static Map<String, AI> gs1 = new HashMap<>();
    static {
        gs1.put("00", new AI("00", "Serial Shipping Container Code (SSCC)", 2, "00", 18, 18, false, "SCCC"));
        gs1.put("01", new AI("01", "Global Trade Item Number (GTIN)", 2, "01", 14, 14, false, "GTIN"));
        gs1.put("02", new AI("02", "GTIN of Contained Trade Items", 2, "02", 14, 14, false, "GTIN_CTI"));
        gs1.put("10", new AI("10", "Batch/Lot Number", 2, "10", 0, 20, false, "batch_num"));
        gs1.put("11", new AI("11", "Production Date", 2, "11", 6, 6, false, "production_date"));
        gs1.put("12", new AI("12", "Due Date", 2, "12", 6, 6, false, "due_date"));
        gs1.put("13", new AI("13", "Packaging Date", 2, "13", 6, 6, false, "packaging_date"));
        gs1.put("15", new AI("15", "Best Before Date (YYMMDD)", 2, "15", 6, 6, false, "best_before_date"));
        gs1.put("17", new AI("17", "Expiration Date", 2, "17", 6, 6, false, "expiration_date"));
        gs1.put("20", new AI("20", "Product Variant", 2, "20", 2, 2, false, "prod_variant"));
        gs1.put("21", new AI("21", "Serial Number", 2, "21", 0, 20, false, "serial_num"));
        gs1.put("22", new AI("22", "Secondary Data Fields", 2, "22", 0, 29, false, "secondary_data"));
        gs1.put("23", new AI("23n", "Lot number n", 3, "23", 0, 19, false, "lot_num"));
        gs1.put("240", new AI("240", "Additional Product Identification", 3, "240", 0, 30, false, "additional_prod_id"));
        gs1.put("241", new AI("241", "Customer Part Number", 3, "241", 0, 30, false, "cust_part_num"));
        gs1.put("242", new AI("242", "Made-to-Order Variation Number", 3, "242", 0, 6, false, "made_to_order_variation_num"));
        gs1.put("250", new AI("250", "Secondary Serial Number", 3, "250", 0, 30, false, "serial_num_2"));
        gs1.put("251", new AI("251", "Reference to Source Entity", 3, "251", 0, 30, false, "ref_source_entity"));
        gs1.put("253", new AI("253", "Global Document Type Identifier", 3, "253", 13, 17, false, "global_doc_type_id"));
        gs1.put("254", new AI("254", "GLN Extension Component", 3, "254", 0, 20, false, "GLN_ext_component"));
        gs1.put("255", new AI("255", "Global Coupon Number (GCN)", 3, "255", 13, 25, false, "global_coupon_num"));
        gs1.put("30", new AI("30", "Count of items", 2, "30", 0, 8, false, "item_count"));
        gs1.put("310", new AI("310y", "Product Net Weight in kg", 4, "310", 6, 6, true, "prod_net_wt_kg"));
        gs1.put("311", new AI("311y", "Product Length/1st Dimension, in meters", 4, "311", 6, 6, true, "prod_length_x_m"));
        gs1.put("312", new AI("312y", "Product Width/Diameter/2nd Dimension, in meters", 4, "312", 6, 6, true, "prod_length_y_m"));
        gs1.put("313", new AI("313y", "Product Depth/Thickness/Height/3rd Dimension, in meters", 4, "313", 6, 6, true, "prod_length_z_m"));
        gs1.put("314", new AI("314y", "Product Area, in square meters", 4, "314", 6, 6, true, "prod_area_m2"));
        gs1.put("315", new AI("315y", "Product Net Volume, in liters", 4, "315", 6, 6, true, "prod_net_volume_L"));
        gs1.put("316", new AI("316y", "Product Net Volume, in cubic meters", 4, "316", 6, 6, true, "prod_net_volume_m3"));
        gs1.put("320", new AI("320y", "Product Net Weight, in pounds", 4, "320", 6, 6, true, "prod_net_wt_lbs"));
        gs1.put("321", new AI("321y", "Product Length/1st Dimension, in inches", 4, "321", 6, 6, true, "prod_length_x_in"));
        gs1.put("322", new AI("322y", "Product Length/1st Dimension, in feet", 4, "322", 6, 6, true, "prod_length_x_ft"));
        gs1.put("323", new AI("323y", "Product Length/1st Dimension, in yards", 4, "323", 6, 6, true, "prod_length_x_yd"));
        gs1.put("324", new AI("324y", "Product Width/Diameter/2nd Dimension, in inches", 4, "324", 6, 6, true, "prod_length_y_in"));
        gs1.put("325", new AI("325y", "Product Width/Diameter/2nd Dimension, in feet", 4, "325", 6, 6, true, "prod_length_y_ft"));
        gs1.put("326", new AI("326y", "Product Width/Diameter/2nd Dimension, in yards", 4, "326", 6, 6, true, "prod_length_y_yd"));
        gs1.put("327", new AI("327y", "Product Depth/Thickness/Height/3rd Dimension, in inches", 4, "327", 6, 6, true, "prod_length_z_in"));
        gs1.put("328", new AI("328y", "Product Depth/Thickness/Height/3rd Dimension, in feet", 4, "328", 6, 6, true, "prod_length_z_ft"));
        gs1.put("329", new AI("329y", "Product Depth/Thickness/3rd Dimension, in yards", 4, "329", 6, 6, true, "prod_length_z_yd"));
        gs1.put("330", new AI("330y", "Container Gross Weight (kg)", 4, "330", 6, 6, true, "container_gross_wt_kg"));
        gs1.put("331", new AI("331y", "Container Length/1st Dimension (Meters)", 4, "331", 6, 6, true, "container_length_x_m"));
        gs1.put("332", new AI("332y", "Container Width/Diameter/2nd Dimension (Meters)", 4, "332", 6, 6, true, "container_length_y_m"));
        gs1.put("333", new AI("333y", "Container Depth/Thickness/3rd Dimension (Meters)", 4, "333", 6, 6, true, "container_length_z_m"));
        gs1.put("334", new AI("334y", "Container Area (Square Meters)", 4, "334", 6, 6, true, "container_area_m2"));
        gs1.put("335", new AI("335y", "Container Gross Volume (Liters)", 4, "335", 6, 6, true, "container_gross_volume_L"));
        gs1.put("336", new AI("336y", "Container Gross Volume (Cubic Meters)", 4, "336", 6, 6, true, "container_gross_volume_m3"));
        gs1.put("340", new AI("340y", "Container Gross Weight (Pounds)", 4, "340", 6, 6, true, "container_gross_wt_lbs"));
        gs1.put("341", new AI("341y", "Container Length/1st Dimension, in inches", 4, "341", 6, 6, true, "container_length_x_in"));
        gs1.put("342", new AI("342y", "Container Length/1st Dimension, in feet", 4, "342", 6, 6, true, "container_length_x_ft"));
        gs1.put("343", new AI("343y", "Container Length/1st Dimension in, in yards", 4, "343", 6, 6, true, "container_length_x_yd"));
        gs1.put("344", new AI("344y", "Container Width/Diameter/2nd Dimension, in inches", 4, "344", 6, 6, true, "container_length_y_in"));
        gs1.put("345", new AI("345y", "Container Width/Diameter/2nd Dimension, in feet", 4, "345", 6, 6, true, "container_length_y_ft"));
        gs1.put("346", new AI("346y", "Container Width/Diameter/2nd Dimension, in yards", 4, "346", 6, 6, true, "container_length_y_yd"));
        gs1.put("347", new AI("347y", "Container Depth/Thickness/Height/3rd Dimension, in inches", 4, "347", 6, 6, true, "container_length_z_in"));
        gs1.put("348", new AI("348y", "Container Depth/Thickness/Height/3rd Dimension, in feet", 4, "348", 6, 6, true, "container_length_z_ft"));
        gs1.put("349", new AI("349y", "Container Depth/Thickness/Height/3rd Dimension, in yards", 4, "349", 6, 6, true, "container_length_z_yd"));
        gs1.put("350", new AI("350y", "Product Area (Square Inches)", 4, "350", 6, 6, true, "prod_area_in2"));
        gs1.put("351", new AI("351y", "Product Area (Square Feet)", 4, "351", 6, 6, true, "prod_area_ft2"));
        gs1.put("352", new AI("352y", "Product Area (Square Yards)", 4, "352", 6, 6, true, "prod_area_yd2"));
        gs1.put("353", new AI("353y", "Container Area (Square Inches)", 4, "353", 6, 6, true, "container_area_in2"));
        gs1.put("354", new AI("354y", "Container Area (Square Feet)", 4, "354", 6, 6, true, "container_area_ft2"));
        gs1.put("355", new AI("355y", "Container Area (Square Yards)", 4, "355", 6, 6, true, "container_area_yd2"));
        gs1.put("356", new AI("356y", "Net Weight (Troy Ounces)", 4, "356", 6, 6, true, "net_wt_troy_oz"));
        gs1.put("357", new AI("357y", "Net Weight/Volume (Ounces)", 4, "357", 6, 6, true, "net_wt_vol_oz"));
        gs1.put("360", new AI("360y", "Product Volume (Quarts)", 4, "360", 6, 6, true, "prod_volume_qt"));
        gs1.put("361", new AI("361y", "Product Volume (Gallons)", 4, "361", 6, 6, true, "prod_volume_gal"));
        gs1.put("362", new AI("362y", "Container Gross Volume (Quarts)", 4, "362", 6, 6, true, "container_gross_volume_qt"));
        gs1.put("363", new AI("363y", "Container Gross Volume (U.S. Gallons)", 4, "363", 6, 6, true, "container_gross_volume_gal"));
        gs1.put("364", new AI("364y", "Product Volume (Cubic Inches)", 4, "364", 6, 6, true, "prod_volume_in3"));
        gs1.put("365", new AI("365y", "Product Volume (Cubic Feet)", 4, "365", 6, 6, true, "prod_volume_ft3"));
        gs1.put("366", new AI("366y", "Product Volume (Cubic Yards)", 4, "366", 6, 6, true, "prod_volume_yd3"));
        gs1.put("367", new AI("367y", "Container Gross Volume (Cubic Inches)", 4, "367", 6, 6, true, "container_gross_volume_in3"));
        gs1.put("368", new AI("368y", "Container Gross Volume (Cubic Feet)", 4, "368", 6, 6, true, "container_gross_volume_ft3"));
        gs1.put("369", new AI("369y", "Container Gross Volume (Cubic Yards)", 4, "369", 6, 6, true, "container_gross_volume_yd3"));
        gs1.put("37", new AI("37", "Number of Units Contained", 2, "37", 0, 8, false, "unit_count"));
        gs1.put("390", new AI("390y", "Amount payable (local currency)", 4, "390", 0, 15, true, "amount_payable_local"));
        gs1.put("391", new AI("391y", "Amount payable (with ISO currency code)", 4, "391", 3, 18, true, "amount_payable_curcode"));
        gs1.put("392", new AI("392y", "Amount payable per single item (local currency)", 4, "392", 0, 15, true, "amount_payable_single_local"));
        gs1.put("393", new AI("393y", "Amount payable per single item (with ISO currency code)", 4, "393", 3, 18, true, "amount_payable_curcode"));
        gs1.put("400", new AI("400", "Customer Purchase Order Number", 3, "400", 0, 30, false, "customer_po_num"));
        gs1.put("401", new AI("401", "Consignment Number", 3, "401", 0, 30, false, "consignment_num"));
        gs1.put("402", new AI("402", "Bill of Lading number", 3, "402", 17, 17, false, "bill_of_landing_num"));
        gs1.put("403", new AI("403", "Routing code", 3, "403", 0, 30, false, "routing_code"));
        gs1.put("410", new AI("410", "Ship To/Deliver To Location Code (Global Location Number)", 3, "410", 13, 13, false, "ship_to_code_global"));
        gs1.put("411", new AI("411", "Bill To/Invoice Location Code (Global Location Number)", 3, "411", 13, 13, false, "bill_to_code_global"));
        gs1.put("412", new AI("412", "Purchase From Location Code (Global Location Number)", 3, "412", 13, 13, false, "purchase_from_code_global"));
        gs1.put("413", new AI("413", "Ship for, Deliver for, or Forward to Location Code (Global Location Number)", 3, "413", 13, 13, false, "ship_for_code_global"));
        gs1.put("414", new AI("414", "Identification of a physical location (Global Location Number)", 3, "414", 13, 13, false, "physical_location_global"));
        gs1.put("420", new AI("420", "Ship To/Deliver To Postal Code (Single Postal Authority)", 3, "420", 0, 20, false, "ship_to_postal"));
        gs1.put("421", new AI("421", "Ship To/Deliver To Postal Code (with ISO country code)", 3, "421", 3, 15, false, "ship_to_postal_countrycode"));
        gs1.put("422", new AI("422", "Country of Origin (ISO country code)", 3, "422", 3, 3, false, "country_origin_iso"));
        gs1.put("423", new AI("423", "Country or countries of initial processing", 3, "423", 3, 15, false, "country_initial_processing"));
        gs1.put("424", new AI("424", "Country of processing", 3, "424", 3, 3, false, "country_processing"));
        gs1.put("425", new AI("425", "Country of disassembly", 3, "425", 3, 3, false, "country_disassembly"));
        gs1.put("426", new AI("426", "Country of full process chain", 3, "426", 3, 3, false, "country_full_process"));
        gs1.put("7001", new AI("7001", "NATO Stock Number (NSN)", 4, "7001", 13, 13, false, "stock_num_NATO"));
        gs1.put("7002", new AI("7002", "UN/ECE Meat Carcasses and cuts classification", 4, "7002", 0, 30, false, "meat_cuts_classification"));
        gs1.put("7003", new AI("7003", "expiration date and time", 4, "7003", 10, 10, false, "expiration_datetime"));
        gs1.put("7004", new AI("7004", "Active Potency", 4, "7004", 0, 4, false, "active_potency"));
        gs1.put("703", new AI("703n", "Processor approval (with ISO country code); n indicates sequence number of several processors", 4, "703", 3, 30, false, "processor_approval_countrycode"));
        gs1.put("8001", new AI("8001", "Roll Products: Width/Length/Core Diameter/Direction/Splices", 4, "8001", 14, 14, false, "roll_info"));
        gs1.put("8002", new AI("8002", "Mobile phone identifier", 4, "8002", 0, 20, false, "mobile_phone_id"));
        gs1.put("8003", new AI("8003", "Global Returnable Asset Identifier", 4, "8003", 14, 30, false, "returnable_asset_id"));
        gs1.put("8004", new AI("8004", "Global Individual Asset Identifier", 4, "8004", 0, 30, false, "individual_asset_id"));
        gs1.put("8005", new AI("8005", "Price per Unit of Measure", 4, "8005", 6, 6, false, "amount_payable_permeasure"));
        gs1.put("8006", new AI("8006", "identification of the components of an item", 4, "8006", 18, 18, false, "component_items_id"));
        gs1.put("8007", new AI("8007", "International Bank Account Number", 4, "8007", 0, 30, false, "bank_account_num"));
        gs1.put("8008", new AI("8008", "Date/time of production", 4, "8008", 8, 12, false, "production_datetime"));
        gs1.put("8018", new AI("8018", "Global Service Relationship Number", 4, "8018", 18, 18, false, "service_relationship_num"));
        gs1.put("8020", new AI("8020", "Payment slip reference number", 4, "8020", 0, 25, false, "receipt_ref_num"));
        gs1.put("8100", new AI("8100", "Coupon Extended Code: Number System and Offer", 4, "8100", 6, 6, false, "cec_numsys_offer"));
        gs1.put("8101", new AI("8101", "Coupon Extended Code: Number System, Offer, End of Offer", 4, "8101", 10, 10, false, "cec_numsys_offer_endoffer"));
        gs1.put("8102", new AI("8102", "Coupon Extended Code: Number System preceded by 0", 4, "8102", 2, 2, false, "cec_0numsys"));
        gs1.put("8110", new AI("8110", "Coupon code ID (North America)", 4, "8110", 0, 30, false, "coupon_code_id_NA"));
        gs1.put("8200", new AI("8200", "Extended Packaging URL", 4, "8200", 0, 70, false, "extended_packaging_url"));
        gs1.put("90", new AI("90", "Mutually Agreed Between Trading Partners", 2, "90", 0, 30, false, "mutually_agreed_code"));
        gs1.put("91", new AI("91", "Internal Company Codes", 2, "91", 0, 30, false, "internal_1"));
        gs1.put("92", new AI("92", "Internal Company Codes", 2, "92", 0, 100, false, "internal_2"));
        gs1.put("93", new AI("93", "Internal Company Codes", 2, "93", 0, 100, false, "internal_3"));
        gs1.put("94", new AI("94", "Internal Company Codes", 2, "94", 0, 100, false, "internal_4"));
        gs1.put("95", new AI("95", "Internal Company Codes", 2, "95", 0, 100, false, "internal_5"));
        gs1.put("96", new AI("96", "Internal Company Codes", 2, "96", 0, 100, false, "internal_6"));
        gs1.put("97", new AI("97", "Internal Company Codes", 2, "97", 0, 100, false, "internal_7"));
        gs1.put("98", new AI("98", "Internal Company Codes", 2, "98", 0, 100, false, "internal_8"));
        gs1.put("99", new AI("99", "Internal Company Codes", 2, "99", 0, 100, false, "internal_9"));
    }


}
