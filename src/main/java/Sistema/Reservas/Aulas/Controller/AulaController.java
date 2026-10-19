package Sistema.Reservas.Aulas.Controller;

import Sistema.Reservas.Aulas.Model.Aula;
import Sistema.Reservas.Aulas.Service.AulaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/aulas")
public class AulaController {

    private final AulaService aulaService;

    public AulaController(AulaService aulaService) {
        this.aulaService = aulaService;
    }

    @GetMapping
    public List<Aula> obtenerTodas() {
        return aulaService.obtenerTodas();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Aula> obtenerPorId(@PathVariable Long id) {
        Aula aula = aulaService.obtenerPorId(id);
        if (aula != null) {
            return ResponseEntity.ok(aula);
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<Aula> crearAula(@Valid @RequestBody Aula aula) {
        Aula nuevaAula = aulaService.guardar(aula);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevaAula);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Aula> actualizarAula(@PathVariable Long id, @Valid @RequestBody Aula aulaDetalles) {
        Aula aulaExistente = aulaService.obtenerPorId(id);
        if (aulaExistente != null) {
            aulaExistente.setNombre(aulaDetalles.getNombre());
            aulaExistente.setCapacidad(aulaDetalles.getCapacidad());
            aulaExistente.setUbicacion(aulaDetalles.getUbicacion());

            Aula aulaActualizada = aulaService.guardar(aulaExistente);
            return ResponseEntity.ok(aulaActualizada);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarAula(@PathVariable Long id) {
        Aula aula = aulaService.obtenerPorId(id);
        if (aula != null) {
            aulaService.eliminar(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}