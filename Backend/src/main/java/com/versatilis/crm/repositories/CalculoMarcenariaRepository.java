package com.versatilis.crm.repositories;

import com.versatilis.crm.model.CalculoMarcenaria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CalculoMarcenariaRepository extends JpaRepository<CalculoMarcenaria, Long> {

    @Query("SELECT c FROM CalculoMarcenaria c WHERE " +
           "(:modo IS NULL OR c.modo = :modo) AND " +
           "(:nome IS NULL OR LOWER(c.nome) LIKE LOWER(CONCAT('%', :nome, '%'))) AND " +
           "c.ativo = true")
    Page<CalculoMarcenaria> findByFilters(
        @Param("modo") CalculoMarcenaria.ModoCalculo modo,
        @Param("nome") String nome,
        Pageable pageable
    );

    @Query("SELECT c FROM CalculoMarcenaria c " +
           "LEFT JOIN FETCH c.pecas " +
           "WHERE c.id = :id AND c.ativo = true")
    Optional<CalculoMarcenaria> findByIdWithPecas(@Param("id") Long id);
}
