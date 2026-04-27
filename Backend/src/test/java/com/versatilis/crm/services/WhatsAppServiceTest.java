package com.versatilis.crm.services;

import com.versatilis.crm.dto.OrcamentoDTO;
import com.versatilis.crm.dto.OrcamentoItemDTO;
import com.versatilis.crm.dto.WhatsAppEnvioDTO;
import com.versatilis.crm.dto.WhatsAppEnvioResponseDTO;
import com.versatilis.crm.exceptions.BadRequestException;
import com.versatilis.crm.model.EnvioWhatsApp;
import com.versatilis.crm.repositories.EnvioWhatsAppRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WhatsAppServiceTest {

    @Mock private EvolutionApiClient evolutionClient;
    @Mock private PdfService pdfService;
    @Mock private OrcamentoService orcamentoService;
    @Mock private EnvioWhatsAppRepository envioRepo;

    @InjectMocks private WhatsAppService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "defaultCountryCode", "55");
    }

    // ── Normalização de telefone ─────────────────────────────────────

    @ParameterizedTest
    @CsvSource({
        "'(47) 99999-8888', 5547999998888",
        "'+55 47 99999-8888', 5547999998888",
        "'47999998888',     5547999998888",
        "'5547999998888',   5547999998888",
        "'47 3333-4444',    554733334444",
        "'  47999998888 ',  5547999998888"
    })
    void normalizarTelefone_deveAplicarDDIQuandoFaltar(String entrada, String esperado) {
        assertThat(service.normalizarTelefone(entrada)).isEqualTo(esperado);
    }

    @Test
    void normalizarTelefone_deveLancarSeVazio() {
        assertThatThrownBy(() -> service.normalizarTelefone(""))
            .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.normalizarTelefone("abc!!"))
            .isInstanceOf(BadRequestException.class);
    }

    // ── Fluxo de envio ───────────────────────────────────────────────

    @Test
    void enviarOrcamento_comPdf_devePersistirEnvioEMarcarComoEnviado() {
        OrcamentoDTO orc = montarOrcamento();
        when(orcamentoService.buscarPorId(1L)).thenReturn(orc);
        when(pdfService.gerarPdf(orc)).thenReturn("PDF-BYTES".getBytes());
        when(evolutionClient.sendMedia(eq("5547999998888"), anyString(),
            eq("orcamento-ORC-0001.pdf"), eq("application/pdf"), anyString()))
            .thenReturn("evo-msg-123");
        when(envioRepo.save(any(EnvioWhatsApp.class)))
            .thenAnswer(inv -> {
                EnvioWhatsApp e = inv.getArgument(0);
                if (e.getId() == null) ReflectionTestUtils.setField(e, "id", 99L);
                return e;
            });

        WhatsAppEnvioDTO dto = new WhatsAppEnvioDTO();
        WhatsAppEnvioResponseDTO resp = service.enviarOrcamento(1L, dto);

        assertThat(resp.getMessageId()).isEqualTo("evo-msg-123");
        assertThat(resp.getStatus()).isEqualTo(EnvioWhatsApp.StatusEnvio.ENVIADO);
        assertThat(resp.getTelefone()).isEqualTo("5547999998888");
        verify(orcamentoService).marcarComoEnviado(1L);
        verify(envioRepo, times(2)).save(any(EnvioWhatsApp.class)); // PENDENTE → ENVIADO
    }

    @Test
    void enviarOrcamento_semPdf_deveUsarSendText() {
        OrcamentoDTO orc = montarOrcamento();
        when(orcamentoService.buscarPorId(1L)).thenReturn(orc);
        when(evolutionClient.sendText(eq("5547999998888"), anyString())).thenReturn("evo-2");
        when(envioRepo.save(any(EnvioWhatsApp.class))).thenAnswer(inv -> inv.getArgument(0));

        WhatsAppEnvioDTO dto = new WhatsAppEnvioDTO();
        dto.setAnexarPdf(false);

        WhatsAppEnvioResponseDTO resp = service.enviarOrcamento(1L, dto);

        assertThat(resp.getMessageId()).isEqualTo("evo-2");
        verify(evolutionClient).sendText(eq("5547999998888"), anyString());
        verify(evolutionClient, never()).sendMedia(any(), any(), any(), any(), any());
        verify(pdfService, never()).gerarPdf(any());
    }

    @Test
    void enviarOrcamento_falhaNaEvolution_deveMarcarFalhaELancar() {
        OrcamentoDTO orc = montarOrcamento();
        when(orcamentoService.buscarPorId(1L)).thenReturn(orc);
        when(pdfService.gerarPdf(orc)).thenReturn("PDF".getBytes());
        when(evolutionClient.sendMedia(any(), any(), any(), any(), any()))
            .thenThrow(new EvolutionApiClient.EvolutionApiException("instance not connected", 503, null));
        when(envioRepo.save(any(EnvioWhatsApp.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> service.enviarOrcamento(1L, new WhatsAppEnvioDTO()))
            .isInstanceOf(EvolutionApiClient.EvolutionApiException.class);

        // Salva PENDENTE no início e FALHA depois
        verify(envioRepo, times(2)).save(any(EnvioWhatsApp.class));
        verify(orcamentoService, never()).marcarComoEnviado(any());
    }

    @Test
    void enviarOrcamento_clienteSemTelefoneEDestinoNulo_deveLancar() {
        OrcamentoDTO orc = montarOrcamento();
        orc.setClienteTelefone(null);
        when(orcamentoService.buscarPorId(1L)).thenReturn(orc);

        assertThatThrownBy(() -> service.enviarOrcamento(1L, new WhatsAppEnvioDTO()))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("Telefone");
    }

    @Test
    void enviarOrcamento_destinoNoBodyTemPrioridadeSobreCliente() {
        OrcamentoDTO orc = montarOrcamento();
        orc.setClienteTelefone("47999990000"); // telefone do cliente
        when(orcamentoService.buscarPorId(1L)).thenReturn(orc);
        when(pdfService.gerarPdf(orc)).thenReturn(new byte[]{1, 2});
        when(evolutionClient.sendMedia(eq("5511988887777"), any(), any(), any(), any()))
            .thenReturn("ok");
        when(envioRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WhatsAppEnvioDTO dto = new WhatsAppEnvioDTO();
        dto.setDestinatario("11988887777"); // override pelo body

        service.enviarOrcamento(1L, dto);
        verify(evolutionClient).sendMedia(eq("5511988887777"), any(), any(), any(), any());
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private OrcamentoDTO montarOrcamento() {
        OrcamentoItemDTO item = new OrcamentoItemDTO();
        item.setDescricao("Consultoria em automação");
        item.setQuantidade(2);
        item.setValorUnitario(new BigDecimal("1500.00"));
        item.setValorTotal(new BigDecimal("3000.00"));

        OrcamentoDTO o = new OrcamentoDTO();
        o.setId(1L);
        o.setNumero("ORC-0001");
        o.setDataEmissao(LocalDate.of(2026, 4, 27));
        o.setDataValidade(LocalDate.of(2026, 5, 27));
        o.setSubtotal(new BigDecimal("3000.00"));
        o.setDesconto(BigDecimal.ZERO);
        o.setTotal(new BigDecimal("3000.00"));
        o.setClienteId(10L);
        o.setClienteNome("ACME Indústria");
        o.setClienteTelefone("(47) 99999-8888");
        o.setItens(List.of(item));
        return o;
    }
}
