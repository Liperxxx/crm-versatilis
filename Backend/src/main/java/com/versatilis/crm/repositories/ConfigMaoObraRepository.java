package com.versatilis.crm.repositories;

import com.versatilis.crm.model.ConfigMaoObra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConfigMaoObraRepository extends JpaRepository<ConfigMaoObra, Long> {

    Optional<ConfigMaoObra> findFirstByOrderByIdAsc();
}
