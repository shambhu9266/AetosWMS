package com.example.backend.service;

import com.example.backend.model.Grn;
import com.example.backend.model.GrnItem;
import com.example.backend.repo.GrnRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

// Using iText 8 (artifact relocated as com.itextpdf:itext-core)
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;

@Service
public class GrnPdfService {
    @Autowired
    private GrnRepository grnRepository;

    public byte[] generateGrnPdf(Long grnId) {
        Grn grn = grnRepository.findById(grnId).orElseThrow();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document doc = new Document(pdf);

        doc.add(new Paragraph("Goods Receipt Note (GRN)").setBold().setFontSize(16));
        doc.add(new Paragraph("GRN ID: " + grn.getId()));
        doc.add(new Paragraph("PO Number: " + grn.getPoNumber()));
        doc.add(new Paragraph("Received By: " + grn.getReceivedBy()));
        doc.add(new Paragraph("Received Date: " + grn.getReceivedDate()));
        if (grn.getOverallRemarks() != null) {
            doc.add(new Paragraph("Remarks: " + grn.getOverallRemarks()));
        }

        float[] widths = {4, 2, 2, 2, 2, 4};
        Table table = new Table(widths);
        table.useAllAvailableWidth();
        table.addHeaderCell("Description");
        table.addHeaderCell("Ordered");
        table.addHeaderCell("Received");
        table.addHeaderCell("Unit");
        table.addHeaderCell("Status");
        table.addHeaderCell("Remarks");

        for (GrnItem item : grn.getItems()) {
            table.addCell(safe(item.getDescription()));
            table.addCell(String.valueOf(item.getOrderedQty() != null ? item.getOrderedQty() : 0));
            table.addCell(String.valueOf(item.getReceivedQty() != null ? item.getReceivedQty() : 0));
            table.addCell(safe(item.getUnit()));
            table.addCell(safe(item.getStatus()));
            table.addCell(safe(item.getRemarks()));
        }

        doc.add(table);
        doc.close();
        return baos.toByteArray();
    }

    private String safe(String s) { return s == null ? "" : s; }
}


