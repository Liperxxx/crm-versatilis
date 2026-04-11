package com.versatilis.crm.repositories;

import com.versatilis.crm.model.Orcamento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrcamentoRepository extends JpaRepository<Orcamento, Long> {

    long countByAtivoTrue();

    List<Orcamento> findByAtivoTrue();

    @Query("SELECT o FROM Orcamento o " +
           "LEFT JOIN FETCH o.itens " +
           "LEFT JOIN FETCH o.cliente " +
           "LEFT JOIN FETCH o.oportunidade " +
           "LEFT JOIN FETCH o.responsavel " +
           "WHERE o.id = :id AND o.ativo = true")
    Optional<Orcamento> findByIdWithItens(@Param("id") Long id);

    @Query("SELECT o FROM Orcamento o WHERE " +
           "(:status IS NULL OR o.status = :status) AND " +
           "(:clienteId IS NULL OR o.cliente.id = :clienteId) AND " +
           "(:oportunidadeId IS NULL OR o.oportunidade.id = :oportunidadeId) AND " +
           "o.ativo = true")
    Page<Orcamento> findByFilters(
            @Param("status") Orcamento.StatusOrcamento status,
            @Param("clienteId") Long clienteId,
            @Param("oportunidadeId") Long oportunidadeId,
            Pageable pageable
    );

    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(o.numero, 5) AS int)), 0) FROM Orcamento o WHERE o.numero LIKE :prefix%")
    int findMaxNumeroByPrefix(@Param("prefix") String prefix);

    @Query("SELECT COALESCE(SUM(o.total), 0) FROM Orcamento o WHERE o.ativo = true")
    BigDecimal sumTotalByAtivoTrue();
}
