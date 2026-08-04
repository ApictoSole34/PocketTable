package com.pockettable.server.model;

import com.pockettable.server.model.base.BaseEntity;
import com.pockettable.server.model.enums.RoomStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

}