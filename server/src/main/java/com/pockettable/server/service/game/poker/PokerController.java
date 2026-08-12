package com.pockettable.server.service.game.poker;

import com.pockettable.server.model.Room;
import com.pockettable.server.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rooms/{roomCode}/poker")
@RequiredArgsConstructor
public class PokerController {

    private final RoomService roomService;
    private final PokerGameService pokerGameService;
    private final PokerActionService pokerActionService;
    private final PokerGameManager pokerGameManager;

    @PostMapping("/start")
    public PokerGame startGame(
            @PathVariable String roomCode
    ) {

        Room room = roomService.getRoomByCode(roomCode);

        return pokerGameService.startGame(room);
    }

    @GetMapping("/state")
    public PokerGame getState(
            @PathVariable String roomCode
    ) {
        return pokerGameManager.getGame(roomCode);
    }

    @PostMapping("/action")
    public PokerGame performAction(
            @PathVariable String roomCode,
            @RequestBody PokerActionRequest request
    ) {

        PokerGame game = pokerGameManager.getGame(roomCode);

        pokerActionService.performAction(
                game,
                request.playerId(),
                request.action(),
                request.amount()
        );

        return game;
    }

    @PostMapping("/next-hand")
    public PokerGame startNextHand(
            @PathVariable String roomCode
    ) {

        PokerGame game = pokerGameManager.getGame(roomCode);

        pokerActionService.startNextHand(game);

        return game;
    }
}
