package com.versatilis.crm.repositories;

import com.versatilis.crm.model.Lead;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeadRepository extends JpaRepository<Lead, Long> {

    Optional<Lead> findByEmail(String email);

    List<Lead> findByStatus(Lead.StatusLead status);

    @Query("SELECT l FROM Lead l WHERE " +
           "(:nome IS NULL OR l.nomeContato LIKE %:nome%) AND " +
           "(:status IS NULL OR l.status = :status) AND " +
           "(:origem IS NULL OR l.origem = :origem) AND " +
           "(:usuarioId IS NULL OR l.responsavel.id = :usuarioId) AND " +
           "l.ativo = true")
    Page<Lead> findByFilters(
            @Param("nome") String nome,
            @Param("status") Lead.StatusLead status,
            @Param("origem") Lead.OrigemLead origem,
            @Param("usuarioId") Long usuarioId,
            Pageable pageable
    );

    List<Lead> findByStatusAndScoreGreaterThanEqual(Lead.StatusLead status, Integer score);
}