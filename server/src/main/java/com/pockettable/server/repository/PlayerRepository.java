package com.pockettable.server.repository;

import com.pockettable.server.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PlayerRepository extends JpaRepository<Player, UUID> {

}