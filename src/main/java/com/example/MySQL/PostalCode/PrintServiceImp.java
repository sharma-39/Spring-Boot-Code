package com.example.MySQL.PostalCode;

import org.springframework.stereotype.Service;


@Service
public class PrintServiceImp {
    public byte[] convertDocxToPdf(byte[] docxBytes) {
//        try (InputStream docxInputStream = new ByteArrayInputStream(docxBytes);
//             ByteArrayOutputStream pdfOutputStream = new ByteArrayOutputStream()) {
//
//            XWPFDocument docxDocument = new XWPFDocument(docxInputStream);
//
//            // Initialize iText PDF writer
//            PdfWriter writer = new PdfWriter(pdfOutputStream);
//            Document pdfDocument = new Document(new com.itextpdf.kernel.pdf.PdfDocument(writer));
//
//            // Convert DOCX to PDF
//            for (XWPFParagraph paragraph : docxDocument.getParagraphs()) {
//                String text = paragraph.getText();
//                pdfDocument.add(new Paragraph(text));
//            }
//
//            pdfDocument.close();
//
//            return pdfOutputStream.toByteArray();
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
        return "".getBytes();
    }
}
