package com.codeoftheweb.salvo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import static java.util.stream.Collectors.toList;

@Entity
public class Salvo {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    private long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "gamePlayer_id")
    private GamePlayer gamePlayer;

    private int turn;

    @ElementCollection
    @Column(name="locations")
    private List<String> locations;

    public Salvo(GamePlayer gamePlayer, int turn, List<String> locations) {
        this.gamePlayer = gamePlayer;
        this.turn = turn;
        this.locations = locations;
    }

    public Salvo(int turn, List<String> locations) {
        this.turn = turn;
        this.locations = locations;
    }

    public Salvo() {
    }

    public long getId() {
        return id;
    }


    @JsonIgnore
    public GamePlayer getGamePlayer() {
        return gamePlayer;
    }

    public void setGamePlayer(GamePlayer gamePlayer) {
        this.gamePlayer = gamePlayer;
    }

    public int getTurn() {
        return turn;
    }

    public void setTurn(int turn) {
        this.turn = turn;
    }

    public List<String> getLocations() {
        return locations;
    }

    public void setLocations(List<String> locations) {
        this.locations = locations;
    }
}
