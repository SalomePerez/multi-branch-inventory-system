package com.inventory.config;

import com.inventory.entity.*;
import com.inventory.entity.enums.EstadoOrden;
import com.inventory.entity.enums.EstadoTransferencia;
import com.inventory.entity.enums.RolUsuario;
import com.inventory.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;
    private final InventarioRepository inventarioRepository;
    private final SucursalRepository sucursalRepository;
    private final UsuarioRepository usuarioRepository;
    private final ListaPrecioRepository listaPrecioRepository;
    private final AlertaRepository alertaRepository;
    private final OrdenCompraRepository ordenCompraRepository;
    private final TransferenciaRepository transferenciaRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        crearAdminSiNoExiste();
        if (productoRepository.count() == 0) {
            seedData();
        }
        seedListasPrecio();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Admin
    // ─────────────────────────────────────────────────────────────────────────

    private void crearAdminSiNoExiste() {
        if (usuarioRepository.existsByEmail("admin@empresa.com")) return;
        log.info("Creando usuario administrador inicial...");
        usuarioRepository.save(Usuario.builder()
                .nombre("Administrador")
                .email("admin@empresa.com")
                .passwordHash(passwordEncoder.encode("Admin1234!"))
                .rol(RolUsuario.ADMINISTRADOR)
                .activo(true)
                .build());
        log.info("Usuario admin creado: admin@empresa.com / Admin1234!");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Main seed
    // ─────────────────────────────────────────────────────────────────────────

    private void seedData() {
        log.info("Sembrando datos de demostración completos...");

        // ── Categorías (creadas por init.sql; se recuperan o crean si faltan) ──
        List<Categoria> cats = categoriaRepository.findAll();
        Categoria elec = cats.stream().filter(c -> c.getNombre().equals("Electrónica")).findFirst()
                .orElseGet(() -> categoriaRepository.save(Categoria.builder()
                        .nombre("Electrónica").descripcion("Dispositivos electrónicos").build()));
        Categoria ferr = cats.stream().filter(c -> c.getNombre().equals("Ferretería")).findFirst()
                .orElseGet(() -> categoriaRepository.save(Categoria.builder()
                        .nombre("Ferretería").descripcion("Herramientas y materiales").build()));
        Categoria ofic = cats.stream().filter(c -> c.getNombre().equals("Oficina")).findFirst()
                .orElseGet(() -> categoriaRepository.save(Categoria.builder()
                        .nombre("Oficina").descripcion("Artículos de oficina").build()));

        // ── Sucursales (creadas por init.sql; se recuperan o crean si faltan) ──
        List<Sucursal> subs = sucursalRepository.findAll();
        Sucursal central = subs.stream().filter(s -> s.getNombre().equals("Casa Central")).findFirst()
                .orElseGet(() -> sucursalRepository.save(Sucursal.builder()
                        .nombre("Casa Central").direccion("Av. Principal 100, Ciudad").principal(true).build()));
        Sucursal norte = subs.stream().filter(s -> s.getNombre().equals("Sucursal Norte")).findFirst()
                .orElseGet(() -> sucursalRepository.save(Sucursal.builder()
                        .nombre("Sucursal Norte").direccion("Calle Norte 200, Ciudad").build()));
        Sucursal sur = subs.stream().filter(s -> s.getNombre().equals("Sucursal Sur")).findFirst()
                .orElseGet(() -> sucursalRepository.save(Sucursal.builder()
                        .nombre("Sucursal Sur").direccion("Calle Sur 300, Ciudad").build()));

        // ── Usuarios ──
        Usuario admin = usuarioRepository.findByEmail("admin@empresa.com").orElseThrow();
        String pass = passwordEncoder.encode("Test1234!");

        Usuario gcen = crearUsuario("Carlos Rodríguez",  "gerente.central@empresa.com", pass, RolUsuario.GERENTE_SUCURSAL,       central);
        Usuario gnor = crearUsuario("Ana Martínez",      "gerente.norte@empresa.com",   pass, RolUsuario.GERENTE_SUCURSAL,       norte);
        Usuario gsur = crearUsuario("Luis Pérez",        "gerente.sur@empresa.com",     pass, RolUsuario.GERENTE_SUCURSAL,       sur);
        Usuario op1  = crearUsuario("María González",    "operador1@empresa.com",        pass, RolUsuario.OPERADOR_INVENTARIO,   central);
        Usuario op2  = crearUsuario("Pedro Sánchez",     "operador2@empresa.com",        pass, RolUsuario.OPERADOR_INVENTARIO,   norte);
        Usuario op3  = crearUsuario("Sofía López",       "operador3@empresa.com",        pass, RolUsuario.OPERADOR_INVENTARIO,   sur);

        // ── Productos ──
        // Electrónica
        Producto laptop  = prod("LPT-001", "Laptop Pro 14",              elec, "800.00",  "1200.00");
        Producto monitor = prod("MON-027", "Monitor 27\" 4K",            elec, "250.00",  "420.00");
        Producto mouse   = prod("MOU-001", "Mouse Inalámbrico Ergonómico",elec, "18.00",  "39.99");
        Producto teclado = prod("TEC-001", "Teclado Mecánico RGB",        elec, "45.00",  "89.99");
        Producto webcam  = prod("WEB-001", "Webcam HD 1080p",             elec, "30.00",  "59.99");
        Producto disco   = prod("DSK-001", "Disco Externo 1TB USB-C",     elec, "55.00",  "99.99");
        Producto cable   = prod("CBL-001", "Cable USB-C 2m",              elec,  "5.00",  "12.99");
        Producto hub     = prod("HUB-001", "Hub USB 7 Puertos",           elec, "22.00",  "45.00");
        Producto auricular = prod("AUR-001","Auriculares Bluetooth Pro",  elec, "40.00",  "85.00");
        // Ferretería
        Producto martillo  = prod("MAR-001", "Martillo 500g",              ferr,  "8.00", "18.50");
        Producto taladro   = prod("TAL-001", "Taladro Inalámbrico 18V",    ferr, "75.00", "149.99");
        Producto cinta     = prod("CIN-001", "Cinta Métrica 5m",           ferr,  "4.00",  "9.99");
        Producto destorn   = prod("DES-001", "Destornillador Eléctrico",   ferr, "35.00",  "68.00");
        // Oficina
        Producto resma     = prod("PAP-001", "Resma Papel A4 500 hojas",   ofic,  "4.00",   "8.50");
        Producto lapiceros = prod("LAP-001", "Lapiceros Caja x12",         ofic,  "3.00",   "6.99");
        Producto cuaderno  = prod("CUA-001", "Cuaderno Espiral A4",        ofic,  "2.50",   "5.99");

        // ── Inventario ──
        // Casa Central – bien surtido, excepto disco (bajo stock)
        inv(laptop,    central,  45, 10, 100);
        inv(monitor,   central,  30,  5,  80);
        inv(mouse,     central, 120, 20, 300);
        inv(teclado,   central,  60, 10, 150);
        inv(webcam,    central,  40,  8, 100);
        inv(disco,     central,   3, 10,  80);   // *** BAJO STOCK ***
        inv(cable,     central, 200, 30, 500);
        inv(hub,       central,  35,  8, 100);
        inv(auricular, central,  25, 10,  80);
        inv(martillo,  central,  80, 15, 200);
        inv(taladro,   central,  20,  5,  60);
        inv(cinta,     central, 100, 20, 300);
        inv(destorn,   central,  30,  8,  80);
        inv(resma,     central, 300, 50, 800);
        inv(lapiceros, central, 150, 30, 400);
        inv(cuaderno,  central, 200, 40, 500);

        // Sucursal Norte – laptop y auriculares con stock crítico
        inv(laptop,    norte,   2, 10,  50);     // *** BAJO STOCK ***
        inv(monitor,   norte,  15,  5,  40);
        inv(mouse,     norte,  55, 15, 150);
        inv(teclado,   norte,  28,  8,  80);
        inv(webcam,    norte,  18,  6,  60);
        inv(disco,     norte,  12, 10,  40);
        inv(cable,     norte,  80, 20, 200);
        inv(auricular, norte,   0,  5,  40);     // *** SIN STOCK ***
        inv(martillo,  norte,  40, 10, 120);
        inv(taladro,   norte,   8,  5,  30);
        inv(resma,     norte, 120, 30, 300);
        inv(lapiceros, norte,  60, 20, 200);

        // Sucursal Sur – teclado y taladro con stock crítico
        inv(laptop,    sur,  18,  8,  40);
        inv(monitor,   sur,  10,  5,  30);
        inv(mouse,     sur,  70, 15, 150);
        inv(teclado,   sur,   4,  8,  60);       // *** BAJO STOCK ***
        inv(webcam,    sur,  22,  6,  50);
        inv(disco,     sur,  20,  8,  40);
        inv(taladro,   sur,   1,  5,  25);       // *** BAJO STOCK ***
        inv(cinta,     sur,  55, 15, 150);
        inv(resma,     sur, 180, 30, 400);
        inv(lapiceros, sur,  90, 20, 250);
        inv(cuaderno,  sur, 110, 25, 300);

        // ── Alertas de stock bajo ──
        alerta("STOCK_BAJO", disco,     central, "Stock bajo: Disco Externo 1TB en Casa Central (3 unid., mínimo 10)");
        alerta("STOCK_BAJO", laptop,    norte,   "Stock crítico: Laptop Pro 14 en Sucursal Norte (2 unid., mínimo 10)");
        alerta("STOCK_BAJO", auricular, norte,   "Sin stock: Auriculares Bluetooth Pro en Sucursal Norte (0 unid., mínimo 5)");
        alerta("STOCK_BAJO", teclado,   sur,     "Stock bajo: Teclado Mecánico RGB en Sucursal Sur (4 unid., mínimo 8)");
        alerta("STOCK_BAJO", taladro,   sur,     "Stock crítico: Taladro Inalámbrico 18V en Sucursal Sur (1 unid., mínimo 5)");

        // ── Órdenes de Compra ──
        crearOrden(central, "TechWorld SA", EstadoOrden.RECIBIDA, gcen, admin, 30,
                List.of(laptop, monitor, mouse), List.of(5, 10, 30), List.of("800.00", "250.00", "18.00"));
        crearOrden(norte, "Distribuidora Norte Ltda", EstadoOrden.RECIBIDA, gnor, admin, 15,
                List.of(mouse, teclado, webcam), List.of(20, 15, 10), List.of("18.00", "45.00", "30.00"));
        crearOrden(central, "TechWorld SA", EstadoOrden.APROBADA, op1, gcen, 30,
                List.of(disco, auricular), List.of(20, 15), List.of("55.00", "40.00"));
        crearOrden(norte, "Herramientas Pro SRL", EstadoOrden.APROBADA, gnor, admin, 0,
                List.of(taladro, destorn), List.of(10, 15), List.of("75.00", "35.00"));
        crearOrden(sur, "OfficeMax SA", EstadoOrden.PENDIENTE, op3, null, 15,
                List.of(resma, lapiceros, cuaderno), List.of(100, 80, 60), List.of("4.00", "3.00", "2.50"));
        crearOrden(central, "Electrocomponents SRL", EstadoOrden.PENDIENTE, op1, null, 0,
                List.of(cable, hub), List.of(200, 50), List.of("5.00", "22.00"));
        crearOrden(sur, "TechWorld SA", EstadoOrden.CANCELADA, gsur, null, 0,
                List.of(laptop), List.of(3), List.of("800.00"));

        // ── Transferencias ──
        LocalDateTime now = LocalDateTime.now();
        // 1. COMPLETADA, a tiempo
        crearTransferencia(central, norte, EstadoTransferencia.COMPLETADA, gcen, admin,
                "Ruta Central-Norte", "Transportes Rápidos SA",
                now.minusDays(20), now.minusDays(17), now.minusDays(17),
                List.of(laptop, monitor), List.of(5, 10), List.of(5, 10), List.of(5, 10));
        // 2. COMPLETADA, a tiempo
        crearTransferencia(central, sur, EstadoTransferencia.COMPLETADA, gcen, gsur,
                "Ruta Central-Sur", "Logística Express",
                now.minusDays(12), now.minusDays(9), now.minusDays(9),
                List.of(mouse, teclado), List.of(20, 8), List.of(20, 8), List.of(20, 8));
        // 3. COMPLETADA, tarde (para bajar tasa de cumplimiento)
        crearTransferencia(norte, sur, EstadoTransferencia.COMPLETADA, gnor, gsur,
                "Ruta Norte-Sur", "Envíos Directos SA",
                now.minusDays(30), now.minusDays(26), now.minusDays(24),
                List.of(webcam, cable), List.of(5, 30), List.of(5, 30), List.of(5, 30));
        // 4. EN_TRANSITO
        crearTransferencia(central, norte, EstadoTransferencia.EN_TRANSITO, op1, gcen,
                "Ruta Central-Norte", "Transportes Rápidos SA",
                now.minusDays(2), now.plusDays(1), null,
                List.of(disco, auricular), List.of(15, 10), List.of(15, 10), null);
        // 5. PENDIENTE
        crearTransferencia(sur, central, EstadoTransferencia.PENDIENTE, op3, null,
                null, null, null, null, null,
                List.of(martillo), List.of(10), null, null);

        // ── Ventas históricas (12 meses) ──
        seedVentasHistoricas(central, norte, sur, gcen, gnor, gsur,
                laptop, monitor, mouse, teclado, webcam, disco, cable, hub, auricular,
                taladro, resma, lapiceros);

        log.info("Datos de demostración sembrados correctamente.");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Ventas históricas (JdbcTemplate para poder fijar created_at)
    // ─────────────────────────────────────────────────────────────────────────

    private void seedVentasHistoricas(
            Sucursal central, Sucursal norte, Sucursal sur,
            Usuario gcen, Usuario gnor, Usuario gsur,
            Producto laptop, Producto monitor, Producto mouse, Producto teclado,
            Producto webcam, Producto disco, Producto cable, Producto hub,
            Producto auricular, Producto taladro, Producto resma, Producto lapiceros) {

        long cId = central.getId(), nId = norte.getId(), sId = sur.getId();
        long gcId = gcen.getId(), gnId = gnor.getId(), gsId = gsur.getId();

        long lapId = laptop.getId(),    monId = monitor.getId(),  mouId = mouse.getId();
        long tecId = teclado.getId(),   webId = webcam.getId(),   dskId = disco.getId();
        long cblId = cable.getId(),     hubId = hub.getId(),      aurId = auricular.getId();
        long talId = taladro.getId(),   papId = resma.getId(),    lapicId = lapiceros.getId();

        BigDecimal pLap  = new BigDecimal("1200.00"), pMon = new BigDecimal("420.00");
        BigDecimal pMou  = new BigDecimal("39.99"),   pTec = new BigDecimal("89.99");
        BigDecimal pWeb  = new BigDecimal("59.99"),   pDsk = new BigDecimal("99.99");
        BigDecimal pCbl  = new BigDecimal("12.99"),   pHub = new BigDecimal("45.00");
        BigDecimal pAur  = new BigDecimal("85.00"),   pTal = new BigDecimal("149.99");
        BigDecimal pPap  = new BigDecimal("8.50"),    pLapic = new BigDecimal("6.99");

        // ── Generar Ventas Dinámicas (12 meses hacia atrás) ──
        LocalDateTime now = LocalDateTime.now();
        
        // Mes -11 (~$5,000)
        v(cId, gcId, lapId,  2, pLap, now.minusMonths(11).withDayOfMonth(5).withHour(10));
        v(nId, gnId, monId,  4, pMon, now.minusMonths(11).withDayOfMonth(8).withHour(10));
        v(sId, gsId, mouId, 15, pMou, now.minusMonths(11).withDayOfMonth(15).withHour(10));
        v(cId, gcId, tecId,  5, pTec, now.minusMonths(11).withDayOfMonth(22).withHour(10));

        // Mes -10 (~$8,000)
        v(cId, gcId, lapId,  3, pLap, now.minusMonths(10).withDayOfMonth(3).withHour(10));
        v(nId, gnId, monId,  5, pMon, now.minusMonths(10).withDayOfMonth(7).withHour(10));
        v(sId, gsId, tecId,  4, pTec, now.minusMonths(10).withDayOfMonth(12).withHour(10));
        v(cId, gcId, aurId,  8, pAur, now.minusMonths(10).withDayOfMonth(18).withHour(10));
        v(nId, gnId, mouId, 20, pMou, now.minusMonths(10).withDayOfMonth(25).withHour(10));

        // Mes -9 (~$6,500)
        v(cId, gcId, lapId,  2, pLap, now.minusMonths(9).withDayOfMonth(4).withHour(10));
        v(sId, gsId, monId,  3, pMon, now.minusMonths(9).withDayOfMonth(10).withHour(10));
        v(nId, gnId, dskId,  6, pDsk, now.minusMonths(9).withDayOfMonth(16).withHour(10));
        v(cId, gcId, tecId,  8, pTec, now.minusMonths(9).withDayOfMonth(24).withHour(10));

        // Mes -8 (~$10,500)
        v(cId, gcId, lapId,  4, pLap, now.minusMonths(8).withDayOfMonth(2).withHour(10));
        v(nId, gnId, lapId,  2, pLap, now.minusMonths(8).withDayOfMonth(8).withHour(10));
        v(sId, gsId, monId,  5, pMon, now.minusMonths(8).withDayOfMonth(14).withHour(10));
        v(cId, gcId, aurId, 12, pAur, now.minusMonths(8).withDayOfMonth(20).withHour(10));
        v(nId, gnId, dskId,  8, pDsk, now.minusMonths(8).withDayOfMonth(27).withHour(10));

        // Mes -7 (~$8,200)
        v(cId, gcId, lapId,  2, pLap, now.minusMonths(7).withDayOfMonth(5).withHour(10));
        v(sId, gsId, lapId,  1, pLap, now.minusMonths(7).withDayOfMonth(9).withHour(10));
        v(nId, gnId, monId,  4, pMon, now.minusMonths(7).withDayOfMonth(15).withHour(10));
        v(cId, gcId, tecId, 10, pTec, now.minusMonths(7).withDayOfMonth(22).withHour(10));
        v(sId, gsId, aurId,  7, pAur, now.minusMonths(7).withDayOfMonth(28).withHour(10));

        // Mes -6 (~$12,000)
        v(cId, gcId, lapId,  5, pLap, now.minusMonths(6).withDayOfMonth(3).withHour(10));
        v(nId, gnId, lapId,  2, pLap, now.minusMonths(6).withDayOfMonth(8).withHour(10));
        v(sId, gsId, monId,  6, pMon, now.minusMonths(6).withDayOfMonth(12).withHour(10));
        v(cId, gcId, dskId, 10, pDsk, now.minusMonths(6).withDayOfMonth(18).withHour(10));
        v(nId, gnId, aurId, 12, pAur, now.minusMonths(6).withDayOfMonth(24).withHour(10));
        v(sId, gsId, tecId,  8, pTec, now.minusMonths(6).withDayOfMonth(28).withHour(10));

        // Mes -5 (~$9,500)
        v(cId, gcId, lapId,  3, pLap, now.minusMonths(5).withDayOfMonth(4).withHour(10));
        v(nId, gnId, monId,  5, pMon, now.minusMonths(5).withDayOfMonth(9).withHour(10));
        v(sId, gsId, tecId,  6, pTec, now.minusMonths(5).withDayOfMonth(15).withHour(10));
        v(cId, gcId, aurId, 10, pAur, now.minusMonths(5).withDayOfMonth(21).withHour(10));
        v(nId, gnId, dskId,  7, pDsk, now.minusMonths(5).withDayOfMonth(28).withHour(10));

        // Mes -4 (~$15,500)
        v(cId, gcId, lapId,  6, pLap, now.minusMonths(4).withDayOfMonth(3).withHour(10));
        v(nId, gnId, lapId,  3, pLap, now.minusMonths(4).withDayOfMonth(7).withHour(10));
        v(sId, gsId, lapId,  2, pLap, now.minusMonths(4).withDayOfMonth(11).withHour(10));
        v(cId, gcId, monId,  8, pMon, now.minusMonths(4).withDayOfMonth(14).withHour(10));
        v(nId, gnId, aurId, 15, pAur, now.minusMonths(4).withDayOfMonth(18).withHour(10));
        v(sId, gsId, dskId, 12, pDsk, now.minusMonths(4).withDayOfMonth(22).withHour(10));
        v(cId, gcId, tecId, 12, pTec, now.minusMonths(4).withDayOfMonth(26).withHour(10));

        // Mes -3 (~$21,000)
        v(cId, gcId, lapId,  8, pLap, now.minusMonths(3).withDayOfMonth(2).withHour(10));
        v(nId, gnId, lapId,  4, pLap, now.minusMonths(3).withDayOfMonth(6).withHour(10));
        v(sId, gsId, lapId,  3, pLap, now.minusMonths(3).withDayOfMonth(9).withHour(10));
        v(cId, gcId, monId, 10, pMon, now.minusMonths(3).withDayOfMonth(12).withHour(10));
        v(nId, gnId, monId,  6, pMon, now.minusMonths(3).withDayOfMonth(15).withHour(10));
        v(cId, gcId, aurId, 18, pAur, now.minusMonths(3).withDayOfMonth(18).withHour(10));
        v(nId, gnId, dskId, 15, pDsk, now.minusMonths(3).withDayOfMonth(21).withHour(10));
        v(sId, gsId, tecId, 14, pTec, now.minusMonths(3).withDayOfMonth(27).withHour(10));

        // Mes -2 (~$7,800)
        v(cId, gcId, lapId,  2, pLap, now.minusMonths(2).withDayOfMonth(5).withHour(10));
        v(nId, gnId, monId,  4, pMon, now.minusMonths(2).withDayOfMonth(10).withHour(10));
        v(sId, gsId, aurId,  8, pAur, now.minusMonths(2).withDayOfMonth(18).withHour(10));
        v(cId, gcId, tecId,  6, pTec, now.minusMonths(2).withDayOfMonth(25).withHour(10));

        // Mes -1 (~$9,500)
        v(cId, gcId, lapId,  3, pLap, now.minusMonths(1).withDayOfMonth(4).withHour(10));
        v(nId, gnId, lapId,  1, pLap, now.minusMonths(1).withDayOfMonth(9).withHour(10));
        v(sId, gsId, monId,  5, pMon, now.minusMonths(1).withDayOfMonth(14).withHour(10));
        v(cId, gcId, dskId, 10, pDsk, now.minusMonths(1).withDayOfMonth(20).withHour(10));
        v(nId, gnId, aurId,  9, pAur, now.minusMonths(1).withDayOfMonth(26).withHour(10));

        // Mes actual (~$12,000)
        int maxDay = now.getDayOfMonth() == 1 ? 1 : Math.min(28, now.getDayOfMonth() - 1);
        v(cId, gcId, lapId,  4, pLap, now.withDayOfMonth(Math.max(1, maxDay - 10)).withHour(10));
        v(nId, gnId, lapId,  2, pLap, now.withDayOfMonth(Math.max(1, maxDay - 8)).withHour(10));
        v(sId, gsId, monId,  6, pMon, now.withDayOfMonth(Math.max(1, maxDay - 6)).withHour(10));
        v(cId, gcId, aurId, 12, pAur, now.withDayOfMonth(Math.max(1, maxDay - 4)).withHour(10));
        v(nId, gnId, dskId, 10, pDsk, now.withDayOfMonth(Math.max(1, maxDay - 2)).withHour(10));
        v(sId, gsId, tecId, 10, pTec, now.withDayOfMonth(Math.max(1, maxDay)).withHour(10));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ListasPrecio (idempotent)
    // ─────────────────────────────────────────────────────────────────────────

    private void seedListasPrecio() {
        listaPrecioRepository.findByNombre("Promociones Invierno")
                .orElseGet(() -> listaPrecioRepository.save(ListaPrecio.builder()
                        .nombre("Promociones Invierno")
                        .descripcion("Precios con descuento por la temporada de invierno")
                        .porcentaje(new BigDecimal("10.00"))
                        .condicionCantidadMinima(1)
                        .activa(true).build()));

        listaPrecioRepository.findByNombre("Mayoristas")
                .orElseGet(() -> listaPrecioRepository.save(ListaPrecio.builder()
                        .nombre("Mayoristas")
                        .descripcion("Descuento automático para clientes mayoristas")
                        .porcentaje(new BigDecimal("15.00"))
                        .condicionCantidadMinima(12)
                        .activa(true).build()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private Usuario crearUsuario(String nombre, String email, String passHash, RolUsuario rol, Sucursal sucursal) {
        return usuarioRepository.findByEmail(email).orElseGet(() ->
                usuarioRepository.save(Usuario.builder()
                        .nombre(nombre).email(email).passwordHash(passHash)
                        .rol(rol).sucursal(sucursal).activo(true).build()));
    }

    private Producto prod(String sku, String nombre, Categoria cat, String costo, String venta) {
        return productoRepository.save(Producto.builder()
                .sku(sku).nombre(nombre).categoria(cat)
                .precioCosto(new BigDecimal(costo))
                .precioVenta(new BigDecimal(venta))
                .build());
    }

    private void inv(Producto p, Sucursal s, int cantidad, int min, int max) {
        inventarioRepository.save(Inventario.builder()
                .producto(p).sucursal(s)
                .cantidad(cantidad).stockMinimo(min).stockMaximo(max)
                .build());
                
        jdbcTemplate.update(
                "INSERT INTO movimientos (producto_id, sucursal_id, tipo, cantidad, cantidad_antes, cantidad_despues, " +
                "referencia_id, referencia_tipo, usuario_id, motivo, observaciones, created_at) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                p.getId(), s.getId(), "ENTRADA", cantidad, 0, cantidad, 
                null, "INICIAL", null, "Stock Inicial", "Carga inicial de demostración", 
                Timestamp.valueOf(LocalDateTime.now().minusMonths(12)));
    }

    private void alerta(String tipo, Producto p, Sucursal s, String msg) {
        alertaRepository.save(Alerta.builder()
                .tipo(tipo).producto(p).sucursal(s).mensaje(msg).leida(false).build());
    }

    private void crearOrden(Sucursal sucursal, String proveedor, EstadoOrden estado,
                             Usuario creadoPor, Usuario aprobadoPor, int plazoPago,
                             List<Producto> productos, List<Integer> cantidades, List<String> precios) {
        OrdenCompra oc = OrdenCompra.builder()
                .sucursal(sucursal).proveedor(proveedor).estado(estado)
                .creadoPor(creadoPor).aprobadoPor(aprobadoPor)
                .plazoPago(plazoPago)
                .observaciones("Orden generada para demostración del sistema")
                .build();

        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < productos.size(); i++) {
            BigDecimal precio = new BigDecimal(precios.get(i));
            total = total.add(precio.multiply(BigDecimal.valueOf(cantidades.get(i))));
            OrdenCompraItem item = OrdenCompraItem.builder()
                    .orden(oc).producto(productos.get(i))
                    .cantidad(cantidades.get(i)).precioUnitario(precio).build();
            oc.getItems().add(item);
        }
        oc.setTotal(total);
        ordenCompraRepository.save(oc);
    }

    private void crearTransferencia(Sucursal origen, Sucursal destino, EstadoTransferencia estado,
                                    Usuario solicitante, Usuario aprobador,
                                    String ruta, String transportista,
                                    LocalDateTime salida, LocalDateTime estimada, LocalDateTime realLlegada,
                                    List<Producto> productos, List<Integer> solicitadas,
                                    List<Integer> enviadas, List<Integer> recibidas) {
        Transferencia t = Transferencia.builder()
                .sucursalOrigen(origen).sucursalDestino(destino).estado(estado)
                .solicitadoPor(solicitante).aprobadoPor(aprobador)
                .rutaNombre(ruta).transportista(transportista)
                .fechaSalida(salida).fechaEstimadaLlegada(estimada).fechaRealLlegada(realLlegada)
                .observaciones("Transferencia de demostración")
                .build();

        if (productos != null) {
            for (int i = 0; i < productos.size(); i++) {
                TransferenciaItem item = TransferenciaItem.builder()
                        .transferencia(t).producto(productos.get(i))
                        .cantidadSolicitada(solicitadas.get(i))
                        .cantidadEnviada(enviadas != null ? enviadas.get(i) : null)
                        .cantidadRecibida(recibidas != null ? recibidas.get(i) : null)
                        .build();
                t.getItems().add(item);
            }
        }
        transferenciaRepository.save(t);
    }

    /** Inserta una venta con fecha backdateada + un item y su movimiento. */
    private void v(long sucursalId, long vendedorId, long productoId,
                   int cantidad, BigDecimal precio, LocalDateTime fecha) {
        BigDecimal total = precio.multiply(BigDecimal.valueOf(cantidad));
        Long ventaId = insertVenta(sucursalId, vendedorId, total, fecha);
        insertVentaItem(ventaId, productoId, cantidad, precio);
        
        jdbcTemplate.update(
            "INSERT INTO movimientos (producto_id, sucursal_id, tipo, cantidad, cantidad_antes, cantidad_despues, " +
            "referencia_id, referencia_tipo, usuario_id, motivo, observaciones, created_at) " +
            "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
            productoId, sucursalId, "SALIDA", cantidad * -1, 100, 100 - cantidad, 
            ventaId, "VENTA", vendedorId, "Venta", "Venta histórica demostración", Timestamp.valueOf(fecha)
        );
    }

    private Long insertVenta(long sucursalId, long vendedorId, BigDecimal total, LocalDateTime fecha) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO ventas (sucursal_id, vendedor_id, total, descuento_total, created_at) VALUES (?,?,?,?,?)",
                    new String[]{"id"});
            ps.setLong(1, sucursalId);
            ps.setLong(2, vendedorId);
            ps.setBigDecimal(3, total);
            ps.setBigDecimal(4, BigDecimal.ZERO);
            ps.setTimestamp(5, Timestamp.valueOf(fecha));
            return ps;
        }, kh);
        return Objects.requireNonNull(kh.getKey()).longValue();
    }

    private void insertVentaItem(long ventaId, long productoId, int cantidad, BigDecimal precio) {
        BigDecimal subtotal = precio.multiply(BigDecimal.valueOf(cantidad));
        jdbcTemplate.update(
                "INSERT INTO venta_items (venta_id, producto_id, cantidad, precio_unitario, descuento_aplicado, subtotal) VALUES (?,?,?,?,?,?)",
                ventaId, productoId, cantidad, precio, BigDecimal.ZERO, subtotal);
    }
}
