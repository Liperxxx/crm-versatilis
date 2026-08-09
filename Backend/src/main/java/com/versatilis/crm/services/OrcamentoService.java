package com.versatilis.crm.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.versatilis.crm.dto.OrcamentoDTO;
import com.versatilis.crm.dto.OrcamentoItemDTO;
import com.versatilis.crm.exceptions.BadRequestException;
import com.versatilis.crm.exceptions.ResourceNotFoundException;
import com.versatilis.crm.model.*;
import com.versatilis.crm.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrcamentoService {

    private final OrcamentoRepository orcamentoRepository;
    private final ClienteRepository clienteRepository;
    private final OportunidadeRepository oportunidadeRepository;
    private final ProdutoRepository produtoRepository;
    private final UsuarioRepository usuarioRepository;
    private final com.versatilis.crm.security.UsuarioAtual usuarioAtual;

    @Transactional
    public OrcamentoDTO criar(OrcamentoDTO dto) {
        log.info("Criando novo orçamento para cliente ID: {}", dto.getClienteId());

        Cliente cliente = clienteRepository.findById(dto.getClienteId())
            .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado com ID: " + dto.getClienteId()));

        Orcamento orcamento = new Orcamento();
        orcamento.setNumero(gerarNumero());
        orcamento.setDataEmissao(dto.getDataEmissao() != null ? dto.getDataEmissao() : LocalDate.now());
        orcamento.setDataValidade(dto.getDataValidade());
        orcamento.setStatus(dto.getStatus() != null ? dto.getStatus() : Orcamento.StatusOrcamento.RASCUNHO);
        orcamento.setObservacoesComerciais(dto.getObservacoesComerciais());
        orcamento.setRodapeInstitucional(dto.getRodapeInstitucional());
        orcamento.setDesconto(dto.getDesconto() != null ? dto.getDesconto() : BigDecimal.ZERO);
        orcamento.setValorTotalManual(dto.getValorTotalManual());
        orcamento.setFotosUrls(serializeFotosUrls(dto.getFotosUrls()));
        orcamento.setCliente(cliente);

        if (dto.getOportunidadeId() != null) {
            Oportunidade oportunidade = oportunidadeRepository.findById(dto.getOportunidadeId())
                .orElseThrow(() -> new ResourceNotFoundException("Oportunidade não encontrada com ID: " + dto.getOportunidadeId()));
            orcamento.setOportunidade(oportunidade);
        }

        if (dto.getResponsavelId() != null) {
            Usuario responsavel = usuarioRepository.findById(dto.getResponsavelId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário responsável não encontrado"));
            orcamento.setResponsavel(responsavel);
        }

        if (dto.getItens() != null) {
            for (OrcamentoItemDTO itemDto : dto.getItens()) {
                OrcamentoItem item = buildItem(itemDto);
                orcamento.addItem(item);
            }
        }

        // Autoria: registra quem está criando (para o admin acompanhar).
        orcamento.setCriadoPor(usuarioAtual.get());

        orcamento.recalcularTotais();
        orcamento = orcamentoRepository.save(orcamento);
        log.info("Orçamento {} criado com sucesso. ID: {}", orcamento.getNumero(), orcamento.getId());
        return toDTO(orcamento);
    }

    @Transactional(readOnly = true)
    public OrcamentoDTO buscarPorId(Long id) {
        log.info("Buscando orçamento por ID: {}", id);
        Orcamento orcamento = orcamentoRepository.findByIdWithItens(id)
            .orElseThrow(() -> new ResourceNotFoundException("Orçamento não encontrado com ID: " + id));
        return toDTO(orcamento);
    }

    @Transactional(readOnly = true)
    public Page<OrcamentoDTO> listar(Orcamento.StatusOrcamento status, Long clienteId, Long oportunidadeId, Pageable pageable) {
        log.info("Listando orçamentos com filtros: status={}, clienteId={}, oportunidadeId={}", status, clienteId, oportunidadeId);
        Page<Orcamento> orcamentos = orcamentoRepository.findByFilters(status, clienteId, oportunidadeId, pageable);
        return orcamentos.map(this::toDTO);
    }

    @Transactional
    public OrcamentoDTO atualizar(Long id, OrcamentoDTO dto) {
        log.info("Atualizando orçamento ID: {}", id);
        Orcamento orcamento = orcamentoRepository.findByIdWithItens(id)
            .orElseThrow(() -> new ResourceNotFoundException("Orçamento não encontrado com ID: " + id));

        Cliente cliente = clienteRepository.findById(dto.getClienteId())
            .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado com ID: " + dto.getClienteId()));
        orcamento.setCliente(cliente);

        orcamento.setDataEmissao(dto.getDataEmissao());
        orcamento.setDataValidade(dto.getDataValidade());
        orcamento.setStatus(dto.getStatus() != null ? dto.getStatus() : orcamento.getStatus());
        orcamento.setObservacoesComerciais(dto.getObservacoesComerciais());
        orcamento.setRodapeInstitucional(dto.getRodapeInstitucional());
        orcamento.setDesconto(dto.getDesconto() != null ? dto.getDesconto() : BigDecimal.ZERO);
        orcamento.setValorTotalManual(dto.getValorTotalManual());
        orcamento.setFotosUrls(serializeFotosUrls(dto.getFotosUrls()));

        if (dto.getOportunidadeId() != null) {
            Oportunidade oportunidade = oportunidadeRepository.findById(dto.getOportunidadeId())
                .orElseThrow(() -> new ResourceNotFoundException("Oportunidade não encontrada"));
            orcamento.setOportunidade(oportunidade);
        } else {
            orcamento.setOportunidade(null);
        }

        if (dto.getResponsavelId() != null) {
            Usuario responsavel = usuarioRepository.findById(dto.getResponsavelId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário responsável não encontrado"));
            orcamento.setResponsavel(responsavel);
        } else {
            orcamento.setResponsavel(null);
        }

        // Replace items
        orcamento.getItens().clear();
        if (dto.getItens() != null) {
            for (OrcamentoItemDTO itemDto : dto.getItens()) {
                OrcamentoItem item = buildItem(itemDto);
                orcamento.addItem(item);
            }
        }

        orcamento.recalcularTotais();
        orcamento = orcamentoRepository.save(orcamento);
        log.info("Orçamento ID {} atualizado com sucesso.", id);
        return toDTO(orcamento);
    }

    @Transactional
    public void deletar(Long id) {
        log.info("Deletando orçamento ID: {}", id);
        Orcamento orcamento = orcamentoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Orçamento não encontrado com ID: " + id));
        orcamento.setAtivo(false);
        orcamentoRepository.save(orcamento);
        log.info("Orçamento ID {} desativado com sucesso.", id);
    }

    // ── Helpers ─────────────────────────────────────────────────────

    private synchronized String gerarNumero() {
        String prefix = "ORC-";
        int maxNum = orcamentoRepository.findMaxNumeroByPrefix(prefix);
        return prefix + String.format("%04d", maxNum + 1);
    }

    private OrcamentoItem buildItem(OrcamentoItemDTO dto) {
        OrcamentoItem item = new OrcamentoItem();
        item.setDescricao(dto.getDescricao());
        // Null-safe: quantidade default 1, valor unitário default 0.
        int quantidade = dto.getQuantidade() != null ? dto.getQuantidade() : 1;
        BigDecimal valorUnitario = dto.getValorUnitario() != null ? dto.getValorUnitario() : BigDecimal.ZERO;
        item.setQuantidade(quantidade);
        item.setValorUnitario(valorUnitario);
        item.setValorTotal(valorUnitario.multiply(BigDecimal.valueOf(quantidade)));

        if (dto.getProdutoId() != null) {
            Produto produto = produtoRepository.findById(dto.getProdutoId())
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado com ID: " + dto.getProdutoId()));
            item.setProduto(produto);
            if (dto.getDescricao() == null || dto.getDescricao().isBlank()) {
                item.setDescricao(produto.getNome());
            }
        }
        return item;
    }

    private OrcamentoDTO toDTO(Orcamento o) {
        OrcamentoDTO dto = new OrcamentoDTO();
        dto.setId(o.getId());
        dto.setNumero(o.getNumero());
        dto.setDataEmissao(o.getDataEmissao());
        dto.setDataValidade(o.getDataValidade());
        dto.setStatus(o.getStatus());
        dto.setSubtotal(o.getSubtotal());
        dto.setDesconto(o.getDesconto());
        dto.setTotal(o.getTotal());
        dto.setValorTotalManual(o.getValorTotalManual());
        dto.setFotosUrls(deserializeFotosUrls(o.getFotosUrls()));
        dto.setObservacoesComerciais(o.getObservacoesComerciais());
        dto.setRodapeInstitucional(o.getRodapeInstitucional());

        if (o.getCliente() != null) {
            dto.setClienteId(o.getCliente().getId());
            dto.setClienteNome(o.getCliente().getNomeEmpresa());
            dto.setClienteCnpj(o.getCliente().getCnpj());
            dto.setClienteEndereco(o.getCliente().getEndereco());
            dto.setClienteObservacoes(o.getCliente().getObservacoes());
            dto.setClienteCidade(o.getCliente().getCidade());
            dto.setClienteEstado(o.getCliente().getEstado());
            dto.setClienteEmail(o.getCliente().getEmail());
            dto.setClienteTelefone(o.getCliente().getTelefone());
        }
        if (o.getOportunidade() != null) {
            dto.setOportunidadeId(o.getOportunidade().getId());
            dto.setOportunidadeTitulo(o.getOportunidade().getTitulo());
        }
        if (o.getResponsavel() != null) {
            dto.setResponsavelId(o.getResponsavel().getId());
            dto.setResponsavelNome(o.getResponsavel().getNome());
        }
        if (o.getCriadoPor() != null) {
            dto.setCriadoPorId(o.getCriadoPor().getId());
            dto.setCriadoPorNome(o.getCriadoPor().getNome());
        }

        List<OrcamentoItemDTO> itensDto = new ArrayList<>();
        if (o.getItens() != null) {
            for (OrcamentoItem item : o.getItens()) {
                OrcamentoItemDTO iDto = new OrcamentoItemDTO();
                iDto.setId(item.getId());
                iDto.setDescricao(item.getDescricao());
                iDto.setQuantidade(item.getQuantidade());
                iDto.setValorUnitario(item.getValorUnitario());
                iDto.setValorTotal(item.getValorTotal());
                if (item.getProduto() != null) {
                    iDto.setProdutoId(item.getProduto().getId());
                    iDto.setProdutoNome(item.getProduto().getNome());
                }
                itensDto.add(iDto);
            }
        }
        dto.setItens(itensDto);
        return dto;
    }

    // ── Fotos do projeto: serialização em JSON ───────────────────────

    private static final ObjectMapper FOTOS_MAPPER = new ObjectMapper();

    /** Serializa a lista de URLs como JSON array para persistir em fotos_urls. */
    private String serializeFotosUrls(List<String> urls) {
        if (urls == null || urls.isEmpty()) return null;
        try {
            return FOTOS_MAPPER.writeValueAsString(urls);
        } catch (Exception e) {
            log.warn("Falha ao serializar fotos do orçamento: {}", e.getMessage());
            return null;
        }
    }

    /** Deserializa o JSON de URLs; retorna lista vazia se nulo ou inválido. */
    private List<String> deserializeFotosUrls(String raw) {
        if (raw == null || raw.isBlank()) return Collections.emptyList();
        try {
            return FOTOS_MAPPER.readValue(raw, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("Falha ao deserializar fotos do orçamento (raw={}): {}", raw, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Transactional
    public void marcarComoEnviado(Long id) {
        Orcamento o = orcamentoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Orçamento não encontrado com ID: " + id));
        if (o.getStatus() == Orcamento.StatusOrcamento.RASCUNHO) {
            o.setStatus(Orcamento.StatusOrcamento.ENVIADO);
            orcamentoRepository.save(o);
        }
    }

    /**
     * Altera apenas o status do orçamento, sem tocar em itens/valores. Usado pela
     * seleção inline na listagem — evita o PUT completo (que exigiria reenviar
     * cliente, itens e datas só para mudar o status).
     */
    @Transactional
    public OrcamentoDTO atualizarStatus(Long id, Orcamento.StatusOrcamento status) {
        log.info("Atualizando status do orçamento ID {} para {}", id, status);
        if (status == null) {
            throw new BadRequestException("Status é obrigatório");
        }
        Orcamento o = orcamentoRepository.findByIdWithItens(id)
            .orElseThrow(() -> new ResourceNotFoundException("Orçamento não encontrado com ID: " + id));
        o.setStatus(status);
        o = orcamentoRepository.save(o);
        return toDTO(o);
    }
}
