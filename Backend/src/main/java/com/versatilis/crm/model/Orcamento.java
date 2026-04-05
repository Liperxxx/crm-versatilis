package com.versatilis.crm.model;

import com.versatilis.crm.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orcamentos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Orcamento extends BaseEntity {

    @Column(name = "numero", nullable = false, unique = true, length = 20)
    private String numero;

    @Column(name = "data_emissao", nullable = false)
    private LocalDate dataEmissao;

    @Column(name = "data_validade", nullable = false)
    private LocalDate dataValidade;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_orcamento", nullable = false, length = 50)
    @Builder.Default
    private StatusOrcamento status = StatusOrcamento.RASCUNHO;

    @Column(name = "subtotal", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "desconto", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal desconto = BigDecimal.ZERO;

    @Column(name = "total", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "observacoes_comerciais", columnDefinition = "TEXT")
    private String observacoesComerciais;

    @Column(name = "rodape_institucional", columnDefinition = "TEXT")
    private String rodapeInstitucional;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oportunidade_id")
    private Oportunidade oportunidade;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsavel_id")
    private Usuario responsavel;

    @OneToMany(mappedBy = "orcamento", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrcamentoItem> itens = new ArrayList<>();

    public void addItem(OrcamentoItem item) {
        itens.add(item);
        item.setOrcamento(this);
    }

    public void removeItem(OrcamentoItem item) {
        itens.remove(item);
        item.setOrcamento(null);
    }

    public void recalcularTotais() {
        this.subtotal = itens.stream()
            .map(OrcamentoItem::getValorTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.total = this.subtotal.subtract(this.desconto != null ? this.desconto : BigDecimal.ZERO);
    }

    public enum StatusOrcamento {
        RASCUNHO,
        ENVIADO,
        APROVADO,
        RECUSADO
    }
}
