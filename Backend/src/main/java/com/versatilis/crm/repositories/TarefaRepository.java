package com.versatilis.crm.repositories;

import com.versatilis.crm.model.Tarefa;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TarefaRepository extends JpaRepository<Tarefa, Long> {

    Page<Tarefa> findByAtivoTrueAndStatus(Tarefa.StatusTarefa status, Pageable pageable);

    long countByAtivoTrueAndStatus(Tarefa.StatusTarefa status);

    List<Tarefa> findByResponsavelIdAndStatus(Long responsavelId, Tarefa.StatusTarefa status);

    List<Tarefa> findByDataVencimentoBeforeAndStatusNot(LocalDate data, Tarefa.StatusTarefa status);

    @Query("SELECT t FROM Tarefa t WHERE " +
           "(:titulo IS NULL OR t.titulo LIKE %:titulo%) AND " +
           "(:status IS NULL OR t.status = :status) AND " +
           "(:usuarioId IS NULL OR t.responsavel.id = :usuarioId) AND " +
           "(:prioridade IS NULL OR t.prioridade = :prioridade) AND " +
           "(:dataInicio IS NULL OR t.dataVencimento >= :dataInicio) AND " +
           "(:dataFim IS NULL OR t.dataVencimento <= :dataFim) AND " +
           "t.ativo = true")
    Page<Tarefa> findByFilters(
            @Param("titulo") String titulo,
            @Param("status") Tarefa.StatusTarefa status,
            @Param("usuarioId") Long usuarioId,
            @Param("prioridade") Tarefa.PrioridadeTarefa prioridade,
            @Param("dataInicio") LocalDate dataInicio,
            @Param("dataFim") LocalDate dataFim,
            Pageable pageable
    );
}
