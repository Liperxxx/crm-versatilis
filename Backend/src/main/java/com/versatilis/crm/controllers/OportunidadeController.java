package com.versatilis.crm.controllers;

import com.versatilis.crm.dto.OportunidadeDTO;
import com.versatilis.crm.dto.ResponseDTO;
import com.versatilis.crm.model.Oportunidade;
import com.versatilis.crm.services.OportunidadeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/oportunidades")
@RequiredArgsConstructor
@Slf4j
@Validated
public class OportunidadeController {

    private final OportunidadeService oportunidadeService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'OPERADOR')")
    public ResponseEntity<ResponseDTO<OportunidadeDTO>> criar(@Valid @RequestBody OportunidadeDTO oportunidadeDTO) {
        log.info("POST /api/oportunidades - Criando nova oportunidade");
        OportunidadeDTO oportunidadeCriada = oportunidadeService.criar(oportunidadeDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ResponseDTO.sucesso("Oportunidade criada com sucesso", oportunidadeCriada));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'OPERADOR')")
    public ResponseEntity<ResponseDTO<OportunidadeDTO>> buscarPorId(@PathVariable Long id) {
        log.info("GET /api/oportunidades/{} - Buscando oportunidade", id);
        OportunidadeDTO oportunidade = oportunidadeService.buscarPorId(id);
        return ResponseEntity.ok(ResponseDTO.sucesso("Oportunidade encontrada", oportunidade));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'OPERADOR')")
    public ResponseEntity<ResponseDTO<Page<OportunidadeDTO>>> listar(
        @RequestParam(required = false) String titulo,
        @RequestParam(required = false) Oportunidade.EtapaOportunidade etapa,
        @RequestParam(required = false) Long usuarioId,
        @RequestParam(required = false) Oportunidade.StatusOportunidade status,
        Pageable pageable) {
        log.info("GET /api/oportunidades - Listando oportunidades com filtros");
        Page<OportunidadeDTO> oportunidades = oportunidadeService.listar(titulo, etapa, usuarioId, status, pageable);
        return ResponseEntity.ok(ResponseDTO.sucesso("Oportunidades listadas com sucesso", oportunidades));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'OPERADOR')")
    public ResponseEntity<ResponseDTO<OportunidadeDTO>> atualizar(
        @PathVariable Long id,
        @Valid @RequestBody OportunidadeDTO oportunidadeDTO) {
        log.info("PUT /api/oportunidades/{} - Atualizando oportunidade", id);
        OportunidadeDTO oportunidadeAtualizada = oportunidadeService.atualizar(id, oportunidadeDTO);
        return ResponseEntity.ok(ResponseDTO.sucesso("Oportunidade atualizada com sucesso", oportunidadeAtualizada));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<ResponseDTO<Void>> deletar(@PathVariable Long id) {
        log.info("DELETE /api/oportunidades/{} - Deletando oportunidade", id);
        oportunidadeService.deletar(id);
        return ResponseEntity.ok(ResponseDTO.sucesso("Oportunidade deletada com sucesso", null));
    }

    @GetMapping("/abertas")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'OPERADOR')")
    public ResponseEntity<ResponseDTO<List<OportunidadeDTO>>> listarAbertas() {
        log.info("GET /api/oportunidades/abertas - Listando oportunidades abertas");
        List<OportunidadeDTO> oportunidades = oportunidadeService.listarAbertas();
        return ResponseEntity.ok(ResponseDTO.sucesso("Oportunidades abertas listadas", oportunidades));
    }

    @GetMapping("/valor-total")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<ResponseDTO<BigDecimal>> calcularValorTotal() {
        log.info("GET /api/oportunidades/valor-total - Calculando valor total");
        BigDecimal valorTotal = oportunidadeService.calcularValorTotal();
        return ResponseEntity.ok(ResponseDTO.sucesso("Valor total calculado", valorTotal));
    }
}