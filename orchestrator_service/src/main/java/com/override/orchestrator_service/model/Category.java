package com.override.orchestrator_service.model;

import com.override.orchestrator_service.constants.Type;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name="categories")
@RequiredArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Category {

    @Id
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @GeneratedValue(generator = "UUID")
    @Column(unique = true)
    private UUID id;

    @Column
    private String name;

    @Column
    private Type type;

    @OneToMany(mappedBy="category")
    private Set<Transaction> transactions;

    @OneToMany(mappedBy="category")
    private Set<Keyword> keywords;

    @ManyToOne
    private OverMoneyAccount account;

    public Category(String name, Type type, OverMoneyAccount account) {
        this.name = name;
        this.type = type;
        this.account = account;
    }
}
