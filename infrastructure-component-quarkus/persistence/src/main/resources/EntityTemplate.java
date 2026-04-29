import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;

@Entity
public class $className extends PanacheEntity {
    public $className() {}

    public void set$propertyName($propertyType $propertyName) {
        this.$propertyName = $propertyName;
    }

    public $propertyType get$propertyName() {
        return $propertyName;
    }
}
