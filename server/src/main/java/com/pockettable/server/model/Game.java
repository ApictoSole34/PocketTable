package com.pockettable.server.model;

import com.pockettable.server.model.base.BaseEntity;
import com.pockettable.server.model.enums.GameType;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "games")
public class Game extends BaseEntity {

    @OneToOne
    @JoinColumn(name = "room_id", nullable = false, unique = true)
    private Room room;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GameType gameType;
}
