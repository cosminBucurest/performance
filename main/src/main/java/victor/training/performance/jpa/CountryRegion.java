package victor.training.performance.jpa;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
@Data
public class CountryRegion {
    @Id
    @GeneratedValue
    private Long id;
    private String name;
}
