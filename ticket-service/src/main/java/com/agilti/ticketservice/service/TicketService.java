package com.agilti.ticketservice.service;

import com.agilti.ticketservice.config.AgiltiProperties;
import com.agilti.ticketservice.dto.PageResponse;
import com.agilti.ticketservice.dto.TicketCreateRequest;
import com.agilti.ticketservice.dto.TicketResponse;
import com.agilti.ticketservice.exception.TicketNotFoundException;
import com.agilti.ticketservice.exception.TransicionInvalidaException;
import com.agilti.ticketservice.messaging.TicketCreadoEvent;
import com.agilti.ticketservice.messaging.TicketEventProducer;
import com.agilti.ticketservice.model.Estado;
import com.agilti.ticketservice.model.Prioridad;
import com.agilti.ticketservice.model.Ticket;
import com.agilti.ticketservice.repository.TicketRepository;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Logica de negocio detras de los 4 endpoints del contrato. Al crear un
 * ticket se publica ademas el evento TicketCreadoEvent en Kafka, que
 * consume notification-service.
 */
@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketEventProducer ticketEventProducer;
    private final List<String> tecnicosDisponibles;

    // Contador en memoria para el reparto round-robin. Valido para una
    // unica instancia del servicio (alcance de este ejercicio).
    private final AtomicInteger contadorAsignacion = new AtomicInteger(0);

    public TicketService(TicketRepository ticketRepository,
                         TicketEventProducer ticketEventProducer,
                         AgiltiProperties agiltiProperties) {
        this.ticketRepository = ticketRepository;
        this.ticketEventProducer = ticketEventProducer;
        this.tecnicosDisponibles = agiltiProperties.getTecnicos();
    }

    @Transactional
    public TicketResponse crearTicket(TicketCreateRequest request) {
        String tecnicoAsignado = asignarTecnicoAutomaticamente();

        Ticket ticket = new Ticket(
                NormalizadorTicket.normalizarTitulo(request.getTitulo()),
                NormalizadorTicket.normalizarDescripcion(request.getDescripcion()),
                request.getPrioridad(),
                tecnicoAsignado);

        Ticket ticketGuardado = ticketRepository.save(ticket);

        TicketCreadoEvent evento = new TicketCreadoEvent(
                ticketGuardado.getId(),
                ticketGuardado.getTitulo(),
                ticketGuardado.getPrioridad().name(),
                ticketGuardado.getTecnicoAsignado());
        ticketEventProducer.publicarTicketCreado(evento);

        return TicketResponse.from(ticketGuardado);
    }

    public PageResponse<TicketResponse> listarTickets(Estado estado, Prioridad prioridad, Pageable pageable) {
        Page<Ticket> pagina = ticketRepository.buscar(estado, prioridad, pageable);
        return PageResponse.from(pagina.map(TicketResponse::from));
    }

    public TicketResponse obtenerTicket(UUID id) {
        return TicketResponse.from(buscarOLanzar(id));
    }

    @Transactional
    public TicketResponse actualizarEstado(UUID id, Estado nuevoEstado) {
        Ticket ticket = buscarOLanzar(id);
        if (!ticket.getEstado().puedeTransicionarA(nuevoEstado)) {
            throw new TransicionInvalidaException(ticket.getEstado(), nuevoEstado);
        }
        ticket.setEstado(nuevoEstado);
        return TicketResponse.from(ticket);
    }

    private Ticket buscarOLanzar(UUID id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));
    }

    private String asignarTecnicoAutomaticamente() {
        if (tecnicosDisponibles.isEmpty()) {
            return null;
        }
        int indice = Math.floorMod(contadorAsignacion.getAndIncrement(), tecnicosDisponibles.size());
        return tecnicosDisponibles.get(indice);
    }
}
