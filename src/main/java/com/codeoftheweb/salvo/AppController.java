package com.codeoftheweb.salvo;

import org.springframework.beans.factory.annotation.Autowired;

public class AppController {
    @Autowired
    private PlayerRepository playerRepository;
    private GameRepository gameRepository;
    private GamePlayerRepository gamePlayerRepository;
    private ShipRepository shipRepository;

   // public AppController(PlayerRepository playerRepository) {
   //     this.playerRepository = playerRepository;
   // }
}
