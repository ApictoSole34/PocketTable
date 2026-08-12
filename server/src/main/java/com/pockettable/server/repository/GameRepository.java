package com.pockettable.server.repository;

import com.pockettable.server.model.Game;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GameRepository extends JpaRepository<Game, UUID> {

    Optional<Game> findByRoomId(UUID roomId);
}
