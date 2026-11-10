package com.umc.pediupatrao.repository;
import com.umc.pediupatrao.entity.Auditoria;
import org.springframework.data.repository.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
public interface AuditoriaRepository extends Repository<Auditoria, String> {
    Auditoria insert(Auditoria auditoria);
    Page<Auditoria> findAll(Pageable paginacao);
}
