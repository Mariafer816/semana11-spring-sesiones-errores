package Sistema.Reservas.Aulas.Controller;

import Sistema.Reservas.Aulas.Model.Reserva;
import Sistema.Reservas.Aulas.Service.ReservaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reservas")
@CrossOrigin(origins = "*")

public class ReservaController {

    private final ReservaService reservaService;

    public ReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @GetMapping
    public List<Reserva> listar() {
        return reservaService.listarTodas();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Reserva> buscar(@PathVariable Long id) {
        return reservaService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/fecha/{fecha}")
    public List<Reserva> buscarPorFecha(
            @PathVariable String fecha) {

        return reservaService.buscarPorFecha(
                LocalDate.parse(fecha)
        );
    }

    @GetMapping("/aula/{aulaId}")
    public List<Reserva> buscarPorAula(
            @PathVariable Long aulaId) {

        return reservaService.buscarPorAula(aulaId);
    }

    @PostMapping
    public ResponseEntity<?> crear(
            @Valid @RequestBody Reserva reserva) {

        try {
            return ResponseEntity.ok(
                    reservaService.guardar(reserva)
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity.badRequest()
                    .body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelar(@PathVariable Long id) {

        if (reservaService.buscarPorId(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        reservaService.eliminar(id);

        return ResponseEntity.noContent().build();
    }
}
