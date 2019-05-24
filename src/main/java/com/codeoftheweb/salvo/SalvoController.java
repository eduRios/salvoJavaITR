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

    @RequestMapping("/games")
    public Map<String, Object> makeLogedPlayer(Authentication authentication) {
        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        authentication = SecurityContextHolder.getContext().getAuthentication();
        Player authenticationPlayer = getAuthentication(authentication);

        if (authenticationPlayer == null)
            dto.put("player", "Guest");
        else
            dto.put("player", makePlayerDTO(authenticationPlayer));
        dto.put("games" , getGames());
        return dto;
    }

    @RequestMapping("/game_view")
    public List<Object> getGameView() {
        return gamePlayerRepository
                .findAll()
                .stream()
                .map(game -> makeGamePlayerDTO(game)).collect(toList());
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

    @RequestMapping("/game_view/{id}")
    private Map<String, Object> getGames(@PathVariable Long id) {
        return  gamePlayerViewDTO(gamePlayerRepository.findById(id).get());
    }

    //players
    @RequestMapping("/leaderBoard")
    public List<Object> getScores() {

        return getPlayerList();
    }

    private Map<String, Object> gamePlayerViewDTO(GamePlayer gamePlayer) {
        GamePlayer opponent = GetOpponent(gamePlayer.getGame(),gamePlayer).orElse(null);
        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        dto.put("id", gamePlayer.getId());
        dto.put("creationDate", gamePlayer.getCreationDate().getTime());
        dto.put("gamePlayers", getGamePlayerList(gamePlayer.getGame().getGamePlayers()));
        dto.put("ships", getShipsList(gamePlayer.getShips()));
        dto.put("salvoes", getSalvoList(gamePlayer.getGame()));
        dto.put("hits",makeHitsDTO(gamePlayer,opponent));
        return dto;
    }

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

    private Map<String, Object> makePlayerDTO(Player player) {
        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        dto.put("id", player.getId());
        dto.put("email", player.getUserName());
        dto.put("score", player.getScore(player)); //makeScoreDTO(player)
        return dto;
    }

    private Map<String, Object> makeGamePlayerDTO(GamePlayer gamePlayer) {

        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        dto.put("gpid", gamePlayer.getId());
        dto.put("player", makePlayerDTO(gamePlayer.getPlayer()));
        //dto.put("ships", getShipsList(gamePlayer.getShips()));
        //dto.put("salvoes",getSalvoList(gamePlayer.getGame()));

        return dto;
    }
    private List<Object> getPlayerList(){
        return playerRepository
                .findAll()
                .stream()
                .map(player -> makePlayerDTO(player)).collect(toList());
    }

    private Map<String, Object> makeSalvoDTO(Salvo salvo) {
        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        dto.put("turn", salvo.getTurn());
        dto.put("player", salvo.getGamePlayer().getPlayer().getId());
        dto.put("locations", salvo.getLocations());
        return dto;
    }


    private List<Map<String, Object>> getGamePlayerList(Set<GamePlayer> gamePlayers){
        return gamePlayers
                .stream()
                .map(gamePlayer -> makeGamePlayerDTO(gamePlayer))
                .collect(toList());
    }

    private List<Map<String, Object>> MakeSalvoList(Set<Salvo> salvoes){
        return salvoes
                .stream()
                .map(salvo -> makeSalvoDTO(salvo))
                .collect(toList());
    }

    private List<Map<String,Object>> getSalvoList(Game game){
        List<Map<String,Object>> myList = new ArrayList<>();
        game.getGamePlayers().forEach(gamePlayer -> myList.addAll(MakeSalvoList(gamePlayer.getSalvoes())));
        return myList;
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

    public Map<String, Object> makeScoreDTO(Player player){
        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        dto.put("name", player.getUserName());
        dto.put("total", player.getScore(player));
        dto.put("won", player.getWins(player.getScores()));
        dto.put("lost", player.getLoses(player.getScores()));
        dto.put("tied", player.getDraws(player.getScores()));

        return dto;
    }

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

    private Player getAuthentication(Authentication authentication) {
        if(authentication == null || authentication instanceof AnonymousAuthenticationToken){
            return null;
        }
        else{
            return (playerRepository.findByUserName(authentication.getName()));
        }
    }

    //crea el juego solicitado
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

    private Map<String, Object> makeMap(String key, Object value) {
        Map<String, Object> map = new HashMap<>();
        map.put(key, value);
        return map;
    }

    //punto 2.3 modulo 5
    @RequestMapping(path = "/game/{nn}/players", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> joinGame(@PathVariable Long nn,Authentication authentication) {
        authentication = SecurityContextHolder.getContext().getAuthentication();
        Player authenticatedPlayer = getAuthentication(authentication);
        Game game = gameRepository.findById(nn).get();
        if(authenticatedPlayer == null){
            return new ResponseEntity<>(makeMap("error","Usuario No autorizado"), HttpStatus.UNAUTHORIZED);
        }
        //no es necesario por ahora
        if (game == null){
            return new ResponseEntity<>(makeMap("error", "El Juego no Existe"), HttpStatus.FORBIDDEN);
        }
        //no es necesario por ahora
        List<Player> list = game.getPlayers();
        if (list.size() >= 2){
            return new ResponseEntity<>(makeMap("error", "El juego excede en jugadores"), HttpStatus.FORBIDDEN);
        }

        GamePlayer auxGameP = new GamePlayer(authenticatedPlayer,game);
        gamePlayerRepository.save(auxGameP);
        return new ResponseEntity<>(makeMap("gpid", auxGameP.getId()), HttpStatus.CREATED);
    }

    //tarea 3.1 modulo 5
    @RequestMapping(path = "/games/players/{gamePlayerId}/ships", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> addShip(@PathVariable Long gamePlayerId,@RequestBody Set<Ship> ships, Authentication authentication) {
        authentication = SecurityContextHolder.getContext().getAuthentication();
        Player authenticatedPlayer = getAuthentication(authentication);

        GamePlayer gamePlayer = gamePlayerRepository.findById(gamePlayerId).orElse(null); //lo busca por id
        //orElse() devuelve el valor si está presente, de lo contrario devuelve otro.

        if(authenticatedPlayer == null){
            return new ResponseEntity<>(makeMap("error","NO player logen in"), HttpStatus.UNAUTHORIZED);
        } else if(gamePlayer == null) {

            return new ResponseEntity<>(makeMap("error", "No gamePlayerID given"), HttpStatus.UNAUTHORIZED);
        }

        if(wrongGamePlayer(gamePlayerId,gamePlayer,authenticatedPlayer)){
            return new ResponseEntity<>(makeMap("error", "Wrong gamePlayer"), HttpStatus.UNAUTHORIZED);
        }
        else if(gamePlayer.getShips().isEmpty()){
            ships.forEach(ship -> ship.setGamePlayer(gamePlayer));
            gamePlayer.setShips(ships);
            shipRepository.saveAll(ships);
            return new ResponseEntity<>(makeMap("ok", "Ships saved"), HttpStatus.CREATED);

        } else{
            return new ResponseEntity<>(makeMap("error", "Player already has ships"), HttpStatus.FORBIDDEN);
        }
    }

    private boolean wrongGamePlayer(long id,GamePlayer gamePlayer, Player player){
        Boolean correctGP = gamePlayer.getPlayer().getId() != player.getId();
        return correctGP;
    }

    @RequestMapping(path = "/games/players/{gamePlayerId}/salvoes", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> addSalvo(@PathVariable Long gamePlayerId,@RequestBody Salvo salvo, Authentication authentication) {
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
        if(equalTurn(gamePlayer,salvo)){

            return new ResponseEntity<>(makeMap("error", "Player already has ships"), HttpStatus.FORBIDDEN);
        }else{
        //System.out.println("llega aqui");
            gamePlayer.addSalvo(salvo);//gamePlayer.getSalvoes().add(salvo);
        salvoRepository.save(salvo);
        return new ResponseEntity<>(makeMap("ok", "Salvoes saved"), HttpStatus.CREATED);}

    }

    public boolean equalTurn(GamePlayer gamePlayer,Salvo salvo){
       return gamePlayer.getSalvoes().stream().anyMatch(salvo1 -> salvo1.getTurn()==salvo.getTurn());
    }


    //punto 5 modulo 5
/*
    public Map<String, Object> makeHitsDTO(GamePlayer self, GamePlayer opponent){
        int ban;
        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        dto.put("self", getHitsList(self.getSalvoes(),opponent.getShips()));
        dto.put("opponent", getHitsList(opponent.getSalvoes(),self.getShips()));

        return dto;
    }
    private List<Map<String, Object>> getHitsList(Set<Salvo> salvos,Set<Ship> ships){
        int dC =0, dB=0, dS=0,dD=0,dP=0;
        return salvos
                .stream()
                .map(salvo -> hitsDTO(salvo,ships,dC,dB,dS,dD,dP))
                .collect(toList());
    }
    public Map<String, Object> hitsDTO(Salvo salvo, Set<Ship> ships,int dc, int db,int ds, int dd, int dp){
        Map<String, Object> dto = new LinkedHashMap<String, Object>();

        dto.put("turn", salvo.getTurn());
        dto.put("hitLocations", salvo.getLocations());
        dto.put("damages", damageDTO(salvo,ships,dc,db,ds,dd,dp));
        //dto.put("missed",player);

        return dto;
    }

    public Map<String, Object> damageDTO(Salvo salvo,Set<Ship> ships,int dc, int db,int ds, int dd, int dp){
        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        dc =acumulador(verificarCoincidencias(salvo,ships,"cruiser"),dc);
        db = acumulador(verificarCoincidencias(salvo,ships,"battleship"),db);
        ds = acumulador(verificarCoincidencias(salvo,ships,"submarine"),ds);
        dd = acumulador(verificarCoincidencias(salvo,ships,"destroyer"),dd);
        dp = acumulador(verificarCoincidencias(salvo,ships,"patrolboat"),dp);
        dto.put("carrierHits", verificarCoincidencias(salvo,ships,"cruiser"));
        dto.put("battleshipHits", verificarCoincidencias(salvo,ships,"battleship"));
        dto.put("submarineHits", verificarCoincidencias(salvo,ships,"submarine"));
        dto.put("destroyerHits", verificarCoincidencias(salvo,ships,"destroyer"));
        dto.put("patrolboatHits", verificarCoincidencias(salvo,ships,"patrolboat"));
        dto.put("carrier",dc);
        dto.put("battleship",db);
        dto.put("submarine", ds);
        dto.put("destroyer", dd);
        dto.put("patrolboat", dp);
        return dto;
    }

    public int acumulador(int coincidencias, int dc){
        dc = coincidencias+dc;

        return dc;
    }



    public int verificarCoincidencias(Salvo salvo,Set<Ship> ships,String type){
        int pointCarrier =0, pointBattleship=0, pointSubmarine=0,pointDestroyer=0,pointPatrolboat=0;

        for (Ship ship:ships) {
            int points;
            points = salvo.getLocations().stream().filter(location-> ship.coincidenSalvo(location)).collect(Collectors.toList()).size();


            switch (ship.getType()){
                case("cruiser"): pointCarrier = pointCarrier+points;
                break;
                case("battleship"): pointBattleship =pointBattleship+ points;
                break;
                case ("submarine"): pointSubmarine = pointSubmarine + points;
                break;
                case("destroyer"): pointDestroyer = pointDestroyer+points;
                break;
                case("patrolboat"): pointPatrolboat =pointPatrolboat+ points;
                break;
            }
        }

        if(type =="cruiser"){
            return pointCarrier;
        }else if(type=="battleship"){
            return pointBattleship;
        } else if(type=="submarine"){
            return pointSubmarine;
        }else if(type=="destroyer"){
            return pointDestroyer;
        }else if(type=="patrolboat"){
            return pointPatrolboat;
        }

        return 0;

    }*/

    public GamePlayer conseguirOponente(GamePlayer gamePlayer){
        return gamePlayer.getGame().getGamePlayers().stream().filter(oponente -> oponente!=gamePlayer).findAny().get();
    }


    public Map<String, Object> makeHitsDTO(GamePlayer self, GamePlayer opponent){

        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        dto.put("self", getHits(self,opponent));
        dto.put("opponent", getHits(opponent,self));

        return dto;
    }

    public List<Map> getHits(GamePlayer self, GamePlayer opponent) {
        //List<Salvo> salvoOrden = opponent.getSalvoes().stream().sorted(.comparing(Salvo::getTurn)).collect(toList())
        List<Map> dto = new ArrayList<>();
        int damageCarrier =0, damageBattleship=0, damageSubmarine=0,damageDestroyer=0,damagePatrolboat=0;
        List<String> carrierLocations = new ArrayList<>();
        List<String> destroyerLocations = new ArrayList<>();
        List<String> submarineLocations = new ArrayList<>();
        List<String> patrolboatLocations = new ArrayList<>();
        List<String> battleshipLocations = new ArrayList<>();

        for (Ship ship:self.getShips()) {
            switch (ship.getType()){
            case("cruises") : carrierLocations = ship.getLocations();

            break;
                case("battleship"): battleshipLocations = ship.getLocations();
                break;
                case ("submarine"): submarineLocations= ship.getLocations();
                break;
                case("destroyer"): destroyerLocations = ship.getLocations();
                break;
                case("patrolboat"): patrolboatLocations = ship.getLocations();
                break;
            }
        }

        for (Salvo salvo : opponent.getSalvoes()) {
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
                    damageCarrier++;
                    carrierHitsInTurn++;
                    hitCellsList.add(salvoShot);
                    missedShots--;
                }
                if (battleshipLocations.contains(salvoShot)) {
                    System. out. println(salvoShot);
                    damageBattleship++;
                    battleshipHitsInTurn++;
                    hitCellsList.add(salvoShot);
                    missedShots--;
                }
                if (submarineLocations.contains(salvoShot)) {
                    damageSubmarine++;
                    submarineHitsInTurn++;
                    hitCellsList.add(salvoShot);
                    missedShots--;
                }
                if (destroyerLocations.contains(salvoShot)) {
                    damageDestroyer++;
                    destroyerHitsInTurn++;
                    hitCellsList.add(salvoShot);
                    missedShots--;
                }
                if (patrolboatLocations.contains(salvoShot)) {
                    damagePatrolboat++;
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
            damagesPerTurn.put("carrier", damageCarrier);
            damagesPerTurn.put("battleship", damageBattleship);
            damagesPerTurn.put("submarine", damageSubmarine);
            damagesPerTurn.put("destroyer", damageDestroyer);
            damagesPerTurn.put("patrolboat", damagePatrolboat);
            hitsMapPerTurn.put("turn", salvo.getTurn());
            hitsMapPerTurn.put("hitLocations", hitCellsList);
            hitsMapPerTurn.put("damages", damagesPerTurn);
            hitsMapPerTurn.put("missed", missedShots);
            dto.add(hitsMapPerTurn);
        }
        return dto;
    }

    private Optional<GamePlayer> GetOpponent(Game game,
                                             GamePlayer gamePlayer){
        return game.getGamePlayers()
                .stream()
                .filter(opponent -> gamePlayer.getId() != opponent.getId())
                .findFirst();
    }
}