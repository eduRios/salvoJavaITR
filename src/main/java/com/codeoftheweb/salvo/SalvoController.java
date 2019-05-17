package com.codeoftheweb.salvo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

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
        dto.put("creationDate", game.getCreationDate().getTime());
        dto.put("gamePlayers", getGamePlayerList(game.getGamePlayers()));
        dto.put("salvoes", getSalvoList(game));
        return dto;
    }

    @RequestMapping("/game_view/{id}") //dice que devo llamar a game_view cuando recibo get para url
    private Map<String, Object> getGames(@PathVariable Long id) {
        return  gamePlayerViewDTO(gamePlayerRepository.findById(id).get());
    }

    //players
    @RequestMapping("/leaderBoard")
    public List<Object> getScores() {

        return getPlayerList();
    }

    private Map<String, Object> gamePlayerViewDTO(GamePlayer gamePlayer) {
        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        dto.put("id", gamePlayer.getId());
        dto.put("creationDate", gamePlayer.getCreationDate().getTime());
        dto.put("gamePlayers", getGamePlayerList(gamePlayer.getGame().getGamePlayers()));
        dto.put("ships", gamePlayer.getShips());
        dto.put("salvoes", getSalvoList(gamePlayer.getGame()));
        return dto;
    }

    private Map<String, Object> makePlayerDTO(Player player) {
        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        dto.put("id", player.getId());
        dto.put("email", player.getUserName());
        dto.put("score", makeScoreDTO(player));
        return dto;
    }

    private Map<String, Object> makeGamePlayerDTO(GamePlayer gamePlayer) {
        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        dto.put("id", gamePlayer.getId());
        dto.put("player", makePlayerDTO(gamePlayer.getPlayer()));
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
        dto.put("turn", salvo.getId());
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
        dto.put("name", score.getPlayer().getUserName());
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
}