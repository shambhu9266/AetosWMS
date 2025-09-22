package com.example.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import com.example.backend.model.PurchaseOrder;
import com.example.backend.model.Requisition;

@Service
public class EmailService {
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Autowired
    private PurchaseOrderPdfService pdfService;
    
    public boolean sendPurchaseOrderEmail(String toEmail, String vendorName, 
                                        PurchaseOrder po, String sessionId) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            // Set email details
            helper.setFrom("noreply@mjwarehousing.com");
            helper.setTo(toEmail);
            helper.setSubject("Purchase Order - " + po.getPoNumber());
            
            // Create email body
            String emailBody = createEmailBody(vendorName, po);
            helper.setText(emailBody, true); // true indicates HTML content
            
            // Generate and attach PDF
            byte[] pdfBytes = pdfService.generatePurchaseOrderPdf(po);
            ByteArrayResource pdfResource = new ByteArrayResource(pdfBytes);
            helper.addAttachment("Purchase_Order_" + po.getPoNumber() + ".pdf", pdfResource);
            
            // Send email
            mailSender.send(message);
            System.out.println("Email sent successfully to: " + toEmail);
            return true;
            
        } catch (MessagingException | IOException e) {
            System.err.println("Error sending email: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private String createEmailBody(String vendorName, PurchaseOrder po) {
        StringBuilder body = new StringBuilder();
        body.append("<html><body>");
        body.append("<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>");
        
        // Header
        body.append("<div style='background-color: #17331F; color: white; padding: 20px; text-align: center;'>");
        body.append("<h1 style='margin: 0;'>Purchase Order</h1>");
        body.append("<p style='margin: 5px 0 0 0;'>M J Warehousing Pvt. Ltd.</p>");
        body.append("</div>");
        
        // Content
        body.append("<div style='padding: 20px; background-color: #f9f9f9;'>");
        body.append("<p>Dear ").append(vendorName).append(",</p>");
        body.append("<p>We are pleased to place the following purchase order with your company:</p>");
        
        // PO Details
        body.append("<div style='background-color: white; padding: 15px; border-left: 4px solid #17331F; margin: 15px 0;'>");
        body.append("<h3 style='color: #17331F; margin-top: 0;'>Order Details</h3>");
        body.append("<p><strong>PO Number:</strong> ").append(po.getPoNumber()).append("</p>");
        body.append("<p><strong>Order Date:</strong> ").append(po.getPoDate()).append("</p>");
        body.append("<p><strong>Scope of Order:</strong> ").append(po.getScopeOfOrder()).append("</p>");
        body.append("<p><strong>Total Amount:</strong> ₹").append(po.getTotalAmount()).append("</p>");
        body.append("</div>");
        
        // Instructions
        body.append("<div style='background-color: #e8f5e8; padding: 15px; border-radius: 5px; margin: 15px 0;'>");
        body.append("<h4 style='color: #17331F; margin-top: 0;'>Next Steps:</h4>");
        body.append("<ul>");
        body.append("<li>Please review the attached Purchase Order PDF</li>");
        body.append("<li>Confirm acceptance by replying to this email</li>");
        body.append("<li>Provide delivery timeline as per the order requirements</li>");
        body.append("<li>Contact us if you have any questions</li>");
        body.append("</ul>");
        body.append("</div>");
        
        // Contact Information
        body.append("<div style='background-color: white; padding: 15px; border: 1px solid #ddd; margin: 15px 0;'>");
        body.append("<h4 style='color: #17331F; margin-top: 0;'>Contact Information</h4>");
        body.append("<p><strong>Company:</strong> M J Warehousing Pvt. Ltd.</p>");
        body.append("<p><strong>Address:</strong> Harichand Melaram Complex, Village Mandoli, Delhi - 110093</p>");
        body.append("</div>");
        
        // Footer
        body.append("<div style='text-align: center; padding: 20px; color: #666; font-size: 12px;'>");
        body.append("<p>Thank you for your business!</p>");
        body.append("<p>This is an automated message. Please do not reply to this email.</p>");
        body.append("</div>");
        
        body.append("</div>");
        body.append("</div>");
        body.append("</body></html>");
        
        return body.toString();
    }
    
    public boolean sendRequisitionEmail(String toEmail, String vendorName, 
                                      Requisition requisition, String sessionId) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            // Set email details
            helper.setFrom("noreply@mjwarehousing.com");
            helper.setTo(toEmail);
            helper.setSubject("Product Requirement Inquiry - " + requisition.getItemName());
            
            // Create email body
            String emailBody = createRequisitionEmailBody(vendorName, requisition);
            helper.setText(emailBody, true); // true indicates HTML content
            
            // Send email
            mailSender.send(message);
            System.out.println("Requisition email sent successfully to: " + toEmail);
            return true;
            
        } catch (MessagingException e) {
            System.err.println("Error sending requisition email: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private String createRequisitionEmailBody(String vendorName, Requisition requisition) {
        StringBuilder body = new StringBuilder();
        body.append("<html><body>");
        body.append("<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>");
        
        // Header
        body.append("<div style='background-color: #17331F; color: white; padding: 20px; text-align: center;'>");
        body.append("<h1 style='margin: 0;'>Product Requirement Inquiry</h1>");
        body.append("<p style='margin: 5px 0 0 0;'>M J Warehousing Pvt. Ltd.</p>");
        body.append("</div>");
        
        // Content
        body.append("<div style='padding: 20px; background-color: #f9f9f9;'>");
        body.append("<p>Dear ").append(vendorName).append(",</p>");
        body.append("<p>We are interested in procuring the following product and would like to request a quotation:</p>");
        
        // Requisition Details
        body.append("<div style='background-color: white; padding: 15px; border-left: 4px solid #17331F; margin: 15px 0;'>");
        body.append("<h3 style='color: #17331F; margin-top: 0;'>Product Details</h3>");
        body.append("<p><strong>Product Name:</strong> ").append(requisition.getItemName()).append("</p>");
        body.append("<p><strong>Quantity Required:</strong> ").append(requisition.getQuantity()).append(" units</p>");
        body.append("<p><strong>Estimated Budget:</strong> ₹").append(requisition.getPrice()).append(" per unit</p>");
        body.append("<p><strong>Total Estimated Value:</strong> ₹").append(requisition.getPrice().multiply(java.math.BigDecimal.valueOf(requisition.getQuantity()))).append("</p>");
        body.append("<p><strong>Department:</strong> ").append(requisition.getDepartment()).append("</p>");
        body.append("<p><strong>Requested By:</strong> ").append(requisition.getCreatedBy()).append("</p>");
        body.append("</div>");
        
        // Instructions
        body.append("<div style='background-color: #e8f5e8; padding: 15px; border-radius: 5px; margin: 15px 0;'>");
        body.append("<h4 style='color: #17331F; margin-top: 0;'>Please Provide:</h4>");
        body.append("<ul>");
        body.append("<li>Detailed product specifications</li>");
        body.append("<li>Unit price and total cost for the required quantity</li>");
        body.append("<li>Delivery timeline</li>");
        body.append("<li>Warranty terms and conditions</li>");
        body.append("<li>Any additional charges (shipping, taxes, etc.)</li>");
        body.append("</ul>");
        body.append("</div>");
        
        // Contact Information
        body.append("<div style='background-color: white; padding: 15px; border: 1px solid #ddd; margin: 15px 0;'>");
        body.append("<h4 style='color: #17331F; margin-top: 0;'>Contact Information</h4>");
        body.append("<p><strong>Company:</strong> M J Warehousing Pvt. Ltd.</p>");
        body.append("<p><strong>Address:</strong> Harichand Melaram Complex, Village Mandoli, Delhi - 110093</p>");
        body.append("<p><strong>Contact Person:</strong> IT Manager</p>");
        body.append("</div>");
        
        // Footer
        body.append("<div style='text-align: center; padding: 20px; color: #666; font-size: 12px;'>");
        body.append("<p>We look forward to your prompt response and competitive quotation.</p>");
        body.append("<p>This is an automated message. Please reply with your quotation details.</p>");
        body.append("</div>");
        
        body.append("</div>");
        body.append("</div>");
        body.append("</body></html>");
        
        return body.toString();
    }
}
