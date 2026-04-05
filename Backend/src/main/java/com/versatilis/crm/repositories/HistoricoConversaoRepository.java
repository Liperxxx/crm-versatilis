package com.versatilis.crm.repositories;

import com.versatilis.crm.model.HistoricoConversao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HistoricoConversaoRepository extends JpaRepository<HistoricoConversao, Long> {
    Optional<HistoricoConversao> findByLeadOriginalId(Long leadId);
}