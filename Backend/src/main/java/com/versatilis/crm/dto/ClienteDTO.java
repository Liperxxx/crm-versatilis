package com.versatilis.crm.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.versatilis.crm.model.Cliente;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClienteDTO {

    private Long id;

    // JSON field "nome" → Java field nomeEmpresa (frontend uses "nome")
    @JsonProperty("nome")
    @NotBlank(message = "Nome da empresa é obrigatório")
    private String nomeEmpresa;

    // CNPJ é opcional — frontend não coleta
    private String cnpj;

    // Contato principal é opcional no fluxo do frontend
    private String contatoPrincipal;

    @Email(message = "Email deve ser válido")
    private String email;

    private String telefone;

    private String segmento;

    private String observacoes;

    private String endereco;
    private String cidade;
    private String estado;

    private Cliente.StatusCliente status;

    // Data de cadastro em formato yyyy-MM-dd (somente leitura — mapeada de dataCriacao)
    private String desde;

    private Long responsavelId;
    private String responsavelNome;
}