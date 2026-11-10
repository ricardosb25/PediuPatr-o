package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.dto.*;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.service.DashboardService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class DashboardController {
    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping("/dashboard")
    public String pagina() {
        return "dashboard";
    }

    @GetMapping("/api/dashboard/filtros")
    @ResponseBody
    public ResponseEntity<DashboardFiltros> filtros() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.filtros());
    }

    @GetMapping("/api/dashboard/resumo")
    @ResponseBody
    public ResponseEntity<DashboardResumo> resumo(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) StatusPedido status) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.consultar(inicio, fim, status));
    }
}
