package com.versatilis.crm.services;

import com.versatilis.crm.dto.AreaPorMaterialDTO;
import com.versatilis.crm.dto.CalculoMarcenariaDTO;
import com.versatilis.crm.dto.CalculoMarcenariaResumoDTO;
import com.versatilis.crm.dto.CalculoMarcenariaSaveDTO;
import com.versatilis.crm.dto.ItemAcessorioDTO;
import com.versatilis.crm.dto.PecaDTO;
import com.versatilis.crm.exceptions.BadRequestException;
import com.versatilis.crm.exceptions.ResourceNotFoundException;
import com.versatilis.crm.model.AcessorioMarcenaria;
import com.versatilis.crm.model.CalculoMarcenaria;
import com.versatilis.crm.model.CalculoMarcenariaAcessorio;
import com.versatilis.crm.model.CalculoMarcenariaAreaM2;
import com.versatilis.crm.model.CalculoMarcenariaPeca;
import com.versatilis.crm.model.ConfigMaoObra;
import com.versatilis.crm.model.MaterialMarcenaria;
import com.versatilis.crm.repositories.AcessorioMarcenariaRepository;
import com.versatilis.crm.repositories.CalculoMarcenariaRepository;
import com.versatilis.crm.repositories.ConfigMaoObraRepository;
import com.versatilis.crm.repositories.MaterialMarcenariaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Persistência de cálculos de marcenaria.
 *
 * Os totais (materiais, acessórios) recebidos no save são confiáveis no
 * sentido de que o cliente já calculou via packer 2D. O backend não
 * recalcula o aproveitamento — apenas valida valores monetários derivados
 * (mão de obra, margem) usando os snapshots da configuração no momento do save.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CalculoMarcenariaService {

    private static final BigDecimal CEM = new BigDecimal("100");

    private final CalculoMarcenariaRepository repository;
    private final ConfigMaoObraRepository configRepo;
    private final MaterialMarcenariaRepository materialRepo;
    private final AcessorioMarcenariaRepository acessorioRepo;

    @Transactional
    public CalculoMarcenariaDTO criar(CalculoMarcenariaSaveDTO dto) {
        log.info("Salvando novo cálculo de marcenaria: nome='{}', modo={}", dto.getNome(), dto.getModo());
        ConfigMaoObra config = obterConfig();

        CalculoMarcenaria entity = new CalculoMarcenaria();
        entity.setNome(dto.getNome().trim());
        aplicarCampos(entity, dto, config);
        entity = repository.save(entity);
        log.info("Cálculo salvo. ID={}", entity.getId());
        return toDTO(entity);
    }

    @Transactional
    public CalculoMarcenariaDTO atualizar(Long id, CalculoMarcenariaSaveDTO dto) {
        log.info("Atualizando cálculo de marcenaria. ID={}", id);
        CalculoMarcenaria entity = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Cálculo não encontrado com ID: " + id));
        if (Boolean.FALSE.equals(entity.getAtivo())) {
            throw new BadRequestException("Cálculo desativado não pode ser atualizado.");
        }
        ConfigMaoObra config = obterConfig();

        entity.setNome(dto.getNome().trim());
        // Limpa coleções existentes (orphanRemoval cuida da exclusão)
        entity.getPecas().clear();
        entity.getAcessorios().clear();
        entity.getAreas().clear();

        aplicarCampos(entity, dto, config);
        entity = repository.save(entity);
        return toDTO(entity);
    }

    @Transactional(readOnly = true)
    public CalculoMarcenariaDTO buscarPorId(Long id) {
        log.info("Buscando cálculo por ID: {}", id);
        CalculoMarcenaria entity = repository.findByIdWithPecas(id)
            .orElseThrow(() -> new ResourceNotFoundException("Cálculo não encontrado com ID: " + id));
        return toDTO(entity);
    }

    @Transactional(readOnly = true)
    public Page<CalculoMarcenariaResumoDTO> listar(CalculoMarcenaria.ModoCalculo modo, String nome, Pageable pageable) {
        log.info("Listando cálculos. modo={}, nome={}", modo, nome);
        return repository.findByFilters(modo, nome, pageable).map(this::toResumoDTO);
    }

    @Transactional
    public void deletar(Long id) {
        log.info("Deletando (soft) cálculo de marcenaria ID: {}", id);
        CalculoMarcenaria entity = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Cálculo não encontrado com ID: " + id));
        entity.setAtivo(false);
        repository.save(entity);
        log.info("Cálculo ID {} desativado.", id);
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private ConfigMaoObra obterConfig() {
        return configRepo.findFirstByOrderByIdAsc()
            .orElseThrow(() -> new BadRequestException(
                "Configuração de mão de obra não encontrada. Acesse Cadastros para configurá-la."));
    }

    /**
     * Aplica todos os campos do DTO na entidade, calcula os totais derivados
     * (mão de obra, custoProducao, lucro, precoVenda) e popula as filhas com snapshots.
     */
    private void aplicarCampos(CalculoMarcenaria entity, CalculoMarcenariaSaveDTO dto, ConfigMaoObra config) {
        BigDecimal custoMateriais = dto.getCustoMateriais();
        BigDecimal custoAcessorios = dto.getCustoAcessorios();
        entity.setModo(dto.getModo());
        entity.setObservacoes(dto.getObservacoes());
        entity.setMargemCorteMm(dto.getMargemCorteMm());
        entity.setPermitirRotacao(dto.getPermitirRotacao());

        BigDecimal diasFunc = nz(dto.getDiasFuncionario());
        BigDecimal diasAjud = nz(dto.getDiasAjudante());
        entity.setDiasFuncionario(diasFunc);
        entity.setDiasAjudante(isPositive(diasAjud) ? diasAjud : null);

        BigDecimal custoDiarioFunc = nz(config.getCustoDiario());
        BigDecimal custoDiarioAjud = config.getCustoDiarioAjudante();
        entity.setCustoDiarioFuncionarioSnapshot(custoDiarioFunc);
        entity.setCustoDiarioAjudanteSnapshot(isPositive(custoDiarioAjud) ? custoDiarioAjud : null);

        BigDecimal margemPct = dto.getMargemLucroPct() != null
            ? dto.getMargemLucroPct()
            : nz(config.getMargemLucroPadraoPct());
        entity.setMargemLucroPctSnapshot(margemPct);

        // Mão de obra
        BigDecimal custoFunc = custoDiarioFunc.multiply(diasFunc);
        BigDecimal custoAjud = (isPositive(diasAjud) && isPositive(custoDiarioAjud))
            ? custoDiarioAjud.multiply(diasAjud)
            : BigDecimal.ZERO;
        BigDecimal custoMaoObra = custoFunc.add(custoAjud);

        BigDecimal cm = nz(custoMateriais);
        BigDecimal ca = nz(custoAcessorios);
        BigDecimal custoProducao = cm.add(ca).add(custoMaoObra);
        BigDecimal valorLucro = custoProducao.multiply(margemPct).divide(CEM, 2, RoundingMode.HALF_UP);
        BigDecimal precoVenda = custoProducao.add(valorLucro);

        entity.setCustoMateriais(cm.setScale(2, RoundingMode.HALF_UP));
        entity.setCustoAcessorios(ca.setScale(2, RoundingMode.HALF_UP));
        entity.setCustoMaoObra(custoMaoObra.setScale(2, RoundingMode.HALF_UP));
        entity.setCustoProducao(custoProducao.setScale(2, RoundingMode.HALF_UP));
        entity.setValorLucro(valorLucro.setScale(2, RoundingMode.HALF_UP));
        entity.setPrecoVenda(precoVenda.setScale(2, RoundingMode.HALF_UP));

        // Filhas (com snapshots de nome/preço)
        if (dto.getModo() == CalculoMarcenaria.ModoCalculo.APROVEITAMENTO && dto.getPecas() != null) {
            for (PecaDTO p : dto.getPecas()) {
                MaterialMarcenaria m = materialRepo.findById(p.getMaterialId()).orElse(null);
                CalculoMarcenariaPeca peca = CalculoMarcenariaPeca.builder()
                    .material(m)
                    .materialNomeSnapshot(m != null ? m.getNome() : null)
                    .precoChapaSnapshot(m != null ? m.getPrecoChapa() : null)
                    .larguraMm(p.getLarguraMm())
                    .alturaMm(p.getAlturaMm())
                    .quantidade(p.getQuantidade())
                    .descricao(p.getDescricao())
                    .build();
                entity.addPeca(peca);
            }
        }
        if (dto.getModo() == CalculoMarcenaria.ModoCalculo.METRO_QUADRADO && dto.getAreasPorMaterial() != null) {
            for (AreaPorMaterialDTO a : dto.getAreasPorMaterial()) {
                MaterialMarcenaria m = materialRepo.findById(a.getMaterialId()).orElse(null);
                CalculoMarcenariaAreaM2 area = CalculoMarcenariaAreaM2.builder()
                    .material(m)
                    .materialNomeSnapshot(m != null ? m.getNome() : null)
                    .precoChapaSnapshot(m != null ? m.getPrecoChapa() : null)
                    .larguraChapaSnapshot(m != null ? m.getLarguraChapaMm() : null)
                    .alturaChapaSnapshot(m != null ? m.getAlturaChapaMm() : null)
                    .areaM2(a.getAreaM2())
                    .build();
                entity.addArea(area);
            }
        }
        if (dto.getAcessorios() != null) {
            for (ItemAcessorioDTO item : dto.getAcessorios()) {
                AcessorioMarcenaria ac = acessorioRepo.findById(item.getAcessorioId()).orElse(null);
                CalculoMarcenariaAcessorio acSalvo = CalculoMarcenariaAcessorio.builder()
                    .acessorio(ac)
                    .acessorioNomeSnapshot(ac != null ? ac.getNome() : null)
                    .precoUnitarioSnapshot(ac != null ? ac.getPrecoUnitario() : null)
                    .quantidade(item.getQuantidade())
                    .build();
                entity.addAcessorio(acSalvo);
            }
        }
    }

    private CalculoMarcenariaDTO toDTO(CalculoMarcenaria e) {
        List<CalculoMarcenariaDTO.PecaSalvaDTO> pecas = e.getPecas().stream().map(p ->
            CalculoMarcenariaDTO.PecaSalvaDTO.builder()
                .materialId(p.getMaterial() != null ? p.getMaterial().getId() : null)
                .materialNomeSnapshot(p.getMaterialNomeSnapshot())
                .precoChapaSnapshot(p.getPrecoChapaSnapshot())
                .larguraMm(p.getLarguraMm())
                .alturaMm(p.getAlturaMm())
                .quantidade(p.getQuantidade())
                .descricao(p.getDescricao())
                .build()
        ).collect(Collectors.toList());

        List<CalculoMarcenariaDTO.AreaSalvaDTO> areas = e.getAreas().stream().map(a ->
            CalculoMarcenariaDTO.AreaSalvaDTO.builder()
                .materialId(a.getMaterial() != null ? a.getMaterial().getId() : null)
                .materialNomeSnapshot(a.getMaterialNomeSnapshot())
                .precoChapaSnapshot(a.getPrecoChapaSnapshot())
                .larguraChapaSnapshot(a.getLarguraChapaSnapshot())
                .alturaChapaSnapshot(a.getAlturaChapaSnapshot())
                .areaM2(a.getAreaM2())
                .build()
        ).collect(Collectors.toList());

        List<CalculoMarcenariaDTO.AcessorioSalvoDTO> acessorios = e.getAcessorios().stream().map(ac ->
            CalculoMarcenariaDTO.AcessorioSalvoDTO.builder()
                .acessorioId(ac.getAcessorio() != null ? ac.getAcessorio().getId() : null)
                .acessorioNomeSnapshot(ac.getAcessorioNomeSnapshot())
                .precoUnitarioSnapshot(ac.getPrecoUnitarioSnapshot())
                .quantidade(ac.getQuantidade())
                .build()
        ).collect(Collectors.toList());

        return CalculoMarcenariaDTO.builder()
            .id(e.getId())
            .nome(e.getNome())
            .modo(e.getModo())
            .dataCriacao(e.getDataCriacao())
            .dataAtualizacao(e.getDataAtualizacao())
            .diasFuncionario(e.getDiasFuncionario())
            .diasAjudante(e.getDiasAjudante())
            .custoDiarioFuncionarioSnapshot(e.getCustoDiarioFuncionarioSnapshot())
            .custoDiarioAjudanteSnapshot(e.getCustoDiarioAjudanteSnapshot())
            .margemLucroPctSnapshot(e.getMargemLucroPctSnapshot())
            .margemCorteMm(e.getMargemCorteMm())
            .permitirRotacao(e.getPermitirRotacao())
            .custoMateriais(e.getCustoMateriais())
            .custoAcessorios(e.getCustoAcessorios())
            .custoMaoObra(e.getCustoMaoObra())
            .custoProducao(e.getCustoProducao())
            .valorLucro(e.getValorLucro())
            .precoVenda(e.getPrecoVenda())
            .observacoes(e.getObservacoes())
            .pecas(pecas)
            .areas(areas)
            .acessorios(acessorios)
            .build();
    }

    private CalculoMarcenariaResumoDTO toResumoDTO(CalculoMarcenaria e) {
        return CalculoMarcenariaResumoDTO.builder()
            .id(e.getId())
            .nome(e.getNome())
            .modo(e.getModo())
            .dataCriacao(e.getDataCriacao())
            .dataAtualizacao(e.getDataAtualizacao())
            .custoProducao(e.getCustoProducao())
            .precoVenda(e.getPrecoVenda())
            .build();
    }

    private static BigDecimal nz(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }
    private static boolean isPositive(BigDecimal v) { return v != null && v.compareTo(BigDecimal.ZERO) > 0; }
}
