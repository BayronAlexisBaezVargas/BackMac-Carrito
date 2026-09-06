package backmac.carrito.service;

import backmac.carrito.client.ProductoClient;
import backmac.carrito.dto.ItemCarritoDTO;
import backmac.carrito.dto.ProductoDTO;
import backmac.carrito.exception.BadRequestException;
import backmac.carrito.exception.ResourceNotFoundException;
import backmac.carrito.model.Carrito;
import backmac.carrito.model.ItemCarrito;
import backmac.carrito.repository.CarritoRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CarritoService {

    private final CarritoRepository carritoRepository;
    private final ProductoClient productoClient;

    public Carrito obtenerCarrito(String usuarioId) {
        return carritoRepository.findByUsuarioId(usuarioId)
                .orElseGet(() -> crearCarrito(usuarioId));
    }

    private Carrito crearCarrito(String usuarioId) {
        Carrito carrito = Carrito.builder()
                .usuarioId(usuarioId)
                .fechaCreacion(LocalDateTime.now())
                .fechaActualizacion(LocalDateTime.now())
                .build();
        return carritoRepository.save(carrito);
    }

    @Transactional
    public Carrito agregarItem(String usuarioId, ItemCarritoDTO itemDTO) {
        Carrito carrito = obtenerCarrito(usuarioId);

        ProductoDTO productoDTO;
        try {
            productoDTO = productoClient.obtenerProducto(itemDTO.getProductoId());
        } catch (FeignException.NotFound e) {
            throw new ResourceNotFoundException("El producto con ID " + itemDTO.getProductoId() + " no existe");
        } catch (FeignException e) {
            throw new RuntimeException("Error al comunicarse con el servicio de productos", e);
        }

        if (productoDTO.getStock() < itemDTO.getCantidad()) {
            throw new BadRequestException("No hay suficiente stock para el producto " + productoDTO.getNombre());
        }

        Optional<ItemCarrito> itemExistente = carrito.getItems().stream()
                .filter(i -> i.getProductoId().equals(itemDTO.getProductoId()))
                .findFirst();

        if (itemExistente.isPresent()) {
            ItemCarrito item = itemExistente.get();
            item.setCantidad(item.getCantidad() + itemDTO.getCantidad());
        } else {
            ItemCarrito nuevoItem = ItemCarrito.builder()
                    .productoId(productoDTO.getId())
                    .cantidad(itemDTO.getCantidad())
                    .precioUnitario(productoDTO.getPrecio())
                    .build();
            carrito.addItem(nuevoItem);
        }

        carrito.setFechaActualizacion(LocalDateTime.now());
        return carritoRepository.save(carrito);
    }

    @Transactional
    public Carrito actualizarCantidad(String usuarioId, UUID productoId, Integer nuevaCantidad) {
        Carrito carrito = obtenerCarrito(usuarioId);

        if (nuevaCantidad <= 0) {
            return removerItem(usuarioId, productoId);
        }

        ItemCarrito item = carrito.getItems().stream()
                .filter(i -> i.getProductoId().equals(productoId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("El producto no está en el carrito"));

        // Validar stock antes de actualizar
        ProductoDTO productoDTO;
        try {
            productoDTO = productoClient.obtenerProducto(productoId);
        } catch (FeignException e) {
            throw new RuntimeException("Error al comunicarse con el servicio de productos", e);
        }

        if (productoDTO.getStock() < nuevaCantidad) {
            throw new BadRequestException("No hay suficiente stock. Stock disponible: " + productoDTO.getStock());
        }

        item.setCantidad(nuevaCantidad);
        carrito.setFechaActualizacion(LocalDateTime.now());
        return carritoRepository.save(carrito);
    }

    @Transactional
    public Carrito removerItem(String usuarioId, UUID productoId) {
        Carrito carrito = obtenerCarrito(usuarioId);

        ItemCarrito itemARemover = carrito.getItems().stream()
                .filter(i -> i.getProductoId().equals(productoId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("El producto no está en el carrito"));

        carrito.removeItem(itemARemover);
        carrito.setFechaActualizacion(LocalDateTime.now());
        return carritoRepository.save(carrito);
    }

    @Transactional
    public void vaciarCarrito(String usuarioId) {
        Carrito carrito = obtenerCarrito(usuarioId);
        carrito.getItems().clear();
        carrito.setFechaActualizacion(LocalDateTime.now());
        carritoRepository.save(carrito);
    }
}
