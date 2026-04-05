package com.versatilis.crm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioPerfilDTO {

    private Long id;
    private String nome;
    private String email;
    private String cargo;
    private String papel;
    private String telefone;
    private LocalDateTime ultimoAcesso;
    private LocalDateTime dataCriacao;
}
