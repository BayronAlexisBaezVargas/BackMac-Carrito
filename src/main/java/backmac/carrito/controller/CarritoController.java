package backmac.carrito.controller;

import backmac.carrito.dto.ItemCarritoDTO;
import backmac.carrito.model.Carrito;
import backmac.carrito.service.CarritoService;
import backmac.carrito.util.JwtUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/carrito")
@RequiredArgsConstructor
public class CarritoController {

    private final CarritoService carritoService;

    @GetMapping
    public ResponseEntity<Carrito> obtenerCarrito(@RequestHeader("Authorization") String token) {
        String usuarioId = JwtUtils.extraerUsuarioId(token);
        return ResponseEntity.ok(carritoService.obtenerCarrito(usuarioId));
    }

    @PostMapping("/items")
    public ResponseEntity<Carrito> agregarItem(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody ItemCarritoDTO itemDTO) {
        String usuarioId = JwtUtils.extraerUsuarioId(token);
        return ResponseEntity.ok(carritoService.agregarItem(usuarioId, itemDTO));
    }

    @PutMapping("/items/{productoId}")
    public ResponseEntity<Carrito> actualizarCantidad(
            @RequestHeader("Authorization") String token,
            @PathVariable UUID productoId,
            @RequestParam Integer cantidad) {
        String usuarioId = JwtUtils.extraerUsuarioId(token);
        return ResponseEntity.ok(carritoService.actualizarCantidad(usuarioId, productoId, cantidad));
    }

    @DeleteMapping("/items/{productoId}")
    public ResponseEntity<Carrito> removerItem(
            @RequestHeader("Authorization") String token,
            @PathVariable UUID productoId) {
        String usuarioId = JwtUtils.extraerUsuarioId(token);
        return ResponseEntity.ok(carritoService.removerItem(usuarioId, productoId));
    }

    @DeleteMapping
    public ResponseEntity<Void> vaciarCarrito(@RequestHeader("Authorization") String token) {
        String usuarioId = JwtUtils.extraerUsuarioId(token);
        carritoService.vaciarCarrito(usuarioId);
        return ResponseEntity.noContent().build();
    }
}
