package com.versatilis.crm.repositories;

import com.versatilis.crm.model.AcessorioMarcenaria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AcessorioMarcenariaRepository extends JpaRepository<AcessorioMarcenaria, Long> {

    @Query("SELECT a FROM AcessorioMarcenaria a WHERE " +
           "(:categoria IS NULL OR a.categoria = :categoria) AND " +
           "a.ativo = true " +
           "ORDER BY a.nome ASC")
    List<AcessorioMarcenaria> findByFilters(
        @Param("categoria") AcessorioMarcenaria.CategoriaAcessorio categoria
    );

    long countByAtivoTrue();
}
