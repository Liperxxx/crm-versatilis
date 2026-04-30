package com.versatilis.crm.repositories;

import com.versatilis.crm.model.MaterialMarcenaria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaterialMarcenariaRepository extends JpaRepository<MaterialMarcenaria, Long> {

    @Query("SELECT m FROM MaterialMarcenaria m WHERE " +
           "(:categoria IS NULL OR m.categoria = :categoria) AND " +
           "m.ativo = true " +
           "ORDER BY m.nome ASC")
    List<MaterialMarcenaria> findByFilters(
        @Param("categoria") MaterialMarcenaria.CategoriaMaterial categoria
    );

    long countByAtivoTrue();
}
