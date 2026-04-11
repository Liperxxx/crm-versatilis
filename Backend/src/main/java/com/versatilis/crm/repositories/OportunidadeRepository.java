package com.versatilis.crm.repositories;

import com.versatilis.crm.model.Oportunidade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface OportunidadeRepository extends JpaRepository<Oportunidade, Long> {

    long countByAtivoTrue();

    List<Oportunidade> findByStatusAndAtivoTrue(Oportunidade.StatusOportunidade status);

    List<Oportunidade> findByStatus(Oportunidade.StatusOportunidade status);

    @Query("SELECT o FROM Oportunidade o WHERE " +
           "(:titulo IS NULL OR o.titulo LIKE %:titulo%) AND " +
           "(:etapa IS NULL OR o.etapa = :etapa) AND " +
           "(:usuarioId IS NULL OR o.responsavel.id = :usuarioId) AND " +
           "(:status IS NULL OR o.status = :status) AND " +
           "o.ativo = true")
    Page<Oportunidade> findByFilters(
            @Param("titulo") String titulo,
            @Param("etapa") Oportunidade.EtapaOportunidade etapa,
            @Param("usuarioId") Long usuarioId,
            @Param("status") Oportunidade.StatusOportunidade status,
            Pageable pageable
    );

    List<Oportunidade> findByStatusAndEtapa(
            Oportunidade.StatusOportunidade status,
            Oportunidade.EtapaOportunidade etapa
    );

    @Query("SELECT SUM(o.valorEstimado) FROM Oportunidade o WHERE o.status = 'ABERTA' AND o.ativo = true")
    BigDecimal sumValorEstimadoOportunidadesAbertas();

    long countByStatusAndAtivoTrue(Oportunidade.StatusOportunidade status);
}