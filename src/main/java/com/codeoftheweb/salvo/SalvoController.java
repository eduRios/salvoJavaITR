package com.codeoftheweb.salvo;

import org.apache.tomcat.util.digester.ArrayStack;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

//Un controlador en Spring es una clase con métodos para ejecutarse cuando se reciben solicitudes con patrones de URL específico
//facilita la definición de un servicio web que devuelve recursos JSON personalizados a un cliente en lugar de HTML
@RestController
@RequestMapping("/api")
public class SalvoController {

    @Autowired
    private GameRepository gameRepository;
    @Autowired
    private GamePlayerRepository gamePlayerRepository;
    @Autowired
    private PlayerRepository playerRepository;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    private ShipRepository shipRepository;
    @Autowired
    private SalvoRepository salvoRepository;
    @Autowired
    private  ScoreRepository scoreRepository;

    //------------Devuelve el jugador actual (si está conectado) y todos los juegos------------
    @RequestMapping("/games")
    public Map<String, Object> makeLogedPlayer(Authentication authentication) {
        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        authentication = SecurityContextHolder.getContext().getAuthentication();
        Player authenticationPlayer = getAuthentication(authentication);

        if (authenticationPlayer == null)
            dto.put("player", "Guest");
        else
            dto.put("player", makePlayerDTO(authenticationPlayer));
        dto.put("games", getGames());
        return dto;
    }

    //-------------------------------Devuelve game_view de TODOS los jugadores---------------------------------
    @RequestMapping("/game_view")
    public List<Object> getGameView() {
        return gamePlayerRepository
                .findAll()
                .stream()
                .map(game -> gamePlayerViewDTO(game)).collect(toList());
    }

    public List<Object> getGames() {
        return gameRepository
                .findAll()
                .stream()
                .map(game -> gameViewDTO(game))
                .collect(toList());
    }

    private Map<String, Object> gameViewDTO(Game game) {
        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        dto.put("id", game.getId());
        dto.put("creationDate", game.getCreationDate());
        dto.put("gamePlayers", getGamePlayerList(game.getGamePlayers()));
        dto.put("scores", getScoreList(game.getScores()));

        return dto;
    }

    private Map<String, Object> gamePlayerViewDTO(GamePlayer gamePlayer) {
        //GamePlayer opponent = GetOpponent(gamePlayer.getGame(),gamePlayer).orElse(null);
        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        dto.put("id", gamePlayer.getId());
        dto.put("creationDate", gamePlayer.getCreationDate());
        dto.put("gameState",getGameState(gamePlayer));
        dto.put("gamePlayers", getGamePlayerList(gamePlayer.getGame().getGamePlayers()));

        if (gamePlayer.getShips().isEmpty()) {
            dto.put("ships", new ArrayList<>());
        }else{
            dto.put("ships", getShipsList(gamePlayer.getShips()));
        }
        if(conseguirOponente(gamePlayer) == null || gamePlayer.getSalvoes().isEmpty()) {
            dto.put("salvoes", new ArrayList<>());
        }else {
            dto.put("salvoes", getSalvoList(gamePlayer.getGame()));
        }
        if (conseguirOponente(gamePlayer).getShips().isEmpty() || conseguirOponente(gamePlayer).getSalvoes().isEmpty()) {
            dto.put("hits", EmptyHits());
        }else {
            dto.put("hits",makeHitsDTO(gamePlayer,conseguirOponente(gamePlayer)));
       }
        return dto;
    }

    private Map<String,Object> EmptyHits(){
        Map<String,Object> dto = new LinkedHashMap<>();
        dto.put("self", new ArrayList<>());
        dto.put("opponent", new ArrayList<>());
        return dto;
    }

    //------------------------------Devuelve game_view de los jugadores del jugador registrado------------------------
    @RequestMapping("/game_view/{id}")
    public ResponseEntity<Map<String, Object>> getGameView(@PathVariable Long id, Authentication authentication) {
        authentication = SecurityContextHolder.getContext().getAuthentication();
        GamePlayer gamePlayer = gamePlayerRepository.findById(id).get();
        Player player = gamePlayer.getPlayer();
        Player authenticationPlayer = getAuthentication(authentication);

        if(authenticationPlayer.getId() == player.getId()){
            return new ResponseEntity<>(gamePlayerViewDTO(gamePlayerRepository.findById(id).get()), HttpStatus.ACCEPTED);}
        else{
            return new ResponseEntity<>(makeMap("error", "Usuario no autorizado"), HttpStatus.UNAUTHORIZED);
        }
    }

