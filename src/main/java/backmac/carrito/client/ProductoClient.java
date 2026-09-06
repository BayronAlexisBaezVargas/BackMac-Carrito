package backmac.carrito.client;

import backmac.carrito.dto.ProductoDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "productos-service", url = "${productos.service.url:http://localhost:8081/api/productos}")
public interface ProductoClient {
    @GetMapping("/{id}")
    ProductoDTO obtenerProducto(@PathVariable("id") UUID id);
}
