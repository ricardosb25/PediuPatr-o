package com.umc.pediupatrao.controller;
import com.umc.pediupatrao.entity.Auditoria;
import com.umc.pediupatrao.service.AuditoriaService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
@Controller
public class AuditoriaController {
    private final AuditoriaService service;
    public AuditoriaController(AuditoriaService service) { this.service = service; }
    @GetMapping("/api/auditoria") @ResponseBody
    public Page<Auditoria> consultar(@RequestParam(defaultValue = "0") int pagina) { return service.consultar(pagina); }
    @GetMapping("/auditoria")
    public String pagina(@RequestParam(defaultValue = "0") int pagina, Model model) {
        model.addAttribute("registros", service.consultar(pagina));
        model.addAttribute("content", "auditoria :: content");
        return "layout";
    }
}
