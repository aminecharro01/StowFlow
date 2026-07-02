package com.stowflow.inventra.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.stowflow.inventra.domain.Sale;
import com.stowflow.inventra.domain.SaleLine;
import com.stowflow.inventra.exception.BusinessException;
import com.stowflow.inventra.util.MoneyFormat;
import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;

@Service
public class SaleInvoicePdfService {

    private static final DateTimeFormatter FR_DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("Europe/Paris"));

    public byte[] build(Sale sale) {
        try {
            Document doc = new Document(PageSize.A4, 48, 48, 48, 48);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Font normal = FontFactory.getFont(FontFactory.HELVETICA, 11);
            Font small = FontFactory.getFont(FontFactory.HELVETICA, 9);

            String tenantName = sale.getTenant() != null ? sale.getTenant().getName() : "StowFlow";
            doc.add(new Paragraph("Facture", titleFont));
            doc.add(new Paragraph(tenantName, normal));
            doc.add(new Paragraph("N° " + sale.getSaleNumber() + " — " + FR_DATE.format(sale.getCreatedAt()), small));
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Vendeur : " + sale.getSoldByEmail(), normal));
            if (sale.getNote() != null && !sale.getNote().isBlank()) {
                doc.add(new Paragraph("Note : " + sale.getNote(), small));
            }
            doc.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(new float[] {3f, 1f, 1.2f, 1.2f});
            table.setWidthPercentage(100);
            addHeader(table, "Article");
            addHeader(table, "Qté");
            addHeader(table, "Prix unit.");
            addHeader(table, "Total");

            for (SaleLine line : sale.getLines()) {
                addCell(table, line.getNameSnapshot() + "\n" + line.getSkuSnapshot(), Element.ALIGN_LEFT);
                addCell(table, String.valueOf(line.getQuantity()), Element.ALIGN_RIGHT);
                addCell(
                        table,
                        MoneyFormat.format(line.getUnitPrice()),
                        Element.ALIGN_RIGHT);
                addCell(
                        table,
                        MoneyFormat.format(line.getLineTotal()),
                        Element.ALIGN_RIGHT);
            }

            doc.add(table);
            doc.add(new Paragraph(" "));
            Paragraph total = new Paragraph(
                    "Total TTC (indicatif) : " + MoneyFormat.format(sale.getTotalAmount()),
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12));
            total.setAlignment(Element.ALIGN_RIGHT);
            doc.add(total);
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph(
                    "Document généré pour traçabilité des sorties de stock. Conservez cette facture.", small));

            doc.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new BusinessException("Impossible de générer le PDF.");
        }
    }

    private static void addHeader(PdfPTable table, String text) {
        PdfPCell c = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setPadding(6);
        table.addCell(c);
    }

    private static void addCell(PdfPTable table, String text, int align) {
        PdfPCell c = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA, 10)));
        c.setHorizontalAlignment(align);
        c.setPadding(5);
        table.addCell(c);
    }
}
