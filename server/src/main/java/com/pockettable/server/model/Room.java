package com.pockettable.server.model;

import com.pockettable.server.model.base.BaseEntity;
import com.pockettable.server.model.enums.GameType;
import com.pockettable.server.model.enums.RoomStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "rooms")
public class Room extends BaseEntity {

    @Column(nullable = false, unique = true, length = 6)
    private String roomCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoomStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GameType gameType;

    @Column(nullable = false)
    private Integer maxPlayers;

    @OneToMany(mappedBy = "room")
    @Builder.Default
    private List<Player> players = new ArrayList<>();
}