    //-------------------------------------Scores-------------------------------------------------
    @RequestMapping("/leaderBoard")
    public List<Object> getScores() {
        return getPlayerList();
    }

    //Utilizado anteriormente por leaderboard Crear lista de distintos players
    private List<Object> getPlayerList(){
        return playerRepository
                .findAll()
                .stream()
                .map(player -> makePlayerDTO(player)).collect(toList());
    }

    private Map<String, Object> makePlayerDTO(Player player) {
        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        dto.put("id", player.getId());
        dto.put("email", player.getUserName());
        //dto.put("score", player.getScore(player)); //makeScoreDTO(player)
        return dto;
    }

    public Map<String, Object> makeScoreDTO(Player player){
        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        dto.put("name", player.getUserName());
        dto.put("total", player.getScore(player));
        dto.put("won", player.getWins(player.getScores()));
        dto.put("lost", player.getLoses(player.getScores()));
        dto.put("tied", player.getDraws(player.getScores()));

        return dto;
    }

    //-------------------------------List y DTO de Ships--------------------------------------
    public List<Object> getShipsList(Set<Ship> ships) {
        return ships
                .stream()
                .map(ship -> makeShipDTO(ship))
                .collect(Collectors.toList());
    }

    private Map<String, Object> makeShipDTO(Ship ship) {

        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        dto.put("type", ship.getType());
        dto.put("locations", ship.getLocations());

        return dto;
    }

    //-------------------------------------List y DTO de gamePlayers---------------------------------------
    private List<Map<String, Object>> getGamePlayerList(Set<GamePlayer> gamePlayers) {
        return gamePlayers
                .stream()
                .map(gamePlayer -> makeGamePlayerDTO(gamePlayer))
                .collect(toList());
    }

    private Map<String, Object> makeGamePlayerDTO(GamePlayer gamePlayer) {

        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        dto.put("gpid", gamePlayer.getId());
        dto.put("player", makePlayerDTO(gamePlayer.getPlayer()));
        dto.put("joinDate", gamePlayer.getCreationDate().getTime());
        //dto.put("ships", getShipsList(gamePlayer.getShips()));
        //dto.put("salvoes",getSalvoList(gamePlayer.getGame()));

        return dto;
    }

    //-------------------------------------List y DTO de Salvos---------------------------------------
    private List<Map<String,Object>> getSalvoList(Game game){
        List<Map<String,Object>> myList = new ArrayList<>();
        game.getGamePlayers().forEach(gamePlayer -> myList.addAll(MakeSalvoList(gamePlayer.getSalvoes())));
        return myList;
    }

    private List<Map<String, Object>> MakeSalvoList(Set<Salvo> salvoes){
        return salvoes
                .stream()
                .map(salvo -> makeSalvoDTO(salvo))
                .collect(toList());
    }

    private Map<String, Object> makeSalvoDTO(Salvo salvo) {
        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        dto.put("turn", salvo.getTurn());
        dto.put("player", salvo.getGamePlayer().getPlayer().getId());
        dto.put("locations", salvo.getLocations());
        return dto;
    }

    private List<Map<String,Object>> getScoreList(Set<Score> scores){
        return scores
                .stream()
                .map(score -> ScoreDTO(score)).collect(toList());
    }

    public Map<String, Object> ScoreDTO(Score score){
        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        dto.put("playerID", score.getPlayer().getId());
        dto.put("score", score.getScore());
        dto.put("finishDate", score.getFinishDate());
        return dto;
    }

