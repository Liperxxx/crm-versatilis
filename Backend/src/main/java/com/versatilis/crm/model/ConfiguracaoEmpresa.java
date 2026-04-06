package com.versatilis.crm.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "configuracao_empresa")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfiguracaoEmpresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chave", nullable = false, unique = true, length = 100)
    private String chave;

    @Column(name = "valor", length = 1000)
    private String valor;
}
