package victor.training.performance.jpa;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class Child {
    @Id
    @GeneratedValue
    private Long id;

    private String name;

    private Child() {
    }

    public Child(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}

