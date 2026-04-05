package com.versatilis.crm.dto;

import com.versatilis.crm.model.Lead;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeadDTO {

    private Long id;

    @NotBlank(message = "Nome do contato é obrigatório")
    private String nomeContato;

    @Email(message = "Email deve ser válido")
    private String email;

    @Pattern(regexp = "^\\(?\\d{2}\\)?\\s?\\d{4,5}-?\\d{4}$", message = "Telefone inválido")
    private String telefone;

    private String empresa;

    @NotNull(message = "Origem do lead é obrigatória")
    private Lead.OrigemLead origem;

    private Lead.StatusLead status;
    private Integer score;
    private String observacoes;
    private Long responsavelId;
    private String responsavelNome;
}