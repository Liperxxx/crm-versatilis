package com.versatilis.crm.model;

import com.versatilis.crm.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "tarefas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tarefa extends BaseEntity {

    @Column(name = "titulo", nullable = false, length = 255)
    private String titulo;

    @Column(name = "descricao", columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "data_vencimento")
    private LocalDate dataVencimento;

    @Enumerated(EnumType.STRING)
    @Column(name = "prioridade", nullable = false, length = 50)
    @Builder.Default
    private PrioridadeTarefa prioridade = PrioridadeTarefa.MEDIA;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_tarefa", nullable = false, length = 50)
    @Builder.Default
    private StatusTarefa status = StatusTarefa.PENDENTE;

    @Column(name = "observacoes", columnDefinition = "TEXT")
    private String observacoes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id")
    private Lead lead;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oportunidade_id")
    private Oportunidade oportunidade;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsavel_id", nullable = false)
    private Usuario responsavel;

    public enum PrioridadeTarefa {
        BAIXA,
        MEDIA,
        ALTA,
        URGENTE
    }

    public enum StatusTarefa {
        PENDENTE,
        EM_PROCESSO,
        CONCLUIDA,
        CANCELADA
    }
}