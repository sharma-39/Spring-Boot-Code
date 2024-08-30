package com.example.MySQL.PostalCode;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

@Service
public class PrintServiceImp {
    public byte[] convertDocxToPdf(byte[] docxBytes) {
        try (InputStream docxInputStream = new ByteArrayInputStream(docxBytes);
             ByteArrayOutputStream pdfOutputStream = new ByteArrayOutputStream()) {

            XWPFDocument docxDocument = new XWPFDocument(docxInputStream);

            // Initialize iText PDF writer
            PdfWriter writer = new PdfWriter(pdfOutputStream);
            Document pdfDocument = new Document(new com.itextpdf.kernel.pdf.PdfDocument(writer));

            // Convert DOCX to PDF
            for (XWPFParagraph paragraph : docxDocument.getParagraphs()) {
                String text = paragraph.getText();
                pdfDocument.add(new Paragraph(text));
            }

            pdfDocument.close();

            return pdfOutputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
