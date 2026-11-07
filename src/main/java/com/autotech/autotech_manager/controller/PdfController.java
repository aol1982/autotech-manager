package com.autotech.autotech_manager.controller;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pdf")
public class PdfController {

    private final JdbcTemplate jdbc;

    public PdfController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/cotizacion/{idOrden}")
    public ResponseEntity<byte[]> generarCotizacion(@PathVariable int idOrden) {
        return generarDocumento(idOrden, false);
    }

    @GetMapping("/factura/{idOrden}")
    public ResponseEntity<byte[]> generarFactura(@PathVariable int idOrden) {
        return generarDocumento(idOrden, true);
    }

    private ResponseEntity<byte[]> generarDocumento(int idOrden, boolean esFactura) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, out);
            document.open();

            Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            String tipoDoc = esFactura ? "Factura Oficial" : "Cotización";
            Paragraph titulo = new Paragraph("AutoTech Manager - " + tipoDoc, fontTitulo);
            titulo.setAlignment(Element.ALIGN_CENTER);
            document.add(titulo);
            document.add(new Paragraph(" "));

            String sqlCliente = """
                SELECT c.nombre, v.placa, v.marca, v.model, o.id_orden 
                FROM OrdenesTrabajo o
                INNER JOIN Vehiculos v ON o.id_vehiculo = v.id_vehiculo
                INNER JOIN Clientes c ON v.id_cliente = c.id_cliente
                WHERE o.id_orden = ?
                """;
            Map<String, Object> cliente = jdbc.queryForMap(sqlCliente, idOrden);

            document.add(new Paragraph("Orden #" + idOrden));
            document.add(new Paragraph("Cliente: " + cliente.get("nombre")));
            document.add(new Paragraph("Vehículo: " + cliente.get("marca") + " " + cliente.get("model") + " - Placa: " + cliente.get("placa")));
            
            if (esFactura) {
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                document.add(new Paragraph("Fecha de Emisión: " + LocalDateTime.now().format(dtf)));
            }
            document.add(new Paragraph(" "));

            // Tabla con 4 columnas
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.addCell("Servicio / Detalle");
            table.addCell("Materiales (L)");
            table.addCell("Mano de Obra (L)");
            table.addCell("Total (L)");

            String sqlDetalles = "SELECT * FROM vw_DetalleFactura WHERE id_orden = ?";
            List<Map<String, Object>> detalles = jdbc.queryForList(sqlDetalles, idOrden);
            
            double totalManoObra = 0;
            double totalMateriales = 0;

            for (Map<String, Object> d : detalles) {
                String nombreServicio = (String) d.get("servicio");
                String materialesUsados = (String) d.get("materiales_utilizados");
                
                double repuestos = ((Number) d.get("costo_repuestos")).doubleValue();
                double insumos = ((Number) d.get("insumos_taller")).doubleValue();
                double manoObra = ((Number) d.get("mano_obra")).doubleValue();
                double totalFila = ((Number) d.get("total_fila")).doubleValue();

                double materialesTotales = repuestos + insumos;

                // Celda del servicio con desglose de repuestos
                PdfPCell cellServicio = new PdfPCell();
                cellServicio.addElement(new Paragraph(nombreServicio, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
                Font fontMini = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8);
                cellServicio.addElement(new Paragraph("Usa: " + materialesUsados + ", Guantes/Desengrasante", fontMini));
                
                table.addCell(cellServicio);
                table.addCell("L. " + materialesTotales);
                table.addCell("L. " + manoObra);
                table.addCell("L. " + totalFila);

                totalManoObra += manoObra;
                totalMateriales += materialesTotales;
            }

            document.add(table);
            document.add(new Paragraph(" "));
            
            Font fontTotal = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Paragraph pMateriales = new Paragraph("Subtotal Repuestos/Materiales (Incluye L. 50 Insumos): L. " + totalMateriales, fontTotal);
            pMateriales.setAlignment(Element.ALIGN_RIGHT);
            document.add(pMateriales);

            Paragraph pManoObra = new Paragraph("Subtotal Mano de Obra (10%): L. " + totalManoObra, fontTotal);
            pManoObra.setAlignment(Element.ALIGN_RIGHT);
            document.add(pManoObra);

            Paragraph pTotal = new Paragraph("TOTAL A PAGAR: L. " + (totalMateriales + totalManoObra), fontTotal);
            pTotal.setAlignment(Element.ALIGN_RIGHT);
            document.add(pTotal);

            if (esFactura) {
                document.add(new Paragraph(" "));
                Font fontPagado = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
                Paragraph pPagado = new Paragraph("*** PAGADO ***", fontPagado);
                pPagado.setAlignment(Element.ALIGN_CENTER);
                document.add(pPagado);
            }

            document.close();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            String filename = (esFactura ? "Factura_" : "Cotizacion_") + "Orden_" + idOrden + ".pdf";
            headers.add("Content-Disposition", "inline; filename=" + filename);

            return ResponseEntity.ok().headers(headers).body(out.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }
}