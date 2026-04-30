package com.versatilis.crm.model;

import com.versatilis.crm.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "calculos_marcenaria")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalculoMarcenaria extends BaseEntity {

    @Column(name = "nome", nullable = false, length = 255)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(name = "modo", nullable = false, length = 20)
    private ModoCalculo modo;

    @Column(name = "dias_funcionario", nullable = false, precision = 6, scale = 2)
    private BigDecimal diasFuncionario;

    @Column(name = "dias_ajudante", precision = 6, scale = 2)
    private BigDecimal diasAjudante;

    @Column(name = "custo_diario_funcionario_snapshot", nullable = false, precision = 12, scale = 2)
    private BigDecimal custoDiarioFuncionarioSnapshot;

    @Column(name = "custo_diario_ajudante_snapshot", precision = 12, scale = 2)
    private BigDecimal custoDiarioAjudanteSnapshot;

    @Column(name = "margem_lucro_pct_snapshot", nullable = false, precision = 5, scale = 2)
    private BigDecimal margemLucroPctSnapshot;

    @Column(name = "margem_corte_mm")
    private Integer margemCorteMm;

    @Column(name = "permitir_rotacao")
    private Boolean permitirRotacao;

    @Column(name = "custo_materiais", nullable = false, precision = 12, scale = 2)
    private BigDecimal custoMateriais;

    @Column(name = "custo_acessorios", nullable = false, precision = 12, scale = 2)
    private BigDecimal custoAcessorios;

    @Column(name = "custo_mao_obra", nullable = false, precision = 12, scale = 2)
    private BigDecimal custoMaoObra;

    @Column(name = "custo_producao", nullable = false, precision = 12, scale = 2)
    private BigDecimal custoProducao;

    @Column(name = "valor_lucro", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorLucro;

    @Column(name = "preco_venda", nullable = false, precision = 12, scale = 2)
    private BigDecimal precoVenda;

    @Column(name = "observacoes", columnDefinition = "TEXT")
    private String observacoes;

    @OneToMany(mappedBy = "calculo", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CalculoMarcenariaPeca> pecas = new ArrayList<>();

    @OneToMany(mappedBy = "calculo", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CalculoMarcenariaAcessorio> acessorios = new ArrayList<>();

    @OneToMany(mappedBy = "calculo", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CalculoMarcenariaAreaM2> areas = new ArrayList<>();

    public void addPeca(CalculoMarcenariaPeca p) { pecas.add(p); p.setCalculo(this); }
    public void addAcessorio(CalculoMarcenariaAcessorio a) { acessorios.add(a); a.setCalculo(this); }
    public void addArea(CalculoMarcenariaAreaM2 a) { areas.add(a); a.setCalculo(this); }

    public enum ModoCalculo {
        APROVEITAMENTO,
        METRO_QUADRADO
    }
}