    //----------------------------REGISTRA UN PLAYER MEDIANTE CONDICIONES DE SEGURIDAD---------------------------
    @RequestMapping(path = "/players", method = RequestMethod.POST)
    public ResponseEntity<Object> register(
            @RequestParam String username, @RequestParam String password) {

        if (username.isEmpty() || password.isEmpty()) {
            return new ResponseEntity<>("Missing data", HttpStatus.FORBIDDEN);
        }

        if (playerRepository.findByUserName(username) !=  null) {
            return new ResponseEntity<>("Name already in use", HttpStatus.FORBIDDEN);
        }

        playerRepository.save(new Player(username, passwordEncoder.encode(password)));
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    //------------------------------------Obtiene jugador registrado--------------------------------------------------
    private Player getAuthentication(Authentication authentication) {
        if(authentication == null || authentication instanceof AnonymousAuthenticationToken){
            return null;
        } else {
            return (playerRepository.findByUserName(authentication.getName()));
        }
    }

    //------------------------------------Crea el juego solicitado----------------------------------------------------
    @RequestMapping(path = "/games", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> createJuego(Authentication authentication) {
        authentication = SecurityContextHolder.getContext().getAuthentication();
        Player authenticatedPlayer = getAuthentication(authentication);
        if (authenticatedPlayer == null) {
            return new ResponseEntity<>(makeMap("error","No esta Registrado"), HttpStatus.FORBIDDEN);
            //FORBIDDEN: si esta solicitud no está permitida, sin importar qué autorización
        } else {
            Date date = Date.from(java.time.ZonedDateTime.now().toInstant());
            Game auxGame = new Game(date);
            gameRepository.save(auxGame);

            GamePlayer auxGameP = new GamePlayer(authenticatedPlayer, auxGame);
            gamePlayerRepository.save(auxGameP);
            return new ResponseEntity<>(makeMap("gpid", auxGameP.getId()), HttpStatus.CREATED);//si se agregaron nuevos datos
        }
    }

    //------------------------------------Creates a map-------------------------------------------------
    private Map<String, Object> makeMap(String key, Object value) {
        Map<String, Object> map = new HashMap<>();
        map.put(key, value);
        return map;
    }

    //------------------------------------punto 2.3 modulo 5 Ingresar jugadores------------------------------------
    @RequestMapping(path = "/game/{nn}/players", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> joinGame(@PathVariable Long nn, Authentication authentication) {
        authentication = SecurityContextHolder.getContext().getAuthentication();
        Player authenticatedPlayer = getAuthentication(authentication);
        Game game = gameRepository.findById(nn).get();
        if (authenticatedPlayer == null) {
            return new ResponseEntity<>(makeMap("error", "Usuario No autorizado"), HttpStatus.UNAUTHORIZED);
        }
        //no es necesario por ahora
        if (game == null) {
            return new ResponseEntity<>(makeMap("error", "El Juego no Existe"), HttpStatus.FORBIDDEN);
        }
        //no es necesario por ahora
        List<Player> list = game.getPlayers();
        if (list.size() >= 2) {
            return new ResponseEntity<>(makeMap("error", "El juego excede en jugadores"), HttpStatus.FORBIDDEN);
        }

        GamePlayer auxGameP = new GamePlayer(authenticatedPlayer, game);
        gamePlayerRepository.save(auxGameP);
        return new ResponseEntity<>(makeMap("gpid", auxGameP.getId()), HttpStatus.CREATED);
    }

    //------------------------------------punto 3.1 modulo 5 Agregar Ships---------------------------------------
    @RequestMapping(path = "/games/players/{gamePlayerId}/ships", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> addShip(@PathVariable Long gamePlayerId, @RequestBody Set<Ship> ships, Authentication authentication) {
        authentication = SecurityContextHolder.getContext().getAuthentication();
        Player authenticatedPlayer = getAuthentication(authentication);

        GamePlayer gamePlayer = gamePlayerRepository.findById(gamePlayerId).orElse(null); //lo busca por id
        //orElse() devuelve el valor si está presente, de lo contrario devuelve otro.

        if (authenticatedPlayer == null) {
            return new ResponseEntity<>(makeMap("error", "NO player logen in"), HttpStatus.UNAUTHORIZED);
        } else if (gamePlayer == null) {

            return new ResponseEntity<>(makeMap("error", "No gamePlayerID given"), HttpStatus.UNAUTHORIZED);
        }

        if (wrongGamePlayer(gamePlayerId, gamePlayer, authenticatedPlayer)) {
            return new ResponseEntity<>(makeMap("error", "Wrong gamePlayer"), HttpStatus.UNAUTHORIZED);
        } else if (gamePlayer.getShips().isEmpty()) {
            ships.forEach(ship -> ship.setGamePlayer(gamePlayer));
            gamePlayer.setShips(ships);
            shipRepository.saveAll(ships);
            return new ResponseEntity<>(makeMap("ok", "Ships saved"), HttpStatus.CREATED);

        } else {
            return new ResponseEntity<>(makeMap("error", "Player already has ships"), HttpStatus.FORBIDDEN);
        }
    }

    //------------------------------------VERIFICA SI SON DISTINTOS GAMEPLAYERS------------------------------------
    private boolean wrongGamePlayer(long id, GamePlayer gamePlayer, Player player) {
        Boolean correctGP = gamePlayer.getPlayer().getId() != player.getId();
        return correctGP;
    }

    //------------------------------------AGREGA SALVOS DEPENDIENDO DE CONDICIONES DE SEGURIDAD------------------------
    @RequestMapping(path = "/games/players/{gamePlayerId}/salvoes", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> addSalvo(@PathVariable Long gamePlayerId, @RequestBody Salvo salvo, Authentication authentication) {
        authentication = SecurityContextHolder.getContext().getAuthentication();
        Player authenticatedPlayer = getAuthentication(authentication);
        GamePlayer gamePlayer = gamePlayerRepository.findById(gamePlayerId).orElse(null);

        if (authenticatedPlayer == null)
            return new ResponseEntity<>(makeMap("error", "no player logged in"), HttpStatus.UNAUTHORIZED);
        if (gamePlayer == null)
            return new ResponseEntity<>(makeMap("error", "no such gamePlayer"), HttpStatus.UNAUTHORIZED);

        if (wrongGamePlayer(gamePlayerId, gamePlayer, authenticatedPlayer)) {
            return new ResponseEntity<>(makeMap("error", "Wrong GamePlayer"), HttpStatus.UNAUTHORIZED);
        }
        if (equalTurn(gamePlayer, salvo)) {

            return new ResponseEntity<>(makeMap("error", "Player already has ships"), HttpStatus.FORBIDDEN);
        } else {
            //System.out.println("llega aqui");
            salvo.setTurn(gamePlayer.getSalvoes().size() + 1);
            salvo.setGamePlayer(gamePlayer);
            gamePlayer.addSalvo(salvo);//gamePlayer.getSalvoes().add(salvo);
            salvoRepository.save(salvo);
            return new ResponseEntity<>(makeMap("ok", "Salvoes saved"), HttpStatus.CREATED);
        }
    }

    public boolean equalTurn(GamePlayer gamePlayer, Salvo salvo) {
        return gamePlayer.getSalvoes().stream().anyMatch(salvo1 -> salvo1.getTurn() == salvo.getTurn());
    }

    //------------------------------------punto 5 modulo 5  DETECTAR HITS Y SKINS DE CADA JUEGO-----------------------
    public GamePlayer conseguirOponente(GamePlayer gamePlayer) {
        return gamePlayer.getGame().getGamePlayers().stream().filter(oponente -> oponente.getId() != gamePlayer.getId()).findAny().orElse(new GamePlayer());
    }

    public Map<String, Object> makeHitsDTO(GamePlayer self, GamePlayer opponent) {
        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        dto.put("self", getHits(self, opponent));
        dto.put("opponent", getHits(opponent, self));
        return dto;
    }

    private List<Map> getHits(GamePlayer self, GamePlayer opponent) {
        List<Map> dto = new ArrayList<>();
        int carrierDamage = 0;
        int destroyerDamage = 0;
        int patrolboatDamage = 0;
        int submarineDamage = 0;
        int battleshipDamage = 0;
        List<String> carrierLocations = new ArrayList<>();
        List<String> destroyerLocations = new ArrayList<>();
        List<String> submarineLocations = new ArrayList<>();
        List<String> patrolboatLocations = new ArrayList<>();
        List<String> battleshipLocations = new ArrayList<>();
        for (Ship ship : self.getShips()) {
            switch (ship.getType()) {
                case "carrier":
                    carrierLocations = ship.getLocations();
                    break;
                case "battleship":
                    battleshipLocations = ship.getLocations();
                    break;
                case "destroyer":
                    destroyerLocations = ship.getLocations();
                    break;
                case "submarine":
                    submarineLocations = ship.getLocations();
                    break;
                case "patrolboat":
                    patrolboatLocations = ship.getLocations();
                    break;
            }
        }

        List<Salvo> salvoOrden = opponent.getSalvoes().stream()
                .sorted(Comparator.comparingInt(Salvo::getTurn))
                .collect(toList());

        for (Salvo salvo : salvoOrden) {
            Integer carrierHitsInTurn = 0;
            Integer battleshipHitsInTurn = 0;
            Integer submarineHitsInTurn = 0;
            Integer destroyerHitsInTurn = 0;
            Integer patrolboatHitsInTurn = 0;
            Integer missedShots = salvo.getLocations().size();
            Map<String, Object> hitsMapPerTurn = new LinkedHashMap<>();
            Map<String, Object> damagesPerTurn = new LinkedHashMap<>();
            List<String> salvoLocationsList = new ArrayList<>();
            List<String> hitCellsList = new ArrayList<>();
            salvoLocationsList.addAll(salvo.getLocations());
            for (String salvoShot : salvoLocationsList) {
                if (carrierLocations.contains(salvoShot)) {
                    carrierDamage++;
                    carrierHitsInTurn++;
                    hitCellsList.add(salvoShot);
                    missedShots--;
                }
                if (battleshipLocations.contains(salvoShot)) {
                    battleshipDamage++;
                    battleshipHitsInTurn++;
                    hitCellsList.add(salvoShot);
                    missedShots--;
                }
                if (submarineLocations.contains(salvoShot)) {
                    submarineDamage++;
                    submarineHitsInTurn++;
                    hitCellsList.add(salvoShot);
                    missedShots--;
                }
                if (destroyerLocations.contains(salvoShot)) {
                    destroyerDamage++;
                    destroyerHitsInTurn++;
                    hitCellsList.add(salvoShot);
                    missedShots--;
                }
                if (patrolboatLocations.contains(salvoShot)) {
                    patrolboatDamage++;
                    patrolboatHitsInTurn++;
                    hitCellsList.add(salvoShot);
                    missedShots--;
                }
            }
            damagesPerTurn.put("carrierHits", carrierHitsInTurn);
            damagesPerTurn.put("battleshipHits", battleshipHitsInTurn);
            damagesPerTurn.put("submarineHits", submarineHitsInTurn);
            damagesPerTurn.put("destroyerHits", destroyerHitsInTurn);
            damagesPerTurn.put("patrolboatHits", patrolboatHitsInTurn);
            damagesPerTurn.put("carrier", carrierDamage);
            damagesPerTurn.put("battleship", battleshipDamage);
            damagesPerTurn.put("submarine", submarineDamage);
            damagesPerTurn.put("destroyer", destroyerDamage);
            damagesPerTurn.put("patrolboat", patrolboatDamage);
            hitsMapPerTurn.put("turn", salvo.getTurn());
            hitsMapPerTurn.put("hitLocations", hitCellsList);
            hitsMapPerTurn.put("damages", damagesPerTurn);
            hitsMapPerTurn.put("missed", missedShots);
            dto.add(hitsMapPerTurn);
        }
        return dto;
    }

    private String getGameState(GamePlayer selfGamePlayer) {
        GamePlayer opponentGamePlayer = conseguirOponente(selfGamePlayer);
        if (selfGamePlayer.getShips().size() == 0) {
            System.out.println("placeShips");
            return "PLACESHIPS";
        }
        if (opponentGamePlayer.getShips() == null) {
            System.out.println("WAITINGFOROPP");
            return "WAITINGFOROPP";
        }
        if (opponentGamePlayer.getShips().size() == 0) {
            //System.out.println("wait");
            return "WAIT";
        }

        if (opponentGamePlayer.getSalvoes().size() == selfGamePlayer.getSalvoes().size()) {
            Player selfPlayer = selfGamePlayer.getPlayer();
            Game game = selfGamePlayer.getGame();

            if (allPlayerShipsSunk(selfGamePlayer.getShips(), opponentGamePlayer.getSalvoes()) && allPlayerShipsSunk(opponentGamePlayer.getShips(), selfGamePlayer.getSalvoes())) {
                Score score = new Score(selfPlayer, game, 0.5f, new Date());
                if (!existScore(score, game)) {
                    scoreRepository.save(score);
                }
                return "TIE";
            }

            if (allPlayerShipsSunk(selfGamePlayer.getShips(), opponentGamePlayer.getSalvoes())) {
                Score score = new Score(selfPlayer, game, 0, new Date());
                if (!existScore(score, game)) {
                    scoreRepository.save(score);
                }
                return "LOST";
            }

            if (allPlayerShipsSunk(opponentGamePlayer.getShips(), selfGamePlayer.getSalvoes())) {
                Score score = new Score(selfPlayer, game, 1, new Date());
                if (!existScore(score, game)) {
                    scoreRepository.save(score);
                }
                return "WON";
            }
        }

        int turn = getCurrentTurn(selfGamePlayer, opponentGamePlayer);
        if (selfGamePlayer.getSalvoes().size() != turn) {
            return "PLAY";
        }
        return "WAIT";
    }

    private int getCurrentTurn(GamePlayer selfGamePlayer, GamePlayer opponentGamePlayer) {

        int total = selfGamePlayer.getSalvoes().size() + opponentGamePlayer.getSalvoes().size();
        //System.out.println("pasa aqui antes de play");
        //Si los turnos son pares
        if (total % 2 == 0) {
            return total / 2 + 1;
        }
        //si son impares
        return (int) (total / 2.0 + 0.5);
    }

    public boolean existScore(Score score, Game game) {
        return game.getScores().stream().anyMatch(score1 -> score1.equals(score));
    }

    //verificado

    private Boolean allPlayerShipsSunk(Set<Ship> selfShips,Set<Salvo> oppSalvoes ){
        Map<String,Object> damages = getDamages(selfShips, oppSalvoes);

        long selfSunkenShips = selfShips
                .stream()
                .filter(ship -> Long.parseLong(String.valueOf(damages.get(ship.getType()))) == ship.getLocations().size())
                .count();

        return selfSunkenShips == 5;
    }

    private Map<String,Object> getDamages(Set<Ship> selfShip,Set<Salvo> oppSalvoes) {
        Map<String, Object> dto = new LinkedHashMap<>();

        int carrierDamage = 0;
        int destroyerDamage = 0;
        int patrolboatDamage = 0;
        int submarineDamage = 0;
        int battleshipDamage = 0;
        List<String> carrierLocations = new ArrayList<>();
        List<String> destroyerLocations = new ArrayList<>();
        List<String> submarineLocations = new ArrayList<>();
        List<String> patrolboatLocations = new ArrayList<>();
        List<String> battleshipLocations = new ArrayList<>();

        for (Ship ship : selfShip) {
            switch (ship.getType()) {
                case "carrier":
                    carrierLocations = ship.getLocations();
                    break;
                case "battleship":
                    battleshipLocations = ship.getLocations();
                    break;
                case "destroyer":
                    destroyerLocations = ship.getLocations();
                    break;
                case "submarine":
                    submarineLocations = ship.getLocations();
                    break;
                case "patrolboat":
                    patrolboatLocations = ship.getLocations();
                    break;
            }
        }

        for (Salvo salvo : oppSalvoes) {
            List<String> salvoShot = new ArrayList<>();
            salvoShot.addAll(salvo.getLocations());

            for (String salvoLocation : salvoShot) {
                if (carrierLocations.contains(salvoLocation)) {
                    carrierDamage++;
                }
                if (battleshipLocations.contains(salvoLocation)) {
                    battleshipDamage++;
                }
                if (submarineLocations.contains(salvoLocation)) {
                    submarineDamage++;
                }
                if (destroyerLocations.contains(salvoLocation)) {
                    destroyerDamage++;
                }
                if (patrolboatLocations.contains(salvoLocation)) {
                    patrolboatDamage++;
                }
            }

        }
        dto.put("carrier", carrierDamage);
        dto.put("battleship", battleshipDamage);
        dto.put("submarine", submarineDamage);
        dto.put("destroyer", destroyerDamage);
        dto.put("patrolboat", patrolboatDamage);
        return dto;
    }
}