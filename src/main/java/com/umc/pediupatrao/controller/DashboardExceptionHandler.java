package com.umc.pediupatrao.controller;

import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = DashboardController.class)
public class DashboardExceptionHandler {
    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Map<String, String>> filtroInvalido(Exception falha) {
        String mensagem = falha instanceof MethodArgumentTypeMismatchException
                ? "Filtro inválido: use datas AAAA-MM-DD e um status permitido." : falha.getMessage();
        return ResponseEntity.badRequest().cacheControl(CacheControl.noStore()).body(Map.of("erro", mensagem));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, String>> bancoIndisponivel() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).cacheControl(CacheControl.noStore())
                .body(Map.of("erro", "Não foi possível consultar os pedidos. Tente novamente em instantes."));
    }
}
