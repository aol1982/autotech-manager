# 🔧 AutoTech Manager - Sistema Integral de Gestión para Taller Mecánico

Este proyecto fue desarrollado como parte del examen de **Programación Avanzada II**. Consiste en una aplicación web completa para gestionar clientes, vehículos, órdenes de trabajo, mecánicos, inventario de repuestos y facturación.

### 🛠️ Tecnologías Utilizadas
*   **Backend:** Java 17, Spring Boot, Spring Data JDBC.
*   **Base de Datos:** Microsoft SQL Server (Azure SQL Database).
*   **Frontend:** HTML5, CSS3, Bootstrap 5, JavaScript (Fetch API).
*   **Generación de Reportes:** OpenPDF (Librería nativa de Java sin JasperReports).

### 🚀 Funcionalidades Principales
*   **Roles:** Login diferenciado para Administrador, Cajero y Mecánico.
*   **Flujo de Órdenes:** Control estricto de estados (Recibida -> Diagnóstico -> Reparación -> Finalizada).
*   **Inventario:** Control de stock y consumo de repuestos.
*   **Sustitución de Motor:** Validación de compatibilidad de motor por VIN y modelo.
*   **Facturación:** Generación de Cotizaciones y Facturas en PDF con cálculo de mano de obra (10%) y materiales (Lempiras).
*   **Auditoría:** Triggers en la base de datos para el historial de cambios.

### 💻 ¿Cómo ejecutarlo localmente?
1. Clonar el repositorio: `git clone https://github.com/tu-usuario/autotech-manager.git`
2. Configurar el archivo `src/main/resources/application.properties` con las credenciales de SQL Server.
3. Ejecutar el proyecto: `./mvnw spring-boot:run`
4. Abrir en el navegador: `http://localhost:8080`

### 🔐 Credenciales de Prueba
*   **Administrador:** usuario `admin`, contraseña `admin123`
*   **Cajero:** usuario `caja`, contraseña `caja123`
*   **Mecánico:** usuario `mecanico`, contraseña `mecanico123`
