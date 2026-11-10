package com.umc.pediupatrao.service;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.pediupatrao.entity.Auditoria;
import com.umc.pediupatrao.repository.AuditoriaRepository;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
@Service
public class AuditoriaService {
    private final AuditoriaRepository repository;
    private final OperadorService operador;
    private final ObjectMapper mapper;
    public AuditoriaService(AuditoriaRepository repository, OperadorService operador, ObjectMapper mapper) {
        this.repository = repository; this.operador = operador; this.mapper = mapper;
    }
    public Map<String, Object> retrato(Object entidade) {
        return entidade == null ? Collections.emptyMap() : mapper.convertValue(entidade, new TypeReference<Map<String, Object>>() {});
    }
    public void registrar(String operacao, String entidade, String entidadeId, Map<String, Object> anterior,
                          Object atualizado, String justificativa) {
        Map<String, Object> novo = retrato(atualizado);
        Map<String, Auditoria.Alteracao> alteracoes = new TreeMap<>();
        Set<String> campos = new HashSet<>(anterior.keySet());
        campos.addAll(novo.keySet());
        for (String campo : campos) {
            if (Set.of("password", "senha", "token", "versao").contains(campo)) continue;
            if (!Objects.equals(anterior.get(campo), novo.get(campo))) {
                alteracoes.put(campo, new Auditoria.Alteracao(anterior.get(campo), novo.get(campo)));
            }
        }
        repository.insert(new Auditoria(null, operador.usuario(), operador.perfil(), Instant.now(),
            operacao, entidade, entidadeId, alteracoes, justificativa));
    }
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public Page<Auditoria> consultar(int pagina) {
        return repository.findAll(PageRequest.of(Math.max(0, pagina), 50, Sort.by(Sort.Direction.DESC, "dataHora")));
    }
}
