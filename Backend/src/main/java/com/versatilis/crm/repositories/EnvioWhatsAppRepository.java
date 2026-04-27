package com.versatilis.crm.repositories;

import com.versatilis.crm.model.EnvioWhatsApp;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnvioWhatsAppRepository extends JpaRepository<EnvioWhatsApp, Long> {

    Optional<EnvioWhatsApp> findByMessageId(String messageId);

    List<EnvioWhatsApp> findByOrcamentoIdOrderByDataEnvioDesc(Long orcamentoId);

    Page<EnvioWhatsApp> findByOrcamentoIdAndAtivoTrue(Long orcamentoId, Pageable pageable);
}
