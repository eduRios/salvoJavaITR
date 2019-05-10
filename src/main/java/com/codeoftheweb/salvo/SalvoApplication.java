package com.codeoftheweb.salvo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

@SpringBootApplication
public class SalvoApplication {

	public static void main(String[] args) {
		SpringApplication.run(SalvoApplication.class, args);
	}

	@Bean
	public CommandLineRunner initData(PlayerRepository playerRepository,GameRepository gameRepository,GamePlayerRepository gamePlayerRepository,ShipRepository shipRepository) {
		return (args) -> {
			Player p1 = new Player("jbauer@ctu.gov");
			Player p2 = new Player("c.obrian@ctu.gov");
			Player p3 = new Player("kim_bauer@gmail.com");
			Player p4 = new Player("davidp@gmail.com");
			Player p5 = new Player("mdessler@ctu.gov");

			Game game1 = new Game();
			Game game2 = new Game(this.timeDifference(game1));
			Game game3 = new Game(this.timeDifference(game2));

			GamePlayer gamePlayer1	=	new GamePlayer(p1,game1);
			GamePlayer gamePlayer2 = new GamePlayer(p2,game2);
			GamePlayer gamePlayer3 = new GamePlayer(p1,game2);
			GamePlayer gamePlayer4 = new GamePlayer(p5,game2);

			List<String> shipLocation1 = Arrays.asList("H1","H2","H3");
			List<String> shipLocation2 = Arrays.asList("H4","H5","H6");

			Ship ship1 = new Ship("cruiser",shipLocation1,gamePlayer1);
			Ship ship2 = new Ship("Destructor",shipLocation2,gamePlayer3);


			// save a couple of customers

			playerRepository.save(p1);
			playerRepository.save(p2);
			playerRepository.save(p3);
			playerRepository.save(p4);
			playerRepository.save(p5);

			gameRepository.save(game1);
			gameRepository.save(game2);
			gameRepository.save(game3);


			gamePlayerRepository.save(gamePlayer1);
			gamePlayerRepository.save(gamePlayer2);
			gamePlayerRepository.save(gamePlayer3);
			gamePlayerRepository.save(gamePlayer4);

			shipRepository.save(ship1);
			shipRepository.save(ship2);







		};
	}

	private Date timeDifference(Game _game){
		Date newDate = Date.from(_game.getCreationDate().toInstant().plusSeconds(3600));
		return newDate;
	}
}
