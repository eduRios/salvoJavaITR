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
	public CommandLineRunner initData(PlayerRepository playerRepository,GameRepository gameRepository,GamePlayerRepository gamePlayerRepository,ShipRepository shipRepository, SalvoRepository salvoRepository, ScoreRepository scoreRepository) {
		return (args) -> {
			Player p1 = new Player("jbauer@ctu.gov","123");
			Player p2 = new Player("c.obrian@ctu.gov","456");
			Player p3 = new Player("kim_bauer@gmail.com","789");
			Player p4 = new Player("davidp@gmail.com","abc");
			Player p5 = new Player("mdessler@ctu.gov","asd");

			Game game1 = new Game();
			Game game2 = new Game(this.timeDifference(game1));
			Game game3 = new Game(this.timeDifference(game2));

			GamePlayer gamePlayer1	=	new GamePlayer(p1,game1);
			GamePlayer gamePlayer2 = new GamePlayer(p2,game1);
			GamePlayer gamePlayer3 = new GamePlayer(p1,game2);
			GamePlayer gamePlayer4 = new GamePlayer(p5,game2);
			GamePlayer gamePlayer5 = new GamePlayer(p3,game3);
			GamePlayer gamePlayer6 = new GamePlayer(p4,game3);


			List<String> shipLocation1 = Arrays.asList("H1","H2","H3");
			List<String> shipLocation2 = Arrays.asList("A1","B1","C1");
			List<String> shipLocation3 = Arrays.asList("A4","A5","A6");
			List<String> shipLocation4 = Arrays.asList("H7","H8","H9");
			List<String> shipLocation5 = Arrays.asList("F4","F5","F6");

			List<String> salvoLocation1 = Arrays.asList("A4","C5","F6");
			List<String> salvoLocation2 = Arrays.asList("A1","B2","C3");
			List<String> salvoLocation3 = Arrays.asList("H7","B1");
			List<String> salvoLocation4 = Arrays.asList("H1","H3");

			Ship ship1 = new Ship("cruiser",shipLocation1,gamePlayer1);
			Ship ship2 = new Ship("Battleship ",shipLocation2,gamePlayer3);
			Ship ship3 = new Ship("Submarine",shipLocation3,gamePlayer2);
			Ship ship4 = new Ship("Destroyer",shipLocation4,gamePlayer1);
			Ship ship5 = new Ship("Patrol Boat",shipLocation5,gamePlayer4);

			Salvo salvo1 = new Salvo(gamePlayer1,1,salvoLocation1);
			Salvo salvo2 = new Salvo(gamePlayer2,1,salvoLocation2);
			Salvo salvo3 = new Salvo(gamePlayer1,2,salvoLocation3);
			Salvo salvo4 = new Salvo(gamePlayer2,2,salvoLocation4);

			Date finishDate = new Date();



			Score score1 = new Score(p1,game1,1,finishDate);
			Score score2 = new Score(p2,game1,(float)0.5,finishDate);
			Score score3 = new Score(p1,game1,0,finishDate);
			Score score4 = new Score(p2,game1,(float)0.5,finishDate);


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
			gamePlayerRepository.save(gamePlayer5);
			gamePlayerRepository.save(gamePlayer6);

			shipRepository.save(ship1);
			shipRepository.save(ship2);
			shipRepository.save(ship3);
			shipRepository.save(ship4);
			shipRepository.save(ship5);

			salvoRepository.save(salvo1);
			salvoRepository.save(salvo2);
			salvoRepository.save(salvo3);
			salvoRepository.save(salvo4);

			scoreRepository.save(score1);
			scoreRepository.save(score2);
			scoreRepository.save(score3);
			scoreRepository.save(score4);

		};
	}

	private Date timeDifference(Game _game){
		Date newDate = Date.from(_game.getCreationDate().toInstant().plusSeconds(3600));
		return newDate;
	}
}
