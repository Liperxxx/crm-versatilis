package com.versatilis.crm.dto;

import com.versatilis.crm.model.Tarefa;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TarefaDTO {

    private Long id;

    @NotBlank(message = "Título da tarefa é obrigatório")
    private String titulo;

    private String descricao;
    private LocalDate dataVencimento;

    @NotNull(message = "Prioridade é obrigatória")
    private Tarefa.PrioridadeTarefa prioridade;

    private Tarefa.StatusTarefa status;

    private Long clienteId;
    private String clienteNome;

    private Long leadId;
    private String leadNome;

    private Long oportunidadeId;
    private String oportunidadeTitulo;

    private String observacoes;

    @NotNull(message = "Responsável é obrigatório")
    private Long responsavelId;
    private String responsavelNome;
}