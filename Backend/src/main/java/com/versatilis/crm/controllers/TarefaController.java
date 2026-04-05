package com.versatilis.crm.controllers;

import com.versatilis.crm.dto.TarefaDTO;
import com.versatilis.crm.dto.ResponseDTO;
import com.versatilis.crm.model.Tarefa;
import com.versatilis.crm.services.TarefaService;
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
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/tarefas")
@RequiredArgsConstructor
@Slf4j
@Validated
public class TarefaController {

    private final TarefaService tarefaService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'OPERADOR')")
    public ResponseEntity<ResponseDTO<TarefaDTO>> criar(@Valid @RequestBody TarefaDTO tarefaDTO) {
        log.info("POST /api/tarefas - Criando nova tarefa");
        TarefaDTO tarefaCriada = tarefaService.criar(tarefaDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ResponseDTO.sucesso("Tarefa criada com sucesso", tarefaCriada));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'OPERADOR')")
    public ResponseEntity<ResponseDTO<TarefaDTO>> buscarPorId(@PathVariable Long id) {
        log.info("GET /api/tarefas/{} - Buscando tarefa", id);
        TarefaDTO tarefa = tarefaService.buscarPorId(id);
        return ResponseEntity.ok(ResponseDTO.sucesso("Tarefa encontrada", tarefa));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'OPERADOR')")
    public ResponseEntity<ResponseDTO<Page<TarefaDTO>>> listar(
        @RequestParam(required = false) String titulo,
        @RequestParam(required = false) Tarefa.StatusTarefa status,
        @RequestParam(required = false) Long usuarioId,
        @RequestParam(required = false) Tarefa.PrioridadeTarefa prioridade,
        @RequestParam(required = false) LocalDate dataInicio,
        @RequestParam(required = false) LocalDate dataFim,
        Pageable pageable) {
        log.info("GET /api/tarefas - Listando tarefas com filtros");
        Page<TarefaDTO> tarefas = tarefaService.listar(titulo, status, usuarioId, prioridade, dataInicio, dataFim, pageable);
        return ResponseEntity.ok(ResponseDTO.sucesso("Tarefas listadas com sucesso", tarefas));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'OPERADOR')")
    public ResponseEntity<ResponseDTO<TarefaDTO>> atualizar(
        @PathVariable Long id,
        @Valid @RequestBody TarefaDTO tarefaDTO) {
        log.info("PUT /api/tarefas/{} - Atualizando tarefa", id);
        TarefaDTO tarefaAtualizada = tarefaService.atualizar(id, tarefaDTO);
        return ResponseEntity.ok(ResponseDTO.sucesso("Tarefa atualizada com sucesso", tarefaAtualizada));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<ResponseDTO<Void>> deletar(@PathVariable Long id) {
        log.info("DELETE /api/tarefas/{} - Deletando tarefa", id);
        tarefaService.deletar(id);
        return ResponseEntity.ok(ResponseDTO.sucesso("Tarefa deletada com sucesso", null));
    }

    @PatchMapping("/{id}/concluir")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'OPERADOR')")
    public ResponseEntity<ResponseDTO<TarefaDTO>> marcarConcluida(@PathVariable Long id) {
        log.info("PATCH /api/tarefas/{}/concluir - Marcando tarefa como concluída", id);
        TarefaDTO tarefaConcluida = tarefaService.marcarConcluida(id);
        return ResponseEntity.ok(ResponseDTO.sucesso("Tarefa marcada como concluída", tarefaConcluida));
    }

    @GetMapping("/vencidas")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'OPERADOR')")
    public ResponseEntity<ResponseDTO<List<TarefaDTO>>> listarVencidas() {
        log.info("GET /api/tarefas/vencidas - Listando tarefas vencidas");
        List<TarefaDTO> tarefas = tarefaService.listarVencidas();
        return ResponseEntity.ok(ResponseDTO.sucesso("Tarefas vencidas listadas", tarefas));
    }

    @GetMapping("/pendentes/{usuarioId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'OPERADOR')")
    public ResponseEntity<ResponseDTO<List<TarefaDTO>>> listarPendentes(@PathVariable Long usuarioId) {
        log.info("GET /api/tarefas/pendentes/{} - Listando tarefas pendentes", usuarioId);
        List<TarefaDTO> tarefas = tarefaService.listarPendentes(usuarioId);
        return ResponseEntity.ok(ResponseDTO.sucesso("Tarefas pendentes listadas", tarefas));
    }
}