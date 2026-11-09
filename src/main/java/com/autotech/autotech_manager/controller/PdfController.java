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