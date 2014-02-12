{
	initial: "main",
	
	data: <board xmlns="http://www.livegameengine.com/schemas/games/tictactoe.xsd" turn="X">
		<row><col/><col/><col/></row>
		<row><col/><col/><col/></row>
		<row><col/><col/><col/></row>
	</board>,
	
	states: {
		"initializing": {
			transitions: {
				"": function(event) { return "waitingForPlayers"; }
			}
		},
		
		"waitingForPlayers": {
			transitions: {
				"game.playerJoin": function(event) {
					if(game.players.size() == 0) {
						this.game.playerJoin({role: "X"});
					}
					else if(game.players.size() == 1) {
						this.game.playerJoin({role: "O"});
					}
					else {
						this.game.error({message: "Tic Tac Toe can only have up to 2 players."});
						return null;
					}
				},
				"game.startGame": function(event) {
					if(game.players.size() != 2) {
						this.game.error({message: "Tic Tac Toe must have exactly 2 players before starting."});
					}
					else {
						return "play";
					}
				}
			}
		},
		
		"play": {
			transitions: {
				"board.click": function(event) {
					var currentPlayer = game.findPlayerByRole(this.data.board.@turn));
					
					if(currentPlayer.userid != event.data.gamens::player) {
						game.error
					}
				}
			}
		}
	}
}