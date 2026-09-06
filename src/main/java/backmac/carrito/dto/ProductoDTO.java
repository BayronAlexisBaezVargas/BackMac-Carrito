package backmac.carrito.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class ProductoDTO {
    private UUID id;
    private String nombre;
    private String descripcion;
    private BigDecimal precio;
    private Integer stock;
}
