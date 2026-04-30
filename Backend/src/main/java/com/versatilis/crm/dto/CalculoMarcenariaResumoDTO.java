package com.versatilis.crm.dto;

import com.versatilis.crm.model.CalculoMarcenaria;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Item da listagem de cálculos salvos — só os campos da tabela. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalculoMarcenariaResumoDTO {

    private Long id;
    private String nome;
    private CalculoMarcenaria.ModoCalculo modo;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataAtualizacao;
    private BigDecimal custoProducao;
    private BigDecimal precoVenda;
}
