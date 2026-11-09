package com.autotech.autotech_manager.controller;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
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

    @GetMapping("/garantia/{idOrden}")
    public ResponseEntity<byte[]> generarGarantia(@PathVariable int idOrden) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, out);
            document.open();

            Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph titulo = new Paragraph("AutoTech Manager - Garantía de Motor", fontTitulo);
            titulo.setAlignment(Element.ALIGN_CENTER);
            document.add(titulo);
            document.add(new Paragraph(" "));

            String sql = """
                SELECT c.nombre AS cliente, v.placa, v.marca, v.model, v.anio, 
                       cm.serie_motor_anterior, m.numero_serie AS nueva_serie, cm.resultados_pruebas
                FROM CambiosMotor cm
                INNER JOIN OrdenesTrabajo o ON cm.id_orden = o.id_orden
                INNER JOIN Vehiculos v ON cm.id_vehiculo = v.id_vehiculo
                INNER JOIN Clientes c ON v.id_cliente = c.id_cliente
                INNER JOIN Motores m ON cm.id_motor_nuevo = m.id_motor
                WHERE cm.id_orden = ?
                """;
            Map<String, Object> data = jdbc.queryForMap(sql, idOrden);

            document.add(new Paragraph("Orden #" + idOrden));
            document.add(new Paragraph("Cliente: " + data.get("cliente")));
            document.add(new Paragraph("Vehículo: " + data.get("marca") + " " + data.get("model") + " " + data.get("anio") + " - Placa: " + data.get("placa")));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.addCell("Serie del Motor Retirado");
            table.addCell((String) data.get("serie_motor_anterior"));
            table.addCell("Serie del Motor Nuevo");
            table.addCell((String) data.get("nueva_serie"));
            table.addCell("Resultados de Pruebas");
            table.addCell((String) data.get("resultados_pruebas"));
            document.add(table);

            document.add(new Paragraph(" "));
            Font fontGarantia = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Paragraph pGarantia = new Paragraph("*** GARANTÍA VÁLIDA POR 1 AÑO ***", fontGarantia);
            pGarantia.setAlignment(Element.ALIGN_CENTER);
            document.add(pGarantia);

            document.close();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.add("Content-Disposition", "inline; filename=Garantia_Orden_" + idOrden + ".pdf");

            return ResponseEntity.ok().headers(headers).body(out.toByteArray());
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
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

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.addCell("Servicio");
            table.addCell("Mano de Obra (L)");
            table.addCell("Materiales (L)");
            table.addCell("Total (L)");

            String sqlDetalles = """
                SELECT d.id_detalle, s.id_servicio, s.nombre AS servicio, d.costo_aplicado 
                FROM DetalleOrden d
                INNER JOIN Servicios s ON d.id_servicio = s.id_servicio
                WHERE d.id_orden = ?
                """;
            List<Map<String, Object>> detalles = jdbc.queryForList(sqlDetalles, idOrden);
            
            double totalManoObra = 0;
            double totalMateriales = 0;

            for (Map<String, Object> d : detalles) {
                String nombreServicio = (String) d.get("servicio");
                double manoObra = ((Number) d.get("costo_aplicado")).doubleValue();
                double costoMaterialesBase = 50.00;
                double totalFila = manoObra + costoMaterialesBase;

                table.addCell(nombreServicio);
                table.addCell("L. " + manoObra);
                table.addCell("L. " + costoMaterialesBase);
                table.addCell("L. " + totalFila);

                totalManoObra += manoObra;
                totalMateriales += costoMaterialesBase;
            }

            document.add(table);
            document.add(new Paragraph(" "));
            
            Font fontTotal = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Paragraph pManoObra = new Paragraph("Subtotal Mano de Obra: L. " + totalManoObra, fontTotal);
            pManoObra.setAlignment(Element.ALIGN_RIGHT);
            document.add(pManoObra);

            Paragraph pMateriales = new Paragraph("Subtotal Materiales/Insumos: L. " + totalMateriales, fontTotal);
            pMateriales.setAlignment(Element.ALIGN_RIGHT);
            document.add(pMateriales);

            Paragraph pTotal = new Paragraph("TOTAL A PAGAR: L. " + (totalManoObra + totalMateriales), fontTotal);
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