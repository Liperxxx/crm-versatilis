package com.versatilis.crm.model;

import com.versatilis.crm.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "clientes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cliente extends BaseEntity {

    @Column(name = "nome_empresa", nullable = false, length = 255)
    private String nomeEmpresa;

    @Column(name = "cnpj", unique = true, length = 18)
    private String cnpj;

    @Column(name = "contato_principal", length = 100)
    private String contatoPrincipal;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "telefone", length = 20)
    private String telefone;

    @Column(name = "endereco", length = 255)
    private String endereco;

    @Column(name = "cidade", length = 100)
    private String cidade;

    @Column(name = "estado", length = 50)
    private String estado;

    @Column(name = "segmento", length = 100)
    private String segmento;

    @Column(name = "observacoes", columnDefinition = "TEXT")
    private String observacoes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_cliente", nullable = false, length = 50)
    @Builder.Default
    private StatusCliente status = StatusCliente.Ativo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsavel_id")
    private Usuario responsavel;

    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Oportunidade> oportunidades;

    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Tarefa> tarefas;

    public enum StatusCliente {
        Ativo,
        Inativo,
        Prospecto
    }
}