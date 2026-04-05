package com.versatilis.crm.repositories;

import com.versatilis.crm.model.Produto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProdutoRepository extends JpaRepository<Produto, Long> {
    Optional<Produto> findByNome(String nome);

    @Query("SELECT p FROM Produto p WHERE " +
           "(:nome IS NULL OR p.nome LIKE %:nome%) AND " +
           "(:categoria IS NULL OR p.categoria LIKE %:categoria%) AND " +
           "(:status IS NULL OR p.status = :status) AND " +
           "p.ativo = true")
    Page<Produto> findByFilters(
        @Param("nome") String nome,
        @Param("categoria") String categoria,
        @Param("status") Produto.StatusProduto status,
        Pageable pageable);

    List<Produto> findByEstoqueLessThan(Integer quantidadeMinima);
}