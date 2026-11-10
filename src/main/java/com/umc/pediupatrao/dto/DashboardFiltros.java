package com.umc.pediupatrao.dto;

import java.time.LocalDate;
import java.util.List;

public record DashboardFiltros(LocalDate inicioPadrao, LocalDate fimPadrao, String fusoHorario,
        int maximoDias, int atualizacaoSegundos, List<OpcaoStatus> status) {
    public record OpcaoStatus(String valor, String rotulo) {}
}
