package com.versatilis.crm.model;

import com.versatilis.crm.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "leads")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lead extends BaseEntity {

    @Column(name = "nome_contato", nullable = false, length = 100)
    private String nomeContato;

    @Column(name = "email", unique = true, length = 100)
    private String email;

    @Column(name = "telefone", length = 20)
    private String telefone;

    @Column(name = "empresa", length = 255)
    private String empresa;

    @Enumerated(EnumType.STRING)
    @Column(name = "origem", nullable = false, length = 50)
    private OrigemLead origem;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_lead", nullable = false, length = 50)
    @Builder.Default
    private StatusLead status = StatusLead.NOVO;

    @Column(name = "score")
    @Builder.Default
    private Integer score = 0;

    @Column(name = "observacoes", columnDefinition = "TEXT")
    private String observacoes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsavel_id")
    private Usuario responsavel;

    @OneToOne(mappedBy = "leadOriginal", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private HistoricoConversao historicoConversao;

    public enum OrigemLead {
        SITE,
        TELEFONE,
        EMAIL,
        EVENTO,
        INDICACAO,
        OUTRO
    }

    public enum StatusLead {
        NOVO,
        QUALIFICADO,
        EM_CONTATO,
        PROPOSTA_ENVIADA,
        PERDIDO,
        CONVERTIDO
    }
}