package com.versatilis.crm.repositories;

import com.versatilis.crm.model.ConfiguracaoEmpresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConfiguracaoEmpresaRepository extends JpaRepository<ConfiguracaoEmpresa, Long> {
    Optional<ConfiguracaoEmpresa> findByChave(String chave);
}
