package com.versatilis.crm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResponseDTO<T> {
    private boolean sucesso;
    private String mensagem;
    private Integer status;
    private T dados;

    public static <T> ResponseDTO<T> sucesso(String mensagem, T dados) {
        return ResponseDTO.<T>builder()
            .sucesso(true)
            .mensagem(mensagem)
            .status(200)
            .dados(dados)
            .build();
    }

    public static <T> ResponseDTO<T> sucesso(String mensagem, T dados, Integer status) {
        return ResponseDTO.<T>builder()
            .sucesso(true)
            .mensagem(mensagem)
            .status(status)
            .dados(dados)
            .build();
    }

    public static <T> ResponseDTO<T> erro(String mensagem, Integer status) {
        return ResponseDTO.<T>builder()
            .sucesso(false)
            .mensagem(mensagem)
            .status(status)
            .dados(null)
            .build();
    }
}