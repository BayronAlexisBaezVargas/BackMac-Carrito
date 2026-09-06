package backmac.carrito.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "carritos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Carrito {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Optional: We can link the cart to a user ID from the Auth microservice
    private String usuarioId;

    @OneToMany(mappedBy = "carrito", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ItemCarrito> items = new ArrayList<>();

    private LocalDateTime fechaCreacion;
    
    private LocalDateTime fechaActualizacion;

    public void addItem(ItemCarrito item) {
        items.add(item);
        item.setCarrito(this);
    }

    public void removeItem(ItemCarrito item) {
        items.remove(item);
        item.setCarrito(null);
    }
}
