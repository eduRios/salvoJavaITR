package com.codeoftheweb.salvo;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toList;

@Entity
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    private long id;

    private String userName;

    @OneToMany(mappedBy="player", fetch= FetchType.EAGER)
    Set<GamePlayer> gamePlayers;

    @OneToMany(mappedBy="player", fetch= FetchType.EAGER)
    List<Score> scores;


    public Player(){}

    public Player(String userName) {
        this.userName = userName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public long getId() {
        return id;
    }

    public Set<GamePlayer> getGamePlayers() {
        return gamePlayers;
    }

    public void addGameplayer(GamePlayer gamePlayer) {
        gamePlayer.setPlayer(this);
        gamePlayers.add(gamePlayer);
    }

    public List<Game> getGames() {
        return gamePlayers.stream().map(sub -> sub.getGame()).collect(toList());
    }

    public void setGamePlayers(Set<GamePlayer> gamePlayers) {
        this.gamePlayers = gamePlayers;
    }

    public List<Score> getScores() {
        return scores;
    }

    public void setScores(List<Score> scores) {
        this.scores = scores;
    }
/*

    punto 5.2
    public Score getScore(Game game) {
        return scores.stream().filter(score -> score.getGame().getId() == game.getId()).findAny().orElse(null);
    }
*/

    public float getScore(Player player) {
        return getWins(player.getScores())+ getDraws(player.getScores())*(float)0.5 + getLoses(player.getScores())*0;
    }

    public float getWins(List<Score> scores){
        return scores.stream().filter(score->score.getScore()==1).count();
    }

    public float getDraws(List<Score> scores){
        return scores.stream().filter(score->score.getScore()==0.5).count();
    }

    public float getLoses(List<Score> scores){
        return scores.stream().filter(score->score.getScore()==0).count();
    }
}
