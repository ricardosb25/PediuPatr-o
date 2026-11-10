package com.umc.pediupatrao.repository;

import java.time.Instant;
import com.umc.pediupatrao.dto.DashboardAgregado;
import com.umc.pediupatrao.entity.StatusPedido;

public interface DashboardRepository {
    DashboardAgregado consultar(Instant inicio, Instant fimExclusivo, StatusPedido status);
}
