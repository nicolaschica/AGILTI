package com.agilti.ticketservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agilti.ticketservice.config.AgiltiProperties;
import com.agilti.ticketservice.dto.TicketCreateRequest;
import com.agilti.ticketservice.dto.TicketResponse;
import com.agilti.ticketservice.exception.TicketNotFoundException;
import com.agilti.ticketservice.exception.TransicionInvalidaException;
import com.agilti.ticketservice.messaging.TicketEventProducer;
import com.agilti.ticketservice.model.Estado;
import com.agilti.ticketservice.model.Prioridad;
import com.agilti.ticketservice.model.Ticket;
import com.agilti.ticketservice.repository.TicketRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Prueba unitaria pura (sin levantar Spring ni base de datos): se
 * verifica la logica de negocio de TicketService usando un
 * TicketRepository simulado (mock).
 */
class TicketServiceTest {

    private TicketRepository ticketRepository;
    private TicketEventProducer ticketEventProducer;
    private TicketService ticketService;

    @BeforeEach
    void setUp() {
        ticketRepository = mock(TicketRepository.class);
        ticketEventProducer = mock(TicketEventProducer.class);
        AgiltiProperties properties = new AgiltiProperties();
        properties.setTecnicos(List.of("Carlos Perez", "Ana Gomez"));
        ticketService = new TicketService(ticketRepository, ticketEventProducer, properties);

        // save() no hace nada especial en el mock, solo se necesita que no falle
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void crearTicket_asignaTecnicosEnRoundRobin() {
        TicketCreateRequest req = new TicketCreateRequest();
        req.setTitulo("Ticket 1");
        req.setDescripcion("Descripcion 1");
        req.setPrioridad(Prioridad.ALTA);

        TicketResponse r1 = ticketService.crearTicket(req);
        TicketResponse r2 = ticketService.crearTicket(req);
        TicketResponse r3 = ticketService.crearTicket(req);

        // Con 2 tecnicos configurados, el 3er ticket debe volver a asignar al primero
        assertThat(r1.getTecnicoAsignado()).isEqualTo("Carlos Perez");
        assertThat(r2.getTecnicoAsignado()).isEqualTo("Ana Gomez");
        assertThat(r3.getTecnicoAsignado()).isEqualTo("Carlos Perez");
    }

    @Test
    void crearTicket_elTicketQuedaEnEstadoAbierto() {
        TicketCreateRequest req = new TicketCreateRequest();
        req.setTitulo("Ticket");
        req.setDescripcion("Descripcion");
        req.setPrioridad(Prioridad.MEDIA);

        TicketResponse response = ticketService.crearTicket(req);

        assertThat(response.getEstado()).isEqualTo(Estado.ABIERTO);
    }

    @Test
    void crearTicket_publicaEventoTicketCreadoUnaVez() {
        TicketCreateRequest req = new TicketCreateRequest();
        req.setTitulo("Ticket");
        req.setDescripcion("Descripcion");
        req.setPrioridad(Prioridad.ALTA);

        ticketService.crearTicket(req);

        verify(ticketEventProducer, times(1)).publicarTicketCreado(any());
    }

    @Test
    void obtenerTicket_lanzaExcepcionSiNoExiste() {
        UUID idInexistente = UUID.randomUUID();
        when(ticketRepository.findById(idInexistente)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.obtenerTicket(idInexistente))
                .isInstanceOf(TicketNotFoundException.class)
                .hasMessageContaining(idInexistente.toString());
    }

    @Test
    void actualizarEstado_permiteTransicionValida() {
        Ticket ticket = new Ticket("Titulo", "Desc", Prioridad.BAJA, "Carlos Perez");
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        TicketResponse response = ticketService.actualizarEstado(ticket.getId(), Estado.EN_ATENCION);

        assertThat(response.getEstado()).isEqualTo(Estado.EN_ATENCION);
    }

    @Test
    void actualizarEstado_rechazaTransicionInvalida() {
        Ticket ticket = new Ticket("Titulo", "Desc", Prioridad.BAJA, "Carlos Perez");
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        // ABIERTO -> CERRADO no es una transicion valida (falta pasar por EN_ATENCION)
        assertThatThrownBy(() -> ticketService.actualizarEstado(ticket.getId(), Estado.CERRADO))
                .isInstanceOf(TransicionInvalidaException.class);
    }
}
