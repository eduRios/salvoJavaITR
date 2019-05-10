package com.codeoftheweb.salvo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;
// informacion util: https://mindhubweb.xtolcorp.com/ebooks/item?id=1168

@RestController
@RequestMapping("/api")
public class SalvoController {

    @Autowired
    private GameRepository repo;

/*
//punto 4)
    @RequestMapping("/games")
    public List<Long> getAll() {
        List<Game> juegos;
        List<Long> indice = new ArrayList<>();

        juegos = repo.findAll();

        for (Game juego:juegos) {
            indice.add(juego.getId());
        }
        return indice;
    }*/
/*
//punto 5)
@RequestMapping("/games")
// informacion para entender el punto el punto 5: https://mindhubweb.xtolcorp.com/ebooks/item?id=1169
  public List<Object> getAllGames() {
    return repo
            .findAll()
            .stream()
            .map(game -> makeOwnerDTO(game))
            .collect(Collectors.toList());
            }

  private Map<String, Object> makeOwnerDTO(Game game) {
    Map<String, Object> dto = new LinkedHashMap<String, Object>();
    dto.put("id", game.getId());
    dto.put("created", game.getCreationDate());
    return dto;
    }
*/

    @RequestMapping("/games")
// informacion para entender el punto el punto 5: https://mindhubweb.xtolcorp.com/ebooks/item?id=1169
    public List<Object> getAllGames() {
        return repo
                .findAll()
                .stream()
                .map(game -> makeGameDTO(game))
                .collect(Collectors.toList());
    }

    private Map<String, Object> makeGameDTO(Game game) {
        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        dto.put("id", game.getId());
        dto.put("creationDate", game.getCreationDate());
        dto.put("gamePlayers", game.getGamePlayers().stream().map(gamePlayer -> makeGamePlayerDTO(gamePlayer)).collect(Collectors.toList()));
        return dto;
    }

    private Map<String, Object> makeGamePlayerDTO(GamePlayer gp) {
        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        dto.put("id", gp.getId());
        dto.put("player", makePlayerDTO(gp.getPlayer()));
        return dto;
    }

    private Map<String, Object> makePlayerDTO(Player p) {
        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        dto.put("id", p.getId());
        dto.put("email", p.getUserName());
        return dto;
    }

}
