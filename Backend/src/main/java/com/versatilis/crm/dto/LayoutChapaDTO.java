package com.versatilis.crm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LayoutChapaDTO {

    private Long materialId;
    private Integer indiceChapa;
    private Integer larguraChapaMm;
    private Integer alturaChapaMm;
    private List<PecaPosicionadaDTO> pecasPosicionadas;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PecaPosicionadaDTO {
        private String descricao;
        private Integer x;
        private Integer y;
        private Integer larguraMm;
        private Integer alturaMm;
        private Boolean rotacionada;
    }
}
