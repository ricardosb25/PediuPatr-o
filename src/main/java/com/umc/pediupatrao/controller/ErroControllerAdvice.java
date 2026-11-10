package com.umc.pediupatrao.controller;
import java.util.Map;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;
@RestControllerAdvice
public class ErroControllerAdvice {
    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<Map<String, String>> requisicaoInvalida(Exception falha) {
        String mensagem = falha instanceof IllegalArgumentException ? falha.getMessage() : "Corpo da requisição inválido";
        return ResponseEntity.badRequest().body(Map.of("erro", mensagem == null ? "Dados inválidos" : mensagem));
    }
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, String>> conflito() {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("erro", "Registro alterado por outro operador. Atualize a página."));
    }
}
