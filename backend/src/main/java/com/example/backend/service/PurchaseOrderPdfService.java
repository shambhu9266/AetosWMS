package com.example.backend.service;

import com.example.backend.model.PurchaseOrder;
import com.example.backend.model.LineItem;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.html2pdf.HtmlConverter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PurchaseOrderPdfService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public byte[] generatePurchaseOrderPdf(PurchaseOrder po) throws IOException {
        String html = generatePurchaseOrderHtml(po);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        HtmlConverter.convertToPdf(html, outputStream);
        
        return outputStream.toByteArray();
    }

    private String generatePurchaseOrderHtml(PurchaseOrder po) {
        List<LineItem> lineItems = parseLineItems(po.getLineItemsJson());
        
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html><head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<style>");
        html.append(getCssStyles());
        html.append("</style>");
        html.append("</head><body>");
        
        // Header
        html.append("<div class='header'>");
        html.append("<h1 class='title'>PURCHASE ORDER</h1>");
        html.append("</div>");
        
        // Main content
        html.append("<div class='main-content'>");
        
        // Left column - Bill To and Vendor
        html.append("<div class='left-column'>");
        
        // Bill To
        html.append("<div class='section'>");
        html.append("<h3>BILL TO:</h3>");
        html.append("<div class='address'>").append(po.getBillToCompany()).append("</div>");
        html.append("<div class='address'>").append(po.getBillToAddress()).append("</div>");
        html.append("<div><strong>PAN</strong> ").append(po.getBillToPAN()).append("</div>");
        html.append("<div><strong>GSTIN</strong> ").append(po.getBillToGSTIN()).append("</div>");
        html.append("</div>");
        
        // Vendor
        html.append("<div class='section'>");
        html.append("<h3>VENDOR:</h3>");
        html.append("<div class='address'>").append(po.getVendorName()).append("</div>");
        html.append("<div class='address'>").append(po.getVendorAddress()).append("</div>");
        html.append("</div>");
        
        html.append("</div>");
        
        // Right column - PO Info, Date, and Ship To
        html.append("<div class='right-column'>");
        
        // PO Number and Date
        html.append("<div class='section'>");
        html.append("<div><strong>P.O. NO.</strong> ").append(po.getPoNumber()).append("</div>");
        html.append("<div><strong>DATE</strong> ").append(formatDate(po.getPoDate())).append("</div>");
        html.append("</div>");
        
        // Ship To
        html.append("<div class='section'>");
        html.append("<h3>Ship To</h3>");
        html.append("<div class='address'>").append(po.getShipToAddress()).append("</div>");
        html.append("</div>");
        
        // Contact Person
        html.append("<div class='section'>");
        html.append("<div><strong>Contact Person</strong> ").append(po.getVendorContactPerson()).append("</div>");
        html.append("<div><strong>Mobile No.</strong> ").append(po.getVendorMobileNo()).append("</div>");
        html.append("</div>");
        
        html.append("</div>");
        
        html.append("</div>");
        
        // Scope of Order
        html.append("<div class='scope-section'>");
        html.append("<h3>Scope of Order: ").append(po.getScopeOfOrder()).append("</h3>");
        html.append("</div>");
        
        // First table: Shipping details (3 columns)
        html.append("<div class='shipping-table'>");
        html.append("<table class='shipping-info'>");
        html.append("<tr>");
        html.append("<td><strong>SHIPPING METHOD</strong></td>");
        html.append("<td><strong>SHIPPING TERMS</strong></td>");
        html.append("<td><strong>DATE OF COMPLETION</strong></td>");
        html.append("</tr>");
        html.append("<tr>");
        html.append("<td>").append(po.getShippingMethod()).append("</td>");
        html.append("<td>").append(po.getShippingTerms()).append("</td>");
        html.append("<td>").append(po.getDateOfCompletion()).append("</td>");
        html.append("</tr>");
        html.append("</table>");
        html.append("</div>");
        
        // Second table: Line items (5 columns)
        html.append("<div class='line-items-table'>");
        html.append("<table class='line-items-info'>");
        html.append("<tr>");
        html.append("<td><strong>Sr. No.</strong></td>");
        html.append("<td><strong>QTY/UNIT</strong></td>");
        html.append("<td><strong>DESCRIPTION</strong></td>");
        html.append("<td><strong>UNIT PRICE</strong></td>");
        html.append("<td><strong>AMOUNT (Rs.)</strong></td>");
        html.append("</tr>");
        
        // Line items
        for (int i = 0; i < lineItems.size(); i++) {
            LineItem item = lineItems.get(i);
            html.append("<tr>");
            html.append("<td>").append(item.getSrNo()).append("</td>");
            html.append("<td>").append(item.getQuantity()).append(" ").append(item.getUnit()).append("</td>");
            html.append("<td>").append(item.getDescription()).append("</td>");
            html.append("<td>").append(formatCurrency(item.getUnitPrice())).append("</td>");
            html.append("<td>").append(formatCurrency(item.getAmount())).append("</td>");
            html.append("</tr>");
        }
        
        // Freight charges row
        html.append("<tr>");
        html.append("<td></td>");
        html.append("<td></td>");
        html.append("<td>Freight Charges</td>");
        html.append("<td></td>");
        html.append("<td>").append(formatCurrency(po.getFreightCharges())).append("</td>");
        html.append("</tr>");
        
        // GST row
        html.append("<tr>");
        html.append("<td></td>");
        html.append("<td></td>");
        html.append("<td>Add + GST @ ").append(po.getGstRate()).append("%</td>");
        html.append("<td></td>");
        html.append("<td>").append(formatCurrency(po.getGstAmount())).append("</td>");
        html.append("</tr>");
        
        html.append("</table>");
        html.append("</div>");
        
        // Terms & Conditions
        html.append("<div class='terms-section'>");
        html.append("<h3>Terms & Conditions</h3>");
        html.append("<div class='terms-grid'>");
        html.append("<div><strong>Taxes:</strong> Included in above</div>");
        html.append("<div><strong>Freight:</strong> Included in above</div>");
        html.append("<div><strong>Payment Terms:</strong> ").append(po.getPaymentTerms() != null ? po.getPaymentTerms() : "").append("</div>");
        html.append("<div><strong>Contact Person:</strong> ").append(po.getVendorContactPerson()).append("</div>");
        html.append("<div><strong>Warranty:</strong> ").append(po.getWarranty() != null ? po.getWarranty() : "NA").append("</div>");
        html.append("</div>");
        html.append("</div>");
        
        // Total Amount
        html.append("<div class='total-section'>");
        html.append("<div class='total-box'>");
        html.append("<div class='total-label'>Total Amount</div>");
        html.append("<div class='total-value'>").append(formatCurrency(po.getTotalAmount())).append("</div>");
        html.append("</div>");
        html.append("</div>");
        
        // Footer
        html.append("<div class='footer'>");
        html.append("<div class='authorized'>Authorized by</div>");
        html.append("<div class='date'>").append(formatDate(LocalDate.now())).append(" Date</div>");
        html.append("</div>");
        
        html.append("</body></html>");
        
        return html.toString();
    }

    private String getCssStyles() {
        return """
            body {
                font-family: Arial, sans-serif;
                margin: 20px;
                font-size: 12px;
                line-height: 1.4;
            }
            .header {
                text-align: center;
                margin-bottom: 20px;
                border-bottom: 2px solid #000;
                padding-bottom: 8px;
            }
            .title {
                font-size: 24px;
                font-weight: bold;
                margin: 0;
            }
            .main-content {
                display: flex;
                margin-bottom: 15px;
            }
            .left-column, .right-column {
                flex: 1;
                margin-right: 20px;
            }
            .section {
                margin-bottom: 15px;
            }
            .section h3 {
                margin: 0 0 5px 0;
                font-size: 14px;
                font-weight: bold;
            }
            .address {
                margin-bottom: 3px;
            }
            .scope-section {
                margin: 15px 0;
                text-align: center;
            }
            .scope-section h3 {
                font-size: 16px;
                font-weight: bold;
                margin: 0;
            }
            .shipping-table {
                margin: 15px 0;
            }
            .shipping-info {
                width: 100%;
                border-collapse: collapse;
                border: 1px solid #000;
                table-layout: fixed;
            }
            .shipping-info td {
                border: 1px solid #000;
                padding: 8px;
                vertical-align: top;
                width: 33.33%;
                text-align: center;
            }
            .shipping-info tr:first-child td {
                font-weight: bold;
                background-color: #f0f0f0;
            }
            .shipping-info td strong {
                display: block;
                margin-bottom: 5px;
                font-size: 12px;
            }
            .line-items-table {
                margin: 15px 0;
            }
            .line-items-info {
                width: 100%;
                border-collapse: collapse;
                border: 1px solid #000;
            }
            .line-items-info td {
                border: 1px solid #000;
                padding: 8px;
                vertical-align: top;
            }
            .line-items-info tr:first-child td {
                font-weight: bold;
                background-color: #f0f0f0;
            }
            .terms-section {
                margin: 15px 0;
            }
            .terms-section h3 {
                font-size: 14px;
                font-weight: bold;
                margin: 0 0 8px 0;
            }
            .terms-grid {
                display: grid;
                grid-template-columns: 1fr 1fr;
                gap: 8px;
            }
            .total-section {
                text-align: right;
                margin: 15px 0;
            }
            .total-box {
                display: inline-block;
                border: 2px solid #000;
                padding: 10px 20px;
                min-width: 200px;
            }
            .total-label {
                font-weight: bold;
                margin-bottom: 5px;
            }
            .total-value {
                font-size: 16px;
                font-weight: bold;
            }
            .footer {
                display: flex;
                justify-content: space-between;
                margin-top: 40px;
                padding-top: 20px;
                border-top: 1px solid #000;
            }
            .authorized, .date {
                font-weight: bold;
            }
            """;
    }

    private List<LineItem> parseLineItems(String lineItemsJson) {
        try {
            if (lineItemsJson == null || lineItemsJson.trim().isEmpty()) {
                return List.of();
            }
            return objectMapper.readValue(lineItemsJson, new TypeReference<List<LineItem>>() {});
        } catch (Exception e) {
            System.err.println("Error parsing line items: " + e.getMessage());
            return List.of();
        }
    }

    private String formatDate(LocalDate date) {
        if (date == null) return "";
        return date.format(DateTimeFormatter.ofPattern("dd-MMM-yy"));
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0.00";
        return String.format("%,.2f", amount);
    }
}
