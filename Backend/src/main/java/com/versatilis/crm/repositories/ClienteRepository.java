package com.versatilis.crm.repositories;

import com.versatilis.crm.model.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByCnpj(String cnpj);

    List<Cliente> findByStatus(Cliente.StatusCliente status);

    @Query("SELECT c FROM Cliente c WHERE " +
           "(:nome IS NULL OR c.nomeEmpresa LIKE %:nome%) AND " +
           "(:status IS NULL OR c.status = :status) AND " +
           "(:usuarioId IS NULL OR c.responsavel.id = :usuarioId) AND " +
           "(:cidade IS NULL OR c.cidade LIKE %:cidade%) AND " +
           "(:segmento IS NULL OR c.segmento = :segmento) AND " +
           "c.ativo = true")
    Page<Cliente> findByFilters(
            @Param("nome") String nome,
            @Param("status") Cliente.StatusCliente status,
            @Param("usuarioId") Long usuarioId,
            @Param("cidade") String cidade,
            @Param("segmento") String segmento,
            Pageable pageable
    );
}