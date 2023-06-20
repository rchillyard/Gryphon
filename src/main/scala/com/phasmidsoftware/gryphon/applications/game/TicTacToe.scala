/*
 * Copyright (c) 2023. Phasmid Software
 */

package com.phasmidsoftware.gryphon.applications.game

import com.phasmidsoftware.gryphon.games.{Game, Positions}


case class TicTacToeSquare(squares: Seq[Option[Boolean]]) extends Positions[Option[Boolean]](squares) {
    def isComplete: Boolean = false

}

case class TicTacToePlayer()

case class TicTacToe(board: TicTacToeSquare) extends Game[Option[Boolean], TicTacToeSquare, TicTacToePlayer] {

    def players: Seq[TicTacToePlayer] = Seq(TicTacToePlayer())

    /**
     * If the result of invoking goal exists, then the game is over.
     * If there is a winner, then the optional value is Some(winner).
     * Otherwise, the game is a draw (stalemate) and the result is Some(None).
     *
     * @param board the game board.
     * @return an Option of Option[Player]
     */
    def goal(board: TicTacToeSquare): Option[Option[TicTacToePlayer]] =
        if (board.isComplete) Some(Some(players.head))
        else None
}
