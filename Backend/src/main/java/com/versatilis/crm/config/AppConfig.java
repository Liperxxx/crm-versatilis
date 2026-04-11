package com.versatilis.crm.config;

import org.modelmapper.ModelMapper;
import org.modelmapper.config.Configuration.AccessLevel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper mapper = new ModelMapper();

        // Evita que o ModelMapper tente acessar coleções lazy do JPA
        // (oportunidades, tarefas, etc.) que causam LazyInitializationException
        mapper.getConfiguration()
            .setFieldMatchingEnabled(true)
            .setFieldAccessLevel(AccessLevel.PRIVATE)
            .setPropertyCondition(context -> {
                // Ignora propriedades que são coleções Hibernate não inicializadas
                Object source = context.getSource();
                if (source instanceof org.hibernate.collection.spi.PersistentCollection) {
                    return ((org.hibernate.collection.spi.PersistentCollection<?>) source).wasInitialized();
                }
                return true;
            });

        return mapper;
    }
}